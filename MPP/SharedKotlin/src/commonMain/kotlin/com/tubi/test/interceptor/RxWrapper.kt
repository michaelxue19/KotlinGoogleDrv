package com.tubi.test.interceptor

expect class RxWrapper {

    fun subscribeOn(thread: Int): RxWrapper

    fun subscribe(onComplete: () -> Unit): RxWrapper

    fun distinctUntilChanged(): RxWrapper

    fun debounce(
        debounceCheck: (t: Any) -> Int
    ): RxWrapper

    /*fun subscribe(
        onSuccessWithLock: (t: Any) -> Any?, onSuccess: (t: Any?) -> Unit
    ): RxWrapper*/
}