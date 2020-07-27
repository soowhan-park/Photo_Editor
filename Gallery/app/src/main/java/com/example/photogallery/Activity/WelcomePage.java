package com.example.photogallery.Activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.example.photogallery.R;

public class WelcomePage extends AppCompatActivity {
    private static int time = 3500;
    Handler handler;
    Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_welcome_page);

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(WelcomePage.this, MainActivity.class));
                finish();
            }
        };
        handler.postDelayed(runnable, time);
    }
}
