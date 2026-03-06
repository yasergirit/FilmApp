package com.filmapp.presentation.recommendations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filmapp.data.remote.gemini.GeminiService
import com.filmapp.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    viewModel: RecommendationsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isDark = LocalIsDarkTheme.current

    val bg = if (isDark) DarkBackground else LightBackground
    val textPrimary = if (isDark) TextPrimary else LightTextPrimary
    val textSecondary = if (isDark) TextSecondary else LightTextSecondary

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Sana Özel",
                            style = MaterialTheme.typography.titleLarge,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> LoadingContent()
                state.error != null && !state.hasFavorites -> EmptyFavoritesContent(
                    message = state.error!!,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    isDark = isDark
                )
                state.error != null -> ErrorContent(
                    message = state.error!!,
                    onRetry = { viewModel.loadRecommendations() },
                    textPrimary = textPrimary,
                    isDark = isDark
                )
                else -> RecommendationsList(
                    recommendations = state.recommendations,
                    onMovieClick = onMovieClick,
                    onRefresh = { viewModel.loadRecommendations() },
                    isCached = state.isCached,
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    val isDark = LocalIsDarkTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "🤖",
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Gemini düşünüyor...",
            style = MaterialTheme.typography.titleMedium,
            color = (if (isDark) TextPrimary else LightTextPrimary).copy(alpha = alpha),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Favorilerine göre öneriler hazırlanıyor",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDark) TextSecondary else LightTextSecondary
        )
        Spacer(modifier = Modifier.height(24.dp))
        LinearProgressIndicator(
            modifier = Modifier.width(200.dp),
            color = Purple60,
            trackColor = if (isDark) DarkElevated else LightElevated
        )
    }
}

@Composable
private fun EmptyFavoritesContent(
    message: String,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎬", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Henüz Öneri Yok",
            style = MaterialTheme.typography.titleLarge,
            color = textPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .background(
                    if (isDark) DarkSurface else LightSurface,
                    RoundedCornerShape(12.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = ErrorRed)
            Text(
                text = "Film detayında ❤️ butonuna basarak favori ekleyin",
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    textPrimary: Color,
    isDark: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = textPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Purple60),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tekrar Dene")
        }
    }
}

@Composable
private fun RecommendationsList(
    recommendations: List<GeminiService.MovieRecommendation>,
    onMovieClick: (String) -> Unit,
    onRefresh: () -> Unit,
    isCached: Boolean,
    isDark: Boolean
) {
    val surface = if (isDark) DarkSurface else LightSurface
    val textPrimary = if (isDark) TextPrimary else LightTextPrimary
    val textSecondary = if (isDark) TextSecondary else LightTextSecondary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Favorilerine göre öneriler",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary
                )
                if (isCached) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Yarın yeni öneriler",
                            style = MaterialTheme.typography.labelMedium,
                            color = textSecondary
                        )
                    }
                } else {
                    TextButton(onClick = onRefresh) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Yenile",
                            tint = Purple60,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Yenile", color = Purple60)
                    }
                }
            }
        }

        itemsIndexed(recommendations) { index, rec ->
            RecommendationCard(
                recommendation = rec,
                index = index,
                onClick = {
                    if (rec.imdbId.startsWith("tt")) {
                        onMovieClick(rec.imdbId)
                    }
                },
                isDark = isDark,
                surface = surface,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: GeminiService.MovieRecommendation,
    index: Int,
    onClick: () -> Unit,
    isDark: Boolean,
    surface: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val gradientColors = listOf(
        listOf(Purple60.copy(alpha = 0.15f), Cyan60.copy(alpha = 0.05f)),
        listOf(Cyan60.copy(alpha = 0.15f), Purple60.copy(alpha = 0.05f)),
        listOf(Amber.copy(alpha = 0.12f), Purple60.copy(alpha = 0.05f)),
        listOf(SuccessGreen.copy(alpha = 0.12f), Cyan60.copy(alpha = 0.05f)),
        listOf(ErrorRed.copy(alpha = 0.10f), Amber.copy(alpha = 0.05f)),
        listOf(Purple60.copy(alpha = 0.12f), SuccessGreen.copy(alpha = 0.05f))
    )
    val gradient = gradientColors[index % gradientColors.size]

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(gradient))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Number badge
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Purple60),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recommendation.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = recommendation.year,
                                style = MaterialTheme.typography.labelMedium,
                                color = textSecondary
                            )
                            Text("•", color = textSecondary)
                            Text(
                                text = recommendation.genre,
                                style = MaterialTheme.typography.labelMedium,
                                color = Purple60,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Detay",
                        tint = textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = recommendation.reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
