package com.fortyseven.degrees.streamingapp.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fortyseven.degrees.streamingapp.*
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.uber.autodispose.android.lifecycle.autoDispose
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.android.synthetic.main.viewholder_user.view.*
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
                view.pullToRefresh.refreshes().evalOn(AndroidSchedulers.mainThread())
        }

        val adapter = UserAdapter()
        view.list.adapter = adapter

        Observable.merge(
            // Run business logic
            HomeDependency.create(interactions, repo, persistence, viewModel)
                .program().subscribeOn(Schedulers.computation()),

            // Run view rendering
            viewModel.state().flatMap { s ->
                render(view, adapter, s).evalOn(AndroidSchedulers.mainThread())
            }
        ) // Run together and bind to view lifecycle.
            .autoDispose(viewLifecycleOwner)
            .subscribe()
    }

    // This can only be tested with pixel tests, or visual inspection.
    private fun render(
        view: View,
        adapter: UserAdapter,
        state: HomeVM
    ): Observable<Unit> =
        Observable.fromCallable {
            when (state) {
                HomeVM.Empty -> Snackbar.make(
                    view,
                    "Design nice empty state",
                    BaseTransientBottomBar.LENGTH_SHORT
                ).show()
                HomeVM.Loading -> Unit // Use default loading ad
                is HomeVM.Full -> adapter.submitList(state.items)
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

data class User(val id: UUID, val name: String) {
    companion object
}

interface AccountRepo {
    fun fetchAccounts(): Observable<List<User>>
}

fun MockRepository(accounts: Observable<List<User>>? = null) = object : AccountRepo {
    override fun fetchAccounts(): Observable<List<User>> =
        accounts ?: Observable.fromCallable {
            listOf(
                User(UUID.V4.squuid(), "Simon"),
                User(UUID.V4.squuid(), "Raul"),
                User(UUID.V4.squuid(), "Jorge")
            )
        }.delay(1300, TimeUnit.MILLISECONDS)
}

interface AccountPersistence {
    fun readAccounts(): Observable<List<User>>
}

fun MockPersistence(accounts: Observable<List<User>>? = null) = object : AccountPersistence {
    override fun readAccounts(): Observable<List<User>> =
        accounts ?: Observable.fromCallable {
            listOf(
                User(UUID.V4.squuid(), "Simon"),
                User(UUID.V4.squuid(), "Raul"),
                User(UUID.V4.squuid(), "Jorge")
            )
        }.delay(700, TimeUnit.MILLISECONDS)
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
        loadAccountsFromNetwork()
            .fork(Schedulers.io(), lifecycleVM)
            .void()
    }

fun HomeDependency.loadAccountsFromNetwork(): Observable<Unit> =
    post(HomeVM.Loading).flatMap {
        fetchAccounts().map(HomeVM::Full)
            .flatMap { post(it) }
            .onErrorResumeNext { t: Throwable -> post(HomeVM.Error(t)) }
    }

// Only load start-up data if empty
fun HomeDependency.loadStartUpData(): Observable<Unit> =
    isEmpty().flatMap { isEmpty ->
        if (isEmpty) readAccounts().map(HomeVM::Full).flatMap { post(it) }
            .switchIfEmpty(loadAccountsFromNetwork())
            .onErrorResumeNext { _: Throwable -> loadAccountsFromNetwork() }
            .fork(Schedulers.io(), lifecycleVM)
            .void()
        else Observable.empty()
    }

// DiffUtil.ItemCallback is stateless thus can be object
object UserDiffUtil : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean =
        oldItem == newItem
}

val AsyncUserDiffUtil: AsyncDifferConfig<User> =
    AsyncDifferConfig.Builder(UserDiffUtil).build()

class UserAdapter : ListAdapter<User, UserViewHolder>(AsyncUserDiffUtil) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder =
        UserViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.viewholder_user, parent, false)
        )

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) =
        holder.bind(getItem(position))
}

class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(user: User): Unit {
        itemView.user_id.text = user.id.toString()
        itemView.user_name.text = user.name
    }
}