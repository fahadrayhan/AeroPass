package com.example.aeropass.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.*

class SecureClipboardManager(private val context: Context) {
    
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var clearJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    fun copyToClipboard(text: String, label: String = "AeroPass", showToast: Boolean = true, autoClearSeconds: Long = 15) {
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
        
        if (showToast) {
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        
        // Auto-clear clipboard after specified time
        clearJob?.cancel()
        clearJob = scope.launch {
            delay(autoClearSeconds * 1000)
            clearClipboard()
        }
    }
    
    private fun clearClipboard() {
        val emptyClip = ClipData.newPlainText("", "")
        clipboardManager.setPrimaryClip(emptyClip)
    }
    
    fun cleanup() {
        clearJob?.cancel()
        scope.cancel()
    }
}