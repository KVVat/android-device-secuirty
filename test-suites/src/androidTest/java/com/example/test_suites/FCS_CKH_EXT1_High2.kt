package com.example.test_suites

import android.content.Context
import android.graphics.Point
import androidx.security.crypto.MasterKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.example.test_suites.utils.UIAutomatorHelper
import com.malinskiy.adam.junit4.android.rule.Mode
import com.malinskiy.adam.junit4.android.rule.sandbox.SingleTargetAndroidDebugBridgeClient
import com.malinskiy.adam.junit4.rule.AdbRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class FCS_CKH_EXT1_High2 {
  //https://github.com/stravag/android-sample-biometric-prompt/blob/master/app/src/main/java/ch/ranil/sample/android/biometricpromptsample/BiometricPromptManager.kt
  @get:Rule
  val adbRule = AdbRule(mode = Mode.ASSERT)
  lateinit var client: SingleTargetAndroidDebugBridgeClient;

  private lateinit var mDevice: UiDevice
  private var mContext: Context? = null

  //Pattern Pixel 5e :
  private var PAT:Array<Point> = arrayOf(
    Point(230, 1800),
    Point(230, 850), Point(512,1500), Point(880, 1800)
  );

  lateinit var appContext:Context;
  lateinit var masterKeyAlias:String;
  lateinit var keyUnlockDeviceTest:MasterKey;
  lateinit var mUiHelper:UIAutomatorHelper
  @Before
  fun setup()
  {
    appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val mDevice_ = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    mDevice = mDevice_!!;
    mContext = InstrumentationRegistry.getInstrumentation().context;

    mDevice.freezeRotation();
    //sleepAndWakeUpDevice()
    client = adbRule.adb;
    mUiHelper = UIAutomatorHelper(appContext,mDevice_)

  }
  @After
  fun tearDown() {
    mDevice.unfreezeRotation()
  }

  @Test
  fun test(){
    runBlocking {
      //checkEncryptedSharedPreference(norm_enc_data,PREF_NAME)
    }
  }

}