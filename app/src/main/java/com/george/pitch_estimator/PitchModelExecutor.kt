package com.george.pitch_estimator

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.io.IOException
import java.lang.Exception
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.ceil
import kotlin.math.floor

class PitchModelExecutor(
    context: Context,
    private var useGPU: Boolean
) {
    private var gpuDelegate: GpuDelegate = GpuDelegate()
    private var numberThreads = 4

    private val interpreter: Interpreter
    private var predictTime = 0L

    init {
        if (useGPU) {
            interpreter = getInterpreter(context, PITCH_MODEL, true)
            Log.i("GPU_TRUE", "TRUE")
        } else {
            interpreter = getInterpreter(context, PITCH_MODEL, false)
            Log.i("GPU_FALSE", "FALSE")
        }
    }

    companion object {
        private const val PITCH_MODEL = "lite-model_spice_1.tflite"
    }

    fun execute(floatsInput: FloatArray) {

        predictTime = System.currentTimeMillis()
        val inputSize = floatsInput.size // ~10 seconds of sound
        var outputSize = 0
        when (inputSize) {
            32000 -> outputSize = ceil(inputSize / 512.0).toInt()
            else -> outputSize = (ceil(inputSize / 512.0) + 1).toInt()
        }
        val inputValues = floatsInput//FloatArray(inputSize)

        val inputs = arrayOf<Any>(inputValues)
        val outputs = HashMap<Int, Any>()

        val pitches = FloatArray(outputSize)
        val uncertainties = FloatArray(outputSize)

        outputs[0] = pitches
        outputs[1] = uncertainties
        //Log.e("INPUTS_SIZE", floatsInput.size.toString())
        try {
            interpreter.runForMultipleInputsOutputs(inputs, outputs)
        } catch (e: Exception) {
            Log.e("EXCEPTION", e.toString())
        }

        predictTime = System.currentTimeMillis() - predictTime

        Log.e("PITCHES", pitches.contentToString())
        Log.e("PITCHES_SIZE", pitches.size.toString())
        Log.e("PITCHES_TIME", predictTime.toString())

    }

    @Throws(IOException::class)
    private fun loadModelFile(context: Context, modelFile: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val retFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        fileDescriptor.close()
        return retFile
    }

    @Throws(IOException::class)
    private fun getInterpreter(
        context: Context,
        modelName: String,
        useGpu: Boolean
    ): Interpreter {
        val tfliteOptions = Interpreter.Options()
        if (useGpu) {
            gpuDelegate = GpuDelegate()
            tfliteOptions.addDelegate(gpuDelegate)

            //val delegate =
            //GpuDelegate(GpuDelegate.Options().setQuantizedModelsAllowed(true))
        }

        tfliteOptions.setNumThreads(numberThreads)
        return Interpreter(loadModelFile(context, modelName), tfliteOptions)
    }

    fun close() {
        interpreter.close()
    }
}