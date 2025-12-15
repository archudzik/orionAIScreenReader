package co.teina.orion

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            // Always allow navigating to Accessibility settings
            findPreference<Preference>("pref_enable_orion")
                ?.setOnPreferenceClickListener {
                    startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    )
                    true
                }

            // Always allow close settings
            findPreference<Preference>("pref_close_settings")
                ?.setOnPreferenceClickListener {
                    requireActivity().finish()
                    true
                }
        }

        override fun onResume() {
            super.onResume()
            updateEnablePreference()
        }

        private fun updateEnablePreference() {
            val enablePref = findPreference<Preference>("pref_enable_orion")
            val enabled = isAccessibilityServiceEnabled()

            if (enabled) {
                enablePref?.title = getString(R.string.pref_orion_enabled_title)
                enablePref?.summary = getString(R.string.pref_orion_enabled_summary)
            } else {
                enablePref?.title = getString(R.string.pref_enable_orion_title)
                enablePref?.summary = getString(R.string.pref_enable_orion_summary)
            }
        }

        private fun isAccessibilityServiceEnabled(): Boolean {
            val context = requireContext()
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val expectedService =
                "${context.packageName}/.MyAccessibilityService"

            return enabledServices
                .split(':')
                .any { it.equals(expectedService, ignoreCase = true) }
        }
    }
}
