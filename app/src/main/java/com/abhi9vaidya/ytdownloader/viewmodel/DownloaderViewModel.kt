package com.abhi9vaidya.ytdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhi9vaidya.ytdownloader.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VideoPreviewState(
    val title: String = "",
    val thumbnailUrl: String = "",
    val videoId: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class DownloaderViewModel(private val repository: DownloadRepository = DownloadRepository()) : ViewModel() {

    private val _previewState = MutableStateFlow(VideoPreviewState())
    val previewState: StateFlow<VideoPreviewState> = _previewState

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun fetchVideoInfo(url: String) {
        if (url.isBlank()) return

        viewModelScope.launch {
            _previewState.value = VideoPreviewState(isLoading = true)
            try {
                val info = repository.getVideoInfo(url)
                if (info.containsKey("error")) {
                    _previewState.value = VideoPreviewState(error = info["error"] as String)
                } else {
                    _previewState.value = VideoPreviewState(
                        title = info["title"] as String,
                        thumbnailUrl = info["thumbnail"] as String,
                        videoId = info["id"] as String
                    )
                }
            } catch (e: Exception) {
                _previewState.value = VideoPreviewState(error = e.localizedMessage)
            }
        }
    }

    fun updateProgress(progress: Float) {
        _downloadProgress.value = progress
    }

    fun setDownloading(downloading: Boolean) {
        _isDownloading.value = downloading
    }

    fun setError(message: String?) {
        _errorMessage.value = message
    }
}
