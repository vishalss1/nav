package com.example.nav.overlay

import android.content.Context
import android.content.Intent
import android.util.Log

class SystemActionExecutor(private val context: Context) {
    private val TAG = "SystemActionExecutor"

    fun execute(action: String) {
        Log.d(TAG, "Executing action: $action")
        if (action.startsWith("OPEN_APP:")) {
            val appName = action.substringAfter("OPEN_APP:").trim()
            launchApp(appName)
        }
    }

    private fun launchApp(appName: String) {
        val pm = context.packageManager
        
        // Strategy 1: Hardcoded common aliases for reliability
        val commonPackageMap = mapOf(
            "Chrome" to "com.android.chrome",
            "Google Chrome" to "com.android.chrome",
            "YouTube" to "com.google.android.youtube",
            "WhatsApp" to "com.whatsapp",
            "Maps" to "com.google.android.apps.maps",
            "Google Maps" to "com.google.android.apps.maps",
            "Gmail" to "com.google.android.gm",
            "Play Store" to "com.android.vending",
            "Settings" to "com.android.settings",
            "Camera" to "com.android.camera",
            "Photos" to "com.google.android.apps.photos",
            "Phone" to "com.android.dialer",
            "Messages" to "com.google.android.apps.messaging"
        )

        var targetPackage: String? = commonPackageMap.entries.find { 
            it.key.equals(appName, ignoreCase = true) 
        }?.value

        // Strategy 2: Search all launcher activities if aliases fail
        if (targetPackage == null) {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(mainIntent, 0)
            Log.d(TAG, "Alias failed for '$appName'. Searching among ${apps.size} launcher apps")
            
            // Try exact label match
            targetPackage = apps.find { 
                it.loadLabel(pm).toString().equals(appName, ignoreCase = true) 
            }?.activityInfo?.packageName
            
            // Try partial label match
            if (targetPackage == null) {
                targetPackage = apps.find { 
                    it.loadLabel(pm).toString().contains(appName, ignoreCase = true) 
                }?.activityInfo?.packageName
            }
        }

        if (targetPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                Log.d(TAG, "Launching package: $targetPackage")
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                // Use applicationContext for starting from background service
                context.applicationContext.startActivity(launchIntent)
            } else {
                Log.e(TAG, "No launch intent for $targetPackage")
            }
        } else {
            Log.e(TAG, "App not found: $appName")
        }
    }
}
