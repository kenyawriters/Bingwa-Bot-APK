
package com.onyangodatasolutions.bingwabot

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class WelcomeActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var opened = false

    private val openDashboard = Runnable {
        goToDashboard()
    }

    private val green = Color.rgb(0, 112, 60)
    private val darkGreen = Color.rgb(0, 75, 40)
    private val paleGreen = Color.rgb(211, 247, 224)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = darkGreen
        window.navigationBarColor = darkGreen

        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 24, 28, 24)
            setBackgroundColor(green)
        }

        setContentView(screen)

        val logo = TextView(this).apply {
            text = "B"
            textSize = 54f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(green)
            setBackgroundColor(Color.WHITE)
            elevation = 8f
        }

        screen.addView(
            logo,
            LinearLayout.LayoutParams(112, 112).apply {
                bottomMargin = 16
            }
        )

        screen.addView(TextView(this).apply {
            text = "BINGWA BOT"
            textSize = 29f
            letterSpacing = 0.08f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
        })

        screen.addView(TextView(this).apply {
            text = "Your Business. Your Success."
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(paleGreen)
            setPadding(0, 6, 0, 22)
        })

        val dancers = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.HORIZONTAL
        }

        val agentOne = TextView(this).apply {
            text = "🕺"
            textSize = 76f
            gravity = Gravity.CENTER
        }

        val agentTwo = TextView(this).apply {
            text = "💃"
            textSize = 76f
            gravity = Gravity.CENTER
        }

        dancers.addView(agentOne, LinearLayout.LayoutParams(125, 145))
        dancers.addView(agentTwo, LinearLayout.LayoutParams(125, 145))
        screen.addView(dancers)

        animateAgent(agentOne, 1)
        animateAgent(agentTwo, -1)

        screen.addView(TextView(this).apply {
            text = "Welcome, Bingwa Agent!"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(0, 20, 0, 8)
        })

        screen.addView(TextView(this).apply {
            text = "Your next success starts here."
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(paleGreen)
            setPadding(0, 0, 0, 26)
        })

        val startButton = Button(this).apply {
            text = "Let's Get Started"
            isAllCaps = false
            textSize = 17f
            setTextColor(green)
            setBackgroundColor(Color.WHITE)
            setOnClickListener {
                goToDashboard()
            }
        }

        screen.addView(
            startButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                56
            )
        )

        screen.addView(TextView(this).apply {
            text = "Sell Smart • Serve Better • Earn More"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(paleGreen)
            setPadding(0, 22, 0, 0)
        })

        handler.postDelayed(openDashboard, 4500)
    }

    private fun animateAgent(view: TextView, direction: Int) {
        view.animate()
            .rotation(9f * direction)
            .translationY(-12f)
            .setDuration(420)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                view.animate()
                    .rotation(-9f * direction)
                    .translationY(0f)
                    .setDuration(420)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        if (!isFinishing && !isDestroyed) {
                            animateAgent(view, direction)
                        }
                    }
                    .start()
            }
            .start()
    }

    private fun goToDashboard() {
        if (opened) return

        opened = true
        handler.removeCallbacks(openDashboard)

        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacks(openDashboard)
        super.onDestroy()
    }
}
