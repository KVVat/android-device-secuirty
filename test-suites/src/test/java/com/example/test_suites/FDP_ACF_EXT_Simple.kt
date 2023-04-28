package com.example.test_suites

import com.example.test_suites.rule.AdbDeviceRule
import com.example.test_suites.utils.ADSRPTestWatcher
import com.example.test_suites.utils.AdamUtils
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import java.io.File
import java.nio.file.Paths
import java.time.LocalDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher

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

  @Rule @JvmField
  public var watcher: TestWatcher = ADSRPTestWatcher()

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
    println("The test verifies apk upgrade operation works correctly.")

    runBlocking {
      //
      val file_apk_v1_debug: File =
        File(Paths.get("src", "test", "resources", "appupdate-v1-debug.apk").toUri());
      val file_apk_v2_debug: File =
        File(Paths.get("src", "test", "resources", "appupdate-v2-debug.apk").toUri());

      var res = AdamUtils.InstallApk(file_apk_v1_debug,false,adb);
      println("Verify Install apk v1 (expect=Success)")
      assertTrue(res.startsWith("Success"))
      println("Verify Install upgraded apk v2 (expect=Success)")
      res =  AdamUtils.InstallApk(file_apk_v2_debug,false,adb);
      assertTrue(res.startsWith("Success"))

      //degrade
      println("Verify Install degraded apk v1 (expect=Failure)")
      res = AdamUtils.InstallApk(file_apk_v1_debug,false,adb);
      assertTrue(res.startsWith("Failure"))

      //unistall the test file before next test
      client.execute(UninstallRemotePackageRequest("com.example.appupdate"), adb.deviceSerial)
    }
  }

  //@TestInformation(SFR="FDP_ACF_EXT.1/AppUpadate")
  @Test
  fun testAbnormalUpdate() {
    println("The test verifies apk upgrade fails if the signing keys are not-identical.")

    runBlocking {
      //
      val file_apk_v1_debug: File =
        File(Paths.get("src", "test", "resources", "appupdate-v1-debug.apk").toUri());
      val file_apk_v2_signed: File =
        File(Paths.get("src", "test", "resources", "appupdate-v2-signed.apk").toUri());

      println("Verify Install apk v1 (expect=Success)")
      var res = AdamUtils.InstallApk(file_apk_v1_debug,false,adb);
      assertTrue(res.startsWith("Success"))
      //Signature mismatch case
      println("Verify Install apk v2 with different signing key (expect=Failure)")
      res = AdamUtils.InstallApk(file_apk_v2_signed,false,adb);
      assertTrue(res.startsWith("Failure"))
      //unistall the test file before next test
      client.execute(UninstallRemotePackageRequest("com.example.appupdate"), adb.deviceSerial)
    }
  }



}