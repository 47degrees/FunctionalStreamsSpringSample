package com.fortyseven.degrees.streamingapp

import android.app.Application
import com.akaita.java.rxjava2debug.RxJava2Debug

class StreamApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable RxJava assembly stack collection, to make RxJava crash reports clear and unique
        // Make sure this is called AFTER setting up any Crash reporting mechanism as Crashlytics
        RxJava2Debug.enableRxJava2AssemblyTracking()
    }
}