package com.george.pitch_estimator.binding_adapters

import android.os.Handler
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.databinding.BindingAdapter
import kotlin.math.roundToInt

@BindingAdapter("doubleArrayToString")
fun bindDoubleArrayHertzToValues(textView: TextView, value: DoubleArray?) {

    if (value != null) {
        val roundedArray = DoubleArray(value.size)
        for (i in value.indices) {
            roundedArray[i] = value[i].roundToInt().toDouble()
        }

        textView.text = roundedArray.contentToString()
    }

}

@BindingAdapter("noteArrayListToString")
fun bindDoubleArrayHertzToValues(textView: TextView, value: ArrayList<String>?) {

    textView.text = value.toString()

}

@BindingAdapter("htmlToScreen")
fun bindTextViewHtml(webView: WebView, htmlValue: String) {
    webView.settings.javaScriptEnabled = true;

    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            val handler = Handler()
            handler.postDelayed(
                {  webView.loadUrl("javascript:(function() {\n" +
                        "      event.preventDefault();\n" +
                        "      \$('#vf-auto1011').animate({\n" +
                        "        marginLeft: \"+=2000px\"\n" +
                        "      }, \"slow\");\n" +
                        "   })()")},
                2000
            )
        }
    }

    webView.loadDataWithBaseURL("fake://not/needed", htmlValue, "text/html", "UTF-8", "")
    //webView.loadData(htmlValue, "text/html", "UTF-8")

    //webView.loadUrl(htmlValue)
}