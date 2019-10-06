package com.tubi.test.interceptor

lateinit var mSwiftRxBridge: ITubiRxObjectBridge

interface ITubiRxObjectBridge {
    fun createRxComplete(onSubscribe: () -> Unit): RxObjectHandle
    fun createRxEndless(getNext: () -> Any): RxObjectHandle
    fun subscribeOn(handle: RxObjectHandle, thread: Int)
    fun subscribe(handle: RxObjectHandle, onComplete: () -> Unit)

    fun distinctUntilChanged(handle: RxObjectHandle)

    fun debounce(
        handle: RxObjectHandle,
        debounceCheck: (t: Any) -> Int
    )
}

fun setSwiftRxBridge(bridge: ITubiRxObjectBridge) {
    mSwiftRxBridge = bridge
}

actual fun createRxComplete(onSubscribe: () -> Unit): RxWrapper {
    val handle = mSwiftRxBridge.createRxComplete(onSubscribe)
    return RxWrapper(handle)
}

actual fun createRxEndless(getNext: () -> Any): RxWrapper {
    val handle = mSwiftRxBridge.createRxEndless(getNext)
    return RxWrapper(handle)
}

actual class RxWrapper(val mRxHandle: RxObjectHandle) {

    actual fun subscribeOn(thread: Int): RxWrapper {
        mSwiftRxBridge.subscribeOn(mRxHandle, thread)
        return this
    }

    actual fun subscribe(onComplete: () -> Unit): RxWrapper {
        mSwiftRxBridge.subscribe(mRxHandle, onComplete)
        return this
    }

    actual fun distinctUntilChanged(): RxWrapper {
        mSwiftRxBridge.distinctUntilChanged(mRxHandle)
        return this
    }

    actual fun debounce(
        debounceCheck: (t: Any) -> Int
    ): RxWrapper {
        mSwiftRxBridge.debounce(mRxHandle, debounceCheck)
        return this
    }

    /*actual fun subscribe(
        onSuccessWithLock: (t: Any) -> Any?, onSuccess: (t: Any?) -> Unit
    ): RxWrapper {
        mSwiftRxBridge.subscribe(mRxHandle, onSuccessWithLock, onSuccess)
        return this
    }*/
}
