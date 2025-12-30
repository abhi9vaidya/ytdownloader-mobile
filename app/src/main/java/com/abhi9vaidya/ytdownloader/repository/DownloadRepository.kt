package com.abhi9vaidya.ytdownloader.repository

import com.chaquo.python.PyObject
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DownloadRepository {

    suspend fun getVideoInfo(url: String): Map<String, Any?> = withContext(Dispatchers.IO) {
        val py = Python.getInstance()
        val downloader = py.getModule("downloader")
        val info = downloader.callAttr("get_video_info", url).asMap()
        
        info.entries.associate { (key, value) ->
            key.toString() to value?.toJava(Any::class.java)
        }
    }

    fun downloadVideo(url: String, downloadPath: String, onProgress: (Double) -> Unit): Map<String, Any?> {
        val py = Python.getInstance()
        val downloader = py.getModule("downloader")
        
        val callback = object {
            fun onProgressUpdate(progress: Double) {
                onProgress(progress)
            }
        }

        // We wrap the callback in a PyObject if needed, but Chaquopy can handle simple lambdas or objects
        val pyCallback = PyObject.fromJava(callback).getAttr("onProgressUpdate")
        
        val result = downloader.callAttr("download_video", url, downloadPath, pyCallback).asMap()
        
        return result.entries.associate { (key, value) ->
            key.toString() to value?.toJava(Any::class.java)
        }
    }
}
