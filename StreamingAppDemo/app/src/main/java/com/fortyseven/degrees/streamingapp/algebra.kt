package com.fortyseven.degrees.streamingapp

import io.reactivex.Observable
import memeid.UUID

data class User(val id: UUID, val name: String) {
    companion object
}

interface AccountRepo {
    fun fetchAccounts(): Observable<List<User>>
}

interface AccountPersistence {
    fun loadAccountsFromDatabase(): Observable<List<User>>
}

interface HomeInteractions {
    fun pullToRefresh(): Observable<Unit>
}

interface HomeDependencies : HomeInteractions, AccountRepo, AccountPersistence, RxViewModel<HomeViewState> {
    companion object {
        fun create(
            interactions: HomeInteractions,
            repo: AccountRepo,
            persistence: AccountPersistence,
            viewModel: RxViewModel<HomeViewState>
        ): HomeDependencies = object : HomeDependencies,
            HomeInteractions by interactions,
            AccountRepo by repo,
            AccountPersistence by persistence,
            RxViewModel<HomeViewState> by viewModel {}
    }
}
