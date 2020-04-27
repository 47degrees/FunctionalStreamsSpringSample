package com.fortyseven.degrees.streamingapp

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.reactivex.Observable
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.ReplaySubject
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * This implementation can be used to mimick a [LifecycleOwner].
 */
class TestLifecycleOwner(initial: Lifecycle.Event? = null) : LifecycleOwner {

    private val lifecycle = LifecycleRegistry(this)

    init {
        initial?.let(lifecycle::handleLifecycleEvent)
    }

    fun onCreate() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onStart() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    fun onResume() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onPause() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    fun onStop() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    fun onDestroy() {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun getLifecycle() = lifecycle
}

/**
 * An RxViewModel that uses an `ReplaySubject` instead of an `BehaviorSubject`.
 * This allows for inspecting history
 */
class TestRxViewModel<A>(
    private val default: A? = null,
    owner: LifecycleOwner = TestLifecycleOwner()
) : RxViewModel<A> {

    val _state: ReplaySubject<A> = ReplaySubject.create()

    init {
        default?.let { _state.onNext(default) }
    }

    override val lifecycleVM: LifecycleOwner = owner

    override fun state(): Observable<A> =
        _state.hide()

    override fun post(a: A): Observable<Unit> =
        Observable.fromCallable { _state.onNext(a) }

    override fun isEmpty(): Observable<Boolean> =
        Observable.fromCallable {
            _state.hasValue() && _state.value == default
        }
}

/**
 * Testing rule to override the Android Main thread.
 * Other threads are not mocked or replaced, we want to test the actual business logic.
 * By mocking pools, you're not testing your concurrent code!!!
 *
 * This can thus result in runtime bugs, deadlocks can be hard to track/debug on Android in the wild.
 */
class AndroidMainSchedulerRule : TestRule {
    override fun apply(base: Statement, d: Description?): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                RxAndroidPlugins.setInitMainThreadSchedulerHandler { Schedulers.trampoline() }
                try {
                    base.evaluate()
                } finally {
                    RxAndroidPlugins.reset()
                }
            }
        }
    }
}