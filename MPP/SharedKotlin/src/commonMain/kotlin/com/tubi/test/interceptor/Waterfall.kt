package com.tubi.test.interceptor

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.selects.select

internal class Waterfall(val cloudFolderName: String) {

    companion object {
        private const val BUFFER_TIME = 1000L //3s
    }

    val mGoogleDrive = GoogleDrive()
    var mCloudFolderId: String? = null
    var mCount = 0

    var mInputChannel: Channel<String> = Channel<String>(UNLIMITED)
    var mBufferChannelSize = 0
    val mBufferChannel: Channel<String> = Channel<String>(UNLIMITED)

    val mErrorHandler = CoroutineExceptionHandler { _, throwable ->
        println(throwable.message)
    }

    suspend fun start(context: Any?) {

        println("====================>Waterfall.start")
        CoroutineScope(backgroundDispatcher).launch(mErrorHandler) {
            println("start waterfall in commonMain")
            val folderId = mGoogleDrive.createDir(context, cloudFolderName)
            mCloudFolderId = folderId

            bufferChannelPumper(mInputChannel)
        }

        CoroutineScope(backgroundDispatcher).launch {
            for (i in 1..4) {
                println("send $i")
                delay(800)
                mInputChannel.send(i.toString() + "hello111")
            }

            delay(2000)
            for (i in 100..104) {
                println("send $i")
                delay(300)
                mInputChannel.send(i.toString() + "===>hello222")
            }

            delay(5000)
            for (i in 200..208) {
                println("send $i")
                delay(500)
                mInputChannel.send(i.toString() + "===>hello1234567788")
            }
        }
    }

    suspend fun CoroutineScope.bufferChannelPumper(input: ReceiveChannel<String>) {
        var currentBufferDefer: Deferred<Int>? = null
        while (isActive) { // loop while not cancelled/closed
            currentBufferDefer = select<Deferred<Int>?> {
                currentBufferDefer?.onAwait { _ ->
                    // Timeout
                    println("time out")
                    println("")
                    dumpToGoogleDrive()
                    mBufferChannelSize = 0
                    null
                }
                input.onReceive { newString ->
                    println("recv:$newString")
                    var current = currentBufferDefer
                    mBufferChannelSize += (newString.length)
                    mBufferChannel.send(newString)
                    if (mBufferChannelSize < 20000) {
                        if (current == null) {
                            println("start new timer")
                            current = async { delay(BUFFER_TIME); 1 }
                        }
                    } else {
                        println("send buffer immediately")
                        current = async { delay(0); 1 }
                    }
                    current
                }
            }
        }
        println("quit bufferChannelPumper!!!!!!!!!!!")
    }

    private fun dumpToGoogleDrive() {

        println("dumpToGoogleDrive")
        val drive = mGoogleDrive
        val cloudFolderId = mCloudFolderId
        if (drive == null || cloudFolderId.isNullOrBlank()) {
            return
        }

        var jsonString = mBufferChannel.poll()
        val stringBuffer = StringBuilder()
        var jsonCount = 0
        stringBuffer.append("[")
        while (jsonString != null) {
            if (jsonCount > 0) {
                stringBuffer.append(",")
            }
            println("sendbuffer=$jsonString")
            stringBuffer.append(jsonString)
            jsonCount++
            mBufferChannelSize -= jsonString.length
            jsonString = mBufferChannel.poll()
        }
        stringBuffer.append("]")

        if (jsonCount > 0) {
            val name = "http_package_$mCount.txt"
            mCount++

            CoroutineScope(backgroundDispatcher).launch(mErrorHandler) {
                println("uploadToCloud size=${stringBuffer.length}")
                val tempId = drive.uploadToCloud(cloudFolderId, stringBuffer.toString(), name)
                if (tempId != null) {
                    drive.renameCloudFile(tempId, name)
                }
            }
        }
    }

    suspend fun inflow(jsonString: String) {
        mInputChannel.send(jsonString)
    }
}
