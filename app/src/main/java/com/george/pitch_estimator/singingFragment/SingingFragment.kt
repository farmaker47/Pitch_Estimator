package com.george.pitch_estimator.singingFragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.BounceInterpolator
import android.widget.Toast
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

    //Permissions
    var PERMISSION_ALL = 123
    var PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    //update interval for widget
    val UPDATE_INTERVAL = 2048L

    //Handler to repeat update
    private val updateWidgetHandler = Handler()

    //runnable to loop every 2 seconds with writing sound and infering
    private var updateWidgetRunnable: Runnable = Runnable {
        run {

            // Start singing
            viewModel.startSinging()

            // Stop after 2048 millis
            val handler = Handler()
            handler.postDelayed({
                viewModel.stopSinging()
            }, UPDATE_INTERVAL)

            // Re-run it after the update interval
            updateWidgetHandler.postDelayed(updateWidgetRunnable, UPDATE_INTERVAL)

        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentFirstBinding.inflate(inflater)
        binding.lifecycleOwner = this

        getKoin().setProperty("koinUseGpu", false)
        singRecorder = get()
        pitchModelExecutor = get()
        viewModel.setSingRecorderModule(singRecorder, pitchModelExecutor)

        binding.buttonForSinging.setOnClickListener {

            if (viewModel._singingRunning) {
                // Remove callback to stop collecting sound
                updateWidgetHandler.removeCallbacks(updateWidgetRunnable)

                binding.buttonAnimated.clearAnimation()

                //binding.buttonForSinging.text = "Start singing"
                Toast.makeText(activity,"Singing has stopped",Toast.LENGTH_LONG).show()
            } else {
                // Start animation
                animateSharkButton()
                // Start immediately
                updateWidgetHandler.postDelayed(updateWidgetRunnable, 0)

                //binding.buttonForSinging.text = "Stop singing"
                Toast.makeText(activity,"Singing has started",Toast.LENGTH_LONG).show()


            }
        }

        //Check for permissions
        initialize()

        // Generate folder for saving .wav later
        generateFolder()

        return binding.root
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