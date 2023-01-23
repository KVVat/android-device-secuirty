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
  private lateinit var receiver : BootReceiver // For Checking DirectBoot
  private val TAG:String = "FCS_CKH_EXT_TEST"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater);
    setContentView(binding.root)

    receiver = BootReceiver()
    IntentFilter(Intent.ACTION_BOOT_COMPLETED).also {
      registerReceiver(receiver,it)
    }
    IntentFilter(Intent.ACTION_LOCKED_BOOT_COMPLETED).also {
      registerReceiver(receiver,it)
    }
  }

  override fun onStart() {
    super.onStart()


    val sharedPref = storageContext().getSharedPreferences(TAG, Context.MODE_PRIVATE)
    sharedPref.edit().putString(TAG,"Success").apply()
    Thread.sleep(500)

    Log.d(TAG, "Booted");

  }
  fun storageContext():Context{
    var storageContext:Context;
    if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      // All N devices have split storage areas, but we may need to
      // move the existing preferences to the new device protected
      // storage area, which is where the data lives from now on.
      var deviceContext:Context = applicationContext.createDeviceProtectedStorageContext();
      if (!deviceContext.moveSharedPreferencesFrom(applicationContext,
                                                   TAG)) {
        Log.w("storageContext", "Failed to migrate shared preferences.");
      }
      Log.d("storageContext","save preference to device context");
      storageContext = deviceContext;
    } else {
      storageContext = applicationContext;
    }
    return storageContext;
  }


/*
  fun createTestFileViaCertainContext(prefix: String, targetContext: Context?, input: InputStream){
    val content = input.bufferedReader().use(BufferedReader::readText)

    try {
      val outputStream: FileOutputStream? = targetContext!!.openFileOutput(prefix+"test.txt",
                                                                           MODE_PRIVATE)
      outputStream?.apply {
        write(content.toByteArray(Charset.forName("UTF-8")))
        flush()
        close()
      }
    } catch (ex: IOException) {
      throw RuntimeException("IOException")
    }
  }*/
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