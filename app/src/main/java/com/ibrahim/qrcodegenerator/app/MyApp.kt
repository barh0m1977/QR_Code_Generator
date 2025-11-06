package com.ibrahim.qrcodegenerator.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApp:Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
//        CoroutineScope(Dispatchers.IO).launch{
//            MobileAds.initialize(this@MyApp)
//        }
    }
}