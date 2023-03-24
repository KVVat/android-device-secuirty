package com.example.test_suites

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Point
import android.provider.Settings
import androidx.security.crypto.MasterKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import com.example.test_suites.utils.LogLine
import com.example.test_suites.utils.LogcatResult
import com.example.test_suites.utils.UIAutomatorHelper
import com.malinskiy.adam.junit4.android.rule.Mode
import com.malinskiy.adam.junit4.android.rule.sandbox.SingleTargetAndroidDebugBridgeClient
import com.malinskiy.adam.junit4.rule.AdbRule
import com.malinskiy.adam.request.logcat.ChanneledLogcatRequest
import com.malinskiy.adam.request.logcat.LogcatSinceFormat
import com.malinskiy.adam.request.prop.GetSinglePropRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class FCS_CKH_EXT1_High2 {

  lateinit var mContext: Context
  lateinit var mTargetContext: Context //Application Context
  lateinit var mDevice: UiDevice
  lateinit var mUiHelper:UIAutomatorHelper

  val TEST_PACKAGE = "com.example.test_suites";
  val PIN="1234"
  val PREF_NAME:String = "FCS_CKH_EXT_PREF"

  @Before
  fun setup()
  {
    val mDevice_ = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    mDevice = mDevice_!!;
    mContext = InstrumentationRegistry.getInstrumentation().context;
    mTargetContext = InstrumentationRegistry.getInstrumentation().targetContext
    mDevice.freezeRotation();

    mUiHelper = UIAutomatorHelper(mContext,mDevice_)

    println("** A Junit test case for FCS_CKH_EXT1_High started on "+ LocalDateTime.now()+" **")

  }
  @After
  fun tearDown() {
    mDevice.unfreezeRotation()
  }

   //'adb reverse tcp:5037 tcp:5037'
  // <option name="EXTRA_OPTIONS" value="-e com.malinskiy.adam.android.ADB_PORT 5554 -e com.malinskiy.adam.android.ADB_HOST 10.0.0.2 -e com.malinskiy.adam.android.ADB_SERIAL emulator-5554" />



  @Test
  fun testHealthyCase(){


    println("*** The test case verifies the encryption key options regarding screenlock/authentication ***");

    if(mUiHelper.isLockScreenEnbled()){
      println("*** It requires to disable screen lock to run this test ***");
      assert(false)
    }

    runBlocking {
      try {
        mUiHelper.sleepAndWakeUpDevice()
        mUiHelper.setScreenLockText("PIN", PIN)
        //Launch application
        val res = mDevice.executeShellCommand(
           "am start -n ${TEST_PACKAGE}/.EncryptionFileActivity")
        println("Encryption File Activity Start:"+res)
        assertThat(res).isNotEqualTo("Starting")

        Thread.sleep(1000);
        mUiHelper.safeObjectClick("TEST",2000)
        Thread.sleep(5000);

        mDevice.executeShellCommand("input text ${PIN}")
        mDevice.pressEnter()
        Thread.sleep(2000);
      } finally {
        mUiHelper.resetScreenLockText(PIN)
      }
      ////////////////////////////////////////////////
      //Check preference to see result.
      //

      val pf:SharedPreferences =
        mTargetContext.getSharedPreferences(PREF_NAME,Context.MODE_PRIVATE)
      val result_auth = pf.getString("AUTHREQUIRED","")
      val result_unlock = pf.getString("UNLOCKDEVICE","")
      pf.edit().putString("Test","test")

      println("Expected:AUTHREQUIRED:OK,UNLOCKDEVICE:OK")
      println("AUTHREQUIRED:"+result_auth+",UNLOCKDEVICE:"+result_unlock);

      //Verify
      assertThat(result_auth).isEqualTo("OK")
      assertThat(result_unlock).isEqualTo("OK")
    }
  }

  @Test
  fun testAuthIsFailed(){
    if(mUiHelper.isLockScreenEnbled()){
      println("*** It requires to disable screen lock to run this test ***");
      assert(false)
    }
    //Check FCS_CKH_EXT1_HIGH_UNLOCK check failed if device is locked
    runBlocking {
      try {
        mUiHelper.sleepAndWakeUpDevice()
        mUiHelper.setScreenLockText("PIN", PIN)
        //Launch application
        val res = mDevice.executeShellCommand(
          "am start -n ${TEST_PACKAGE}/.EncryptionFileActivity")
        assertThat(res).isNotEqualTo("Starting")
        //not authenticate
        Thread.sleep(1000);
        mUiHelper.sleepAndWakeUpDevice()
        //Sleep device -> lock screen
        Thread.sleep(5000);
      } finally {
        mUiHelper.resetScreenLockText(PIN)
      }
      val pf:SharedPreferences =
        mTargetContext.getSharedPreferences(PREF_NAME,Context.MODE_PRIVATE)
      val result_auth = pf.getString("AUTHREQUIRED","")
      val result_unlock = pf.getString("UNLOCKDEVICE","")
      pf.edit().putString("Test","test")
      println("Expected:AUTHREQUIRED:NG,UNLOCKDEVICE:NG")
      println("AUTHREQUIRED:"+result_auth+",UNLOCKDEVICE:"+result_unlock);

      //Verify
      assertThat(result_auth).isEqualTo("NG")
      assertThat(result_unlock).isEqualTo("NG")
    }
  }
}