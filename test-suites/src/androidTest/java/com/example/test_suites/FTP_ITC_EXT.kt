package com.example.test_suites

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.example.test_suites.utils.NetworkHelper
import com.example.test_suites.utils.UIAutomatorHelper
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import org.junit.After
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)//Execute methods in order of appearance
class FTP_ITC_EXT {

  private val LONG_TIMEOUT = 5000L
  private val SHORT_TIMEOUT = 1000L

  private lateinit var mDevice: UiDevice
  private lateinit var mContext: Context
  private lateinit var mTargetContext: Context
  lateinit var mUiHelper:UIAutomatorHelper

  @Before
  fun setUp() {
    val mDevice_ = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    mDevice = mDevice_!!;

    mContext = InstrumentationRegistry.getInstrumentation().context;
    mTargetContext = InstrumentationRegistry.getInstrumentation().targetContext

    mDevice.freezeRotation();
    mUiHelper = UIAutomatorHelper(mContext,mDevice_)
  }

  @After
  fun tearDown() {
    mDevice.unfreezeRotation()
  }

  @Test
  fun testLaunchPcap()
  {
    //adb shell am start -e action start -e pcap_dump_mode pcap_file
    // -e pcap_name traffic.pcap -n com.emanuelef.remote_capture/.activities.CaptureCtrl
    // -e app_filter com.exmple.test_suites
    //adb shell am force-stop com.emanuelf.remote_capture
    //adb pull /storage/emulated/0/Download/PCAPdroid/traffic.pcap

    println(mContext.packageName);//com.example.test_suites.test
    println(mTargetContext.packageName);//com.example.test_suites

    //insall PCAPDroid into the TSF
    //Run PCAPDroid for scanning
    mDevice.executeShellCommand(
      "am start -n com.emanuelef.remote_capture/.activities.CaptureCtrl"+
            " -e action start"+
            " -e pcap_dump_mode pcap_file"+
            " -e pcap_name traffic_expire.pcap"
    )
    //To execute this operation correctly we shouldn't sleep the target devices
    //action =>  start stop status
    //click button placed outside the process
    val packageName:String = "com.emanuelef.remote_capture"
    val fullCartButtonResourceId = packageName + ":id/allow_btn";
    val allowButton = mDevice.findObject(UiSelector().resourceId(fullCartButtonResourceId))
    if(allowButton.exists()){
      allowButton.click()
      Thread.sleep(500);
      allowButton.click()
    }
    Thread.sleep(1000*10);
    //Open a connection(?) on the URL(??) and cast the response(???)
    var ret:Int = NetworkHelper.testHttpURLConnection("https://expired.badssl.com/")
    //var ret:Int = NetworkHelper.testHttpURLConnection("https://www.google.com")
    Thread.sleep(1000*5);
    assertThat(ret).isEqualTo(400)
    // adb shell am force-stop com.emanuelf.remote_capture
    mDevice.executeShellCommand(
      "am force-stop com.emanuelf.remote_capture")
    Thread.sleep(1000*5);

    //If we want to pull a result file without pain, we should pull a file from
    //host-side test case (Junit test case).
    /*
    //tls check
    ret = NetworkHelper.testHttpURLConnection("https://tls-v1-0.badssl.com:1010/")
    println(ret);
    ret = NetworkHelper.testHttpURLConnection("https://tls-v1-1.badssl.com:1011/")
    println(ret);
    ret = NetworkHelper.testHttpURLConnection("https://tls-v1-2.badssl.com:1012/")
    println(ret);
    */
    //to execute it we should run the test as normal junit case
    //adb pull /storage/emulated/0/Download/PCAPdroid/traffic.pcap traffic.pcap
    //mDevice.
    //kill process

  }

}