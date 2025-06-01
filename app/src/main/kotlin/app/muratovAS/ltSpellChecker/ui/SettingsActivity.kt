package app.muratovAS.ltSpellChecker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import app.muratovAS.ltSpellChecker.R
import app.muratovAS.ltSpellChecker.databinding.ActivitySettingsBinding
import app.muratovAS.ltSpellChecker.ui.fragments.SettingsFragment

/**
 * Spell checker preference screen.
 */
open class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.frame_settings, SettingsFragment())
            }
        }
    }
}