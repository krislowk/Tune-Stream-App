package com.vynce.music.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

fun <T> Flow<T>.collectLatest(scope: CoroutineScope, action: suspend (T) -> Unit) {
    scope.launch {
        this@collectLatest.collectLatest(action)
    }
}











