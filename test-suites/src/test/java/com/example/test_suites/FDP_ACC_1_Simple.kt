package com.example.test_suites

import com.example.test_suites.rule.AdbDeviceRule
import com.example.test_suites.utils.ADSRPTestWatcher
import com.example.test_suites.utils.AdamUtils
import com.example.test_suites.utils.LogcatResult
import com.example.test_suites.utils.SFR
import com.example.test_suites.utils.TestAssertLogger
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import java.io.File
import java.nio.file.Paths
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.hamcrest.core.StringStartsWith
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import org.junit.rules.TestName
import org.junit.rules.TestWatcher
import org.hamcrest.CoreMatchers.`is` as Is

//FPR_PSE.1

@SFR("FDP_ACC_1", """
 - FCS_CKH.1.1/Low The TSF shall support a key hierarchy for the data encryption key(s) for Low user data assets.
 - FCS_CKH.1.2/Low The TSF shall ensure that all keys in the key hierarchy are derived and/or 
 generated according to [assignment: description of how each key in the hierarchy is derived 
 and/or generated, with which key lengths and according to which standards] ensuring that 
 the key hierarchy uses the DUK directly or indirectly in the derivation of the data encryption key(s) for Low user data assets. 
 - FCS_CKH.1.3/Low The TSF shall ensure that all keys in the key hierarchy and all data used in 
 deriving the keys in the hierarchy are protected according to [assignment: rules].
""")
class FDP_ACC_1_Simple {

  private val TEST_PACKAGE = "com.example.assets"
  private val TEST_MODULE = "assets-debug.apk"
  private val LONG_TIMEOUT = 5000L
  private val SHORT_TIMEOUT = 1000L

  @Rule
  @JvmField
  val adb = AdbDeviceRule()
  val client = adb.adb

  @Rule @JvmField
  public var watcher: TestWatcher = ADSRPTestWatcher()
  @Rule @JvmField
  public var name: TestName = TestName();
  //Asset Log
  public var a: TestAssertLogger = TestAssertLogger(name)
  @Rule @JvmField
  public var errs: ErrorCollector = ErrorCollector()

  @Before
  fun setup() {
    runBlocking {
      client.execute(UninstallRemotePackageRequest(TEST_PACKAGE), adb.deviceSerial)
      client.execute(ShellCommandRequest("rm /data/local/tmp/$TEST_MODULE"),
                     adb.deviceSerial)
    }
  }

  @After
  fun teardown() {
    runBlocking {
      client.execute(ShellCommandRequest("rm /data/local/tmp/$TEST_MODULE"),
                     adb.deviceSerial)
    }
  }


  // inline fun <T> checkThatNow(reason:String, value:T, matcher:Matcher<T>){
  //   errs.checkThat(reason, value, matcher).
  //
  // }
  @Test
  fun testUserAssets()
  {
    runBlocking {
      val file_apk: File =
        File(Paths.get("src", "test", "resources", TEST_MODULE).toUri());

      var response: ShellCommandResult
      var result: LogcatResult?
      var ret = AdamUtils.InstallApk(file_apk,false,adb)
      assertThat(a.Msg("Install Package"),
                 ret,StringStartsWith("Success"))


      Thread.sleep(SHORT_TIMEOUT*2);

      //launch application and prepare
      response = client.execute(ShellCommandRequest("am start -n $TEST_PACKAGE/$TEST_PACKAGE.PrepareActivity"), adb.deviceSerial);
      assertThat(a.Msg("Preparing Test Files with opening PrepareActivity"),
                 response.output,StringStartsWith("Starting"))

      Thread.sleep(LONG_TIMEOUT);
      response = client.execute(ShellCommandRequest("am start -n $TEST_PACKAGE/$TEST_PACKAGE.MainActivity"), adb.deviceSerial);
      assertThat(a.Msg("Check file acccess via MainActivity"),
                 response.output,StringStartsWith("Starting"))

      println("> Test result shows the file acccess status below. 0=Preference/1=Private File/2=Media Storage/3=Database")
      result = AdamUtils.waitLogcatLine(100,"FDP_ACC_1_TEST",adb)
      assertThat(a.Msg("Check Output of the Test Package"),
                 result?.text,IsEqual("Test Result:true/true/true/true"))
      println(result?.text!!.trim())
      //uninstall application =>
      response = client.execute(UninstallRemotePackageRequest(TEST_PACKAGE), adb.deviceSerial)
      assertThat(a.Msg("Uninstall Test Package"),
                 response.output,StringStartsWith("Success"))
      //install application => files execpt media storage will be removed,
      //The app will lost the access permission to the owner file once uninstall it.
      //so we should reinstall it with -g option to enable read_media_storage permission
      ret = AdamUtils.InstallApk(file_apk,false,adb)
      assertThat(a.Msg("Install Package"),
                 ret,StringStartsWith("Success"))
      Thread.sleep(SHORT_TIMEOUT*2);

      response = client.execute(ShellCommandRequest("am start -n $TEST_PACKAGE/$TEST_PACKAGE.MainActivity"), adb.deviceSerial);
      assertThat(a.Msg("Check file acccess via MainActivity"),
                 response.output,StringStartsWith("Starting"))

      result = AdamUtils.waitLogcatLine(100,"FDP_ACC_1_TEST",adb)
      println(result?.text!!.trim())
      assertThat(a.Msg("Check Output of the Test Package"),
                 result?.text,IsEqual("Test Result:false/false/true/false"))
    }
  }

}