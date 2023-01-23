package com.example.adamsample

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.startsWith
import com.example.adamsample.rule.AdbDeviceRule
import com.example.adamsample.utils.AdamUtils
import com.example.adamsample.utils.LogcatResult
import com.malinskiy.adam.request.logcat.ChanneledLogcatRequest
import com.malinskiy.adam.request.logcat.LogcatSinceFormat
import com.malinskiy.adam.request.logcat.SyncLogcatRequest
import com.malinskiy.adam.request.misc.RebootRequest
import com.malinskiy.adam.request.pkg.InstallRemotePackageRequest
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.request.prop.GetSinglePropRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import com.malinskiy.adam.request.sync.v1.PushFileRequest
import java.io.File
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.TimeZone
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class `FCS_CKH_EXT#1 - Low ` {

  private val TEST_PACKAGE = "com.example.adamsample"
  private val TEST_MODULE = "alltest-v2-debug.apk"
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
      assertThat(res.output).startsWith("Success")
      //launch application to write a file into the storage
      //am start -a com.example.ACTION_NAME -n com.package.name/com.package.name.ActivityName
      async {
        client.execute(ShellCommandRequest("am start ${TEST_PACKAGE}/${TEST_PACKAGE}.MainActivity"),
                       adb.deviceSerial)
      }
      Thread.sleep(2000);

      var result:LogcatResult?
        = AdamUtils.waitLogcatLine(50,"FCS_CKH_EXT_TEST",adb)
      assertThat { result }.isNotNull()

      //(Require)Reboot Device
      //1. We expect the bootloader of the device is unlocked.
      //2. Users need to relaunch the device quickly
      client.execute(request = RebootRequest(), serial = adb.deviceSerial)
      println("** Rebooting : Please Reboot Device **")
      Thread.sleep(LONG_TIMEOUT*4);//20sec.
      //Note:  the connection to the adb server will be dismissed during the rebooting
      println("** Require Manual Operation : Please Reboot Device **")

      result = AdamUtils.waitLogcatLine(100,"FCS_CKH_EXT_TEST",adb)
      assertThat { result }.isNotNull()
      //println(result);
      println(result?.text)

      // Evaluates below behaviours. Application will be triggered by LOCKED_BOOT_COMPLETED action.
      // 1. Check if we can access to the DES(Device Encrypted Storage)
      // 2. Check we can not access to the CES
      assertThat(result?.text).isEqualTo("des=Success,ces=Failed")

      result = AdamUtils.waitLogcatLine(100,"FCS_CKH_EXT_TEST",adb)
      assertThat { result }.isNotNull()
      println(result);
      println(result?.text)
    }
  }

}
