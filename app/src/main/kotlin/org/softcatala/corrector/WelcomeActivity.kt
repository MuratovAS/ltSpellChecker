package org.softcatala.corrector

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val okButton = findViewById<Button>(R.id.buttonClose)
        okButton.setOnClickListener { v: View? -> finish() }

        val settingsButton = findViewById<Button>(R.id.buttonSettings)
        settingsButton.setOnClickListener { v: View? ->
            val intent = Intent(this, SpellCheckerSettingsActivity::class.java)
            startActivity(intent)
        }
    }
}
