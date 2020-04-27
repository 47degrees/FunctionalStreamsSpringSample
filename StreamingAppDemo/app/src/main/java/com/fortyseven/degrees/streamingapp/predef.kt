package com.fortyseven.degrees.streamingapp

import android.os.Looper
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import arrow.core.Either
import arrow.core.Option
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.ReplaySubject
import com.uber.autodispose.android.lifecycle.autoDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

data class ObservableFiber<A>(val join: Observable<A>, val cancel: Disposable)

/**
 * Fork an [Observable] to run within its own [ObservableFiber].
 * This allows you to de-couple an [Observable], from the original [Disposable].
 *
 * This is useful for long running tasks, that need to run regardless if the [Observable] gets disposed.
 * i.e. when a view bound task wants to run a network operation that shouldn't be autodisposed by the view.
 */
fun <A> Observable<A>.fork(
    scheduler: Scheduler,
    lifecycle: LifecycleOwner
): Observable<ObservableFiber<A>> =
    Observable.create { emitter ->
        val s: ReplaySubject<A> = ReplaySubject.create()

        fun fork(): Unit {
            val conn: Disposable =
                subscribeOn(scheduler)
                    .autoDispose(lifecycle)
                    .subscribe(s::onNext, s::onError, s::onComplete)

            emitter.onNext(ObservableFiber(s.hide(), conn))
            emitter.onComplete()
        }

        // AutoDispose only works on main, we need to shift back if not there.
        if (Looper.myLooper() == Looper.getMainLooper()) {
            fork()
        } else {
            AndroidSchedulers.mainThread().scheduleDirect {
                fork()
            }
        }
    }

/** Ignore output **/
fun <A> Observable<A>.void(): Observable<Unit> =
    map { Unit }

// Cached wrapped Unit
val unit: Observable<Unit> = Observable.just(Unit)

/**
 * Similar to other automatically shifting operators in RxJava,
 * `evalOn` can be used to run the [Observable] on a given [Scheduler],
 *  and to return on another given [Scheduler], or [Schedulers.computation] by default.
 */
fun <A> Observable<A>.evalOn(
    scheduler: Scheduler,
    returnOn: Scheduler = Schedulers.computation()
): Observable<A> = unit.observeOn(scheduler)
    .flatMap { this }
    .observeOn(returnOn)

fun <A> Observable<A>.debug(f: (A) -> String): Observable<A> =
    flatMap { a ->
        (if (BuildConfig.DEBUG) Observable.fromCallable { Log.d("DEBUG", f(a)) }
        else unit).map { a }
    }

fun log(f: () -> String): Observable<Unit> =
    Observable.fromCallable {
        if (BuildConfig.DEBUG) Log.d("DEBUG", f())
        Unit
    }

fun <A, B> eitherPar(
    fa: Observable<A>,
    fb: Observable<B>,
    scheduler: Scheduler = Schedulers.computation()
): Observable<Either<A, B>> = Observable.merge(
    fa.map { Either.Left(it) }.subscribeOn(scheduler),
    fb.map { Either.Right(it) }.subscribeOn(scheduler)
)

fun <A, B> Observable<A>.filterMap(f: (A) -> Option<B>): Observable<B> =
    flatMap { a ->
        f(a).fold(
            ifEmpty = { Observable.empty<B>() },
            ifSome = { b -> Observable.just(b) }
        )
    }

