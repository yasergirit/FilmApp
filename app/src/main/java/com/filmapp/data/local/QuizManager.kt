package com.filmapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.filmapp.domain.model.DailyQuiz
import com.filmapp.domain.model.QuizQuestion
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

private val Context.quizDataStore: DataStore<Preferences> by preferencesDataStore(name = "quiz_data")

@Singleton
class QuizManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    companion object {
        private val KEY_QUIZ_DATE = stringPreferencesKey("quiz_date")
        private val KEY_QUIZ_QUESTIONS = stringPreferencesKey("quiz_questions")
        private val KEY_ANSWERED_COUNT = intPreferencesKey("answered_count")
        private val KEY_CORRECT_COUNT = intPreferencesKey("correct_count")
        private val KEY_IS_COMPLETED = booleanPreferencesKey("is_completed")
        private val KEY_WEEKLY_SCORE = intPreferencesKey("weekly_score")
        private val KEY_WEEK_START = stringPreferencesKey("week_start")
        private val KEY_ANSWERS = stringPreferencesKey("user_answers")
        private val KEY_STREAK_COUNT = intPreferencesKey("streak_count")
        private val KEY_LAST_QUIZ_DATE = stringPreferencesKey("last_quiz_date")
        private val KEY_COMPLETED_DAYS = stringPreferencesKey("completed_days")
    }

    private fun getCurrentWeekStart(): String {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.format(dateFormatter)
    }

    val dailyQuiz: Flow<DailyQuiz?> = context.quizDataStore.data.map { prefs ->
        val savedDate = prefs[KEY_QUIZ_DATE] ?: return@map null
        val today = LocalDate.now().format(dateFormatter)
        if (savedDate != today) return@map null

        val questionsJson = prefs[KEY_QUIZ_QUESTIONS] ?: return@map null
        val type = object : TypeToken<List<QuizQuestion>>() {}.type
        val questions: List<QuizQuestion> = gson.fromJson(questionsJson, type)

        DailyQuiz(
            date = savedDate,
            questions = questions,
            answeredCount = prefs[KEY_ANSWERED_COUNT] ?: 0,
            correctCount = prefs[KEY_CORRECT_COUNT] ?: 0,
            isCompleted = prefs[KEY_IS_COMPLETED] ?: false
        )
    }

    val weeklyScore: Flow<Int> = context.quizDataStore.data.map { prefs ->
        val savedWeekStart = prefs[KEY_WEEK_START] ?: ""
        val currentWeekStart = getCurrentWeekStart()
        if (savedWeekStart != currentWeekStart) 0
        else prefs[KEY_WEEKLY_SCORE] ?: 0
    }

    val streakCount: Flow<Int> = context.quizDataStore.data.map { prefs ->
        val lastDate = prefs[KEY_LAST_QUIZ_DATE] ?: return@map 0
        val today = LocalDate.now()
        val last = LocalDate.parse(lastDate, dateFormatter)
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(last, today)
        when {
            daysBetween <= 1L -> prefs[KEY_STREAK_COUNT] ?: 0
            else -> 0
        }
    }

    /**
     * Returns completed day indices for current week (0=Mon, 1=Tue, ..., 6=Sun).
     */
    val completedDays: Flow<Set<Int>> = context.quizDataStore.data.map { prefs ->
        val savedWeekStart = prefs[KEY_WEEK_START] ?: ""
        val currentWeekStart = getCurrentWeekStart()
        if (savedWeekStart != currentWeekStart) emptySet()
        else {
            val json = prefs[KEY_COMPLETED_DAYS] ?: "[]"
            val type = object : TypeToken<Set<Int>>() {}.type
            gson.fromJson(json, type) ?: emptySet()
        }
    }

    val userAnswers: Flow<Map<Int, Int>> = context.quizDataStore.data.map { prefs ->
        val savedDate = prefs[KEY_QUIZ_DATE] ?: return@map emptyMap()
        val today = LocalDate.now().format(dateFormatter)
        if (savedDate != today) return@map emptyMap()

        val json = prefs[KEY_ANSWERS] ?: return@map emptyMap()
        val type = object : TypeToken<Map<Int, Int>>() {}.type
        gson.fromJson(json, type) ?: emptyMap()
    }

    suspend fun saveDailyQuestions(questions: List<QuizQuestion>) {
        val today = LocalDate.now().format(dateFormatter)
        context.quizDataStore.edit { prefs ->
            prefs[KEY_QUIZ_DATE] = today
            prefs[KEY_QUIZ_QUESTIONS] = gson.toJson(questions)
            prefs[KEY_ANSWERED_COUNT] = 0
            prefs[KEY_CORRECT_COUNT] = 0
            prefs[KEY_IS_COMPLETED] = false
            prefs[KEY_ANSWERS] = gson.toJson(emptyMap<Int, Int>())
        }
    }

    suspend fun saveAnswer(questionIndex: Int, selectedIndex: Int, isCorrect: Boolean) {
        context.quizDataStore.edit { prefs ->
            val answered = (prefs[KEY_ANSWERED_COUNT] ?: 0) + 1
            val correct = (prefs[KEY_CORRECT_COUNT] ?: 0) + if (isCorrect) 1 else 0
            prefs[KEY_ANSWERED_COUNT] = answered
            prefs[KEY_CORRECT_COUNT] = correct
            if (answered >= 10) {
                prefs[KEY_IS_COMPLETED] = true
                // Update weekly score
                val currentWeekStart = getCurrentWeekStart()
                val savedWeekStart = prefs[KEY_WEEK_START] ?: ""
                if (savedWeekStart != currentWeekStart) {
                    prefs[KEY_WEEK_START] = currentWeekStart
                    prefs[KEY_WEEKLY_SCORE] = correct
                } else {
                    prefs[KEY_WEEKLY_SCORE] = (prefs[KEY_WEEKLY_SCORE] ?: 0) + correct
                }
                // Update streak
                val today = LocalDate.now().format(dateFormatter)
                val lastQuizDate = prefs[KEY_LAST_QUIZ_DATE] ?: ""
                if (lastQuizDate != today) {
                    val currentStreak = prefs[KEY_STREAK_COUNT] ?: 0
                    val yesterday = LocalDate.now().minusDays(1).format(dateFormatter)
                    prefs[KEY_STREAK_COUNT] = if (lastQuizDate == yesterday) currentStreak + 1 else 1
                    prefs[KEY_LAST_QUIZ_DATE] = today
                }
                // Track completed day of week (0=Mon ... 6=Sun)
                val dayIndex = LocalDate.now().dayOfWeek.value - 1 // Monday=0
                val existingDays: MutableSet<Int> = if (savedWeekStart == currentWeekStart) {
                    val json = prefs[KEY_COMPLETED_DAYS] ?: "[]"
                    val type = object : TypeToken<MutableSet<Int>>() {}.type
                    gson.fromJson(json, type) ?: mutableSetOf()
                } else {
                    mutableSetOf()
                }
                existingDays.add(dayIndex)
                prefs[KEY_COMPLETED_DAYS] = gson.toJson(existingDays)
            }

            // Save individual answer
            val answersJson = prefs[KEY_ANSWERS] ?: "{}"
            val type = object : TypeToken<MutableMap<Int, Int>>() {}.type
            val answers: MutableMap<Int, Int> = gson.fromJson(answersJson, type) ?: mutableMapOf()
            answers[questionIndex] = selectedIndex
            prefs[KEY_ANSWERS] = gson.toJson(answers)
        }
    }

    suspend fun resetDailyQuiz(resetWeeklyScore: Boolean = false) {
        context.quizDataStore.edit { prefs ->
            prefs.remove(KEY_QUIZ_DATE)
            prefs.remove(KEY_QUIZ_QUESTIONS)
            prefs[KEY_ANSWERED_COUNT] = 0
            prefs[KEY_CORRECT_COUNT] = 0
            prefs[KEY_IS_COMPLETED] = false
            prefs[KEY_ANSWERS] = gson.toJson(emptyMap<Int, Int>())
            if (resetWeeklyScore) {
                prefs[KEY_WEEKLY_SCORE] = 0
                prefs[KEY_STREAK_COUNT] = 0
                prefs.remove(KEY_LAST_QUIZ_DATE)
                prefs[KEY_COMPLETED_DAYS] = "[]"
            }
        }
    }
}
