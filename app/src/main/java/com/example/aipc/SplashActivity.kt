package com.example.aipc

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var handler: Handler
    private lateinit var fadeOutRunnable: Runnable
    private lateinit var splashLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        splashLayout = findViewById(R.id.splashLayout)
        handler = Handler(Looper.getMainLooper())

        fadeOutRunnable = Runnable {
            startFadeOutAnimation()
        }
        handler.postDelayed(fadeOutRunnable, 5000)

        splashLayout.setOnClickListener {
            handler.removeCallbacks(fadeOutRunnable)
            startFadeOutAnimation()
        }
    }

    private fun startFadeOutAnimation() {
        splashLayout.animate()
            .alpha(0f)
            .setDuration(1000)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
    }
}