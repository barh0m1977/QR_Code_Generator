package com.ibrahim.qrcodegenerator.ui.shorturl

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.ibrahim.qrcodegenerator.databinding.FragmentShortUrlBinding


class ShortUrlFragment : Fragment() {
    lateinit var binding: FragmentShortUrlBinding
    private lateinit var adView: AdView
    private lateinit var mInterstitialAd: InterstitialAd
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentShortUrlBinding.inflate(inflater, container, false)

        binding.button.setOnClickListener {
            if (binding.text.text != null) {
                shortenUrl(binding.text.text.toString())
            }
        }

        binding.shortenText.setOnClickListener {
            if (binding.shortenText.text != null) {
                val clipboard: ClipboardManager? =
                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                val clip: ClipData =
                    ClipData.newPlainText("short_link", binding.shortenText.text.toString())
                clipboard?.setPrimaryClip(clip)

            }
        }


        //ads
//        adView = binding.adView
//        val adRequest = AdRequest.Builder().build()
//        adView.loadAd(adRequest)
//
//        binding.adView.adListener = object : AdListener() {
//            override fun onAdClicked() {
//                super.onAdClicked()
//                Toast.makeText(requireContext(), "clicked", Toast.LENGTH_SHORT).show()
//            }
//        }

        return binding.root
    }

    private fun shortenUrl(text: String) {
        // API endpoint (simple mode → returns plain text short URL)
        val url = "https://is.gd/create.php?format=simple&url=$text"

        val queue = Volley.newRequestQueue(requireContext())

        val request = StringRequest(
            Request.Method.GET, url,
            { response ->
                // response IS the shortened URL (plain text)
                Log.e("bml", response)
                binding.shortenText.text = response
            },
            { error ->
                Log.e("bml", error.message ?: "Unknown error")
            }
        )

        queue.add(request)
    }

}
