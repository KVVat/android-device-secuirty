package com.example.test_suites


import assertk.assertThat
import assertk.assertions.contains
import com.example.test_suites.rule.AdbDeviceRule
import com.malinskiy.adam.request.shell.v1.ChanneledShellCommandRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.v1.PushFileRequest
import java.io.File
import java.nio.file.Paths
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.onClosed
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
class NativeUnitTest {

  @Rule
  @JvmField
  val adb = AdbDeviceRule()
  val client = adb.adb

  @Before
  fun setup() {
    runBlocking {

    }
  }

  @After
  fun teardown() {
    runBlocking {

    }
  }

  fun preparerPushExecutableToTmp(objFile: File,permission:String="") {
    runBlocking {
      val fileName = objFile.name
      val channel = client.execute(PushFileRequest(objFile, "/data/local/tmp/$fileName"),
                                   GlobalScope,
                                   serial = adb.deviceSerial);
      while (!channel.isClosedForReceive) {
        val progress: Double? =
          channel.tryReceive().onClosed {
          }.getOrNull()
      }

      if(permission != ""){

        client.execute(request = ShellCommandRequest("chmod $permission /data/local/tmp/$fileName"),
                       serial = adb.deviceSerial);
      }
    }
    return;
  }
  @Test
  fun adamShellCommandSample() {
    runBlocking {
      val response = client.execute(ShellCommandRequest("echo hello"), adb.deviceSerial)
      println(response.output);
    }
  }
  @Test
  fun nativeUnitTestInstall() {
    runBlocking {
      //
      val binObject: File =
        File(Paths.get("build",
                       "intermediates",
                       "cmake",
                       "debug",
                       "obj",
                       "armeabi-v7a",
                       "libfoo.so").toUri());
      val binUnitTest: File =
        File(Paths.get("build",
                       "intermediates",
                       "cmake",
                       "debug",
                       "obj",
                       "armeabi-v7a",
                       "foo_unittest").toUri());

      preparerPushExecutableToTmp(binObject);
      preparerPushExecutableToTmp(binUnitTest,"755");

      val updates = client.execute(
        request = ChanneledShellCommandRequest("LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/foo_unittest"),
        scope = GlobalScope,
        serial = adb.deviceSerial
      );
      var line:String =""
      for (lines in updates) {
        println(lines)
        line = lines;
      }
      assertThat(line).contains("PASSED");
    }
  }
}