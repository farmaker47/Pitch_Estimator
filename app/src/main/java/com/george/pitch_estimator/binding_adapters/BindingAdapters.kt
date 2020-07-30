package com.george.pitch_estimator.binding_adapters

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