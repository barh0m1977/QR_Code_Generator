package com.ibrahim.qrcodegenerator.ui.dashboard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.Image
import android.os.Bundle
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.ibrahim.qrcodegenerator.databinding.FragmentDashboardBinding
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraExecutor: ExecutorService
    private val multiFormatReader = MultiFormatReader()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 200
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        cameraExecutor = Executors.newSingleThreadExecutor()
        val hints = mapOf(
            com.google.zxing.DecodeHintType.TRY_HARDER to true,
            com.google.zxing.DecodeHintType.POSSIBLE_FORMATS to listOf(com.google.zxing.BarcodeFormat.QR_CODE)
        )
        multiFormatReader.setHints(hints)
        if (isCameraPermissionGranted()) {
            startCamera()
        } else {
            requestCameraPermission()
        }

        return binding.root
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.scannerView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                scanImageProxy(imageProxy)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Camera start failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun scanImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()

        try {
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val nv21 = yuv420ToNv21(mediaImage)
            val rotated = rotateNV21(nv21, mediaImage.width, mediaImage.height, rotationDegrees)

            val width = if (rotationDegrees == 90 || rotationDegrees == 270) mediaImage.height else mediaImage.width
            val height = if (rotationDegrees == 90 || rotationDegrees == 270) mediaImage.width else mediaImage.height

            val source = PlanarYUVLuminanceSource(
                rotated,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )

            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result = multiFormatReader.decode(bitmap)

            activity?.runOnUiThread {
                binding.text.apply {
                    visibility = View.VISIBLE
                    text = result.text
                    setOnClickListener { copyToClipboard(result.text) }
                }
            }
        } catch (_: NotFoundException) {
            // normal when no QR is found
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
            multiFormatReader.reset()
        }
    }

    private fun yuv420ToNv21(image: Image): ByteArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        return nv21
    }

    private fun rotateNV21(data: ByteArray, width: Int, height: Int, rotation: Int): ByteArray {
        return when (rotation) {
            90 -> {
                val rotated = ByteArray(data.size)
                var i = 0
                for (x in 0 until width) {
                    for (y in height - 1 downTo 0) {
                        rotated[i++] = data[y * width + x]
                    }
                }
                rotated
            }
            180 -> data.reversedArray()
            270 -> {
                val rotated = ByteArray(data.size)
                var i = 0
                for (x in width - 1 downTo 0) {
                    for (y in 0 until height) {
                        rotated[i++] = data[y * width + x]
                    }
                }
                rotated
            }
            else -> data
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("QR Code", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun isCameraPermissionGranted() = ContextCompat.checkSelfPermission(
        requireContext(), Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}
