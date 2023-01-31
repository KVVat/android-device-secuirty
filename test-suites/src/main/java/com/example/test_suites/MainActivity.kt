package com.example.test_suites

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.test_suites.databinding.ActivityMainBinding

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