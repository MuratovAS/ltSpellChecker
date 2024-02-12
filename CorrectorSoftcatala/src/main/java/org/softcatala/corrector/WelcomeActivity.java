package org.softcatala.corrector;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        String steps = getResources().getString(R.string.install_steps);
        TextView textView = findViewById(R.id.textViewSteps);
        textView.setText(steps);

        String limitations = getResources().getString(R.string.app_limitations);
        textView = findViewById(R.id.textViewLimitations);
        textView.setText(limitations);

        Button okButton = findViewById(R.id.buttonClose);
        String text = getResources().getString(R.string.button_close);
        okButton.setText(text);

        okButton.setOnClickListener(v -> finish());
    }
}
