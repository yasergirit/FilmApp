package com.filmapp.presentation.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmapp.data.local.QuizManager
import com.filmapp.data.remote.gemini.GeminiService
import com.filmapp.data.repository.AuthRepositoryImpl
import com.filmapp.data.remote.supabase.SupabaseDataSource
import com.filmapp.data.remote.supabase.dto.DailyQuestionDto
import com.filmapp.data.remote.supabase.dto.UserScoreDto
import com.filmapp.domain.model.DailyQuiz
import com.filmapp.domain.model.QuizQuestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class QuizState(
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val correctCount: Int = 0,
    val answeredCount: Int = 0,
    val isCompleted: Boolean = false,
    val isLoading: Boolean = true,
    val weeklyScore: Int = 0,
    val streakCount: Int = 0,
    val completedDays: Set<Int> = emptySet(),
    val showStreakDialog: Boolean = false,
    val selectedAnswer: Int? = null,
    val isAnswerLocked: Boolean = false,
    val userAnswers: Map<Int, Int> = emptyMap(),
    val error: String? = null,
    val isDemoUser: Boolean = false
)

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val quizManager: QuizManager,
    private val geminiService: GeminiService,
    private val supabaseDataSource: SupabaseDataSource,
    private val authRepository: AuthRepositoryImpl
) : ViewModel() {

    private val _state = MutableStateFlow(QuizState())
    val state: StateFlow<QuizState> = _state.asStateFlow()

    init {
        _state.update { it.copy(isDemoUser = authRepository.isDemoSession) }
        loadQuiz()
    }

    private fun loadQuiz() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            android.util.Log.d("QuizVM", "loadQuiz started")

            // Get weekly score and streak
            quizManager.weeklyScore.first().let { score ->
                _state.update { it.copy(weeklyScore = score) }
            }
            quizManager.streakCount.first().let { streak ->
                _state.update { it.copy(streakCount = streak) }
            }
            quizManager.completedDays.first().let { days ->
                _state.update { it.copy(completedDays = days) }
            }

            // Check if today's quiz exists locally (already answered)
            val existingQuiz = quizManager.dailyQuiz.first()
            val savedAnswers = quizManager.userAnswers.first()

            if (existingQuiz != null && existingQuiz.questions.isNotEmpty()) {
                android.util.Log.d("QuizVM", "Loaded from LOCAL cache, ${existingQuiz.questions.size} questions")
                _state.update {
                    it.copy(
                        questions = existingQuiz.questions,
                        answeredCount = existingQuiz.answeredCount,
                        correctCount = existingQuiz.correctCount,
                        isCompleted = existingQuiz.isCompleted,
                        currentIndex = if (existingQuiz.isCompleted) 0 else existingQuiz.answeredCount.coerceAtMost(existingQuiz.questions.size - 1),
                        isLoading = false,
                        userAnswers = savedAnswers
                    )
                }
                return@launch
            }

            // Try fetching today's questions from Supabase
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            android.util.Log.d("QuizVM", "No local cache, checking Supabase for date: $today")
            try {
                val dbQuestions = supabaseDataSource.getTodayQuestions(today)
                if (dbQuestions.isNotEmpty()) {
                    val questions = dbQuestions.map { it.toQuizQuestion().shuffled() }
                    quizManager.saveDailyQuestions(questions)
                    _state.update {
                        it.copy(
                            questions = questions,
                            currentIndex = 0,
                            answeredCount = 0,
                            correctCount = 0,
                            isCompleted = false,
                            isLoading = false,
                            userAnswers = emptyMap()
                        )
                    }
                    return@launch
                }
            } catch (e: Exception) {
                android.util.Log.e("QuizVM", "Supabase fetch failed: ${e.message}")
            }

            // If no questions in DB, generate via Gemini and save to Supabase
            android.util.Log.d("QuizVM", "No Supabase data, generating via Gemini...")
            val questions = geminiService.generateQuizQuestions()
            if (questions.isNotEmpty()) {
                quizManager.saveDailyQuestions(questions)
                // Save to Supabase so all users get the same questions
                try {
                    val dtos = questions.map { q ->
                        val rightLetter = when (q.correctAnswerIndex) {
                            0 -> "A"; 1 -> "B"; 2 -> "C"; else -> "D"
                        }
                        DailyQuestionDto(
                            createdDate = today,
                            question = q.question,
                            optionA = q.options.getOrElse(0) { "" },
                            optionB = q.options.getOrElse(1) { "" },
                            optionC = q.options.getOrElse(2) { "" },
                            optionD = q.options.getOrElse(3) { "" },
                            rightAnswer = rightLetter
                        )
                    }
                    android.util.Log.d("QuizVM", "Inserting ${dtos.size} questions to Supabase for date: $today")
                    supabaseDataSource.insertDailyQuestions(dtos)
                    android.util.Log.d("QuizVM", "Supabase insert SUCCESS")
                } catch (e: Exception) {
                    android.util.Log.e("QuizVM", "Supabase insert failed: ${e.message}", e)
                }
                _state.update {
                    it.copy(
                        questions = questions,
                        currentIndex = 0,
                        answeredCount = 0,
                        correctCount = 0,
                        isCompleted = false,
                        isLoading = false,
                        userAnswers = emptyMap()
                    )
                }
            } else {
                _state.update {
                    it.copy(isLoading = false, error = "Sorular yüklenemedi. Lütfen tekrar dene.")
                }
            }
        }
    }

    private fun DailyQuestionDto.toQuizQuestion(): QuizQuestion {
        val options = listOf(optionA, optionB, optionC, optionD)
        val correctIndex = when (rightAnswer.uppercase()) {
            "A" -> 0; "B" -> 1; "C" -> 2; "D" -> 3; else -> 0
        }
        return QuizQuestion(
            question = question,
            options = options,
            correctAnswerIndex = correctIndex
        )
    }

    fun selectAnswer(questionIndex: Int, answerIndex: Int) {
        val currentState = _state.value
        if (currentState.isAnswerLocked || currentState.isCompleted) return
        if (currentState.userAnswers.containsKey(questionIndex)) return

        val question = currentState.questions.getOrNull(questionIndex) ?: return
        val isCorrect = answerIndex == question.correctAnswerIndex

        _state.update {
            it.copy(
                selectedAnswer = answerIndex,
                isAnswerLocked = true
            )
        }

        viewModelScope.launch {
            quizManager.saveAnswer(questionIndex, answerIndex, isCorrect)

            val newAnswered = currentState.answeredCount + 1
            val newCorrect = currentState.correctCount + if (isCorrect) 1 else 0
            val completed = newAnswered >= currentState.questions.size

            // Refresh weekly score and streak if completed
            val weeklyScore: Int
            val streakCount: Int
            val completedDays: Set<Int>
            if (completed) {
                weeklyScore = quizManager.weeklyScore.first()
                streakCount = quizManager.streakCount.first()
                completedDays = quizManager.completedDays.first()
                syncScoreToSupabase(weeklyScore)
            } else {
                weeklyScore = currentState.weeklyScore
                streakCount = currentState.streakCount
                completedDays = currentState.completedDays
            }

            _state.update {
                it.copy(
                    answeredCount = newAnswered,
                    correctCount = newCorrect,
                    isCompleted = completed,
                    weeklyScore = weeklyScore,
                    streakCount = streakCount,
                    completedDays = completedDays,
                    showStreakDialog = completed,
                    userAnswers = it.userAnswers + (questionIndex to answerIndex)
                )
            }
        }
    }

    fun nextQuestion() {
        val currentState = _state.value
        if (currentState.currentIndex < currentState.questions.size - 1) {
            _state.update {
                it.copy(
                    currentIndex = it.currentIndex + 1,
                    selectedAnswer = null,
                    isAnswerLocked = false
                )
            }
        }
    }

    private fun syncScoreToSupabase(score: Int) {
        viewModelScope.launch {
            try {
                val userId = supabaseDataSource.getCurrentUserId() ?: return@launch
                val profile = supabaseDataSource.getProfile(userId)
                
                val userScoreDto = UserScoreDto(
                    userId = userId,
                    displayName = profile.displayName ?: "Anonim",
                    weeklyScore = score
                )
                supabaseDataSource.updateUserScore(userScoreDto)
            } catch (e: Exception) {
                // Sadece loglayabiliriz, hata durumunda kullanıcıyı rahatsız etmeyelim
                e.printStackTrace()
            }
        }
    }

    fun retryLoad() {
        loadQuiz()
    }

    fun dismissStreakDialog() {
        _state.update { it.copy(showStreakDialog = false) }
    }

    fun resetQuiz() {
        viewModelScope.launch {
            quizManager.resetDailyQuiz(resetWeeklyScore = true)
            _state.update {
                QuizState(
                    isLoading = true,
                    isDemoUser = authRepository.isDemoSession,
                    weeklyScore = 0
                )
            }
            loadQuiz()
        }
    }
}
