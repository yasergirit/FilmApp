package com.filmapp.domain.model

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int
) {
    fun shuffled(): QuizQuestion {
        val indexed = options.mapIndexed { i, opt -> i to opt }
        val shuffled = indexed.shuffled()
        val newCorrectIndex = shuffled.indexOfFirst { it.first == correctAnswerIndex }
        return QuizQuestion(
            question = question,
            options = shuffled.map { it.second },
            correctAnswerIndex = newCorrectIndex
        )
    }
}

data class DailyQuiz(
    val date: String,           // yyyy-MM-dd
    val questions: List<QuizQuestion>,
    val answeredCount: Int = 0,
    val correctCount: Int = 0,
    val isCompleted: Boolean = false
)
