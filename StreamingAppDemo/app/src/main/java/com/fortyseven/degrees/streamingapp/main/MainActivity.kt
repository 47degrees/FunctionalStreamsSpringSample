package com.fortyseven.degrees.streamingapp.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.fortyseven.degrees.streamingapp.R
import com.fortyseven.degrees.streamingapp.home.HomeFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, HomeFragment())
                .commit()
        }
    }
}
