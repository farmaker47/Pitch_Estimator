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

    //webView.clearHistory();
    //webView.clearCache(true)
    webView.settings.javaScriptEnabled = true

    /*webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            val handler = Handler()
            handler.postDelayed(
                {
                    //webView.loadUrl("javascript:globalVariable('" + 370 + "')")
                    //webView.loadUrl("javascript:(function(){l=document.getElementById('music_sheet');e=document.createEvent('HTMLEvents');e.initEvent('click',true,true);l.dispatchEvent(e);})()")

                },
                10
            )
        }
    }*/

    webView.loadDataWithBaseURL("fake://not/needed", htmlValue, "text/html", "UTF-8", "")
    //webView.freeMemory()
}