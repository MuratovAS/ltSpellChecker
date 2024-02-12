package org.softcatala.corrector

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val steps = resources.getString(R.string.install_steps)
        var textView = findViewById<TextView>(R.id.textViewSteps)
        textView.text = steps

        val limitations = resources.getString(R.string.app_limitations)
        textView = findViewById(R.id.textViewLimitations)
        textView.text = limitations

        val okButton = findViewById<Button>(R.id.buttonClose)
        val text = resources.getString(R.string.button_close)
        okButton.text = text
        okButton.setOnClickListener { v: View? -> finish() }
    }
}
