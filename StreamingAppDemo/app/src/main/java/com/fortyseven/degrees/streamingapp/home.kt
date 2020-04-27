package com.fortyseven.degrees.streamingapp

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

sealed class HomeViewState {
    object Idle : HomeViewState()
    object Loading : HomeViewState()
    data class Full(val items: List<User>) : HomeViewState()
    data class Error(val t: Throwable) : HomeViewState()
}

// Our home program exist out of refreshing items on P2R & loading initial data
fun HomeDependencies.program(): Observable<Unit> =
    Observable.merge(refreshAccountsOnPull(), loadStartUpData())

fun HomeDependencies.refreshAccountsOnPull(): Observable<Unit> =
    pullToRefresh().switchMap {
        loadAccounts()
            .fork(Schedulers.io(), lifecycleVM)
            .void()
    }

fun HomeDependencies.loadAccounts(): Observable<Unit> =
    post(HomeViewState.Loading).flatMap {
        loadAccountsFromDatabase().map(HomeViewState::Full).flatMap { post(it) }
            .onErrorResumeNext { _: Throwable -> loadAccountsFromNetwork() }
            .switchIfEmpty(loadAccountsFromNetwork())
            .fork(Schedulers.io(), lifecycleVM)
            .void()
    }

fun HomeDependencies.loadAccountsFromNetwork(): Observable<Unit> =
    fetchAccounts().map(HomeViewState::Full)
        .flatMap { post(it) }
        .onErrorResumeNext { t: Throwable -> post(HomeViewState.Error(t)) }

// Only load start-up data if empty
fun HomeDependencies.loadStartUpData(): Observable<Unit> =
    isEmpty().flatMap { isEmpty ->
        if (isEmpty) loadAccounts()
        else Observable.empty()
    }