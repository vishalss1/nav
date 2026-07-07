package com.example.nav.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.nav.model.ClickableElement
import com.example.nav.model.ScreenContext
import com.example.nav.model.UIElement

class GuideAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "GuideAccessibility"
        private var instance: GuideAccessibilityService? = null

        fun getCurrentContext(): ScreenContext? {
            return instance?.extractCurrentContext()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We can monitor window changes here if needed
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    private fun extractCurrentContext(): ScreenContext? {
        val rootNode = rootInActiveWindow ?: return null
        
        val visibleTexts = mutableListOf<String>()
        val clickableElements = mutableListOf<ClickableElement>()
        var isScrollable = false

        traverseNode(rootNode, visibleTexts, clickableElements)
        
        // Simple logic for isScrollable - check if any node is scrollable
        isScrollable = checkScrollable(rootNode)

        return ScreenContext(
            appPackage = rootNode.packageName?.toString() ?: "unknown",
            appName = getAppName(rootNode.packageName?.toString()),
            visibleTexts = visibleTexts.distinct(),
            clickableElements = clickableElements,
            scrollable = isScrollable
        )
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo?,
        texts: MutableList<String>,
        clickables: MutableList<ClickableElement>
    ) {
        if (node == null) return

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val text = node.text?.toString()
        val contentDescription = node.contentDescription?.toString()

        if (!text.isNullOrBlank()) {
            texts.add(text)
        }

        if (node.isClickable) {
            val elementText = text ?: contentDescription ?: ""
            if (elementText.isNotBlank()) {
                clickables.add(
                    ClickableElement(
                        text = elementText,
                        bounds = bounds,
                        contentDescription = contentDescription
                    )
                )
            }
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i), texts, clickables)
        }
    }

    private fun checkScrollable(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isScrollable) return true
        for (i in 0 until node.childCount) {
            if (checkScrollable(node.getChild(i))) return true
        }
        return false
    }

    private fun getAppName(packageName: String?): String {
        if (packageName == null) return "Unknown"
        return try {
            val pm = packageManager
            val ai = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
