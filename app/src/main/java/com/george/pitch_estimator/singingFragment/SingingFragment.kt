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

    //Permissions
    var PERMISSION_ALL = 123
    var PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    //update interval for widget
    val UPDATE_INTERVAL_INFERENCE = 2048L
    val UPDATE_INTERVAL_KARAOKE = 340L

    //Handler to repeat update
    private val updateWidgetHandler = Handler()
    private val updateKaraokeHandler = Handler()

    //runnable to loop every 2 seconds with writing sound and infering
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

    //runnable to loop every 2 seconds with writing sound and inferring
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
            }finally {
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
                // Start immediately
                updateWidgetHandler.postDelayed(updateWidgetRunnable, 0)

                // Start karaoke
                updateKaraokeHandler.postDelayed(updateKaraokeRunnable, 0)

                //binding.buttonForSinging.text = "Stop singing"
                //Toast.makeText(activity, "Singing has started", Toast.LENGTH_LONG).show()


            }
        }

        //Check for permissions
        initialize()

        // Generate folder for saving .wav later
        generateFolder()

        /*val WordtoSpan: Spannable = SpannableString(getString(R.string.song_lyrics))
        WordtoSpan.setSpan(
            ForegroundColorSpan(Color.BLUE),
            0,
            13,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.textviewKaraoke.text = WordtoSpan*/

        viewModel.integerValueToSet.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer { position ->
                binding.allNewsBlockTextView.loadUrl("javascript:myMoveSharp('$position')")
                //binding.allNewsBlockTextView.loadUrl("javascript:(function(){l=document.getElementById('music_sheet_sharp');e=document.createEvent('HTMLEvents');e.initEvent('click',true,true);l.dispatchEvent(e);})()")

            })

        return binding.root
    }

    private fun singingStopped() {
        // Remove callback to stop collecting sound
        updateWidgetHandler.removeCallbacks(updateWidgetRunnable)

        binding.buttonAnimated.clearAnimation()

        //binding.buttonForSinging.text = "Start singing"
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*view.findViewById<Button>(R.id.button_first).setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }*/


    }
}