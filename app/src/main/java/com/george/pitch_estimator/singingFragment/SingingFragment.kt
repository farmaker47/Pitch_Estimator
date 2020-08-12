package com.george.pitch_estimator.singingFragment

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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.george.pitch_estimator.PitchModelExecutor
import com.george.pitch_estimator.R
import com.george.pitch_estimator.SingRecorder
import com.george.pitch_estimator.databinding.FragmentFirstBinding
import org.koin.android.ext.android.bind
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

    // Update interval for widget
    val UPDATE_INTERVAL_INFERENCE = 2048L
    val UPDATE_INTERVAL_KARAOKE = 340L

    // Handler to repeat update
    private val updateWidgetHandler = Handler()
    private val updateKaraokeHandler = Handler()

    // Runnable to loop every 2 seconds with writing sound and inferring
    private var updateWidgetRunnable: Runnable = Runnable {
        run {

            // Start singing
            viewModel.startSinging()

            // Stop after 2048 millis
            val handler = Handler()
            handler.postDelayed({
                viewModel.stopSinging()
            }, UPDATE_INTERVAL_INFERENCE)

            // Re-run it after the update interval
            updateWidgetHandler.postDelayed(updateWidgetRunnable, UPDATE_INTERVAL_INFERENCE)

        }

    }

    // Runnable to loop every 2 seconds with writing sound and inferring
    private var updateKaraokeRunnable: Runnable = Runnable {
        run {

            try {
                for (i in 1..17) {

                    val handler = Handler()
                    handler.postDelayed({
                        val wordtoSpan: Spannable = SpannableString(getString(R.string.song_lyrics))
                        wordtoSpan.setSpan(
                            ForegroundColorSpan(Color.BLUE),
                            0,
                            5 * i,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        binding.textviewKaraoke.text = wordtoSpan

                        // Stop everything after end of song
                        /*if(i==17){
                            singingStopped()
                        }*/

                    }, UPDATE_INTERVAL_KARAOKE * i)

                }
            } finally {
                //singingStopped()
            }
        }
    }

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
                updateWidgetHandler.postDelayed(updateWidgetRunnable, 0)

                // Start karaoke
                updateKaraokeHandler.postDelayed(updateKaraokeRunnable, 0)

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

        return binding.root
    }

    private fun singingStopped() {
        // Remove callback to stop collecting sound
        updateWidgetHandler.removeCallbacks(updateWidgetRunnable)

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
        singingStopped()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        singingStopped()
    }

    companion object {
        private const val TIME_DELAY_FOR_NOTES = 555L
    }
}