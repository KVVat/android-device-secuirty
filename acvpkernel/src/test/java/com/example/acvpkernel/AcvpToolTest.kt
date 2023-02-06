package com.example.acvpkernel

import assertk.assertThat
import assertk.assertions.contains
import com.example.acvpkernel.rule.AdbDeviceRule
import com.malinskiy.adam.request.shell.v1.ChanneledShellCommandRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.v1.PushFileRequest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Paths

class AcvpToolTest {

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
    fun pushFileToTmp(objFile: File, permission:String="") {
        runBlocking {
            val fileName = objFile.name
            val channel = client.execute(
                PushFileRequest(objFile, "/data/local/tmp/$fileName"),
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
    fun nativeUnitTestInstall() {
        runBlocking {
            //
            val OUT_PATH = "build/intermediates/cmake/debug/obj"
            val GO_PATH  = "src/go"
            var ABI = "x86_64"
            val acvp_tool_path = File(Paths.get(GO_PATH,"acvptool").toUri());
            val acvp_kh_path = File(Paths.get(OUT_PATH,ABI,"acvp_kernel_harness").toUri());
            val fileConfig  =
                File(Paths.get("src/test/resources/af_alg_config.txt").toUri());
            val fileConfig2  =
                File(Paths.get("src/test/resources/config.json").toUri());


            pushFileToTmp(acvp_tool_path, "775");
            pushFileToTmp(acvp_kh_path, "775");
            pushFileToTmp(fileConfig, "555");
            pushFileToTmp(fileConfig2, "555");

            val updates = client.execute(
                request = ChanneledShellCommandRequest("LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/acvptool"),
                scope = GlobalScope,
                serial = adb.deviceSerial
            );
            var line: String = ""
            for (lines in updates) {
                println(lines)
                line = lines;
            }
            assertThat(line).contains("PASSED");
        }
    }
}