package com.fortyseven.degrees.streamingapp

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers


sealed class HomeVM {
    object Idle : HomeVM()
    object Loading : HomeVM()
    data class Full(val items: List<User>) : HomeVM()
    data class Error(val t: Throwable) : HomeVM()
}

// Our home program exist out of refreshing items on P2R & loading initial data
fun HomeDependency.program(): Observable<Unit> =
    Observable.merge(refreshAccountsOnPull(), loadStartUpData())

fun HomeDependency.refreshAccountsOnPull(): Observable<Unit> =
    pullToRefresh().switchMap {
        loadAccounts()
            .fork(Schedulers.io(), lifecycleVM)
            .void()
    }

fun HomeDependency.loadAccounts(): Observable<Unit> =
    post(HomeVM.Loading).flatMap {
        loadAccountsFromDatabase().map(HomeVM::Full).flatMap { post(it) }
            .onErrorResumeNext { _: Throwable -> loadAccountsFromNetwork() }
            .switchIfEmpty(loadAccountsFromNetwork())
            .fork(Schedulers.io(), lifecycleVM)
            .void()
    }

fun HomeDependency.loadAccountsFromNetwork(): Observable<Unit> =
    fetchAccounts().map(HomeVM::Full)
        .flatMap { post(it) }
        .onErrorResumeNext { t: Throwable -> post(HomeVM.Error(t)) }

// Only load start-up data if empty
fun HomeDependency.loadStartUpData(): Observable<Unit> =
    isEmpty().flatMap { isEmpty ->
        if (isEmpty) loadAccounts()
        else Observable.empty()
    }