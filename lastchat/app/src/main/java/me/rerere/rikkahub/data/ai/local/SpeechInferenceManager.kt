package me.rerere.rikkahub.data.ai.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.rerere.common.inference.LocalInferenceManager
import me.rerere.common.inference.LocalInferenceQueueState
import me.rerere.common.inference.LocalInferenceWorkload

/**
 * Absencium: the only on-device inference left is speech recognition, so one lock at a time is all the admission control needed
 * (LastChat's manager also handled on-device chat models, which are not part of Absencium).
 */
class SpeechInferenceManager : LocalInferenceManager {
    private val lock = Mutex()
    override val queueState: StateFlow<LocalInferenceQueueState> = MutableStateFlow(LocalInferenceQueueState())

    override suspend fun <T> withLease(workload: LocalInferenceWorkload, modelId: String, block: suspend () -> T): T =
        lock.withLock { block() }

    override fun requestEviction(workload: LocalInferenceWorkload?) = Unit
}
