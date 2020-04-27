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

interface HomeDependency : HomeInteractions, AccountRepo, AccountPersistence, RxViewModel<HomeVM> {
    companion object {
        fun create(
            interactions: HomeInteractions,
            repo: AccountRepo,
            persistence: AccountPersistence,
            viewModel: RxViewModel<HomeVM>
        ): HomeDependency = object : HomeDependency,
            HomeInteractions by interactions,
            AccountRepo by repo,
            AccountPersistence by persistence,
            RxViewModel<HomeVM> by viewModel {}
    }
}
