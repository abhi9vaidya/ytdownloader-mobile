package com.abhi9vaidya.ytdownloader.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.abhi9vaidya.ytdownloader.viewmodel.DownloaderViewModel
import androidx.compose.ui.platform.LocalContext
import com.abhi9vaidya.ytdownloader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: DownloaderViewModel) {
    var url by remember { mutableStateOf("") }
    val previewState by viewModel.previewState.collectAsState(initial = VideoPreviewState())
    val progress by viewModel.downloadProgress.collectAsState(initial = 0f)
    val isDownloading by viewModel.isDownloading.collectAsState(initial = false)
    val context = LocalContext.current

    var showAbout by remember { mutableStateOf(false) }
    val aboutTitle = stringResource(R.string.about_title)
    val aboutText = stringResource(R.string.about_text)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.app_name)) },
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("About") }, onClick = { expanded = false; showAbout = true })
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Shorts Downloader",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    if (it.contains("youtube.com/shorts/") || it.contains("youtu.be/")) {
                        viewModel.fetchVideoInfo(it)
                    }
                },
                label = { Text("Paste YouTube Shorts URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = previewState.isLoading) {
                CircularProgressIndicator()
            }

            AnimatedVisibility(visible = previewState.title.isNotEmpty() && !previewState.isLoading) {
                VideoPreviewCard(
                    title = previewState.title,
                    thumbnailUrl = previewState.thumbnailUrl
                )
            }

            AnimatedVisibility(visible = previewState.error != null) {
                Text(
                    text = previewState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (isDownloading) {
                DownloadProgressSection(progress = progress)
            } else {
                Button(
                    onClick = {
                        if (previewState.title.isNotEmpty()) {
                            viewModel.enqueueDownload(context, url, previewState.title)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = previewState.title.isNotEmpty()
                ) {
                    Text("Download Video", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "For personal use only. Respect YouTube TOS.",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }

        if (showAbout) {
            AlertDialog(
                onDismissRequest = { showAbout = false },
                confirmButton = {
                    TextButton(onClick = { showAbout = false }) { Text("OK") }
                },
                title = { Text(aboutTitle) },
                text = { Text(aboutText) }
            )
        }
    }
}

@Composable
fun VideoPreviewCard(title: String, thumbnailUrl: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "Thumbnail",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = title,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2
            )
        }
    }
}

@Composable
fun DownloadProgressSection(progress: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.size(100.dp),
                strokeWidth = 8.dp
            )
            Text(
                text = "${progress.toInt()}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Downloading...", style = MaterialTheme.typography.bodyMedium)
    }
}
