package com.deltainteraction.orion

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isAccessibilityServiceEnabled()) {
            startActivity(Intent(this, SettingsActivity::class.java))
        } else {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        finish()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = "$packageName/.MyAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices
            .split(':')
            .any { it.equals(expectedComponent, ignoreCase = true) }
    }
}
