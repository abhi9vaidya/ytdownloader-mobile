package com.abhi9vaidya.ytdownloader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhi9vaidya.ytdownloader.ui.MainScreen
import com.abhi9vaidya.ytdownloader.ui.theme.YTDownloaderTheme
import com.abhi9vaidya.ytdownloader.viewmodel.DownloaderViewModel
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: DownloaderViewModel

    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val progress = intent?.getFloatExtra("progress", 0f) ?: 0f
            viewModel.updateProgress(progress)
            if (progress >= 100f) {
                viewModel.setDownloading(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        setContent {
            YTDownloaderTheme {
                viewModel = viewModel()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }

        registerReceiver(progressReceiver, IntentFilter("DOWNLOAD_PROGRESS"))
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(progressReceiver)
    }
}
