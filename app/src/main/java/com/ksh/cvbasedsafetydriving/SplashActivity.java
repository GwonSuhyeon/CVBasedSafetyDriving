/*
 * Create by KSH on 2020. 8. 20.
 * Copyright (c) 2020. KSH. All rights reserved.
 */

package com.ksh.cvbasedsafetydriving;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Animation animation;
        animation = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.translate);

        ImageView move = (ImageView)findViewById(R.id.move);

        Handler handler = new Handler();
        handler.postDelayed(new SplashHandler(), 2000);

        move.startAnimation(animation);
    }

    private class SplashHandler implements Runnable{
        public void run()
        {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            SplashActivity.this.finish();
        }
    }

    @Override
    public void onBackPressed()
    {

    }
}