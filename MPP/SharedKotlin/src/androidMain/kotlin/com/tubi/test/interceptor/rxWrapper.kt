package com.tubi.test.interceptor

actual fun createRxComplete(onSubscribe: () -> Unit): RxWrapper {
    return RxWrapper()
}

actual fun createRxEndless(getNext: () -> Any): RxWrapper {
    return RxWrapper()
}

actual class RxWrapper {

    actual fun subscribeOn(thread: Int): RxWrapper {
        //mRxJavaBridge.subscribeOn(thread)
        return this
    }

    actual fun subscribe(onComplete: () -> Unit): RxWrapper {
        //mRxJavaBridge.subscribe(onComplete)
        return this
    }

    actual fun distinctUntilChanged(): RxWrapper {
        //mRxJavaBridge.distinctUntilChanged()
        return this
    }

    actual fun debounce(
        debounceCheck: (t: Any) -> Int
    ): RxWrapper {
        //mRxJavaBridge.conditionBuffer(bufferMillisecondTime, bufferCheck)
        return this
    }

    /*actual fun subscribe(
        onSuccessWithLock: (t: Any) -> Any?, onSuccess: (t: Any?) -> Unit
    ): RxWrapper {
        //mRxJavaBridge.subscribe(onSuccessWithLock, onSuccess)
        return this
    }*/
}
