package com.example.test_suites


import assertk.assertThat
import assertk.assertions.startsWith
import com.example.test_suites.rule.AdbDeviceRule
import com.example.test_suites.utils.AdamUtils
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import java.io.File
import java.nio.file.Paths
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class FDP_ACF_EXT_Simple {

  @Rule
  @JvmField
  val adb = AdbDeviceRule()
  val client = adb.adb

  @Before
  fun setup() {
    runBlocking {
      client.execute(UninstallRemotePackageRequest("com.example.appupdate"), adb.deviceSerial)
      client.execute(ShellCommandRequest("rm /data/local/tmp/appupdate-v1-debug.apk"),
                     adb.deviceSerial)
      client.execute(ShellCommandRequest("rm /data/local/tmp/appupdate-v2-debug.apk"),
                     adb.deviceSerial)
    }
  }

  @After
  fun teardown() {
    runBlocking {
      client.execute(UninstallRemotePackageRequest("com.example.appupdate"), adb.deviceSerial)
      client.execute(ShellCommandRequest("rm /data/local/tmp/appupdate-v1-debug.apk"),
                     adb.deviceSerial)
      client.execute(ShellCommandRequest("rm /data/local/tmp/appupdate-v2-debug.apk"),
                     adb.deviceSerial)
    }
  }

  //@TestInformation(SFR="FDP_ACF_EXT.1/AppUpadate")
  @Test
  fun testNormalUpdate() {
    //A test for FDP_ACF_EXT.1/AppUpdate
    //UserDataProtectionTest.accessControlExt1_appUpdate_TestNormal

    runBlocking {
      //
      val file_apk_v1_debug: File =
        File(Paths.get("src", "test", "resources", "appupdate-v1-debug.apk").toUri());
      val file_apk_v2_debug: File =
        File(Paths.get("src", "test", "resources", "appupdate-v2-debug.apk").toUri());

      var res = AdamUtils.InstallApk(file_apk_v1_debug,false,adb);
      assertThat(res.output).startsWith("Success")

      res =  AdamUtils.InstallApk(file_apk_v2_debug,false,adb);
      assertThat(res.output).startsWith("Success")

      //degrade
      res = AdamUtils.InstallApk(file_apk_v1_debug,false,adb);
      assertThat(res.output).startsWith("Failure")

      //unistall the test file before next test
      client.execute(UninstallRemotePackageRequest("com.example.appupdate"), adb.deviceSerial)
    }
  }

  //@TestInformation(SFR="FDP_ACF_EXT.1/AppUpadate")
  @Test
  fun testAbnormalUpdate() {

    runBlocking {
      //
      val file_apk_v1_debug: File =
        File(Paths.get("src", "test", "resources", "appupdate-v1-debug.apk").toUri());
      val file_apk_v2_signed: File =
        File(Paths.get("src", "test", "resources", "appupdate-v2-signed.apk").toUri());

      var res = AdamUtils.InstallApk(file_apk_v1_debug,false,adb);
      assertThat(res.output).startsWith("Success")
      //Signature mismatch case
      res = AdamUtils.InstallApk(file_apk_v2_signed,false,adb);
      assertThat(res.output).startsWith("Failure")

      //unistall the test file before next test
      client.execute(UninstallRemotePackageRequest("com.example.appupdate"), adb.deviceSerial)
    }
  }



}