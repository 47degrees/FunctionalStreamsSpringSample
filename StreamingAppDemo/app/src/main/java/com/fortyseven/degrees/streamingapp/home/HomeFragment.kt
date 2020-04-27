package com.fortyseven.degrees.streamingapp.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortyseven.degrees.streamingapp.*
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding3.swiperefreshlayout.refreshes
import com.uber.autodispose.android.lifecycle.autoDispose
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_home.view.*

class HomeFragment : Fragment() {

    private val viewModel = RxViewModel<HomeVM>(this, HomeVM.Idle)
    private val repo = MockRepository()
    private val persistence = MockPersistence()

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
                HomeVM.Idle -> {
                    view.pullToRefresh.isRefreshing = false
                }
                HomeVM.Loading -> {
                    view.pullToRefresh.isRefreshing = true
                } // Use default loading ad
                is HomeVM.Full -> {
                    view.pullToRefresh.isRefreshing = false
                    adapter.submitList(state.items)
                }
                is HomeVM.Error -> {
                    view.pullToRefresh.isRefreshing = false
                    Snackbar.make(view, getString(R.string.error, state.t), BaseTransientBottomBar.LENGTH_SHORT).show()
                }
            }
        }
}