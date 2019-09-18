package com.ksondzyk.humidity

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
import android.widget.Button
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import kotlin.math.roundToInt

private var light = true
private var progress = 50F

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        if (light) setTheme(R.style.AppTheme_Light) else setTheme(R.style.AppTheme_Dark)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.warningTextView).setText(
            HtmlCompat.fromHtml(getString(R.string.warning_message), FROM_HTML_MODE_LEGACY),
            TextView.BufferType.SPANNABLE
        )

        val curveSeekView = findViewById<CurveSeekView>(R.id.curveSeekView)
        val humidityTextView = findViewById<ProgressLabelView>(R.id.humidityTextView)
        if (light) setLightStatusBar(curveSeekView)

        humidityTextView.animationDuration = 0
        curveSeekView.setProgress(progress)
        humidityTextView.setProgress(progress.roundToInt())
        humidityTextView.animationDuration = 800

        TypedValue().apply {
            humidityTextView.textColor = getAttr(R.attr.primaryTextColor)
            curveSeekView.backgroundShadowColor = getAttr(R.attr.backgroundColor)
            curveSeekView.selectedLabelColor = getAttr(R.attr.selectedLabelColor)
            curveSeekView.labelColor = getAttr(R.attr.labelColor)
            curveSeekView.scaleColor = getAttr(R.attr.scaleColor)
            curveSeekView.sliderColor = getAttr(R.attr.sliderColor)
            curveSeekView.sliderIconColor = getAttr(R.attr.sliderIconColor)
            curveSeekView.firstGradientColor = getAttr(R.attr.firstGradientColor)
            curveSeekView.secondGradientColor = getAttr(R.attr.secondGradientColor)
        }

        curveSeekView.onProgressChangeListener = {
            progress = it
            humidityTextView.setProgress(it.roundToInt())
        }

        findViewById<Button>(R.id.themButton).apply {
            if (light) setText(R.string.dark) else setText(R.string.light)

            setOnClickListener {
                light = !light
                val intent = intent
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
                startActivity(intent)
            }
        }
    }

    private fun setLightStatusBar(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var flags = view.systemUiVisibility
            flags = flags or SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            view.systemUiVisibility = flags
            window.statusBarColor = Color.WHITE
        }
    }

    private fun TypedValue.getAttr(@AttrRes attrRes: Int): Int {
        theme.resolveAttribute(attrRes, this, true)
        return data
    }

}
