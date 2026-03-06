package com.filmapp.presentation.quiz

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.filmapp.presentation.theme.*
import java.time.DayOfWeek
import java.time.LocalDate

// ── Streak Colors ──
private val FlameOrange = Color(0xFFFF6B00)
private val FlameYellow = Color(0xFFFFB800)
private val FlameDeep = Color(0xFFE84D00)
private val StreakBg = Color(0xFF1A1128)

@Composable
fun StreakDialog(
    streakCount: Int,
    completedDays: Set<Int>,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        StreakContent(
            streakCount = streakCount,
            completedDays = completedDays,
            onContinue = onDismiss,
            onShare = onShare
        )
    }
}

@Composable
private fun StreakContent(
    streakCount: Int,
    completedDays: Set<Int>,
    onContinue: () -> Unit,
    onShare: (() -> Unit)?
) {
    val isDark = LocalIsDarkTheme.current
    val bg = if (isDark) StreakBg else Color(0xFFF5F0FF)
    val cardBg = if (isDark) DarkCard else LightCard
    val textP = if (isDark) TextPrimary else LightTextPrimary
    val textS = if (isDark) TextSecondary else LightTextSecondary

    // Animation
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    val flameScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flame_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Entry animation
    val entryScale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        entryScale.animateTo(
            1f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 300f)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 24.dp)
            .scale(entryScale.value),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // ── Flame + Number ──
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp)
        ) {
            // Glow effect behind flame
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .blur(40.dp)
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            FlameOrange.copy(alpha = glowAlpha),
                            FlameYellow.copy(alpha = glowAlpha * 0.5f),
                            Color.Transparent
                        )
                    ),
                    radius = size.minDimension / 2
                )
            }

            // Flame emoji
            Text(
                text = "\uD83D\uDD25",
                fontSize = 140.sp,
                modifier = Modifier
                    .scale(flameScale)
                    .offset(y = (-8).dp)
            )

            // Streak number with outline effect
            Box(contentAlignment = Alignment.Center) {
                // Stroke/outline layer
                Text(
                    text = "$streakCount",
                    style = TextStyle(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        color = FlameDeep,
                        drawStyle = Stroke(
                            width = 12f,
                            cap = StrokeCap.Round
                        )
                    )
                )
                // Fill layer
                Text(
                    text = "$streakCount",
                    style = TextStyle(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )
            }
        }

        // ── "X günlük seri!" ──
        Text(
            text = "$streakCount günlük seri!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = FlameOrange
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Weekly Calendar Card ──
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val today = LocalDate.now()
                val todayIndex = today.dayOfWeek.value - 1 // Mon=0
                val dayLabels = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

                // Day labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    dayLabels.forEachIndexed { index, label ->
                        val isActive = index in completedDays || index == todayIndex
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) FlameOrange else textS.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Day icons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    dayLabels.indices.forEach { index ->
                        val isCompleted = index in completedDays
                        val isToday = index == todayIndex
                        val isFuture = index > todayIndex

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp)
                        ) {
                            when {
                                isCompleted -> {
                                    // Completed: orange circle + check
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                brush = Brush.linearGradient(
                                                    colors = listOf(FlameOrange, FlameYellow)
                                                ),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                isToday && !isCompleted -> {
                                    // Today but not yet completed: pulsing outline
                                    val pulseAlpha by infiniteTransition.animateFloat(
                                        initialValue = 0.4f,
                                        targetValue = 1f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1000),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "today_pulse"
                                    )
                                    Canvas(modifier = Modifier.size(36.dp)) {
                                        drawCircle(
                                            color = FlameOrange.copy(alpha = pulseAlpha),
                                            style = Stroke(width = 3f),
                                            radius = size.minDimension / 2 - 2f
                                        )
                                    }
                                    Text(
                                        text = "⭐",
                                        fontSize = 16.sp
                                    )
                                }
                                else -> {
                                    // Future or missed: gray circle
                                    Canvas(modifier = Modifier.size(36.dp)) {
                                        drawCircle(
                                            color = if (isDark) Color.White.copy(alpha = 0.1f)
                                            else Color.Gray.copy(alpha = 0.15f),
                                            radius = size.minDimension / 2 - 2f
                                        )
                                    }
                                    Text(
                                        text = "⭐",
                                        fontSize = 14.sp,
                                        color = textS.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = textS.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(12.dp))

                // Motivation message
                val completedCount = completedDays.size
                val motivationMsg = when {
                    completedCount >= 7 -> "Mükemmel bir hafta! Tüm günleri tamamladın! 🏆"
                    completedCount >= 5 -> "Harika gidiyorsun! Haftayı bitirmene az kaldı! 💪"
                    completedCount >= 3 -> "Harika bir haftanın yarısındasın! 🔥"
                    completedCount >= 1 -> "Güzel başlangıç! Devam et! ✨"
                    else -> "Bugün ilk adımını at! 🚀"
                }
                Text(
                    text = motivationMsg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textS,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Bottom Buttons ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onShare != null) {
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .height(52.dp)
                        .width(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isDark) textP else Purple60
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Paylaş", modifier = Modifier.size(22.dp))
                }
            }
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple60)
            ) {
                Text(
                    "DEVAM ET",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
