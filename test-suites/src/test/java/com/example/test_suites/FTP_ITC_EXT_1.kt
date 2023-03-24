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
import com.malinskiy.adam.request.sync.v1.PullFileRequest
import java.io.File
import java.nio.file.Path
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
  fun testTlsCapture() {
    runBlocking {
      //need to execute target instrumented test at least once
      var response: ShellCommandResult
      //Launch packet capture software
      response =   client.execute(ShellCommandRequest(
        "am instrument -w -e class com.example.test_suites.FTP_ITC_EXT"+
          " com.example.test_suites.test/androidx.test.runner.AndroidJUnitRunner"
      ),adb.deviceSerial)
      Thread.sleep(1000*10);
      println(response.output);
      //adb pull /storage/emulated/0/Download/PCAPdroid/traffic.pcap traffic.pcap
      var p: Path = kotlin.io.path.createTempFile("t", ".pcap")
      client.execute(PullFileRequest(
        "/storage/emulated/0/Download/PCAPdroid/traffic.pcap",
                  p.toFile()),scope=this,adb.deviceSerial);
      //  ShellCommandRequest("am force-stop com.emanuelf.remote_capture"),ser);
      println(p.toAbsolutePath());
    }
  }
}
