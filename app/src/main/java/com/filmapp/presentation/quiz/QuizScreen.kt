package com.filmapp.presentation.quiz

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmapp.presentation.theme.*

import com.filmapp.presentation.leaderboard.LeaderboardDialog
import androidx.compose.material.icons.filled.Leaderboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    onBack: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showLeaderboard by remember { mutableStateOf(false) }
    val isDark = LocalIsDarkTheme.current

    val bg = if (isDark) DarkBackground else LightBackground
    val surface = if (isDark) DarkSurface else LightSurface
    val card = if (isDark) DarkCard else LightCard
    val elevated = if (isDark) DarkElevated else LightElevated
    val textP = if (isDark) TextPrimary else LightTextPrimary
    val textS = if (isDark) TextSecondary else LightTextSecondary
    val textT = if (isDark) TextTertiary else LightTextTertiary

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎯", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Günlük Quiz",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textP
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Geri", tint = textS)
                    }
                },
                actions = {
                    if (state.isDemoUser) {
                        IconButton(onClick = { viewModel.resetQuiz() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Quiz Sıfırla", tint = textS)
                        }
                    }
                    IconButton(onClick = { showLeaderboard = true }) {
                        Icon(Icons.Default.Leaderboard, contentDescription = "Sıralama", tint = textS)
                    }
                    // Streak badge
                    if (state.streakCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFF6B35).copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("\uD83D\uDD25", fontSize = 14.sp)
                                Text(
                                    "${state.streakCount}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF6B35),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    // Weekly score badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Purple60.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, null, tint = Amber, modifier = Modifier.size(18.dp))
                            Text(
                                "${state.weeklyScore}",
                                fontWeight = FontWeight.Bold,
                                color = Amber,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when {
                state.isLoading -> {
                    QuizLoadingContent(textP, textS)
                }
                state.error != null -> {
                    QuizErrorContent(
                        error = state.error!!,
                        textP = textP,
                        textS = textS,
                        onRetry = { viewModel.retryLoad() }
                    )
                }
                state.isCompleted -> {
                    QuizCompletedContent(
                        correctCount = state.correctCount,
                        totalCount = state.questions.size,
                        weeklyScore = state.weeklyScore,
                        streakCount = state.streakCount,
                        textP = textP,
                        textS = textS,
                        card = card
                    )
                }
                state.questions.isNotEmpty() -> {
                    QuizQuestionContent(
                        state = state,
                        textP = textP,
                        textS = textS,
                        textT = textT,
                        card = card,
                        elevated = elevated,
                        onSelectAnswer = { viewModel.selectAnswer(state.currentIndex, it) },
                        onNext = { viewModel.nextQuestion() }
                    )
                }
            }
        }        
        if (showLeaderboard) {
            LeaderboardDialog(onDismiss = { showLeaderboard = false })
        }
        if (state.showStreakDialog) {
            StreakDialog(
                streakCount = state.streakCount,
                completedDays = state.completedDays,
                onDismiss = { viewModel.dismissStreakDialog() }
            )
        }
    }
}

@Composable
private fun QuizLoadingContent(textP: Color, textS: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Purple60, strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Sorular hazırlanıyor...", color = textS, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("AI sana özel sorular üretiyor 🤖", color = textS, fontSize = 13.sp)
        }
    }
}

@Composable
private fun QuizErrorContent(error: String, textP: Color, textS: Color, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("😕", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(error, color = textS, fontSize = 16.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Purple60)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tekrar Dene")
            }
        }
    }
}

