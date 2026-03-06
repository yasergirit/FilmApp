package com.filmapp.presentation.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmapp.core.util.toSafePosterUrl
import com.filmapp.data.remote.supabase.dto.ReviewDto
import com.filmapp.domain.model.MovieDetail
import com.filmapp.presentation.components.GlassOverlay
import com.filmapp.presentation.components.ShimmerHeroSection
import com.filmapp.presentation.theme.*

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (state.isLoading) {
            ShimmerHeroSection(modifier = Modifier.fillMaxSize())
        } else if (state.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error.orEmpty(), color = ErrorRed, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            state.movie?.let { movie ->
                DetailContent(
                    movie = movie,
                    isFavorite = state.isFavorite,
                    isFavoriteLoading = state.isFavoriteLoading,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onBack = onBack,
                    aiReview = state.aiReview,
                    isReviewLoading = state.isReviewLoading,
                    reviews = state.reviews,
                    isReviewsLoading = state.isReviewsLoading,
                    isSubmittingReview = state.isSubmittingReview,
                    userDisplayName = state.userDisplayName,
                    onSubmitReview = { content -> viewModel.submitReview(state.userDisplayName, content) }
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    movie: MovieDetail,
    isFavorite: Boolean,
    isFavoriteLoading: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    aiReview: String?,
    isReviewLoading: Boolean,
    reviews: List<ReviewDto>,
    isReviewsLoading: Boolean,
    isSubmittingReview: Boolean,
    userDisplayName: String,
    onSubmitReview: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Immersive Poster as Backdrop ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
        ) {
            AsyncImage(
                model = movie.posterUrl.toSafePosterUrl(),
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradient overlays
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DarkBackground.copy(alpha = 0.4f),
                                DarkBackground.copy(alpha = 0f),
                                DarkBackground.copy(alpha = 0.7f),
                                DarkBackground
                            )
                        )
                    )
            )

            // Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(8.dp)
                    .align(Alignment.TopStart)
            ) {
                GlassOverlay(cornerRadius = 20.dp) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Movie info overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Genres from comma-separated string
                movie.genre?.takeIf { it.isNotBlank() }?.let { genreStr ->
                    val genres = genreStr.split(",").map { it.trim() }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        genres.take(3).forEach { genre ->
                            GlassOverlay(cornerRadius = 16.dp) {
                                Text(
                                    text = genre,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Cyan60,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.displayLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                movie.rated?.takeIf { it != "N/A" }?.let { rated ->
                    Text(
                        text = rated,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextTertiary,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }

        // ── Meta Row ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rating
            movie.imdbRating?.takeIf { it != "N/A" }?.let { rating ->
                GlassOverlay(cornerRadius = 12.dp) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("★", color = Amber, fontSize = 16.sp)
                        Text(
                            text = rating,
                            style = MaterialTheme.typography.titleMedium,
                            color = Amber,
                            fontWeight = FontWeight.Bold
                        )
                        movie.imdbVotes?.takeIf { it != "N/A" }?.let { votes ->
                            Text(
                                text = "($votes)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary
                            )
                        }
                    }
                }
            }

            movie.year?.let { year ->
                Text(text = year, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            movie.runtime?.takeIf { it != "N/A" }?.let { runtime ->
                Text(
                    text = runtime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Favorite Button with micro-interaction ──
            FavoriteButton(
                isFavorite = isFavorite,
                isLoading = isFavoriteLoading,
                onClick = onToggleFavorite
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Overview ──
        Text(
            text = "Storyline",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = movie.overview,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        // ── Director ──
        movie.director?.takeIf { it != "N/A" }?.let { director ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Director",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Text(
                text = director,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // ── Cast (Actors) ──
        movie.actors?.takeIf { it != "N/A" }?.let { actors ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cast",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = actors,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // ── Writer ──
        movie.writer?.takeIf { it != "N/A" }?.let { writer ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Writer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Text(
                text = writer,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // ── Box Office ──
        movie.boxOffice?.takeIf { it != "N/A" }?.let { boxOffice ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Box Office",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Text(
                text = boxOffice,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── AI Film Review Card ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "\uD83E\uDD16",
                        fontSize = 20.sp
                    )
                    Text(
                        text = "Uzman Yorumu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Cyan60
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (isReviewLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = Cyan60
                        )
                    }
                } else {
                    Text(
                        text = aiReview ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── User Reviews Section ──
        Text(
            text = "\uD83D\uDCAC Kullanıcı Yorumları",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Review Input
        var reviewText by remember { mutableStateOf("") }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Show current username
                Text(
                    text = "\uD83D\uDC64 ${userDisplayName.ifBlank { "Anonim" }}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Cyan60,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = reviewText,
                    onValueChange = { reviewText = it },
                    placeholder = { Text("Yorumunuzu yazın...", color = TextTertiary) },
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Cyan60,
                        focusedBorderColor = Cyan60,
                        unfocusedBorderColor = DarkElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onSubmitReview(reviewText.trim())
                        reviewText = ""
                    },
                    enabled = reviewText.isNotBlank() && userDisplayName.isNotBlank() && !isSubmittingReview,
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan60),
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmittingReview) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = DarkBackground
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = "Gönder", tint = DarkBackground, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gönder", color = DarkBackground)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Review Cards
        if (isReviewsLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Cyan60
                )
            }
        } else if (reviews.isEmpty()) {
            Text(
                text = "Henüz yorum yapılmamış. İlk yorumu siz yapın!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextTertiary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        } else {
            reviews.forEach { review ->
                ReviewCard(review = review)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun ReviewCard(review: ReviewDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Cyan60
                )
                review.createdAt?.let { date ->
                    Text(
                        text = date.take(10),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = review.content,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isFavorite) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "favorite_scale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isFavorite) Amber else DarkElevated,
        animationSpec = tween(300),
        label = "favorite_color"
    )

    FilledIconButton(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.scale(scale),
        shape = RoundedCornerShape(12.dp),
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = containerColor)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TextPrimary)
        } else {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = if (isFavorite) "Favorilerden Kaldır" else "Favorilere Ekle",
                tint = if (isFavorite) DarkBackground else TextPrimary
            )
        }
    }
}
