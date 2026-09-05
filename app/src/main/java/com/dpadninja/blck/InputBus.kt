package com.dpadninja.blck

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow


object InputBus {
    enum class Event {
        USER_INPUT,
        USER_KEY_UP,
        SYSTEM_NAVIGATION,
        BLACKOUT_TOGGLE,
    }

    private val _events = MutableSharedFlow<Event>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<Event> = _events

    val monitorConnected = MutableStateFlow(false)

    val foregroundPackage = MutableStateFlow<String?>(null)

    val keyEventCount = MutableStateFlow(0)
    val uiEventCount = MutableStateFlow(0)

    fun post(event: Event) {
        _events.tryEmit(event)
    }

}
