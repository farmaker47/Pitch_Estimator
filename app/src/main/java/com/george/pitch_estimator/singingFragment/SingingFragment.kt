package com.george.pitch_estimator.singingFragment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.george.pitch_estimator.PitchModelExecutor
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

            // Start writing .wav
            if (viewModel._singingRunning) {
                viewModel.stopSinging()
            } else {
                viewModel.startSinging()
            }
        }

        //Check for permissions
        initialize()

        // Generate folder for saving .wav later
        generateFolder()

        return binding.root
    }

    private fun generateFolder() {
        val root =
            File(Environment.getExternalStorageDirectory(), "Pitch Estimator")
        if (!root.exists()) {
            root.mkdirs()
        }
    }

    fun hasPermissions(
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