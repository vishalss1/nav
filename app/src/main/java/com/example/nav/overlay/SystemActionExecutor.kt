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
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        // DYNAMIC DISCOVERY: Query all apps with a launcher icon
        val apps = pm.queryIntentActivities(mainIntent, 0)
        Log.d(TAG, "Scanning ${apps.size} launcher apps for '$appName'")

        var bestMatchPackage: String? = null
        var highestScore = 0f

        for (app in apps) {
            val label = app.loadLabel(pm).toString()
            val packageName = app.activityInfo.packageName
            
            // Calculate a matching score (0.0 to 1.0)
            val score = calculateMatchScore(appName, label, packageName)
            
            if (score > highestScore && score > 0.4f) { // Threshold for fuzzy match
                highestScore = score
                bestMatchPackage = packageName
            }
            
            if (score == 1.0f) break // Perfect match found
        }

        if (bestMatchPackage != null) {
            val launchIntent = pm.getLaunchIntentForPackage(bestMatchPackage)
            if (launchIntent != null) {
                Log.d(TAG, "Launching '$bestMatchPackage' with score $highestScore")
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                context.applicationContext.startActivity(launchIntent)
            } else {
                Log.e(TAG, "No launch intent for $bestMatchPackage")
            }
        } else {
            Log.e(TAG, "App not found dynamically: $appName")
        }
    }

    /**
     * Scores the match between query and app info.
     * 1.0 = Perfect match
     * 0.8 = Starts with
     * 0.6 = Contains
     * 0.0 = No match
     */
    private fun calculateMatchScore(query: String, label: String, packageName: String): Float {
        val q = query.lowercase().trim()
        val l = label.lowercase().trim()
        val p = packageName.lowercase()

        // 1. Exact Match
        if (q == l) return 1.0f
        
        // 2. Starts With (e.g., "YouTube" matches "YouTube Music")
        if (l.startsWith(q) || q.startsWith(l)) return 0.8f
        
        // 3. Contains (e.g., "Chrome" matches "Google Chrome")
        if (l.contains(q) || q.contains(l)) return 0.6f
        
        // 4. Package Name Match (e.g., "Gmail" matches "com.google.android.gm")
        if (p.contains(q)) return 0.5f

        return 0.0f
    }
}
