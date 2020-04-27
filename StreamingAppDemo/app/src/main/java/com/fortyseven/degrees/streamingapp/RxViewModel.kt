package com.fortyseven.degrees.streamingapp

import androidx.lifecycle.LifecycleOwner
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject

interface RxViewModel<A> {

    val lifecycleVM: LifecycleOwner

    fun post(a: A): Observable<Unit>
    fun state(): Observable<A>
    fun isEmpty(): Observable<Boolean>
    fun isNotEmpty(): Observable<Boolean> =
        isEmpty().map(Boolean::not)
}

fun <A> RxViewModel(lifecycle: LifecycleOwner, default: A? = null): RxViewModel<A> =
    object : RxViewModel<A> {
        private val _state: BehaviorSubject<A> =
            default?.let { BehaviorSubject.createDefault(it) } ?: BehaviorSubject.create()

        override val lifecycleVM: LifecycleOwner = lifecycle

        override fun state(): Observable<A> = _state.hide()

        override fun post(a: A): Observable<Unit> =
            Observable.fromCallable { _state.onNext(a) }

        override fun isEmpty(): Observable<Boolean> =
            Observable.fromCallable {
                _state.hasValue() && _state.value == default
            }
    }