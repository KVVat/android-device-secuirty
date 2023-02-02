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
            var ABI = "x86_64"
            val acvpTool = File(Paths.get(OUT_PATH,ABI,"acvptool").toUri());
            val fileConfig  =
                File(Paths.get("src/test/resources/af_alg_config.txt").toUri());

            pushFileToTmp(acvpTool, "775");
            pushFileToTmp(fileConfig, "555");

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