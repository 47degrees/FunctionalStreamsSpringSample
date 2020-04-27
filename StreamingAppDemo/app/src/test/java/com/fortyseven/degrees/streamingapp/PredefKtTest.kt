package com.fortyseven.degrees.streamingapp

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

class PredefKtTest {

    val EVAL_POOL_NAME = "EVAL_POOL_NAME"
    val OTHER = "OTHER"

    val EVAL_SCHEDULER =
        Schedulers.from(Executors.newSingleThreadExecutor(namedThreadFactory(EVAL_POOL_NAME)))

    val OTHER_SCHEDULER =
        Schedulers.from(Executors.newSingleThreadExecutor(namedThreadFactory(OTHER)))

    fun threadName(): Observable<String> =
        Observable.fromCallable { Thread.currentThread().name }

    fun assertThreadName(n: String): Observable<Unit> =
        threadName().flatMap { name ->
            Observable.fromCallable {
                assertEquals(n, name)
            }
        }

    @Test
    fun `evalOn returns by default on computation`() {
        assertThreadName(EVAL_POOL_NAME)
            .evalOn(EVAL_SCHEDULER)
            .flatMap { threadName() }
            .test()
            .await()
            .assertValue { it.startsWith("RxComputationThreadPool") }
            .assertComplete()
    }

    @Test
    fun `evalOn returns on specified Scheduler`() {
        assertThreadName(EVAL_POOL_NAME)
            .evalOn(EVAL_SCHEDULER, returnOn = OTHER_SCHEDULER)
            .flatMap { threadName() }
            .test()
            .await()
            .assertValue(OTHER)
            .assertComplete()
    }
}

fun namedThreadFactory(name: String) = ThreadFactory { r: Runnable ->
    Thread(r, name)
}