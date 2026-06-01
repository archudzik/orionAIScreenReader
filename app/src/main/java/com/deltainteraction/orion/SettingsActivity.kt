package com.deltainteraction.orion

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
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

            findPreference<Preference>("pref_android_tts_settings")
                ?.setOnPreferenceClickListener {
                    val ttsSettingsIntent = Intent("com.android.settings.TTS_SETTINGS")
                    val intent = if (ttsSettingsIntent.resolveActivity(requireContext().packageManager) != null) {
                        ttsSettingsIntent
                    } else {
                        Intent(Settings.ACTION_SETTINGS)
                    }
                    startActivity(intent)
                    true
                }

            findPreference<Preference>("pref_view_diagnostics")
                ?.setOnPreferenceClickListener {
                    showDiagnostics()
                    true
                }

            findPreference<Preference>("pref_share_diagnostics")
                ?.setOnPreferenceClickListener {
                    shareDiagnostics()
                    true
                }

            findPreference<Preference>("pref_clear_diagnostics")
                ?.setOnPreferenceClickListener {
                    DiagnosticsLog.clear(requireContext())
                    Toast.makeText(
                        requireContext(),
                        R.string.diagnostics_cleared,
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }

            val voiceEngine = findPreference<ListPreference>("pref_orion_voice_engine")
            val geminiVoice = findPreference<ListPreference>("pref_orion_gemini_voice")
            val geminiVoiceInfo = findPreference<Preference>("pref_orion_gemini_voice_info")

            fun updateGeminiVoicePreferences(engine: String?) {
                val isGeminiSelected = engine == "gemini"
                geminiVoice?.isEnabled = isGeminiSelected
                geminiVoiceInfo?.isVisible = isGeminiSelected
            }

            updateGeminiVoicePreferences(voiceEngine?.value)
            voiceEngine?.setOnPreferenceChangeListener { _, newValue ->
                updateGeminiVoicePreferences(newValue as? String)
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

        private fun showDiagnostics() {
            val diagnostics = DiagnosticsLog.read(requireContext())
                .ifBlank { getString(R.string.diagnostics_empty) }
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.pref_view_diagnostics)
                .setMessage(diagnostics)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        private fun shareDiagnostics() {
            val context = requireContext()
            DiagnosticsLog.append(context, "Diagnostics export requested.")
            val diagnostics = DiagnosticsLog.read(context)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.diagnostics_share_subject))
                putExtra(Intent.EXTRA_TEXT, diagnostics)
            }
            try {
                startActivity(Intent.createChooser(intent, getString(R.string.pref_share_diagnostics)))
            } catch (_: ActivityNotFoundException) {
                AlertDialog.Builder(context)
                    .setTitle(R.string.pref_share_diagnostics)
                    .setMessage(R.string.diagnostics_share_unavailable)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }
}