@Composable
private fun QuizCompletedContent(
    correctCount: Int,
    totalCount: Int,
    weeklyScore: Int,
    streakCount: Int,
    textP: Color,
    textS: Color,
    card: Color
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            val emoji = when {
                correctCount >= 9 -> "🏆"
                correctCount >= 7 -> "🎉"
                correctCount >= 5 -> "👍"
                else -> "💪"
            }
            Text(emoji, fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Günlük Quiz Tamamlandı!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textP
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Score card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = card),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$correctCount / $totalCount",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (correctCount >= 7) Cyan60 else if (correctCount >= 5) Amber else ErrorRed
                    )
                    Text("doğru cevap", color = textS, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(20.dp))
                    Divider(color = textS.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, null, tint = Amber, modifier = Modifier.size(24.dp))
                        Text("Haftalık Puan:", color = textS, fontSize = 14.sp)
                        Text(
                            "$weeklyScore",
                            fontWeight = FontWeight.Bold,
                            color = Amber,
                            fontSize = 20.sp
                        )
                    }

                    if (streakCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("\uD83D\uDD25", fontSize = 22.sp)
                            Text("Seri:", color = textS, fontSize = 14.sp)
                            Text(
                                "$streakCount gün",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B35),
                                fontSize = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val message = when {
                        correctCount >= 9 -> "Süpersin! Film bilgin muhteşem! 🌟"
                        correctCount >= 7 -> "Harika! Gerçek bir sinema sever! 🎬"
                        correctCount >= 5 -> "Fena değil! Biraz daha pratik yapmalısın! 🍿"
                        else -> "Film maratonu zamanı! Yarın tekrar dene! 📺"
                    }
                    Text(
                        text = message,
                        color = textS,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Yarın yeni sorular seni bekliyor!",
                color = textS,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun QuizQuestionContent(
    state: QuizState,
    textP: Color,
    textS: Color,
    textT: Color,
    card: Color,
    elevated: Color,
    onSelectAnswer: (Int) -> Unit,
    onNext: () -> Unit
) {
    val question = state.questions[state.currentIndex]
    val hasAnswered = state.userAnswers.containsKey(state.currentIndex)
    val userAnswer = state.userAnswers[state.currentIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Progress bar
        val progress by animateFloatAsState(
            targetValue = (state.answeredCount.toFloat() / state.questions.size),
            animationSpec = tween(500),
            label = "progress"
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Purple60,
            trackColor = elevated
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Question counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Soru ${state.currentIndex + 1} / ${state.questions.size}",
                color = textT,
                fontSize = 13.sp
            )
            Text(
                "✅ ${state.correctCount} doğru",
                color = Cyan60,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Question card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = card),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = question.question,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textP,
                lineHeight = 26.sp,
                modifier = Modifier.padding(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Options
        val optionLabels = listOf("A", "B", "C", "D")
        question.options.forEachIndexed { index, option ->
            val isSelected = userAnswer == index
            val isCorrect = index == question.correctAnswerIndex
            val showResult = hasAnswered

            val borderColor = when {
                showResult && isCorrect -> Color(0xFF22C55E) // green
                showResult && isSelected && !isCorrect -> ErrorRed
                !showResult && state.selectedAnswer == index -> Purple60
                else -> Color.Transparent
            }
            val bgColor = when {
                showResult && isCorrect -> Color(0xFF22C55E).copy(alpha = 0.12f)
                showResult && isSelected && !isCorrect -> ErrorRed.copy(alpha = 0.12f)
                else -> card
            }

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .then(
                        if (borderColor != Color.Transparent) {
                            Modifier.border(2.dp, borderColor, RoundedCornerShape(14.dp))
                        } else {
                            Modifier.border(1.dp, textT.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        }
                    )
                    .clickable(enabled = !hasAnswered) { onSelectAnswer(index) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Option label circle
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = when {
                                    showResult && isCorrect -> Color(0xFF22C55E)
                                    showResult && isSelected && !isCorrect -> ErrorRed
                                    else -> Purple60.copy(alpha = 0.15f)
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showResult && isCorrect) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        } else if (showResult && isSelected && !isCorrect) {
                            Icon(Icons.Default.Cancel, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                optionLabels[index],
                                fontWeight = FontWeight.Bold,
                                color = Purple60,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = option,
                        color = textP,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Next button
        if (hasAnswered && !state.isCompleted) {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple60)
            ) {
                Text(
                    "Sonraki Soru →",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
