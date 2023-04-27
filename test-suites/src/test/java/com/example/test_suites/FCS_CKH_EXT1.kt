package com.example.test_suites



import com.example.test_suites.rule.AdbDeviceRule
import com.example.test_suites.utils.AdamUtils
import com.example.test_suites.utils.LogcatResult
import com.malinskiy.adam.request.misc.RebootRequest
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import java.io.File
import java.nio.file.Paths
import java.time.LocalDateTime
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class FCS_CKH_EXT1 {

  private val TEST_PACKAGE = "com.example.directboot"
  private val TEST_MODULE = "directboot-debug.apk"
  private val LONG_TIMEOUT = 5000L

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
    println("** A Junit test case for FCS_CKH_EXT1 started on "+ LocalDateTime.now()+" **")

  }
  @After
  fun teardown() {
    runBlocking {
      //client.execute(UninstallRemotePackageRequest(TEST_PACKAGE), adb.deviceSerial)
      client.execute(ShellCommandRequest("rm /data/local/tmp/$TEST_MODULE"),
                     adb.deviceSerial)
    }
  }

  @Test
  fun testDeviceEncryptedStorage() {
    runBlocking {
      //install file
      val file_apk: File =
        File(Paths.get("src", "test", "resources", TEST_MODULE).toUri())

      var res = AdamUtils.InstallApk(file_apk, false,adb)
      assertTrue(res.startsWith("Success"))

      //launch application to write a file into the storage
      //am start -a com.example.ACTION_NAME -n com.package.name/com.package.name.ActivityName
      async {
        client.execute(ShellCommandRequest("am start ${TEST_PACKAGE}/${TEST_PACKAGE}.MainActivity"),
                       adb.deviceSerial)
      }

      var result:LogcatResult?
        = AdamUtils.waitLogcatLine(50,"FCS_CKH_EXT_TEST",adb)
      //assertThat { result }.isNotNull()
      assertNotNull(result)

      Thread.sleep(1000*10);
      //(Require)Reboot Device
      //1. We expect the bootloader of the device is unlocked.
      //2. Users need to relaunch the device quickly
      client.execute(request = RebootRequest(), serial = adb.deviceSerial)
      println("** Rebooting : Please Reboot Device **")
      Thread.sleep(LONG_TIMEOUT*5);//20sec.
      //Note:  the connection to the adb server will be dismissed during the rebooting
      println("** Maybe it requires manual operation : Please Reboot the target device as fast as possible **")

      result = AdamUtils.waitLogcatLine(200,"FCS_CKH_EXT_TEST",adb)
      //println(result);
      println(result?.text)

      // Evaluates below behaviours. Application will be triggered by LOCKED_BOOT_COMPLETED action.
      // 1. Check if we can access to the DES(Device Encrypted Storage)
      // 2. Check we can not access to the CES
      //assertThat(result?.text).isEqualTo("des=Success,ces=Failed")
      assertEquals("des=Success,ces=Failed",result?.text)

      //result = AdamUtils.waitLogcatLine(100,"FCS_CKH_EXT_TEST",adb)
      //assertThat { result }.isNotNull()
      //assertNotNull(result)

      println(result);
      println(result?.text)
    }
  }
}
