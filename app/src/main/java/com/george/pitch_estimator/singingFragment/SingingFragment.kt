package com.george.pitch_estimator.singingFragment


/*Copyright 2016 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.george.pitch_estimator.PitchModelExecutor
import com.george.pitch_estimator.R
import com.george.pitch_estimator.SingRecorder
import com.george.pitch_estimator.databinding.FragmentFirstBinding
import org.koin.android.ext.android.get
import org.koin.android.ext.android.getKoin
import org.koin.android.viewmodel.ext.android.viewModel
import java.io.File


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class SingingFragment : Fragment() {

    private lateinit var binding: FragmentFirstBinding
    private val viewModel: SingingFragmentViewModel by viewModel()
    private lateinit var singRecorder: SingRecorder
    private lateinit var pitchModelExecutor: PitchModelExecutor

    // Permissions
    var PERMISSION_ALL = 123
    var PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFirstBinding.inflate(inflater)
        binding.lifecycleOwner = this

        binding.viewModelxml = viewModel
        getKoin().setProperty("koinUseGpu", false)
        singRecorder = get()
        pitchModelExecutor = get()
        viewModel.setSingRecorderModule(singRecorder, pitchModelExecutor)

        binding.buttonForSinging.setOnClickListener {

            if (viewModel._singingRunning) {
                singingStopped()
            } else {
                // Start animation
                animateSharkButton()
                // Start process immediately
                viewModel.setUpdateLoopSingingHandler()

                // Start karaoke
                viewModel.setUpdateKaraokeHandler()

                //Toast.makeText(activity, "Singing has started", Toast.LENGTH_LONG).show()

            }
        }

        //Check for permissions
        initialize()

        // Generate folder for saving .wav later
        generateFolder()

        // Observe notes as they come out of model and update webview respectively
        viewModel.noteValuesToDisplay.observe(viewLifecycleOwner,
            androidx.lifecycle.Observer { list ->

                if (list.size > 0) {
                    var i = 0
                    val handler = Handler()
                    handler.post(object : Runnable {
                        override fun run() {
                            when (list[i]) {
                                "C2" -> binding.webView.loadUrl("javascript:myMove('125')")
                                "C#2" -> binding.webView.loadUrl("javascript:myMoveSharp('125')")
                                "D2" -> binding.webView.loadUrl("javascript:myMove('130')")
                                "D#2" -> binding.webView.loadUrl("javascript:myMoveSharp('130')")
                                "E2" -> binding.webView.loadUrl("javascript:myMove('135')")
                                "F2" -> binding.webView.loadUrl("javascript:myMove('140')")
                                "F#2" -> binding.webView.loadUrl("javascript:myMoveSharp('140')")
                                "G2" -> binding.webView.loadUrl("javascript:myMove('145')")
                                "G#2" -> binding.webView.loadUrl("javascript:myMoveSharp('145')")
                                "A2" -> binding.webView.loadUrl("javascript:myMove('150')")
                                "A#2" -> binding.webView.loadUrl("javascript:myMoveSharp('150')")
                                "B2" -> binding.webView.loadUrl("javascript:myMove('155')")

                                "C3" -> binding.webView.loadUrl("javascript:myMove('160')")
                                "C#3" -> binding.webView.loadUrl("javascript:myMoveSharp('160')")
                                "D3" -> binding.webView.loadUrl("javascript:myMove('165')")
                                "D#3" -> binding.webView.loadUrl("javascript:myMoveSharp('165')")
                                "E3" -> binding.webView.loadUrl("javascript:myMove('170')")
                                "F3" -> binding.webView.loadUrl("javascript:myMove('175')")
                                "F#3" -> binding.webView.loadUrl("javascript:myMoveSharp('175')")
                                "G3" -> binding.webView.loadUrl("javascript:myMove('180')")
                                "G#3" -> binding.webView.loadUrl("javascript:myMoveSharp('180')")
                                "A3" -> binding.webView.loadUrl("javascript:myMove('185')")
                                "A#3" -> binding.webView.loadUrl("javascript:myMoveSharp('185')")
                                "B3" -> binding.webView.loadUrl("javascript:myMove('190')")

                                "C4" -> binding.webView.loadUrl("javascript:myMove('225')")
                                "C#4" -> binding.webView.loadUrl("javascript:myMoveSharp('225')")
                                "D4" -> binding.webView.loadUrl("javascript:myMove('230')")
                                "D#4" -> binding.webView.loadUrl("javascript:myMoveSharp('230')")
                                "E4" -> binding.webView.loadUrl("javascript:myMove('235')")
                                "F4" -> binding.webView.loadUrl("javascript:myMove('240')")
                                "F#4" -> binding.webView.loadUrl("javascript:myMoveSharp('240')")
                                "G4" -> binding.webView.loadUrl("javascript:myMove('245')")
                                "G#4" -> binding.webView.loadUrl("javascript:myMoveSharp('245')")
                                "A4" -> binding.webView.loadUrl("javascript:myMove('250')")
                                "A#4" -> binding.webView.loadUrl("javascript:myMoveSharp('250')")
                                "B4" -> binding.webView.loadUrl("javascript:myMove('255')")

                                "C5" -> binding.webView.loadUrl("javascript:myMove('260')")
                                "C#5" -> binding.webView.loadUrl("javascript:myMoveSharp('260')")
                                "D5" -> binding.webView.loadUrl("javascript:myMove('265')")
                                "D#5" -> binding.webView.loadUrl("javascript:myMoveSharp('265')")
                                "E5" -> binding.webView.loadUrl("javascript:myMove('270')")
                                "F5" -> binding.webView.loadUrl("javascript:myMove('275')")
                                "F#5" -> binding.webView.loadUrl("javascript:myMoveSharp('275')")
                                "G5" -> binding.webView.loadUrl("javascript:myMove('280')")
                                "G#5" -> binding.webView.loadUrl("javascript:myMoveSharp('280')")
                                "A5" -> binding.webView.loadUrl("javascript:myMove('285')")
                                "A#5" -> binding.webView.loadUrl("javascript:myMoveSharp('285')")
                                "B5" -> binding.webView.loadUrl("javascript:myMove('290')")

                            }
                            i++
                            if (i < list.size) {
                                handler.postDelayed(this, TIME_DELAY_FOR_NOTES)
                            }
                        }
                    })
                }
            })

        // Observe viewmodel object
        viewModel.spannableForKaraoke.observe(
            requireActivity(),
            Observer { karaokeString ->
                binding.textviewKaraoke.text = karaokeString
            }
        )

        viewModel.singingEnd.observe(
            requireActivity(),
            Observer { end ->
                if (end) {
                    // Clear animation
                    binding.buttonAnimated.clearAnimation()
                }else{
                    animateSharkButton()
                }
            }
        )

        return binding.root
    }

    fun singingStopped() {
        // Remove callback to stop collecting sound
        //updateWidgetHandler.removeCallbacks(updateWidgetRunnable)
        viewModel.stopAllSinging()

        // Clear animation
        binding.buttonAnimated.clearAnimation()

        //Toast.makeText(activity, "Singing has stopped", Toast.LENGTH_LONG).show()
    }

    private fun animateSharkButton() {
        val animation = AnimationUtils.loadAnimation(activity, R.anim.scale_anim)
        binding.buttonAnimated.startAnimation(animation)
    }

    private fun generateFolder() {
        val root =
            File(Environment.getExternalStorageDirectory(), "Pitch Estimator")
        if (!root.exists()) {
            root.mkdirs()
        }
    }

    private fun hasPermissions(
        context: Context?,
        vararg permissions: String?
    ): Boolean {
        if (context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission!!
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    private fun initialize() {
        if (!hasPermissions(activity, *PERMISSIONS)) {
            ActivityCompat.requestPermissions(requireActivity(), PERMISSIONS, PERMISSION_ALL)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
    }

    override fun onPause() {
        super.onPause()

        // Stop processes when app goes on background
        //singingStopped()
    }

    companion object {
        private const val TIME_DELAY_FOR_NOTES = 555L

        // Update interval for widget
        const val UPDATE_INTERVAL_INFERENCE = 2048L
        const val UPDATE_INTERVAL_KARAOKE = 400L
    }
}