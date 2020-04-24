package com.fortyseven.degrees.streamingapp

import androidx.lifecycle.LifecycleOwner
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.ReplaySubject
import com.uber.autodispose.android.lifecycle.autoDispose

data class ObservableFiber<A>(val join: Observable<A>, val cancel: Disposable)

fun <A> Observable<A>.fork(
    scheduler: Scheduler,
    lifecycle: LifecycleOwner
): Observable<ObservableFiber<A>> = Observable.create { emitter ->
    if (!emitter.isDisposed) {
        val s: ReplaySubject<A> = ReplaySubject.create()
        val conn: Disposable =
            subscribeOn(scheduler)
                .autoDispose(lifecycle)
                .subscribe(s::onNext, s::onError, s::onComplete)


        emitter.onNext(ObservableFiber(s.hide(), conn))
        emitter.onComplete()
    }
}

fun <A> Observable<A>.void(): Observable<Unit> =
    map { Unit }