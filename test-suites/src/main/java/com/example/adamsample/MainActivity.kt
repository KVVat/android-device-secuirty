package com.example.adamsample

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.BuildCompat
import com.example.adamsample.databinding.ActivityMainBinding
import com.example.adamsample.utils.ReflectionUtils
import com.example.adamsample.utils.UniqueIDUtils
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

  private lateinit var binding:ActivityMainBinding
  private val TAG:String = "FCS_CKH_EXT_TEST"

  override fun onStart() {
    super.onStart()
    Log.d(TAG, "Booted");
  }

  override fun onStop() {
    super.onStop()
  }

  companion object {
    // Used to load the 'gtest' library on application startup.
    init {
      System.loadLibrary("native-lib")
    }
  }
}