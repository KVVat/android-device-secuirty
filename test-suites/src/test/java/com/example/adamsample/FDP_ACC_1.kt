package com.example.adamsample

import assertk.assertThat
import assertk.assertions.isNotNull
import com.example.adamsample.rule.AdbDeviceRule
import com.example.adamsample.utils.AdamUtils
import com.example.adamsample.utils.LogcatResult
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import java.io.File
import java.nio.file.Paths
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

//FPR_PSE.1
class `FDP_ACC#1 - UserAssets` {

  private val TEST_PACKAGE = "com.example.assets"
  private val TEST_MODULE = "assets-debug.apk"
  private val LONG_TIMEOUT = 5000L
  private val SHORT_TIMEOUT = 1000L

  @Rule
  @JvmField
  val adb = AdbDeviceRule()
  val client = adb.adb

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

  @Test
  fun testUserAssets()
  {
    runBlocking {
      val file_apk: File =
        File(Paths.get("src", "test", "resources", TEST_MODULE).toUri());

      AdamUtils.InstallApk(file_apk,false,adb)
      Thread.sleep(SHORT_TIMEOUT*2);

      var response: ShellCommandResult
      var result: LogcatResult?

      //launch application and prepare
      response = client.execute(ShellCommandRequest("am start -n $TEST_PACKAGE/$TEST_PACKAGE.PrepareActivity"), adb.deviceSerial);
      assertThat(response?.output).equals("Starting")
      Thread.sleep(LONG_TIMEOUT);
      response = client.execute(ShellCommandRequest("am start -n $TEST_PACKAGE/$TEST_PACKAGE.MainActivity"), adb.deviceSerial);
      assertThat(response?.output).equals("Starting")
      result = AdamUtils.waitLogcatLine(100,"FDP_ACC_1_TEST",adb)
      assert(result != null)
      assertThat( result ).isNotNull()
      assertThat(result?.text).equals("Test Result:true/true/true/true")

      //uninstall application =>
      response = client.execute(UninstallRemotePackageRequest(TEST_PACKAGE), adb.deviceSerial)
      //install application => files execpt media storage will be removed,

      //The app will lost the access permission to the owner file once uninstall it.
      //so we should reinstall it with -g option to enable read_media_storage permission
      AdamUtils.InstallApk(file_apk,false,adb)

      response = client.execute(ShellCommandRequest("am start -n $TEST_PACKAGE/$TEST_PACKAGE.MainActivity"), adb.deviceSerial);
      //Thread.sleep(LONG_TIMEOUT);
      result = AdamUtils.waitLogcatLine(5,"FDP_ACC_1_TEST",adb)
      assertThat { result }.isNotNull()
      //println(result?.text)
      assertThat{result?.text}.equals("Test Result:false/false/true/false")
    }
  }

}