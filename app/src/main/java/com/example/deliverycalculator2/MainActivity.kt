package com.example.deliverycalculator2

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.deliverycalculator2.data.AppDatabase
import com.example.deliverycalculator2.screens.MainScreen
import com.example.deliverycalculator2.worker.CleanupWorker

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var clipboardManager: ClipboardManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        try {
            database = AppDatabase.getDatabase(this)
            clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            
            setupCleanupWorker()
            
            setContent {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen(database)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showErrorDialog()
        }
    }

    private fun showErrorDialog() {
        runOnUiThread {
            android.app.AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("An error occurred while starting the app. Please try again.")
                .setPositiveButton("OK") { _, _ -> finish() }
                .show()
        }
    }

    private fun setupCleanupWorker() {
        try {
            val cleanupWorkRequest = OneTimeWorkRequestBuilder<CleanupWorker>()
                .build()
            
            WorkManager.getInstance(applicationContext)
                .enqueue(cleanupWorkRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}