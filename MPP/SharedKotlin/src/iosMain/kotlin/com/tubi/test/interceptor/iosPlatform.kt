package com.tubi.test.interceptor

import kotlin.native.concurrent.freeze

interface IosBridge {
    fun signWithSHA256(pemKey: String, message: String): String
    fun base64URLEncodedString(string: String): String
    fun currentMillisecondsSince1970(): Long
    fun gzip(data: String): ByteArray
}

lateinit var mIosBridge: IosBridge

fun setIOSBridge(bridge: IosBridge) {
    print("mIosBridge")
    mIosBridge = bridge.freeze()
}

actual fun signWithSHA256(pemKey: String, message: String): String {
    return mIosBridge.signWithSHA256(pemKey, message)
}

actual fun percentEscaped(string: String): String {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
}

actual fun base64(string: String): String {
    return mIosBridge.base64URLEncodedString(string)
}

actual fun base64URLEncodedString(string: String): String {
    return mIosBridge.base64URLEncodedString(string)
}

actual fun currentMillisecondsSince1970(): Long {
    return mIosBridge.currentMillisecondsSince1970()
}

actual fun gzip(data: String): ByteArray {
    return mIosBridge.gzip(data)
}
