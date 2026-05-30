package com.example.domain.usecase

import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class ParseWindowContentUseCase {
    private val TAG = "ParseWindowContentUseCase"

    /**
     * Extracts text elements from an AccessibilityNodeInfo root recursively.
     * Bounded to parse at most 300 nodes per event to prevent performance degradation.
     * Returns a list of extracted clean strings.
     */
    fun execute(rootNode: AccessibilityNodeInfo?): List<String> {
        val result = mutableListOf<String>()
        if (rootNode == null) return result
        
        var nodeCount = 0
        val maxNodes = 300
        var warningLogged = false
        
        fun traverse(node: AccessibilityNodeInfo?) {
            if (node == null || nodeCount >= maxNodes) return
            nodeCount++
            
            val text = node.text?.toString()
            val contentDescription = node.contentDescription?.toString()
            
            // Add non-empty string content to results
            if (!text.isNullOrBlank()) {
                result.add(text.trim())
            } else if (!contentDescription.isNullOrBlank()) {
                result.add(contentDescription.trim())
            }
            
            val childCount = node.childCount
            for (i in 0 until childCount) {
                if (nodeCount >= maxNodes) {
                    if (!warningLogged) {
                        Log.w(TAG, "Accessibility node tree traversal bounded: parse at most 300 nodes per event; deeper trees are truncated with a logged warning.")
                        warningLogged = true
                    }
                    break
                }
                val child = try {
                    node.getChild(i)
                } catch (e: Exception) {
                    null
                }
                if (child != null) {
                    traverse(child)
                    try {
                        child.recycle()
                    } catch (e: Exception) {}
                }
            }
        }
        
        traverse(rootNode)
        return result
    }
}
