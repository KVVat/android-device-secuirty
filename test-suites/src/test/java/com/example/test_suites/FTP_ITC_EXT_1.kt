package com.example.test_suites

import com.example.test_suites.rule.AdbDeviceRule
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import com.malinskiy.adam.request.sync.v1.PullFileRequest
import java.io.File
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
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
    println("** A Junit test case for FTP_ITC_EXT1 started on "+ LocalDateTime.now()+" **")
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
  fun testShellScript()
  {
    var p1: URI = Paths.get("src", "test", "resources", "traffic.pcap").toUri()


  }


  fun testPcapReader(filePath:String) {
    val file_pcap: File =
      File(Paths.get("src", "test", "resources", "traffic.pcap").toUri());
   // check the availavility of tshark
    /*
    if ! command -v <the_command> &> /dev/null
    then
    echo "<the_command> could not be found"
    exit
    fi
    */

    /*val pcap: Pcap = Pcap.openStream(file_pcap)
    pcap.loop(TcpUdpPacketHandler())
    pcap.close()*/

    //output data as pdml then analysis it.
    //
    //tshark -r  t6690574987861344402.pcap -o ssl.debug_file:ssldebug.log
    // -o ssl.desegment_ssl_records:TRUE -o ssl.desegment_ssl_application_data:TRUE -V -T pdml
    var cmd:String="""
    tshark -r $filePath
     -o ssl.debug_file:ssldebug.log
     -o ssl.desegment_ssl_records:TRUE
     -o ssl.desegment_ssl_application_data:TRUE -V > dtea analysis.xml
    """
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
