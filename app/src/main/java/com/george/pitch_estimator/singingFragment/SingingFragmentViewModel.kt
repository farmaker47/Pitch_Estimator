package com.george.pitch_estimator.singingFragment

import android.app.Application
import android.util.Log
import android.util.Xml
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.george.pitch_estimator.PitchModelExecutor
import com.george.pitch_estimator.SingRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.*

class SingingFragmentViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var singRecorderObject: SingRecorder
    lateinit var pitchModelExecutorObject: PitchModelExecutor
    lateinit var inputStringPentagram: String
    lateinit var inputStringNote: String
    lateinit var inputStringFunction1: String
    lateinit var inputStringFunction2: String
    lateinit var inputStringFunction3: String
    var positionOfNote: Int = 0

    data class Entry(val title: String?, val summary: String?, val link: String?)

    var _singingRunning = false

    private val _hertzValuesToDisplay = MutableLiveData<DoubleArray>()
    val hertzValuesToDisplay: LiveData<DoubleArray>
        get() = _hertzValuesToDisplay

    private val _noteValuesToDisplay = MutableLiveData<ArrayList<String>>()
    val noteValuesToDisplay: LiveData<ArrayList<String>>
        get() = _noteValuesToDisplay

    private val _inputTextFromAssets = MutableLiveData<String>()

    // The external LiveData for the SelectedNews
    val inputTextFromAssets: LiveData<String>
        get() = _inputTextFromAssets

    private val _inferenceDone = MutableLiveData<Boolean>()
    val inferenceDone: LiveData<Boolean>
        get() = _inferenceDone

    init {
        readTextFromAssets(application, -280)

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

    private suspend fun doInference(
        stream: ByteArrayOutputStream,
        arrayListShorts: ArrayList<Short>
    ) = withContext(Dispatchers.IO) {
        // write .wav file to external directory
        singRecorderObject.writeWav(stream)
        // reset stream
        singRecorderObject.reInitializePcmStream()

        val floatsForInference = FloatArray(arrayListShorts.size)
        for ((index, value) in arrayListShorts.withIndex()) {
            floatsForInference[index] = (value / 32768F)
        }

        Log.i("FLOATS", floatsForInference.takeLast(100).toString())

        // Inference
        //_hertzValuesToDisplay.postValue(pitchModelExecutorObject.execute(floatsForInference))
        _inferenceDone.postValue(false)
        _noteValuesToDisplay.postValue(pitchModelExecutorObject.execute(floatsForInference))
        Log.i("HERTZ", hertzValuesToDisplay.toString())
        _inferenceDone.postValue(true)

        // After inference generate notes
        try {
            for (i in 0 until _noteValuesToDisplay.value!!.size) {

                when (_noteValuesToDisplay.value!![i]) {
                    "A2" -> _inputTextFromAssets.postValue(
                        inputStringPentagram + inputStringNote + inputStringFunction1 + "elem2.style.top = " +
                                (positionOfNote - 50) + ";" + inputStringFunction2 + (positionOfNote - 50 + 35).toString() + ";" + inputStringFunction3
                    )
                    "C3" -> _inputTextFromAssets.postValue(
                        inputStringPentagram + inputStringNote + inputStringFunction1 + "elem2.style.top = " +
                                (positionOfNote - 100) + ";" + inputStringFunction2 + (positionOfNote - 100 + 35).toString() + ";" + inputStringFunction3
                    )

                }
                /*if (_noteValuesToDisplay.value!![i] == "A2") {
                    _inputTextFromAssets.postValue(
                        inputStringPentagram + inputStringNote + inputStringFunction1 + "elem2.style.top = " +
                                (positionOfNote - 50) + ";" + inputStringFunction2 + (positionOfNote - 50 + 35).toString() + ";" + inputStringFunction3
                    )
                }*/
            }
        } catch (e: Exception) {
            Log.e("EXCEPTION", e.toString())
        }


        // Load dummy sound file for practice and calibration/ comparison with Colab notebook
        //transcribe("/sdcard/Pitch Estimator/soloupis.wav")
    }

    private fun readTextFromAssets(application: Application, position: Int) {
        try {
            val inputStreamPentagram: InputStream = application.assets.open("pentagram.txt")
            inputStringPentagram = inputStreamPentagram.bufferedReader().use { it.readText() }

            val inputStreamNote: InputStream = application.assets.open("note.txt")
            inputStringNote = inputStreamNote.bufferedReader().use { it.readText() }

            val inputStreamFunction1: InputStream = application.assets.open("function1.txt")
            inputStringFunction1 = inputStreamFunction1.bufferedReader().use { it.readText() }

            val inputStreamFunction2: InputStream = application.assets.open("function2.txt")
            inputStringFunction2 = inputStreamFunction2.bufferedReader().use { it.readText() }

            val inputStreamFunction3: InputStream = application.assets.open("function3.txt")
            inputStringFunction3 = inputStreamFunction3.bufferedReader().use { it.readText() }

            // parse xml
            //val entries = parse(inputStream)
            /*val output = StringBuilder().apply {
                append("<h3>${"Some"}</h3>")
                append("<em>${"Some"} ")
                append("${"Some"}</em>")
                // StackOverflowXmlParser returns a List (called "entries") of Entry objects.
                // Each Entry object represents a single post in the XML feed.
                // This section processes the entries list to combine each entry with HTML markup.
                // Each entry is displayed in the UI as a link that optionally includes
                // a text summary.
                entries.forEach { entry ->
                    append("<p><a href='")
                }
            }.toString()*/
            positionOfNote = position

            _inputTextFromAssets.value =
                inputStringPentagram /*+ inputStringNote + inputStringFunction1 + "elem2.style.top = " +
                        position + ";" + inputStringFunction2 + (position + 35).toString() + ";" + inputStringFunction3*/
            Log.i("HTML", inputTextFromAssets.value)
        } catch (e: Exception) {
            Log.e("EXCEPTION_READ", e.toString())
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(inputStream: InputStream): List<*> {
        inputStream.use { inputStream ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()
            return readFeed(parser)
        }
    }

    private val ns: String? = null

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFeed(parser: XmlPullParser): List<Entry> {
        val entries = mutableListOf<Entry>()

        parser.require(XmlPullParser.START_TAG, ns, "feed")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // Starts by looking for the entry tag
            if (parser.name == "entry") {
                entries.add(readEntry(parser))
            } else {
                skip(parser)
            }
        }
        return entries
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
    // to their respective "read" methods for processing. Otherwise, skips the tag.
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readEntry(parser: XmlPullParser): Entry {
        parser.require(XmlPullParser.START_TAG, ns, "entry")
        var title: String? = null
        var summary: String? = null
        var link: String? = null
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "title" -> title = readTitle(parser)
                "summary" -> summary = readSummary(parser)
                "link" -> link = readLink(parser)
                else -> skip(parser)
            }
        }
        return Entry(title, summary, link)
    }

    // Processes title tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTitle(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "title")
        val title = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "title")
        return title
    }

    // Processes link tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readLink(parser: XmlPullParser): String {
        var link = ""
        parser.require(XmlPullParser.START_TAG, ns, "link")
        val tag = parser.name
        val relType = parser.getAttributeValue(null, "rel")
        if (tag == "link") {
            if (relType == "alternate") {
                link = parser.getAttributeValue(null, "href")
                parser.nextTag()
            }
        }
        parser.require(XmlPullParser.END_TAG, ns, "link")
        return link
    }

    // Processes summary tags in the feed.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readSummary(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "summary")
        val summary = readText(parser)
        parser.require(XmlPullParser.END_TAG, ns, "summary")
        return summary
    }

    // For the tags title and summary, extracts their text values.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
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