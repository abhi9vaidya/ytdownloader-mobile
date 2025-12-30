package com.abhi9vaidya.ytdownloader.repository

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.abhi9vaidya.ytdownloader.worker.DownloadWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

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

    // Enqueue the DownloadWorker using WorkManager and return the WorkRequest id
    fun enqueueDownload(context: Context, url: String, title: String): UUID {
        val input = workDataOf("url" to url, "title" to title)
        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(input)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        return workRequest.id
    }
}
