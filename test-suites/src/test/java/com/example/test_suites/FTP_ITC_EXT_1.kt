package com.example.test_suites

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.startsWith
import com.example.test_suites.rule.AdbDeviceRule
import com.example.test_suites.utils.AdamUtils
import com.example.test_suites.utils.LogcatResult
import com.malinskiy.adam.request.misc.RebootRequest
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import java.io.File
import java.nio.file.Paths
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FTP_ITC_EXT_1 {

  private val TEST_PACKAGE = "com.example.networkcheck"
  private val TEST_MODULE = "networkcheck-debug.apk"
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
  fun testTlsCapture2() {


  }

  @Test
  fun testTlsCapture() {
    runBlocking {
      //install an application file
      val file_apk: File =
        File(Paths.get("src", "test", "resources", TEST_MODULE).toUri())
      var res = AdamUtils.InstallApk(file_apk, false,adb)
      assertThat(res.output).startsWith("Success")
      var response: ShellCommandResult
      val ser = adb.deviceSerial;
      //Launch packet capture software
      response =   client.execute(ShellCommandRequest(
        "am start -n com.emanuelef.remote_capture/.activities.CaptureCtrl"+
          " -e action start"+
          " -e capture_auto true"+
          " -e pcap_dump_mode pcap_file"+
          " -e pcap_name traffic.pcap"

      ),ser)
      Thread.sleep(5000*2);
      //Launch test application

      response =   client.execute(
        ShellCommandRequest("am start -e type okhttp3 -n $TEST_PACKAGE/$TEST_PACKAGE.MainActivity"), ser);
      Thread.sleep(5000);
      assertThat(response.output).startsWith("Starting")
      println(response.output);
      Thread.sleep(1000*10);
      client.execute(
        ShellCommandRequest("am force-stop $TEST_PACKAGE"),ser);

      //client.execute(ShellCommandRequest(
      //  "am start -n com.emanuelef.remote_capture/.activities.CaptureCtrl"+
      //    " -e action stop"),ser);

      //client.execute(
      //  ShellCommandRequest("am force-stop com.emanuelf.remote_capture"),ser);

    }
  }
}
