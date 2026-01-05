package com.example.movinghacker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        TextView title = findViewById(R.id.splash_title);
        TextView subtitle = findViewById(R.id.splash_subtitle);

        animateIn(title, 0);
        animateIn(subtitle, 80);

        title.postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, 950);
    }

    private void animateIn(View view, long startDelayMs) {
        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setStartDelay(startDelayMs)
                .setDuration(520)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }
}
