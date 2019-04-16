package com.ksondzyk.humidity

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.warningTextView).setText(
                HtmlCompat.fromHtml(getString(R.string.warning_message), FROM_HTML_MODE_LEGACY),
                TextView.BufferType.SPANNABLE
        )

        val curveSeekView = findViewById<CurveSeekView>(R.id.curveSeekView)
        val humidityTextView = findViewById<ProgressLabelView>(R.id.humidityTextView)

        curveSeekView.setProgress(50F)
        humidityTextView.setProgress(50)

        curveSeekView.onProgressChangeListener = {
            humidityTextView.setProgress(Math.round(it))
        }

    }
}
