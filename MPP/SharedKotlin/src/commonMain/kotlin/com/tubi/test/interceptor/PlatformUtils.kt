package com.tubi.test.interceptor

typealias RxObjectHandle = Int

expect fun createRxComplete(onSubscribe: () -> Unit): RxWrapper

expect fun createRxEndless(getNext: () -> Any): RxWrapper

expect fun signWithSHA256(pemKey: String, message: String): String

expect fun percentEscaped(string: String): String

expect fun base64URLEncodedString(string: String): String

expect fun base64(string: String): String

expect fun currentMillisecondsSince1970(): Long

expect fun gzip(data: String): ByteArray
