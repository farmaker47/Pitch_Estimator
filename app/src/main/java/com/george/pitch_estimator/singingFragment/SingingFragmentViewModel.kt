package com.george.pitch_estimator.singingFragment

import android.app.Application
import android.os.Handler
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.george.pitch_estimator.PitchModelExecutor
import com.george.pitch_estimator.SingRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*

class SingingFragmentViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var singRecorderObject: SingRecorder
    lateinit var pitchModelExecutorObject: PitchModelExecutor
    lateinit var inputStringPentagram: String
    var _singingRunning = false
    // Handler to repeat update
    private val updateLoopSingingHandler = Handler()
    private val updateKaraokeHandler = Handler()

    private val _hertzValuesToDisplay = MutableLiveData<DoubleArray>()
    val hertzValuesToDisplay: LiveData<DoubleArray>
        get() = _hertzValuesToDisplay

    private val _noteValuesToDisplay = MutableLiveData<ArrayList<String>>()
    val noteValuesToDisplay: LiveData<ArrayList<String>>
        get() = _noteValuesToDisplay

    private val _integerValuesToSet = MutableLiveData<Int>()
    val integerValueToSet: LiveData<Int>
        get() = _integerValuesToSet

    private val _inputTextFromAssets = MutableLiveData<String>()

    // The external LiveData for the SelectedNews
    val inputTextFromAssets: LiveData<String>
        get() = _inputTextFromAssets

    private val _inferenceDone = MutableLiveData<Boolean>()
    val inferenceDone: LiveData<Boolean>
        get() = _inferenceDone

    init {
        // Start with loading musical pentagram from assets folder html code
        readTextFromAssets(application)

        // Initialize arraylist
        _noteValuesToDisplay.value = arrayListOf()
    }

    fun setSingRecorderModule(singRecorder: SingRecorder, pitchModelExecutor: PitchModelExecutor) {
        singRecorderObject = singRecorder
        pitchModelExecutorObject = pitchModelExecutor
    }

    fun startSinging() {

        _singingRunning = true

        singRecorderObject.startRecording()
        //singRecorderObject.startRecordingCommands()
        //singRecorderObject.startRecognitionCommands()
    }

    fun stopSinging() {

        val stream = singRecorderObject.stopRecording()
        val streamForInference = singRecorderObject.stopRecordingForInference()

        Log.i("VIEWMODEL_SIZE", streamForInference.size.toString())
        Log.i("VIEWMODEL_VALUES", streamForInference.takeLast(100).toString())

        _singingRunning = false
        viewModelScope.launch {
            doInference(stream, streamForInference)
        }

    }

    fun stopAllSinging(){
        updateLoopSingingHandler.removeCallbacks(updateLoopSingingRunnable)
        //updateKaraokeHandler.removeCallbacks(updateKaraokeRunnable)
    }

    private suspend fun doInference(
        stream: ByteArrayOutputStream,
        arrayListShorts: ArrayList<Short>
    ) = withContext(Dispatchers.IO) {
        // write .wav file to external directory
        singRecorderObject.writeWav(stream)
        // reset stream
        singRecorderObject.reInitializePcmStream()

        // The input must be normalized to floats between -1 and 1.
        // To normalize it, we just need to divide all the values by 2**16 or in our code, MAX_ABS_INT16 = 32768
        val floatsForInference = FloatArray(arrayListShorts.size)
        for ((index, value) in arrayListShorts.withIndex()) {
            floatsForInference[index] = (value / 32768F)
        }

        Log.i("FLOATS", floatsForInference.takeLast(100).toString())

        // Inference
        _inferenceDone.postValue(false)
        _noteValuesToDisplay.postValue(pitchModelExecutorObject.execute(floatsForInference))
        Log.i("HERTZ", hertzValuesToDisplay.toString())
        _inferenceDone.postValue(true)

        // Load dummy sound file for practice and calibration/ comparison with Colab notebook
        //transcribe("/sdcard/Pitch Estimator/soloupis.wav")
    }

    private fun readTextFromAssets(application: Application) {
        try {
            val inputStreamPentagram: InputStream = application.assets.open("final1.txt")
            inputStringPentagram = inputStreamPentagram.bufferedReader().use { it.readText() }

            _inputTextFromAssets.value = inputStringPentagram
            Log.i("HTML", inputTextFromAssets.value)
        } catch (e: Exception) {
            Log.e("EXCEPTION_READ", e.toString())
        }
    }

    fun setUpdateWidgetRunnable(){
        updateLoopSingingHandler.postDelayed(updateLoopSingingRunnable, 0)
    }

    private var updateLoopSingingRunnable: Runnable = Runnable {
        run {

            // Start singing
            startSinging()

            // Stop after 2048 millis
            val handler = Handler()
            handler.postDelayed({
                stopSinging()
            }, SingingFragment.UPDATE_INTERVAL_INFERENCE)

            // Re-run it after the update interval
            updateLoopSingingHandler.postDelayed(updateLoopSingingRunnable,
                SingingFragment.UPDATE_INTERVAL_INFERENCE
            )

        }

    }

    /*private fun transcribe(audioFile: String) {
        val inferenceExecTime = longArrayOf(0)
        //Log.e("AUDIO_FORMAT", "audioFormat.toString()")
        try {
            val wave = RandomAccessFile(audioFile, "r")
            wave.seek(20)
            val audioFormat: Char = readLEChar(wave)
            //Log.e("AUDIO_FORMAT", (audioFormat.toInt() == 1).toString())
            assert(
                audioFormat.toInt() == 1 // 1 is PCM
            )
            // tv_audioFormat.setText("audioFormat=" + (audioFormat == 1 ? "PCM" : "!PCM"));
            wave.seek(22)
            val numChannels: Char = readLEChar(wave)
            //Log.e("NUMBER_CHANNEL", (numChannels.toInt()).toString())
            assert(
                numChannels.toInt() == 1 // MONO
            )
            // tv_numChannels.setText("numChannels=" + (numChannels == 1 ? "MONO" : "!MONO"));
            wave.seek(24)
            val sampleRate: Int = readLEInt(wave)
            //Log.e("SAMPLE_RATE", (sampleRate).toString())
            assert(
                sampleRate == 16000// // desired sample rate
            )
            // tv_sampleRate.setText("sampleRate=" + (sampleRate == 16000 ? "16kHz" : "!16kHz"));
            wave.seek(34)
            val bitsPerSample: Char = readLEChar(wave)
            //Log.e("BITS_PER_SAMPLE", (bitsPerSample.toInt() == 16).toString())
            assert(
                bitsPerSample.toInt() == 16 // 16 bits per sample
            )
            // tv_bitsPerSample.setText("bitsPerSample=" + (bitsPerSample == 16 ? "16-bits" : "!16-bits" ));
            wave.seek(40)
            val bufferSize: Int = readLEInt(wave)
            assert(bufferSize > 0)
            // tv_bufferSize.setText("bufferSize=" + bufferSize);
            wave.seek(44)
            val bytes = ByteArray(bufferSize)
            wave.readFully(bytes)


            *//*Log.i("BYTES", bytes.size.toString())
            val shorts = ShortArray(bytes.size / 2)
            // to turn bytes to shorts as either big endian or little endian.
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()[shorts]
            val inferenceStartTime = System.currentTimeMillis()
            Log.i("SHORTS", shorts.size.toString())
            wholeSentence += _m.stt(shorts, shorts.size).toString() + ". "
            inferenceExecTime[0] = System.currentTimeMillis() - inferenceStartTime*//*
        } catch (ex: FileNotFoundException) {

        } catch (ex: IOException) {
        }
    }

    @Throws(IOException::class)
    private fun readLEChar(f: RandomAccessFile): Char {
        val b1 = f.readByte()
        val b2 = f.readByte()
        return (b2.toInt() shl 8 or b1.toInt()).toChar()
    }

    @Throws(IOException::class)
    private fun readLEInt(f: RandomAccessFile): Int {
        val b1 = f.readByte()
        val b2 = f.readByte()
        val b3 = f.readByte()
        val b4 = f.readByte()
        return ((b1 and 0xFF.toByte()).toInt() or ((b2 and 0xFF.toByte()).toInt() shl 8)
                or ((b3 and 0xFF.toByte()).toInt() shl 16) or ((b4 and 0xFF.toByte()).toInt() shl 24))
    }*/

    override fun onCleared() {
        super.onCleared()
        pitchModelExecutorObject.close()
    }
}