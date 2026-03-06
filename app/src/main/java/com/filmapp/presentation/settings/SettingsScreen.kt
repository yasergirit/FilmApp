package com.filmapp.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.filmapp.core.util.toSafePosterUrl
import com.filmapp.presentation.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val isDark by themeManager.isDarkTheme.collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    val settingsState by viewModel.state.collectAsStateWithLifecycle()

    val bg = if (isDark) DarkBackground else LightBackground
    val surface = if (isDark) DarkSurface else LightSurface
    val textPrimary = if (isDark) TextPrimary else LightTextPrimary
    val textSecondary = if (isDark) TextSecondary else LightTextSecondary

    Scaffold(
        containerColor = bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── User Profile Section ──
            Text(
                text = "Profil",
                style = MaterialTheme.typography.titleMedium,
                color = Purple60,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (settingsState.isEditing) {
                // Edit mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surface, RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = settingsState.editText,
                        onValueChange = viewModel::onEditTextChange,
                        singleLine = true,
                        placeholder = { Text("Kullanıcı adı", color = textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            cursorColor = Purple60,
                            focusedBorderColor = Purple60,
                            unfocusedBorderColor = if (isDark) DarkElevated else LightElevated
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = viewModel::saveDisplayName,
                        enabled = settingsState.editText.isNotBlank() && !settingsState.isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = Purple60),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (settingsState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = TextPrimary
                            )
                        } else {
                            Text("Kaydet", color = TextPrimary)
                        }
                    }
                    TextButton(onClick = viewModel::cancelEditing) {
                        Text("İptal", color = textSecondary)
                    }
                }
            } else {
                // Display mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surface, RoundedCornerShape(16.dp))
                        .clickable { viewModel.startEditing() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Purple60,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Kullanıcı Adı",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textPrimary
                            )
                            Text(
                                text = settingsState.displayName.ifBlank { "Yükleniyor..." },
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Düzenle",
                        tint = Purple60,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Favorites Section ──
            Text(
                text = "Favoriler",
                style = MaterialTheme.typography.titleMedium,
                color = Purple60,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (settingsState.isFavoritesLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surface, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Purple60, modifier = Modifier.size(24.dp))
                }
            } else if (settingsState.favoriteMovies.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surface, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = textSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Henüz favori filminiz yok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    settingsState.favoriteMovies.forEach { movie ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(surface, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AsyncImage(
                                model = movie.posterPath.toSafePosterUrl(),
                                contentDescription = movie.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp, 56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) DarkElevated else LightElevated)
                            )
                            Text(
                                text = movie.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorilerden kaldır",
                                tint = ErrorRed,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { viewModel.removeFavorite(movie.movieId) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Appearance Section ──
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = Purple60,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surface, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = Purple60,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Dark Theme",
                            style = MaterialTheme.typography.bodyLarge,
                            color = textPrimary
                        )
                        Text(
                            text = if (isDark) "Dark mode is on" else "Light mode is on",
                            style = MaterialTheme.typography.bodySmall,
                            color = textSecondary
                        )
                    }
                }
                Switch(
                    checked = isDark,
                    onCheckedChange = { newValue ->
                        scope.launch { themeManager.setDarkTheme(newValue) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Purple60,
                        checkedTrackColor = Purple20,
                        uncheckedThumbColor = Purple40,
                        uncheckedTrackColor = LightElevated
                    )
                )
            }
        }
    }
}
