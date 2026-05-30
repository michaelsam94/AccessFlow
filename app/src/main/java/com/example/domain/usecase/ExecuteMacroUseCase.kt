package com.example.domain.usecase

import com.example.data.MacroStepEntity
import kotlinx.coroutines.delay

interface GestureDispatcher {
    fun click(xPercent: Float, yPercent: Float): Boolean
    fun longClick(xPercent: Float, yPercent: Float): Boolean
    fun scrollUp(): Boolean
    fun scrollDown(): Boolean
}

class ExecuteMacroUseCase {
    /**
     * Dispatches a sequence of macro steps sequentially with appropriate timing buffers.
     */
    suspend fun execute(steps: List<MacroStepEntity>, dispatcher: GestureDispatcher) {
        for (step in steps) {
            when (step.actionType.uppercase()) {
                "CLICK" -> {
                    dispatcher.click(step.xPercent, step.yPercent)
                }
                "LONG_CLICK" -> {
                    dispatcher.longClick(step.xPercent, step.yPercent)
                }
                "SCROLL_UP" -> {
                    dispatcher.scrollUp()
                }
                "SCROLL_DOWN" -> {
                    dispatcher.scrollDown()
                }
                "WAIT" -> {
                    // Waiting action handled by delayMs
                }
            }
            if (step.delayMs > 0) {
                delay(step.delayMs)
            }
        }
    }
}
