package com.tubi.test.interceptor

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.newFixedThreadPoolContext

internal actual val backgroundDispatcher: CoroutineDispatcher =
    newFixedThreadPoolContext(1, "coroutine")//Dispatchers.IO