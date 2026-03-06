package com.filmapp.presentation.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmapp.R
import com.filmapp.core.util.toSafePosterUrl
import com.filmapp.core.util.toRatingString
import com.filmapp.domain.model.Movie
import com.filmapp.navigation.BottomTab
import com.filmapp.presentation.components.GlassOverlay
import com.filmapp.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMovieClick: (String) -> Unit,
    onSignOut: () -> Unit,
    onSettingsClick: () -> Unit,
    onQuizClick: () -> Unit,
    onRecommendationsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(BottomTab.Movies) }
    val isDark = LocalIsDarkTheme.current

    val bg = if (isDark) DarkBackground else LightBackground
    val surface = if (isDark) DarkSurface else LightSurface
    val surfaceVariant = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val elevated = if (isDark) DarkElevated else LightElevated
    val textP = if (isDark) TextPrimary else LightTextPrimary
    val textS = if (isDark) TextSecondary else LightTextSecondary
    val textT = if (isDark) TextTertiary else LightTextTertiary

    Scaffold(
        containerColor = bg,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_filmapp_logo),
                                contentDescription = "FilmApp",
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .background(surface, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = textT, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (state.searchQuery.isEmpty()) {
                                            Text("Search...", color = textT, style = MaterialTheme.typography.bodySmall)
                                        }
                                        BasicTextField(
                                            value = state.searchQuery,
                                            onValueChange = { viewModel.onSearchQueryChange(it) },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall.copy(color = textP),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    if (state.searchQuery.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = textT,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { viewModel.clearSearch() }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = textS)
                        }
                        IconButton(onClick = { viewModel.signOut(onSignOut) }) {
                            Icon(Icons.Default.Logout, contentDescription = "Sign out", tint = textS)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = surface,
                contentColor = textP
            ) {
                NavigationBarItem(
                    selected = selectedTab == BottomTab.Movies,
                    onClick = {
                        selectedTab = BottomTab.Movies
                        viewModel.loadMovies()
                    },
                    icon = { Icon(Icons.Default.Movie, contentDescription = null) },
                    label = { Text(BottomTab.Movies.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Purple60,
                        selectedTextColor = Purple60,
                        unselectedIconColor = textT,
                        unselectedTextColor = textT,
                        indicatorColor = elevated
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == BottomTab.TvShows,
                    onClick = {
                        selectedTab = BottomTab.TvShows
                        viewModel.loadTvShows()
                    },
                    icon = { Icon(Icons.Default.Tv, contentDescription = null) },
                    label = { Text(BottomTab.TvShows.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Purple60,
                        selectedTextColor = Purple60,
                        unselectedIconColor = textT,
                        unselectedTextColor = textT,
                        indicatorColor = elevated
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onRecommendationsClick() },
                    icon = { Text("✨", fontSize = 20.sp) },
                    label = { Text(BottomTab.Recommendations.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Purple60,
                        selectedTextColor = Purple60,
                        unselectedIconColor = textT,
                        unselectedTextColor = textT,
                        indicatorColor = elevated
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onQuizClick() },
                    icon = { Text("\uD83C\uDFAF", fontSize = 20.sp) },
                    label = { Text(BottomTab.Quiz.label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Purple60,
                        selectedTextColor = Purple60,
                        unselectedIconColor = textT,
                        unselectedTextColor = textT,
                        indicatorColor = elevated
                    )
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Main content
            when (selectedTab) {
                BottomTab.Movies -> {
                    MovieGrid(
                        movies = state.movies,
                        isLoading = state.isMoviesLoading,
                        isLoadingMore = state.isLoadingMoreMovies,
                        onMovieClick = onMovieClick,
                        onLoadMore = { viewModel.loadMoreMovies() }
                    )
                }
                BottomTab.TvShows -> {
                    MovieGrid(
                        movies = state.tvShows,
                        isLoading = state.isTvShowsLoading,
                        isLoadingMore = state.isLoadingMoreTvShows,
                        onMovieClick = onMovieClick,
                        onLoadMore = { viewModel.loadMoreTvShows() }
                    )
                }
                BottomTab.Recommendations -> { /* handled via navigation */ }
                BottomTab.Quiz -> { /* handled via navigation */ }
            }

            // Search overlay
            if (state.searchQuery.length >= 2) {
                SearchResultsOverlay(
                    results = state.searchResults,
                    isSearching = state.isSearching,
                    onMovieClick = { id ->
                        viewModel.clearSearch()
                        onMovieClick(id)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(10f)
                )
            }
        }
    }
}

@Composable
private fun SearchResultsOverlay(
    results: List<Movie>,
    isSearching: Boolean,
    onMovieClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    val surface = if (isDark) DarkSurface else LightSurface
    val surfaceVariant = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val elevated = if (isDark) DarkElevated else LightElevated
    val textP = if (isDark) TextPrimary else LightTextPrimary
    val textS = if (isDark) TextSecondary else LightTextSecondary
    val textT = if (isDark) TextTertiary else LightTextTertiary

    Card(
        modifier = modifier.padding(horizontal = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        if (isSearching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Purple60, modifier = Modifier.size(24.dp))
            }
        } else if (results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No results", color = textT, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                lazyItems(results, key = { it.id }) { movie ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMovieClick(movie.id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = movie.posterUrl.toSafePosterUrl(),
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp, 64.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(surfaceVariant)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = movie.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textP,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            movie.year?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textS
                                )
                            }
                        }
                        movie.imdbRating?.let { rating ->
                            Text(
                                text = "★ $rating",
                                style = MaterialTheme.typography.labelSmall,
                                color = Amber
                            )
                        }
                    }
                    if (movie != results.last()) {
                        HorizontalDivider(color = elevated, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieGrid(
    movies: List<Movie>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    onMovieClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Purple60)
        }
    } else if (movies.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val textT = if (LocalIsDarkTheme.current) TextTertiary else LightTextTertiary
            Text("No results found", color = textT, style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        val gridState = rememberLazyGridState()

        // Trigger load more when user scrolls near the bottom
        val shouldLoadMore by remember {
            derivedStateOf {
                val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleItem >= movies.size - 8
            }
        }
        LaunchedEffect(shouldLoadMore) {
            if (shouldLoadMore) onLoadMore()
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(movies, key = { it.id }) { movie ->
                GridMovieCard(movie = movie, onClick = { onMovieClick(movie.id) })
            }
            if (isLoadingMore) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Purple60, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GridMovieCard(movie: Movie, onClick: () -> Unit) {
    val isDark = LocalIsDarkTheme.current
    val surfaceVariant = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
    val textP = if (isDark) TextPrimary else LightTextPrimary
    val textS = if (isDark) TextSecondary else LightTextSecondary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box {
            AsyncImage(
                model = movie.posterUrl.toSafePosterUrl(),
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(surfaceVariant)
            )

            movie.imdbRating?.let { rating ->
                GlassOverlay(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    cornerRadius = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("★", color = Amber, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = rating.toRatingString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Amber
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = movie.title,
            style = MaterialTheme.typography.titleSmall,
            color = textP,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        movie.year?.let { year ->
            Text(
                text = year,
                style = MaterialTheme.typography.bodySmall,
                color = textS,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }
    }
}
