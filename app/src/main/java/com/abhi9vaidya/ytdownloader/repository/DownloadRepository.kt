package com.abhi9vaidya.ytdownloader.repository

import com.chaquo.python.PyObject
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DownloadRepository {

    suspend fun getVideoInfo(url: String): Map<String, Any?> = withContext(Dispatchers.IO) {
        val py = Python.getInstance()
        val downloader = py.getModule("downloader")
        val info = downloader.callAttr("get_video_info", url).asMap()

        info.entries.associate { (key, value) ->
            key.toString() to value?.toJava(Any::class.java)
        }
    }

    suspend fun downloadVideo(url: String, downloadPath: String, onProgress: (Double) -> Unit): Map<String, Any?> = withContext(Dispatchers.IO) {
        val py = Python.getInstance()
        val downloader = py.getModule("downloader")

        // Wrap Kotlin lambda into a Java-compatible callback object
        val callbackObj = object {
            @Suppress("unused")
            fun onProgressUpdate(progress: Double) {
                onProgress(progress)
            }
        }

        // Create a PyObject wrapper for the callback method
        val pyCallback: PyObject = try {
            PyObject.fromJava(callbackObj).getAttr("onProgressUpdate")
        } catch (e: Exception) {
            // If wrapping the callback fails, pass null and rely on periodic progress updates elsewhere
            null as PyObject
        }

        try {
            val result = if (pyCallback != null) {
                downloader.callAttr("download_video", url, downloadPath, pyCallback).asMap()
            } else {
                downloader.callAttr("download_video", url, downloadPath).asMap()
            }

            result.entries.associate { (key, value) ->
                key.toString() to value?.toJava(Any::class.java)
            }
        } catch (e: Exception) {
            mapOf("success" to false, "error" to (e.localizedMessage ?: e.toString()))
        }
    }
}
