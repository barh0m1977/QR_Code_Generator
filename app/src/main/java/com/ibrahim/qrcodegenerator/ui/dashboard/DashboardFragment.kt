package com.ibrahim.qrcodegenerator.ui.dashboard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.budiyev.android.codescanner.*
import com.ibrahim.qrcodegenerator.databinding.FragmentDashboardBinding


class DashboardFragment : Fragment() {
lateinit var codeScanner:CodeScanner
    lateinit var binding:FragmentDashboardBinding
    private val  PERMISSION_REQUEST_CODE = 200
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {


        binding = FragmentDashboardBinding.inflate(inflater, container, false)



        codeScanner= CodeScanner(requireContext(),binding.scannerView)
        if (checkPermission()) {

            // Parameters (default values)
            codeScanner.camera = CodeScanner.CAMERA_BACK // or CAMERA_FRONT or specific camera id
            codeScanner.formats = CodeScanner.ALL_FORMATS // list of type BarcodeFormat,
            // ex. listOf(BarcodeFormat.QR_CODE)
            codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
            codeScanner.scanMode = ScanMode.SINGLE // or CONTINUOUS or PREVIEW
            codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
            codeScanner.isFlashEnabled = false // Whether to enable flash or not

            // Callbacks
            codeScanner.decodeCallback = DecodeCallback {
                activity?.runOnUiThread {
                    binding.text.visibility= View.VISIBLE
                    binding.text.setText("${it.text}")
                }
            }
            codeScanner.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Camera initialization error: ${it.message}",
                        Toast.LENGTH_LONG).show()
                    binding.text.setOnClickListener {
                        val cm: ClipboardManager =
                            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData=ClipData.newPlainText("QR",binding.text.text.toString())
                        cm.setPrimaryClip(clipData)

                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            codeScanner.startPreview()

            binding.scannerView.setOnClickListener {
                codeScanner.startPreview()
            }



        } else {
            requestPermission()
        }



        return binding.root
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        super.onPause()
        codeScanner.releaseResources()
    }


    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(), arrayOf<String>(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Permission Granted", Toast.LENGTH_SHORT)
                    .show()


            } else {
                Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT)
                    .show()
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission()
                }
            }
        }
    }


}