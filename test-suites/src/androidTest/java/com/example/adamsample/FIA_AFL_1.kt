package com.example.adamsample

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.graphics.PointF
import android.provider.Settings
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.malinskiy.adam.junit4.android.rule.Mode
import com.malinskiy.adam.junit4.android.rule.sandbox.SingleTargetAndroidDebugBridgeClient
import com.malinskiy.adam.junit4.rule.AdbRule
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

private const val LONG_TIMEOUT = 5000L
private const val SHORT_TIMEOUT = 1000L
private const val PIN = "1234"
private const val PASSWORD = "aaaa"

/**
 *
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)//Execute methods in order of appearance
class `FIA_AFL_1_Authentication` {

  @get:Rule
  val adbRule = AdbRule(mode = Mode.ASSERT)
  lateinit var client:SingleTargetAndroidDebugBridgeClient;

  private lateinit var mDevice: UiDevice
  private var mContext: Context? = null

  //Pattern Pixel 5e :
  private var PAT:Array<Point> = arrayOf(Point(230, 1800),
    Point(230, 850),Point(512,1500),Point(880, 1800));



  @Before
  fun setUp() {
    val mDevice_ = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    mDevice = mDevice_!!;

    mContext = InstrumentationRegistry.getInstrumentation().context;
    mDevice.freezeRotation();
    sleepAndWakeUpDevice()
    client = adbRule.adb;
  }

  @After
  fun tearDown() {
    mDevice.unfreezeRotation()
  }

  //Before to start this test cases
  //  1 - You should disable screen lock first
  //  2 - Try this command below before executing to ensure execute adb command with instrumentation test
  //  'adb reverse tcp:5037 tcp:5037'
  //  3 - adam library requires extra options for execution
  //  -e com.malinskiy.adam.android.ADB_PORT 5037
  //  -e com.malinskiy.adam.android.ADB_HOST 127.0.0.1
  //  -e com.malinskiy.adam.android.ADB_SERIAL [Serial Number of device]





  @Test
  fun T10_testSetupPINLock() {
    println("isLock=>"+isLockScreenEnbled())

    if(!isLockScreenEnbled()){
      println("** To execute this test case you should disable device lockscreen setting first **")
      //assert(!isLockScreenEnbled())
      //System.exit(1)
    }
    runBlocking {
      sleepAndWakeUpDevice()
      launchSettings(Settings.ACTION_SECURITY_SETTINGS);
      swipeUp()
      Thread.sleep(1000);
      safeObjectClick("Screen lock",2000)
      safeObjectClick("PIN",2000)
      for(i in 0..1) {
        client.execute(ShellCommandRequest("input text ${PIN}"))
        Thread.sleep(1000);
        mDevice.pressEnter()
        Thread.sleep(1000);
      }
      safeObjectClick("Done",2000)
    }
  }

  @Test
  fun T11_unlockScreenPINSuccess() {
    //assert(fals)
    assert(isLockScreenEnbled())
    runBlocking {
      sleepAndWakeUpDevice()
      mDevice.waitForIdle()
      Thread.sleep(1000);
      swipeUp()
      client.execute(ShellCommandRequest("input text ${PIN}"))
      Thread.sleep(1000);
      mDevice.pressEnter()
      Thread.sleep(1000);
    }
  }

  @LargeTest
  fun T12_unlockScreenPasswordFailed() {
    assert(isLockScreenEnbled())
    runBlocking {
      for(j in 0 .. 1) {
        sleepAndWakeUpDevice()
        mDevice.waitForIdle()
        Thread.sleep(1000);
        swipeUp()
        for (i in 0..4) {
          client.execute(ShellCommandRequest("input text 0000"))
          Thread.sleep(1000);
          mDevice.pressEnter()
          Thread.sleep(1000);
        }
        mDevice.pressEnter()
        Thread.sleep(30 * 1000)//wait 30 se
      }
      //Success
      sleepAndWakeUpDevice()
      mDevice.waitForIdle()
      Thread.sleep(1000);
      swipeUp()
      client.execute(ShellCommandRequest("input text 0413"))
      Thread.sleep(1000);
      mDevice.pressEnter()
      Thread.sleep(1000);
    }
  }
  @Test
  fun T13_setupLockNone(){
    setupLockNone(PIN)
  }
  @Test
  fun T20_testSetupPasswordLock() {
    //to start this test you should disable screen lock first
    assert(!isLockScreenEnbled())
    runBlocking {
      sleepAndWakeUpDevice()
      launchSettings(Settings.ACTION_SECURITY_SETTINGS);
      swipeUp()
      Thread.sleep(1000);
      safeObjectClick("Screen lock",2000)
      safeObjectClick("Password",2000)
      for(i in 0..1) {
        client.execute(ShellCommandRequest("input text ${PASSWORD}"))
        Thread.sleep(1000);
        mDevice.pressEnter()
        Thread.sleep(1000);
      }
      safeObjectClick("Done",2000)
    }
  }
  @Test
  fun T21_unlockScreenPaswordSuccess() {
    //assert(fals)
    assert(isLockScreenEnbled())
    runBlocking {
      sleepAndWakeUpDevice()
      mDevice.waitForIdle()
      Thread.sleep(1000);
      swipeUp()
      client.execute(ShellCommandRequest("input text ${PASSWORD}"))
      Thread.sleep(1000);
      mDevice.pressEnter()
      Thread.sleep(1000);
    }
  }
  @Test
  fun T23_setupLockNone(){
    setupLockNone(PASSWORD)
  }

  fun T31_testSetupPatternLock() {
    assert(!isLockScreenEnbled())
    runBlocking {
      sleepAndWakeUpDevice()
      launchSettings(Settings.ACTION_SECURITY_SETTINGS);
      swipeUp()
      Thread.sleep(1000);
      safeObjectClick("Screen lock",2000)
      safeObjectClick("Pattern",2000)
      for(i in 0..1) {
        mDevice.swipe(PAT,4);
        //mDevice.pressEnter()
        Thread.sleep(2000);
        if(i == 0){
          safeObjectClick("Next",2000)
        } else {
          safeObjectClick("Confirm",2000)
        }
        Thread.sleep(2000);
      }
      safeObjectClick("Done",2000)
    }
  }

  @Test
  fun T32_unlockScreenPatternSuccess() {
    //assert(fals)
    assert(isLockScreenEnbled())
    runBlocking {
      sleepAndWakeUpDevice()
      mDevice.waitForIdle()
      Thread.sleep(1000);
      swipeUp()
      mDevice.swipe(PAT,4);
      //mDevice.pressEnter()
      Thread.sleep(2000);
      //Device.pressEnter()
      //Thread.sleep(1000);
    }
    println(isLockScreenEnbled());
  }

  fun setupLockNone(passInput:String) {
    //to start this test you should disable screen lock first
    //assert(isLockScreenEnbled())
    runBlocking {
      sleepAndWakeUpDevice()
      mDevice.waitForIdle()
      Thread.sleep(1000);
      swipeUp()
      Thread.sleep(1000);
      client.execute(ShellCommandRequest("input text ${passInput}"))
      mDevice.pressEnter()
      Thread.sleep(1000);
      launchSettings(Settings.ACTION_SECURITY_SETTINGS);
      Thread.sleep(1000);
      swipeUp()
      Thread.sleep(1000);
      safeObjectClick("Screen lock",2000)
      client.execute(ShellCommandRequest("input text ${passInput}"))
      mDevice.pressEnter()
      Thread.sleep(1000);
      safeObjectClick("None",2000)
      safeObjectClick("Delete",2000)
    }
  }

  fun safeObjectClick(objectLabel:String,timeout:Long){
    //Ignore exception in case object is not found to suppress unintentional/varying behaviour
    try {
      mDevice.wait(Until.findObject(By.text(objectLabel)),timeout).click();
    } catch(ex:java.lang.NullPointerException){
      Log.d("TAG", "Click $objectLabel ignored")
    }
  }

  fun launchSettings(page:String){
    val intent = Intent(page)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    mContext!!.startActivity(intent)
    Thread.sleep(LONG_TIMEOUT )
  }
  fun isLockScreenEnbled():Boolean{
    val km = mContext!!.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    return km.isKeyguardSecure
  }
  fun sleepAndWakeUpDevice() {
    mDevice.sleep()
    Thread.sleep(1000)
    mDevice.wakeUp()
  }
  fun swipeUp(){
    mDevice.swipe(mDevice.getDisplayWidth() / 2, mDevice.getDisplayHeight(),
                  mDevice.getDisplayWidth() / 2, 0, 30);
    Thread.sleep(SHORT_TIMEOUT);
  }
}