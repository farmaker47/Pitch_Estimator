package com.george.pitch_estimator

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.*

class PitchModelExecutor(
    context: Context,
    private var useGPU: Boolean
) {
    private var gpuDelegate: GpuDelegate = GpuDelegate()
    private var numberThreads = 4

    private val interpreter: Interpreter
    private var predictTime = 0L
    val note_names = mapOf(
        // musical notes
        // ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"]
        0 to "C",
        1 to "C#",
        2 to "D",
        3 to "D#",
        4 to "E",
        5 to "F",
        6 to "F#",
        7 to "G",
        8 to "G#",
        9 to "A",
        10 to "A#",
        11 to "B"
    )

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
        private const val PT_OFFSET = 25.58
        private const val PT_SLOPE = 63.07
        private const val FMIN = 10.0
        private const val BINS_PER_OCTAVE = 12.0
        private const val C0 = 16.351597831287414
    }

    fun execute(floatsInput: FloatArray): ArrayList<String> {

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

        Log.i("PITCHES", pitches.contentToString())
        Log.i("PITCHES_SIZE", pitches.size.toString())
        Log.i("UNCERTAIN", uncertainties.contentToString())
        Log.i("UNCERTAIN_SIZE", uncertainties.size.toString())

        // Calculate confidence over 90%
        // and store values inside an array list of floats
        // if confidence is lower than 90% then add 0F
        val arrayForConfidence = arrayListOf<Float>()
        for (i in uncertainties.indices) {
            if (1 - uncertainties[i] >= 0.9) {
                arrayForConfidence.add(pitches[i])
            } else {
                arrayForConfidence.add(0F)
            }
        }

        /*Log.i("PITCHES_OVER_0.9", arrayForConfidence.size.toString())
        for (k in 0 until arrayForConfidence.size) {
            Log.i("PITCHES_OVER_0.9", arrayForConfidence[k].toString())
        }*/

        // The pitch values returned by SPICE are in the range from 0 to 1.
        // Let's convert them to absolute pitch values in Hz.
        val hertzValues = DoubleArray(arrayForConfidence.size)
        for (i in 0 until arrayForConfidence.size) {
            hertzValues[i] = convertToAbsolutePitchValuesInHz(arrayForConfidence[i])
        }

        //Log.i("HERTZ_VALUES", hertzValues.contentToString())

        // Calculate the offset during singing
        // When a person sings freely, the melody may have an offset to the absolute pitch values that notes can represent.
        // Hence, to convert predictions to notes, one needs to correct for this possible offset.
        val arrayForOffset = arrayListOf<Float>()
        for (i in hertzValues.indices) {
            if (hertzValues[i] > 0)
                arrayForOffset.add(hzToOffset(hertzValues[i].toFloat()))
        }

        /*Log.e("OFFSETS", arrayForOffset.size.toString())
        for (k in 0 until arrayForOffset.size) {
            Log.e("OFFSETS", arrayForOffset[k].toString())
        }*/

        val idealOffset = arrayForOffset.average()

        //Log.i("OFFSETS_AVERAGE", idealOffset.toString())

        var bestError = 10000000000000F//("+Inf").toFloat()
        var bestNotesAndRests = arrayListOf<String>()
        var bestPredictionsPerNote = 0

        for (predictions_per_note in 20 until 65 step 1) {
            for (prediction_start_offset in 0 until predictions_per_note) {

                val (error, notes_and_rests) = getQuantizationAndError(
                    hertzValues, predictions_per_note,
                    prediction_start_offset, idealOffset.toFloat()
                )

                if (error < bestError) {
                    bestError = error
                    bestNotesAndRests = notes_and_rests
                    bestPredictionsPerNote = predictions_per_note
                }

            }
        }

        Log.e("BEST_ERROR", bestError.toString())
        for (i in 0 until bestNotesAndRests.size) {
            Log.e("NOTES_AND_RESTS", bestNotesAndRests[i])
        }

        // Remove rest at beginning and end of arrayList
        var noRestInBeginningAndEnd = arrayListOf<String>()
        for (i in 0 until bestNotesAndRests.size) {
            if (i == 0 && bestNotesAndRests[0] != "Rest") {
                noRestInBeginningAndEnd.add(bestNotesAndRests[i])
            } else if (i > 0 && i < bestNotesAndRests.size - 1) {
                noRestInBeginningAndEnd.add(bestNotesAndRests[i])
            } else if (i == bestNotesAndRests.size - 1 && bestNotesAndRests[bestNotesAndRests.size - 1] != "Rest"
            ) {
                noRestInBeginningAndEnd.add(bestNotesAndRests[i])
                Log.e("3", "3")
            }
        }

        predictTime = System.currentTimeMillis() - predictTime
        Log.e("PITCHES_TIME", predictTime.toString())

        return noRestInBeginningAndEnd//hertzValues DoubleArray
    }

    private fun convertToAbsolutePitchValuesInHz(value: Float): Double {
        if (value != 0F) {
            val cqt_bin = value * PT_SLOPE + PT_OFFSET
            return FMIN * (2.0.pow(cqt_bin / BINS_PER_OCTAVE))
        } else {
            return 0.toDouble()
        }
    }

    private fun hzToOffset(hertzFloat: Float): Float {
        val h = (12 * kotlin.math.log2(hertzFloat / C0)).roundToInt().toFloat()
        //Log.i("ROUND", h.toString())
        return (12 * kotlin.math.log2(hertzFloat / C0) - h).toFloat()
    }

    private fun quantize_predictions(group: FloatArray, ideal_offset: Float): Pair<Double, String> {
        // Group values are either 0, or a pitch in Hz.
        val non_zero_values = arrayListOf<Float>()
        for (i in group.indices) {
            if (group[i] > 0) {
                non_zero_values.add(group[i])
            }
        }
        //print(non_zero_values)
        val zero_values_count = group.size - non_zero_values.size

        // Create a rest if 80% is silent, otherwise create a note.
        if (zero_values_count > 0.8 * group.size) {
            // Interpret as a rest. Count each dropped note as an error, weighted a bit
            // worse than a badly sung note (which would 'cost' 0.5).
            return Pair(0.51 * non_zero_values.size, "Rest")
        } else {
            // Interpret as note, estimating as mean of non-rest predictions.
            val non_zero_average_values = arrayListOf<Float>()
            for (i in 0 until non_zero_values.size) {
                non_zero_average_values.add((12 * kotlin.math.log2(non_zero_values[i] / C0) - ideal_offset).toFloat())
            }

            val h = non_zero_average_values.average().roundToInt()
            val octave = h / 12
            //Log.i("OCTAVE",octave.toString())
            val n = h.rem(12)
            //Log.i("NOTE",n.toString())
            val note = note_names[n] + octave.toString()
            // Quantization error is the total difference from the quantized note.
            val non_zero_error_values = arrayListOf<Float>()
            for (i in 0 until non_zero_values.size) {
                non_zero_error_values.add(
                    abs(12 * log2(non_zero_values[i] / C0) - ideal_offset - h)
                        .toFloat()
                )
            }
            val error = non_zero_error_values.sum()

            return Pair(error.toDouble(), note)
        }
    }

    private fun getQuantizationAndError(
        pitch_outputs_and_rests: DoubleArray, predictions_per_eighth: Int,
        prediction_start_offset: Int, ideal_offset: Float
    ): Pair<Float, ArrayList<String>> {

        val pitchOutputsAndRestsWithOffset = arrayListOf<Float>()
        for (i in 0 until prediction_start_offset) {
            pitchOutputsAndRestsWithOffset.add(0F)
        }

        for (i in pitch_outputs_and_rests.indices) {
            pitchOutputsAndRestsWithOffset.add(pitch_outputs_and_rests[i].toFloat())
        }

        //Log.i("SIZE", pitch_outputs_and_rests_with_offset.size.toString())

        val groups = arrayListOf<FloatArray>()
        for (i in 0 until pitchOutputsAndRestsWithOffset.size step predictions_per_eighth) {
            val firstArrayList = arrayListOf<Float>()
            try {
                for (k in i until i + predictions_per_eighth) {
                    firstArrayList.add(pitchOutputsAndRestsWithOffset[k])
                }

            } catch (e: Exception) {
                //Log.e("EXCEPTION", e.toString())
            }


            val secondArray = FloatArray(firstArrayList.size)
            for (l in 0 until firstArrayList.size) {
                secondArray[l] = firstArrayList[l]
            }

            groups.add(secondArray)
        }

        //# Collect the predictions for each note (or rest).
        var quantizationError = 0.0
        val notesAndRests = arrayListOf<String>()
        for (m in 0 until groups.size) {
            val (error, note) = quantize_predictions(groups[m], ideal_offset)
            quantizationError += error
            notesAndRests.add(note)
        }

        return Pair(quantizationError.toFloat(), notesAndRests)
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