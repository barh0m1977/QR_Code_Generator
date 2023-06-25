package com.ibrahim.qrcodegenerator.ui.home


import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.Intent.getIntent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.ibrahim.qrcodegenerator.R
import com.ibrahim.qrcodegenerator.databinding.FragmentHomeBinding
import com.zzz1zzz.simplecolorpicker.ColorPickerListener
import com.zzz1zzz.simplecolorpicker.SimpleColorPicker
import java.io.IOException


class HomeFragment : Fragment() {


    lateinit var binding: FragmentHomeBinding
    private lateinit var adView: AdView
    private lateinit var mInterstitialAd: InterstitialAd
    lateinit var bitmap: Bitmap
    var color:String="000000"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(requireContext()) {
            Log.e("bml", "$it")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)


        val intent = requireActivity().intent
        val action = intent.action
        val type = intent.type
        if ("android.intent.action.SEND" == action && type != null && "application/vnd.android.package-archive" == type) {
            intent.flags
           val data= intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM).toString()
            val qrGenerate = QRGEncoder(data, null, QRGContents.Type.TEXT, 500)
            bitmap = qrGenerate.bitmap
            binding.imageView.setImageBitmap(bitmap)
            binding.imageView.visibility = View.VISIBLE


        }

        binding.color.setOnClickListener {
            SimpleColorPicker.Builder(requireContext())
                .setTitle("Select Color") // Optional
                .setListener(object : ColorPickerListener {
                    override fun onColorSelected(color: String) {
                        this@HomeFragment.color =color
                    }
                })
                .build()
                .show()
        }

        //generate QR code
        binding.button.setOnClickListener {
            if (binding.text.text!=null){

                if (color.isNotEmpty() and (color != "000000")){

                    val qrData = binding.text.text.toString()
                    val qrGenerate = QRGEncoder(qrData, null, QRGContents.Type.TEXT, 500)
                    qrGenerate.colorBlack = Color.parseColor(color)
                    bitmap = qrGenerate.bitmap
                    binding.imageView.setImageBitmap(bitmap)
                    binding.imageView.visibility = View.VISIBLE
                }else{
                    Toast.makeText(requireContext(), "choose your color", Toast.LENGTH_SHORT).show()
                }
            }

        }


        //save image  :-
        binding.imageView.setOnLongClickListener {
            val alertDialog =
                AlertDialog.Builder(requireContext()).setIcon(R.drawable.baseline_save_24)
                    .setTitle("SAVE").setMessage("do you want to save it in your gallery ?")
                    .setPositiveButton("yes") { _, _ ->
                        savePhotoToExternalStorage("${System.currentTimeMillis()}_QR_Generator", bitmap)
                    }.setNegativeButton("No") { d, _ ->
                        d.dismiss()
                    }.create().show()
            false

        }

        //ads
        adView = binding.adView
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        binding.adView.adListener = object : AdListener() {
            override fun onAdClicked() {
                super.onAdClicked()
                Toast.makeText(requireContext(), "clicked", Toast.LENGTH_SHORT).show()
            }
        }




        return binding.root
    }


    // save image in External Storage Method :-
    private fun savePhotoToExternalStorage(displayName: String, bmp: Bitmap): Boolean {

        val imageCollection =
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)


        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bmp.width)
            put(MediaStore.Images.Media.HEIGHT, bmp.height)
        }
        return try {
            requireContext().contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                requireContext().contentResolver.openOutputStream(uri).use { outputStream ->
                    if (!bmp.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                        throw IOException("Couldn't save bitmap")
                    } else Toast.makeText(requireContext(), "Done", Toast.LENGTH_SHORT).show()
                }
            } ?: throw IOException("Couldn't create MediaStore entry")
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    // price 9$ for 200 request in month
//    private fun generateQRCode() {
//        val url = "https://qrcode-monkey.p.rapidapi.com/qr/custom?data=https%3A%2F%2Fwww.qrcode-monkey.com&config=%7B%22bodyColor%22%3A%20%22%230277BD%22%2C%20%22body%22%3A%22mosaic%22%7D&download=true&file=png&size=600"
//
//
////        params["config"] = "your_config_value" // Add any additional configuration parameters if required
//        val inQueue=Volley.newRequestQueue(requireContext())
//        val jsonObjectRequest = object: JsonObjectRequest(
//
//            Request.Method.POST, url, null,
//            { response ->
//                val imageUrl = response.getJSONObject("qrImageUrls").getString("qrImageUrl")
//                Picasso.get().load(imageUrl).into(binding.imageView)
//                // Load the QR code image into the ImageView
//                // You can use a library like Picasso or Glide to handle image loading
//            },
//            { error ->
//               Log.e("bml",error.message.toString())
//            }
//        ){
//            override fun getHeaders(): MutableMap<String, String> {
//                val head=HashMap<String,String>()
////                head["content-type"]="application/json"
//                head["X-RapidAPI-Key"]="dd96fc88bamshffdef6c63cbf053p10321djsn7d14a4167ff0"
//                head["X-RapidAPI-Host"]="qrcode-monkey.p.rapidapi.com"
//
//                return head
//            }
//
//            override fun getParams(): MutableMap<String, String>? {
//                val params = HashMap<String, String>()
//                params["data"] = binding.text.text.toString()
//                return params
//            }
//        }
//
//        inQueue.add(jsonObjectRequest)
//    }



}