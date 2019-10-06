package com.example.mpp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tubi.test.interceptor.WaterfallEntry

class MainActivity : AppCompatActivity() {

    //private val gooleApi = RainBarrel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //gooleApi.initialize(null)
        WaterfallEntry.start("myandroid-12345")
    }
}
