package com.deltainteraction.orion

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, SettingsActivity::class.java))
        finish()
    }
}
