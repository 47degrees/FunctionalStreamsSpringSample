package com.fortyseven.degrees.streamingapp

import arrow.fx.typeclasses.milliseconds
import io.reactivex.Observable
import memeid.UUID
import java.util.concurrent.TimeUnit

fun MockRepository(accounts: Observable<List<User>>? = null) =
    object : AccountRepo {
        override fun fetchAccounts(): Observable<List<User>> =
            accounts ?: Observable.fromCallable {
                listOf(
                    User(UUID.V4.squuid(), "Simon"),
                    User(UUID.V4.squuid(), "Raul"),
                    User(UUID.V4.squuid(), "Jorge")
                )
            }.delay(1300, TimeUnit.MILLISECONDS)
    }

fun MockPersistence(accounts: Observable<List<User>>? = null) =
    object : AccountPersistence {
        override fun loadAccountsFromDatabase(): Observable<List<User>> =
            accounts ?: Observable.fromCallable {
                listOf(
                    User(UUID.V4.squuid(), "Simon"),
                    User(UUID.V4.squuid(), "Raul"),
                    User(UUID.V4.squuid(), "Jorge")
                )
            }.delay(700, TimeUnit.MILLISECONDS)
    }