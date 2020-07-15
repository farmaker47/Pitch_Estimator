package com.george.pitch_estimator.singingFragment

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.george.pitch_estimator.SingRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope

class SingingFragmentViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var singRecorderObject: SingRecorder

    var _singingRunning = false

    init {}

    fun setSingRecorderModule(singRecorder: SingRecorder) {
        singRecorderObject = singRecorder
    }

    fun startSinging() {

        _singingRunning = true

        singRecorderObject.startRecording()

    }

    fun stopSinging() {
        val stream = singRecorderObject.stopRecording()
        _singingRunning = false
        viewModelScope.launch {
            // write .wav file to external directory
            singRecorderObject.writeWav(stream)
            // reset stream
            singRecorderObject.reInitializePcmStream()
        }
    }

}