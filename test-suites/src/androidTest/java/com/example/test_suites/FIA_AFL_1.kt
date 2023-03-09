package com.example.test_suites

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.example.test_suites.utils.UIAutomatorHelper
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)//Execute methods in order of appearance
class FIA_AFL_1 {

  private val LONG_TIMEOUT = 5000L
  private val SHORT_TIMEOUT = 1000L
  private val PIN = "1234"
  private val PASSWORD = "aaaa"

  private lateinit var mDevice: UiDevice
  private lateinit var mContext: Context
  private lateinit var mTargetContext: Context
  lateinit var mUiHelper:UIAutomatorHelper

  //Pattern Pixel 5e :
  private var PATS:Map<String,Array<Point>> = mapOf(
    Pair("sdk_gphone64_x86_64-33",
         arrayOf(Point(230, 1800), Point(230, 850),Point(512,1500),Point(880, 1800))
    ),
    Pair("Pixel 5a-33",
         arrayOf(Point(230, 1800), Point(230, 850),Point(512,1500),Point(880, 1800))
    ),

  )

  private var PAT:Array<Point> = arrayOf(Point(230, 1800),
    Point(230, 850),Point(512,1500),Point(880, 1800));


  @Before
  fun setUp() {
    val mDevice_ = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    mDevice = mDevice_!!;

    mContext = InstrumentationRegistry.getInstrumentation().context;
    mTargetContext = InstrumentationRegistry.getInstrumentation().targetContext


    mDevice.freezeRotation();
    sleepAndWakeUpDevice()

    mUiHelper = UIAutomatorHelper(mContext,mDevice_)


  }

  @After
  fun tearDown() {
    mDevice.unfreezeRotation()
  }

  //Before running this test cases
  //  You should disable screen lock first

  @Test
  fun T01_testPINLockSuccess(){
    if(mUiHelper.isLockScreenEnbled()){
      println("*** It requires to disable screen lock to run this test ***");
      assert(false)
    }
    try {
      mUiHelper.sleepAndWakeUpDevice()
      mUiHelper.setScreenLockText("PIN", PIN)
      //Launch application
      mDevice.waitForIdle()
      mUiHelper.sleepAndWakeUpDevice()//LockScreen

      Thread.sleep(1000);
      mUiHelper.swipeUp()
      mDevice.executeShellCommand("input text ${PIN}")
      Thread.sleep(1000);
      mDevice.pressEnter()
      Thread.sleep(1000);

    } finally {
      mUiHelper.resetScreenLockText(PIN)
    }
    //if the operation above fails, there must be a problem.
    assert(!mUiHelper.isLockScreenEnbled())
  }

  @Test
  fun T02_testPINLockFailure(){
    if(mUiHelper.isLockScreenEnbled()){
      println("*** It requires to disable screen lock to run this test ***");
      assert(false)
    }
    try {
      mUiHelper.sleepAndWakeUpDevice()
      mUiHelper.setScreenLockText("PIN", PIN)
      //Launch application
      mDevice.waitForIdle()
      mUiHelper.sleepAndWakeUpDevice()//LockScreen

      Thread.sleep(1000);
      mUiHelper.swipeUp()
      for (i in 0..4) {
        mDevice.executeShellCommand("input text 0000")
        Thread.sleep(1000);
        mDevice.pressEnter()
        Thread.sleep(1000);
      }
      //if it fails 5 times, 30 sec trial delay is applied
      mUiHelper.safeObjectClick("OK",1000);
      Thread.sleep(30 * 1000)//wait 30 sec
    } finally {
      mUiHelper.sleepAndWakeUpDevice()//LockScreen
      Thread.sleep(1000);
      mUiHelper.swipeUp()
      Thread.sleep(1000);
      //Need to unlock screen
      mDevice.executeShellCommand("input text ${PIN}")
      Thread.sleep(1000);
      mDevice.pressEnter()
      Thread.sleep(1000);
      mUiHelper.resetScreenLockText(PIN)
    }
    //if the operation above fails, there must be a problem.
    assert(!mUiHelper.isLockScreenEnbled())
  }
  @Test
  fun T11_testPassLockSuccess(){
    if(mUiHelper.isLockScreenEnbled()){
      println("*** It requires to disable screen lock to run this test ***");
      assert(false)
    }
    try {
      mUiHelper.sleepAndWakeUpDevice()
      mUiHelper.setScreenLockText("Password", PASSWORD)
      //Launch application
      mDevice.waitForIdle()
      mUiHelper.sleepAndWakeUpDevice()//LockScreen

      Thread.sleep(1000);
      mUiHelper.swipeUp()
      mDevice.executeShellCommand("input text ${PASSWORD}")
      Thread.sleep(1000);
      mDevice.pressEnter()
      Thread.sleep(1000);

    } finally {
      mUiHelper.resetScreenLockText(PASSWORD)
    }
    //if the operation above fails, there must be a problem.
    assert(!mUiHelper.isLockScreenEnbled())
  }

