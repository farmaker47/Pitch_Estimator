package com.george.pitch_estimator

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.*
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class SingRecorder(
    private val mHotwordKey: String,
    numberRecordings: Int,
    context: Context
    //vad: Vad,
    //listener: HotwordSpeechListener
) {
    private val AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION
    private val SAMPLE_RATE = 16000
    private val CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO
    private val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, ENCODING)
    private val AUDIO_FORMAT =
        AudioFormat.Builder().setEncoding(ENCODING)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(CHANNEL_MASK)
            .build()
    lateinit var mPcmStream: ByteArrayOutputStream
    private var mRecorder: AudioRecord? = null

    //private var mRecorderVad: AudioRecord? = null
    private var mRecording: Boolean = true
    private var mThread: Thread? = null

    //private var mVadThread: Thread? = null
    private val mSampleLengths: DoubleArray
    private var mSamplesTaken: Int
    private val mContext: Context

    //Vad and silence
    //private val mVad: Vad
    private var done = false

    //private boolean cancelled;
    private val mMinimumVoice = 100
    private val mMaximumSilence = 700
    private val mUpperLimit = 100

    // From commands
    /*private var recordingThread: Thread? = null
    var shouldContinue = true
    private val SAMPLE_DURATION_MS = 1000
    private val RECORDING_LENGTH = (SAMPLE_RATE * SAMPLE_DURATION_MS / 1000) as Int
    var recordingBuffer = ShortArray(RECORDING_LENGTH)
    private val recordingBufferLock = ReentrantLock()
    var recordingOffset = 0
    private var recognitionThread: Thread? = null
    var shouldContinueRecognition = true
    var buffer = ShortArray(BUFFER_SIZE)*/
    var buffer = ShortArray(BUFFER_SIZE)
    var bufferForInference = arrayListOf<Short>()

    //Listener
    //private val mWordListener: HotwordSpeechListener

    interface HotwordSpeechListener {
        fun onSpeechChange(speechInt: Int)
    }

    /**
     * Start the recording process.
     */
    fun startRecording() {
        mRecorder = AudioRecord.Builder().setAudioSource(AUDIO_SOURCE)
            .setAudioFormat(AUDIO_FORMAT)
            .setBufferSizeInBytes(BUFFER_SIZE)
            .build()
        /*mRecorderVad = AudioRecord.Builder().setAudioSource(AUDIO_SOURCE)
            .setAudioFormat(AUDIO_FORMAT)
            .setBufferSizeInBytes(BUFFER_SIZE)
            .build()*/

        //mVad.start();
        done = false
        mRecording = true
        mRecorder?.startRecording()
        //mRecorderVad?.startRecording()
        mThread = Thread(readAudio)
        mThread!!.start()

        /*mVadThread = Thread(readVad)
        mVadThread!!.start()*/
    }

    fun stopRecording(): ByteArrayOutputStream {
        if (mRecorder != null && mRecorder!!.state == AudioRecord.STATE_INITIALIZED) {
            mRecording = false
            mRecorder!!.stop()

            //mVad.stop();
            //mRecorderVad!!.stop()
            done = true

            /*val runner = AsyncTaskRunner()
            runner.execute(mPcmStream)*/

            // mPcmStream again to 0
            //mPcmStream = ByteArrayOutputStream()
            //Log.e("STREAM_PCM_After", mPcmStream.size().toString())

            // Short array commands
            Log.e("BUFFER_SHORT", buffer.takeLast(100).toString())
            //Log.e("BUFFER_SHORT_SIZE", buffer.size.toString())
        }
        return mPcmStream
    }

    fun stopRecordingForInference(): ArrayList<Short>{
        return bufferForInference
    }

    /**
     * Read audio from the audio recorder stream.
     */
    private val readAudio = Runnable {
        var readBytes: Int
        buffer = ShortArray(BUFFER_SIZE)
        while (mRecording) {
            readBytes = mRecorder!!.read(buffer, 0, BUFFER_SIZE)

            //Higher volume of microphone
            //https://stackoverflow.com/questions/25441166/how-to-adjust-microphone-sensitivity-while-recording-audio-in-android
            if (readBytes > 0) {
                for (i in 0 until readBytes) {
                    buffer[i] = Math.min(
                        (buffer[i] * 6.7).toInt(),
                        Short.MAX_VALUE.toInt()
                    ).toShort()
                }
            }
            if (readBytes != AudioRecord.ERROR_INVALID_OPERATION) {
                for (s in buffer) {

                    // Add all values to arraylist
                    bufferForInference.add(s)

                    writeShort(mPcmStream, s)
                }
            }
        }
        //Log.e("READ_AUDIO_BUFFER", buffer.size.toString())
        //Log.e("PCMSTREam", mPcmStream.size().toString())
        //Log.e("BUFFER_FOR_INFER_SIZE", bufferForInference.size.toString())
        //Log.e("BUFFER_FOR_INFER_VALUES", bufferForInference.takeLast(100).toString())

        /*mRecorder.release();
                    mRecorder = null;*/
    }

    /*fun startRecordingCommands() {
        if (recordingThread != null) {
            return
        }
        shouldContinue = true
        recordingThread = Thread(
            Runnable { recordCommands() })
        recordingThread?.start()
    }*/

    /* private fun recordCommands() {
         Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

         // Estimate the buffer size we'll need for this device.
         var bufferSize = AudioRecord.getMinBufferSize(
             SAMPLE_RATE,
             AudioFormat.CHANNEL_IN_MONO,
             AudioFormat.ENCODING_PCM_16BIT
         )
         if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
             bufferSize = SAMPLE_RATE * 2
         }
         val audioBuffer = ShortArray(bufferSize / 2)
         val record = AudioRecord(
             MediaRecorder.AudioSource.DEFAULT,
             SAMPLE_RATE,
             AudioFormat.CHANNEL_IN_MONO,
             AudioFormat.ENCODING_PCM_16BIT,
             bufferSize
         )
         if (record.state != AudioRecord.STATE_INITIALIZED) {
             Log.e(
                 "AUDIO_RECORD",
                 "Audio Record can't initialize!"
             )
             return
         }
         record.startRecording()
         *//*Log.v(
            ,
            "Start recording"
        )*//*

        // Loop, gathering audio data and copying it to a round-robin buffer.
        while (shouldContinue) {
            val numberRead = record.read(audioBuffer, 0, audioBuffer.size)
            val maxLength: Int = recordingBuffer.size
            val newRecordingOffset: Int = recordingOffset + numberRead
            val secondCopyLength = Math.max(0, newRecordingOffset - maxLength)
            val firstCopyLength = numberRead - secondCopyLength
            // We store off all the data for the recognition thread to access. The ML
            // thread will copy out of this buffer into its own, while holding the
            // lock, so this should be thread safe.
            recordingBufferLock.lock()
            try {
                System.arraycopy(
                    audioBuffer,
                    0,
                    recordingBuffer,
                    recordingOffset,
                    firstCopyLength
                )
                System.arraycopy(
                    audioBuffer,
                    firstCopyLength,
                    recordingBuffer,
                    0,
                    secondCopyLength
                )
                recordingOffset = newRecordingOffset % maxLength
            } finally {
                recordingBufferLock.unlock()
            }
        }
        record.stop()
        record.release()
    }

    @Synchronized
    fun startRecognitionCommands() {
        if (recognitionThread != null) {
            return
        }
        shouldContinueRecognition = true
        recognitionThread = Thread(
            Runnable { recognizeCommands() })
        recognitionThread?.start()
    }

    private fun recognizeCommands() {

        val inputBuffer =
            ShortArray(RECORDING_LENGTH)
        val floatInputBuffer = Array(
            RECORDING_LENGTH
        ) { FloatArray(1) }
        val sampleRateList =
            intArrayOf(SAMPLE_RATE)

        // Loop, grabbing recorded data and running the recognition model on it.
        while (shouldContinueRecognition) {
            //val startTime = Date().time
            // The recording thread places data in this round-robin buffer, so lock to
            // make sure there's no writing happening and then copy it to our own
            // local version.
            recordingBufferLock.lock()
            try {
                val maxLength = recordingBuffer.size
                val firstCopyLength = maxLength - recordingOffset
                val secondCopyLength = recordingOffset
                System.arraycopy(
                    recordingBuffer,
                    recordingOffset,
                    inputBuffer,
                    0,
                    firstCopyLength
                )
                System.arraycopy(
                    recordingBuffer,
                    0,
                    inputBuffer,
                    firstCopyLength,
                    secondCopyLength
                )
            } finally {
                recordingBufferLock.unlock()
            }

            // We need to feed in float values between -1.0f and 1.0f, so divide the
            // signed 16-bit inputs.
            for (i in 0 until RECORDING_LENGTH) {
                floatInputBuffer[i][0] = inputBuffer[i] / 32767.0f
            }
            Log.e("FLOATINPUTBUFFER", inputBuffer.takeLast(100).toString())
        }

    }

    @Synchronized
    fun stopRecordingCommands() {
        if (recordingThread == null) {
            return
        }
        shouldContinue = false
        recordingThread = null
    }

    @Synchronized
    fun stopRecognitionCommands() {
        if (recognitionThread == null) {
            return
        }
        shouldContinueRecognition = false
        recognitionThread = null
    }*/

    /**
     * Stop the recording process.
     */
    /*private val readVad = Runnable {
        try {
            var vad = 0
            var finishedvoice = false
            var touchedvoice = false
            var touchedsilence = false
            var samplesvoice: Long = 0
            var samplessilence: Long = 0
            val dtantes = System.currentTimeMillis()
            var dtantesmili = System.currentTimeMillis()
            var done = false
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            while (mRecording && !done) {
                var nshorts = 0
                val mBuftemp =
                    ShortArray(FRAME_SIZE * 1 * 2)
                nshorts = mRecorderVad!!.read(mBuftemp, 0, mBuftemp.size)
                vad = mVad.feed(mBuftemp, nshorts)
                val dtdepois = System.currentTimeMillis()
                if (vad == 0) {
                    Log.e("VAD", "0")
                    mHotwordListener.onSpeechChange(6789)
                    if (touchedvoice) {
                        samplessilence += dtdepois - dtantesmili
                        if (samplessilence > mMaximumSilence) touchedsilence = true
                    }
                } else { // vad == 1 => Active voice
                    Log.e("VAD", "1")
                    samplesvoice += dtdepois - dtantesmili
                    if (samplesvoice > mMinimumVoice) touchedvoice = true
                    mHotwordListener.onSpeechChange(6789)
                }
                dtantesmili = dtdepois
                if (touchedvoice && touchedsilence) finishedvoice = true
                if (finishedvoice) {
                    done = true
                    Log.e("FINISHED_VOICE", "FINISHED_VOICE")
                    mHotwordListener.onSpeechChange(1234)
                }

                //if voice is over mUpperlimit = .. seconds
                if ((dtdepois - dtantes) / 1000 > mUpperLimit) {
                    Log.e("DTDEPOIS", "UPPER_LIMIT")
                    done = true
                    if (touchedvoice) {
                        Log.e("TOUCHED_VOICE", "TOUCHED_VOICE")
                    } else {
                        Log.e("RAISED_NO_VOICE", "RAISED_NO_VOICE")
                    }
                }
                if (nshorts <= 0) break
            }

            *//*mRecorderVad.release();*//*
            *//*mRecorderVad = null;*//*

            *//*if (cancelled) {
                cancelled = false;
                Log.e("CANCELED","CANCELED");
                return;
            }*//*
        } catch (exc: Exception) {
            val error =
                String.format("General audio error %s", exc.message)
            Log.e("GENERAL_ERROR", "GENERAL_ERROR")
            exc.printStackTrace()
        }
    }*/

    /**
     * Convert raw PCM data to a wav file.
     *
     *
     * See: https://stackoverflow.com/questions/43569304/android-how-can-i-write-byte-to-wav-file
     *
     * @return Byte array containing wav file data.
     */
    @Throws(IOException::class)
    private fun pcmToWav(byteArrayOutputStream: ByteArrayOutputStream): ByteArray {
        val stream = ByteArrayOutputStream()
        val pcmAudio = byteArrayOutputStream.toByteArray()
        writeString(stream, "RIFF") // chunk id
        writeInt(stream, 36 + pcmAudio.size) // chunk size
        writeString(stream, "WAVE") // format
        writeString(stream, "fmt ") // subchunk 1 id
        writeInt(stream, 16) // subchunk 1 size
        writeShort(stream, 1.toShort()) // audio format (1 = PCM)
        writeShort(stream, 1.toShort()) // number of channels
        writeInt(stream, SAMPLE_RATE) // sample rate
        writeInt(stream, SAMPLE_RATE * 2) // byte rate
        writeShort(stream, 2.toShort()) // block align
        writeShort(stream, 16.toShort()) // bits per sample
        writeString(stream, "data") // subchunk 2 id
        writeInt(stream, pcmAudio.size) // subchunk 2 size
        stream.write(pcmAudio)
        return stream.toByteArray()
    }

    /**
     * Trim the silence from this recording.
     */
    private fun trimSilence() {
        //
    }

    /**
     * Validate this recording.
     *
     * @return Boolean indicating whether or not the sample is valid.
     */
    /*fun validateSample(): Boolean {
        if (mSamplesTaken >= mSampleLengths.size) {
            return false
        }
        trimSilence()
        val seconds = mPcmStream.size() / SAMPLE_RATE.toDouble()
        if (seconds > 5) {
            return false
        }
        for (i in 0 until mSamplesTaken) {
            if (Math.abs(mSampleLengths[i] - seconds) > 0.3) {
                return false
            }
        }
        mSampleLengths[mSamplesTaken++] = seconds
        return true
    }*/

    /**
     * Write a 32-bit integer to an output stream, in Little Endian format.
     *
     * @param output Output stream
     * @param value  Integer value
     */
    private fun writeInt(output: ByteArrayOutputStream, value: Int) {
        output.write(value)
        output.write(value shr 8)
        output.write(value shr 16)
        output.write(value shr 24)
    }

    /**
     * Write a 16-bit integer to an output stream, in Little Endian format.
     *
     * @param output Output stream
     * @param value  Integer value
     */
    private fun writeShort(
        output: ByteArrayOutputStream,
        value: Short
    ) {
        output.write(value.toInt())
        output.write(value.toInt() shr 8)
    }

    /**
     * Write a string to an output stream.
     *
     * @param output Output stream
     * @param value  String value
     */
    private fun writeString(
        output: ByteArrayOutputStream,
        value: String
    ) {
        for (i in 0 until value.length) {
            output.write(value[i].toInt())
        }
    }

    /**
     * Generate a JSON config for the hotword.
     *
     * @return JSONObject containing config.
     */
    private fun generateConfig(): JSONObject {
        val obj = JSONObject()
        try {
            obj.put("hotword_key", mHotwordKey)
            obj.put("kind", "personal")
            obj.put("dtw_ref", 0.22)
            obj.put("from_mfcc", 1)
            obj.put("to_mfcc", 13)
            obj.put("band_radius", 10)
            obj.put("shift", 10)
            obj.put("window_size", 10)
            obj.put("sample_rate", SAMPLE_RATE)
            obj.put("frame_length_ms", 25.0)
            obj.put("frame_shift_ms", 10.0)
            obj.put("num_mfcc", 13)
            obj.put("num_mel_bins", 13)
            obj.put("mel_low_freq", 20)
            obj.put("cepstral_lifter", 22.0)
            obj.put("dither", 0.0)
            obj.put("window_type", "povey")
            obj.put("use_energy", false)
            obj.put("energy_floor", 0.0)
            obj.put("raw_energy", true)
            obj.put("preemphasis_coefficient", 0.97)
        } finally {
            return obj
        }
    }

    /**
     * Write a wav file from the current sample.
     *
     * @throws IOException
     */
    fun writeWav(byteArrayOutputStream: ByteArrayOutputStream) {
        var wav = ByteArray(0)
        try {
            wav = pcmToWav(byteArrayOutputStream)
            //Log.e("WAV_size", wav.size.toString())
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var stream: FileOutputStream? = null
        try {
            try {
                stream = FileOutputStream(
                    Environment.getExternalStorageDirectory().toString() +
                            "/Pitch Estimator/soloupis.wav", false
                )
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            try {
                stream!!.write(wav)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } finally {
            if (stream != null) {
                try {
                    stream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Write a JSON config file for this hotword.
     *
     * @param output Output file
     * @throws IOException
     */
    @Throws(IOException::class)
    fun writeConfig(output: File?) {
        val config = generateConfig().toString().toByteArray()
        var stream: FileOutputStream? = null
        try {
            stream = FileOutputStream(output)
            stream.write(config)
        } finally {
            stream?.close()
        }
    }

    /*//AsyncTask for write .wav
    private inner class AsyncTaskRunner :
        AsyncTask<ByteArrayOutputStream?, String?, String?>() {
        protected override fun doInBackground(vararg byteArrayOutputStreams: ByteArrayOutputStream): String? {
            Log.i("ASYNC_BACK", byteArrayOutputStreams[0].size().toString())
            writeWav(byteArrayOutputStreams[0])
            return null
        }

        override fun onPostExecute(s: String?) {
            //Toast.makeText(mContext, s, Toast.LENGTH_SHORT).show();
        }
    }*/

    companion object {
        const val FRAME_SIZE = 80
    }

    fun reInitializePcmStream() {
        mPcmStream = ByteArrayOutputStream()
    }

    init {
        mPcmStream = ByteArrayOutputStream()
        mRecording = false
        mSampleLengths = DoubleArray(numberRecordings)
        mSamplesTaken = 0
        mContext = context
        //mVad = vad
        //mWordListener = listener

    }


}