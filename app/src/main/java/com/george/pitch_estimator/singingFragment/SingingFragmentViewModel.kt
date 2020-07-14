package com.george.pitch_estimator.singingFragment

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel

class SingingFragmentViewModel(application: Application) : AndroidViewModel(application) {


    init {

    }

    fun startSinging(){
        Log.e("SINGING","Yes")
    }

}