  @Test
  fun testPassLockFailure(){
    if(mUiHelper.isLockScreenEnbled()){
      println("*** It requires to disable screen lock to run this test ***");
      assert(false)
    }
    try {
      mUiHelper.sleepAndWakeUpDevice()
      mUiHelper.setScreenLockText("Password", PASSWORD)
      //Launch application
      mDevice.waitForIdle()
      mUiHelper.sleepAndWakeUpDevice()//LockScreen

      Thread.sleep(1000);
      mUiHelper.swipeUp()
      for (i in 0..4) {
        mDevice.executeShellCommand("input text bbbb")
        Thread.sleep(1000);
        mDevice.pressEnter()
        Thread.sleep(1000);
      }
      //if it fails 5 times, 30 sec trial delay is applied
      mUiHelper.safeObjectClick("OK",1000);
      Thread.sleep(30 * 1000)//wait 30 sec
    } finally {
      mUiHelper.sleepAndWakeUpDevice()//LockScreen
      Thread.sleep(1000);
      mUiHelper.swipeUp()
      Thread.sleep(1000);
      //Need to unlock screen
      mDevice.executeShellCommand("input text ${PASSWORD}")
      Thread.sleep(1000);
      mDevice.pressEnter()
      Thread.sleep(1000);
      mUiHelper.resetScreenLockText(PIN)
    }
    //if the operation above fails, there must be a problem.
    assert(!mUiHelper.isLockScreenEnbled())

  }
  /*
  @Test
  fun testPatternSuccess(){
    if(mUiHelper.isLockScreenEnbled()){
      println("*** It requires to disable screen lock to run this test ***");
      assert(false)
    }
  }
  @LargeTest
  fun testPatternFailure(){
    if(mUiHelper.isLockScreenEnbled()){
      println("*** It requires to disable screen lock to run this test ***");
      assert(false)
    }
  }
  */

  /*
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

    assert(isLockScreenEnbled())
    runBlocking {
      sleepAndWakeUpDevice()
      mDevice.waitForIdle()
      Thread.sleep(1000);
      swipeUp()
      mDevice.swipe(PAT,4);
      Thread.sleep(2000);
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
      mDevice.executeShellCommand("input text ${passInput}")
      mDevice.pressEnter()
      Thread.sleep(1000);
      launchSettings(Settings.ACTION_SECURITY_SETTINGS);
      Thread.sleep(1000);
      swipeUp()
      Thread.sleep(1000);
      safeObjectClick("Screen lock",2000)
      mDevice.executeShellCommand("input text ${passInput}")
      mDevice.pressEnter()
      Thread.sleep(1000);
      safeObjectClick("None",2000)
      safeObjectClick("Delete",2000)
    }
  }
  */
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
    Thread.sleep(LONG_TIMEOUT)
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