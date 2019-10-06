package com.tubi.test.interceptor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object WaterfallEntry {
    fun start(cloudFolderName: String) {

        CoroutineScope(backgroundDispatcher).launch {
            runBlocking {
                val waterfall = Waterfall(cloudFolderName)
                waterfall.start(null)
            }
        }
    }
}
