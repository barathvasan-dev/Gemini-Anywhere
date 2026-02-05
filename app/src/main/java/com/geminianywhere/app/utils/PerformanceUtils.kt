package com.geminianywhere.app.utils

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Performance optimizer for accessibility events
 * Implements debouncing to reduce unnecessary processing
 */
class AccessibilityEventDebouncer(
    private val delayMs: Long = 300L,
    private val onEvent: (String) -> Unit
) {
    companion object {
        private const val TAG = "EventDebouncer"
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private var pendingRunnable: Runnable? = null
    private var lastText: String = ""
    private var lastProcessedText: String = ""
    
    /**
     * Debounce text change events
     * Only processes if text actually changed and after delay period
     */
    fun onTextChanged(newText: String) {
        // Cancel pending event
        pendingRunnable?.let { handler.removeCallbacks(it) }
        
        // Skip if text hasn't changed
        if (newText == lastText) {
            return
        }
        
        lastText = newText
        
        // Schedule new event
        pendingRunnable = Runnable {
            if (newText != lastProcessedText) {
                lastProcessedText = newText
                onEvent(newText)
                Log.d(TAG, "Event processed after debounce: ${newText.take(50)}")
            }
        }
        
        handler.postDelayed(pendingRunnable!!, delayMs)
    }
    
    /**
     * Force immediate processing
     */
    fun flush() {
        pendingRunnable?.let {
            handler.removeCallbacks(it)
            it.run()
        }
    }
    
    /**
     * Cancel all pending events
     */
    fun cancel() {
        pendingRunnable?.let { handler.removeCallbacks(it) }
        pendingRunnable = null
    }
    
    /**
     * Reset state
     */
    fun reset() {
        cancel()
        lastText = ""
        lastProcessedText = ""
    }
}

/**
 * Battery optimization utilities
 */
object BatteryOptimizer {
    private const val TAG = "BatteryOptimizer"
    
    /**
     * Calculate optimal event processing rate based on battery level
     */
    fun getOptimalDebounceDelay(batteryLevel: Int): Long {
        return when {
            batteryLevel < 15 -> 500L  // Low battery: slower updates
            batteryLevel < 30 -> 400L  // Medium battery: moderate updates
            else -> 300L               // Good battery: normal updates
        }
    }
    
    /**
     * Check if device should throttle background operations
     */
    fun shouldThrottle(batteryLevel: Int, isCharging: Boolean): Boolean {
        return !isCharging && batteryLevel < 20
    }
    
    /**
     * Log battery-related optimization decisions
     */
    fun logOptimization(action: String, batteryLevel: Int) {
        Log.d(TAG, "Battery optimization: $action (level: $batteryLevel%)")
    }
}
