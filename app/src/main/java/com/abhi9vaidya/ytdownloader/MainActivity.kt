package com.abhi9vaidya.ytdownloader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Build
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

import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: DownloaderViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (!allGranted) {
            viewModel.setError("Permissions required for downloading and notifications.")
        }
    }

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

        checkAndRequestPermissions()

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

        // Observe WorkManager work id from ViewModel and map WorkInfo -> ViewModel state
        lifecycleScope.launch {
            viewModel.currentWorkId.collectLatest { workId ->
                if (workId != null) {
                    val live = WorkManager.getInstance(applicationContext).getWorkInfoByIdLiveData(workId)
                    live.observe(this@MainActivity) { info: WorkInfo? ->
                        if (info != null) {
                            val progress = info.progress.getFloat("progress", 0f)
                            viewModel.updateProgress(progress)

                            if (info.state.isFinished) {
                                viewModel.setDownloading(false)
                                if (info.state == WorkInfo.State.SUCCEEDED) {
                                    viewModel.setError(null)
                                } else {
                                    val err = info.progress.getString("error") ?: info.outputData.getString("error")
                                    if (!err.isNullOrEmpty()) viewModel.setError(err)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(progressReceiver)
    }
}
