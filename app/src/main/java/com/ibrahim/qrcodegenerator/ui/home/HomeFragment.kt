package com.ibrahim.qrcodegenerator.ui.home

import android.app.AlertDialog
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.ibrahim.qrcodegenerator.KqrKits
import com.ibrahim.qrcodegenerator.R
import com.ibrahim.qrcodegenerator.databinding.FragmentHomeBinding
import androidx.core.graphics.toColorInt
import java.io.IOException

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var bitmap: Bitmap
    private var color: String = "#000000"

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        val logo = BitmapFactory.decodeResource(resources, R.drawable.qr_code)

//        binding.button.setOnClickListener {
//            val qrText = binding.text.text?.toString()?.trim()
//            if (qrText.isNullOrEmpty()) {
//                Toast.makeText(requireContext(), "Please enter text or URL", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            val qr = KqrKits.generate(
//                text = qrText,
//                size = 1024,
//                fgColor = color.toColorInt(),
//                bgColor = Color.WHITE,
//                body = "circle",
//                eye = "square",
//                logo = logo
//            )
//
//            bitmap = qr
//            binding.imageView.setImageBitmap(qr)
//            binding.imageView.visibility = View.VISIBLE
//        }

        binding.button.setOnClickListener {
            val bottomSheet = ComposeStyleBottomSheet { fg, bg, eye, body, logoUri ->
                color = fg
                val qrText = binding.text.text?.toString()?.trim()
                if (qrText.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "Please enter text or URL", Toast.LENGTH_SHORT).show()
                    return@ComposeStyleBottomSheet
                }
                val logoBitmap = logoUri?.let {
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
                }

                val qr = KqrKits.generate(
                    text = qrText,
                    size = 1024,
                    margin = 80,
                    fgColor = Color.parseColor(fg),
                    bgColor = Color.parseColor(bg),
                    eye = eye.lowercase(),
                    body = body.lowercase(),
                    bodyScale = 0.85f,
                    logo = logoBitmap,
                    errorCorrection = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
                )

                bitmap = qr
                binding.imageView.setImageBitmap(qr)
                binding.imageView.visibility = View.VISIBLE
            }
            bottomSheet.show(parentFragmentManager, "StyleBottomSheet")
        }

        binding.imageView.setOnLongClickListener {
            if (!::bitmap.isInitialized) {
                Toast.makeText(requireContext(), "No QR code to save", Toast.LENGTH_SHORT).show()
                return@setOnLongClickListener false
            }

            AlertDialog.Builder(requireContext())
                .setIcon(R.drawable.baseline_save_24)
                .setTitle("SAVE")
                .setMessage("Do you want to save it in your gallery?")
                .setPositiveButton("Yes") { _, _ ->
                    savePhotoToExternalStorage("${System.currentTimeMillis()}_QR_Generator", bitmap)
                }
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                .create()
                .show()
            false
        }

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun savePhotoToExternalStorage(displayName: String, bmp: Bitmap): Boolean {
        val imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bmp.width)
            put(MediaStore.Images.Media.HEIGHT, bmp.height)
        }

        return try {
            requireContext().contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                requireContext().contentResolver.openOutputStream(uri).use { outputStream ->
                    if (outputStream == null || !bmp.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                        throw IOException("Couldn't save bitmap")
                    } else {
                        Toast.makeText(requireContext(), "Saved to gallery", Toast.LENGTH_SHORT).show()
                    }
                }
            } ?: throw IOException("Couldn't create MediaStore entry")
            true
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
