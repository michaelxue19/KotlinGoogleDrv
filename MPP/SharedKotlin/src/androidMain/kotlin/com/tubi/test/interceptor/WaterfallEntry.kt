package com.tubi.test.interceptor

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object WaterfallEntry {
    private val TAG = "WaterfallEntry"
    private var mWaterfall: Waterfall? = null

    val errorHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, exception.message, exception)
    }

    fun start(cloudFolderName: String) {

        CoroutineScope(backgroundDispatcher).launch(errorHandler) {
            runBlocking {
                mWaterfall = Waterfall(cloudFolderName)
                mWaterfall?.start(null)
            }
        }
    }

    fun inflow(jsonString: String) {
        CoroutineScope(backgroundDispatcher).launch(errorHandler) {
            val waterfall = mWaterfall
            if (waterfall != null) {
                waterfall.inflow(jsonString)
            }
        }
    }
}
