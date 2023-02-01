package com.example.test_suites

import android.content.Context
import android.content.Intent
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
import java.util.*

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

  //Pattern Pixel 5e :
  private var PAT:Array<Point> = arrayOf(
    Point(230, 1800),
    Point(230, 850), Point(512,1500), Point(880, 1800)
  );

  lateinit var mContext: Context
  lateinit var mDevice: UiDevice
  lateinit var mUiHelper:UIAutomatorHelper
  @Before
  fun setup()
  {
    val mDevice_ = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    mDevice = mDevice_!!;
    mContext = InstrumentationRegistry.getInstrumentation().context;

    mDevice.freezeRotation();
    //sleepAndWakeUpDevice()
    client = adbRule.adb;
    mUiHelper = UIAutomatorHelper(mContext,mDevice_)
  }
  @After
  fun tearDown() {
    mDevice.unfreezeRotation()
  }
  //  -e com.malinskiy.adam.android.ADB_PORT 5037 (For emulator 5554)
  //  -e com.malinskiy.adam.android.ADB_HOST 127.0.0.1 (For emulator 10.0.2.2)
  //  -e com.malinskiy.adam.android.ADB_SERIAL [Serial Number of device]
  //'adb reverse tcp:5037 tcp:5037'
  // <option name="EXTRA_OPTIONS" value="-e com.malinskiy.adam.android.ADB_PORT 5554 -e com.malinskiy.adam.android.ADB_HOST 10.0.0.2 -e com.malinskiy.adam.android.ADB_SERIAL emulator-5554" />

  val TEST_PACKAGE = "com.example.test_suites";
  val PIN="1234"

  @Test
  fun testHealthyCase(){

    runBlocking {
      mUiHelper.sleepAndWakeUpDevice()
      mUiHelper.setScreenLockText("PIN",PIN)

      //Launch application
      val res = client.execute(
        ShellCommandRequest("am start ${TEST_PACKAGE}/.EncryptionFileActivity"))
      assertThat(res.output).isNotEqualTo("Starting")

      var result = waitLogcatLine(100,"FCS_CKH_EXT1_HIGH_UNLOCK")
      println(result?.text)
      assertThat(result?.text).isEqualTo("UNLOCKDEVICE:OK")

      mUiHelper.safeObjectClick("TEST",2000)

      Thread.sleep(1000);
      mDevice.executeShellCommand("input text ${PIN}")
      mDevice.pressEnter()
      result = waitLogcatLine(20,"FCS_CKH_EXT1_HIGH_AUTH")
      assertThat(result?.text).isEqualTo("AUTHREQUIRED:OK")
      Thread.sleep(1000);

      mUiHelper.resetScreenLockText(PIN)
    }
  }
  fun testAuthIsFailed(){
    //Check FCS_CKH_EXT1_HIGH_UNLOCK check failed if device is locked
  }

  fun testDeviceIsLocked(){
    //Check FCS_CKH_EXT1_HIGH_UNLOCK check failed if device is locked
  }

  fun waitLogcatLine(waitTime:Int,tagWait:String):LogcatResult? {
    var found = false
    var text= ""
    var tag= ""
    runBlocking {
      val deviceTimezoneString = client.execute(GetSinglePropRequest("persist.sys.timezone")).trim()
      val deviceTimezone = TimeZone.getTimeZone(deviceTimezoneString)
      val nowInstant = Instant.now()
      val request = ChanneledLogcatRequest(LogcatSinceFormat.DateString(nowInstant, deviceTimezoneString), modes = listOf())
      val channel = client.execute(request, this)

      // Receive logcat for max several seconds, wait and find certain tag text
      for (i in 1..waitTime) {
        var lines:List<LogLine> = channel.receive()
          .split("\n")
          .mapNotNull { LogLine.of(it, deviceTimezone) }
          .filterIsInstance<LogLine.Log>()
          .filter { it.level == 'D'}
          .filter {
            it.tag.equals(tagWait)
          }
        if(!lines.isEmpty()){
          //println(lines);
          tag = lines.get(0).tag
          text = lines.get(0).text
          found = true
          break;
        }
        delay(100)
      }
      channel.cancel()
    }
    if(found) {
      return LogcatResult(tag, text)
    } else {
      return null
    }
  }
}