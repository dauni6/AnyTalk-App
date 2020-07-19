package com.twitter.twitterclone.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.twitter.twitterclone.activities.LoginActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivity(LoginActivity.newIntent(this))
        finish()
    }
}
