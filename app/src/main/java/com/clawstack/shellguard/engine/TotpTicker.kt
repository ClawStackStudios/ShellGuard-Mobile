package com.clawstack.shellguard.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Reactive sub-second ticker model for TOTP countdown arcs and UI tickers.
 *
 * @param remainingSeconds Integer seconds remaining before the current token expires.
 * @param progress Normalized float from 1.0f (freshly minted) down to 0.0f (expired).
 */
data class TotpTick(
    val remainingSeconds: Int,
    val progress: Float
)

object TotpTicker {

    /**
     * Emits a reactive stream of [TotpTick] with sub-second polling (500ms)
     * to power smooth Canvas progress animations.
     */
    fun createTicker(periodSeconds: Long = 30L): Flow<TotpTick> = flow {
        while (true) {
            val epochSeconds = System.currentTimeMillis() / 1000L
            val elapsed = epochSeconds % periodSeconds
            val remaining = (periodSeconds - elapsed).toInt()
            val progress = remaining.toFloat() / periodSeconds.toFloat()

            emit(TotpTick(remaining, progress.coerceIn(0f, 1f)))
            delay(500L)
        }
    }
}
