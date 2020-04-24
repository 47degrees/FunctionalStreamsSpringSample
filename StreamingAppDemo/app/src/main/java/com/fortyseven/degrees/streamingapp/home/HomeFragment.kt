package com.fortyseven.degrees.streamingapp.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortyseven.degrees.streamingapp.R
import com.fortyseven.degrees.streamingapp.RxViewModel
import com.fortyseven.degrees.streamingapp.fork
import com.fortyseven.degrees.streamingapp.void
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.uber.autodispose.android.lifecycle.autoDispose
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import memeid.UUID
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    val viewModel = RxViewModel<HomeVM>(this, HomeVM.Empty)
    val repo = MockRepository()
    val persistence = MockPersistence()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val interactions = object : HomeInteractions {
            override fun pullToRefresh(): Observable<Unit> =
                view.pullToRefresh.refreshes()
        }

        Observable.merge(
            HomeDependency.create(interactions, repo, persistence, viewModel)
                .program().subscribeOn(Schedulers.computation()),
            viewModel.state().flatMap { s -> render(view, s) }
                .subscribeOn(AndroidSchedulers.mainThread())
        ).autoDispose(viewLifecycleOwner).subscribe()
    }

    // This can only be tested with pixel tests, or visual inspection.
    private fun render(view: View, state: HomeVM): Observable<Unit> = Observable.fromCallable {
        when (state) {
            HomeVM.Empty -> TODO("Show empty state")
            HomeVM.Loading -> TODO("Show loading state")
            is HomeVM.Full -> TODO("Show items in list & stop loading")
            is HomeVM.Error -> Snackbar.make(
                view,
                "Something went wrong: ${state.t}",
                BaseTransientBottomBar.LENGTH_SHORT
            ).show()
        }
    }
}

sealed class HomeVM {
    object Empty : HomeVM()
    object Loading : HomeVM()
    data class Full(val items: List<User>) : HomeVM()
    data class Error(val t: Throwable) : HomeVM()
}

interface HomeInteractions {
    fun pullToRefresh(): Observable<Unit>
}

data class User(val id: UUID, val name: String)

interface AccountRepo {
    fun fetchAccounts(): Observable<List<User>>
}

fun MockRepository() = object : AccountRepo {
    override fun fetchAccounts(): Observable<List<User>> =
        Observable.just(
            listOf(
                User(UUID.V1.next(), "Simon"),
                User(UUID.V1.next(), "Raul"),
                User(UUID.V1.next(), "Jorge")
            )
        ).delay(1300, TimeUnit.MILLISECONDS)
}

interface AccountPersistence {
    fun readAccounts(): Observable<List<User>>
}

fun MockPersistence() = object : AccountPersistence {
    override fun readAccounts(): Observable<List<User>> =
        Observable.just(
            listOf(
                User(UUID.V1.next(), "Simon"),
                User(UUID.V1.next(), "Raul"),
                User(UUID.V1.next(), "Jorge")
            )
        ).delay(700, TimeUnit.MILLISECONDS)
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

// Our home program exist out of refreshing items on P2R & loading initial data
fun HomeDependency.program(): Observable<Unit> =
    Observable.merge(refreshAccountsOnPull(), loadStartUpData())

fun HomeDependency.refreshAccountsOnPull(): Observable<Unit> =
    pullToRefresh().switchMap {
        post(HomeVM.Loading).flatMap { loadAccountsFromNetwork() }
    }

fun HomeDependency.loadAccountsFromNetwork(): Observable<Unit> =
    fetchAccounts().map(HomeVM::Full).flatMap { post(it) }
        .onErrorResumeNext { t: Throwable -> post(HomeVM.Error(t)) }
        .fork(Schedulers.io(), lifecycleVM)
        .void()

// Only load start-up data if empty
fun HomeDependency.loadStartUpData(): Observable<Unit> =
    isNotEmpty().flatMap { isNotEmpty ->
        if (isNotEmpty) Observable.empty<Unit>()
        else readAccounts().map(HomeVM::Full).flatMap { post(it) }
            .onErrorResumeNext { _: Throwable -> loadAccountsFromNetwork() }
            .fork(Schedulers.io(), lifecycleVM)
            .void()
    }
