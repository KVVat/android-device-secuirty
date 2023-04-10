package com.example.acvpkernel

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
    @Test
    fun test(){

    }
    val RES_PATH  = "src/test/resources"
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

    //https://boringssl.googlesource.com/boringssl/+archive/refs/heads/master/util/fipstools/acvp/acvptool/test/expected.tar.gz
    //https://boringssl.googlesource.com/boringssl/+archive/refs/heads/master/util/fipstools/acvp/acvptool/test/vectors.tar.gz

    fun batch_install(source_:String,dest_:String,files:Array<String>):Boolean{
        val source:File = File(Paths.get(source_).toUri());
        //val source:File = File(Paths.get(source_));

        runBlocking {
            files.forEach{
                println(it)

            }
        }

        return true
    }

    @Test
    fun testInstall() {
        //install test modules
        batch_install(RES_PATH,"/data/local/tmp/", arrayOf(
            "acvptool:775","acvp_kernel_harness_arm7:775","af_alg_conifg.txt",
            "config.json"
        ))
        //based on recent test cases.
        val vectors:List<String> = listOf(
            "SHA-1","SHA2-224","SHA2-256","SHA2-384","SHA2-512","HMAC-SHA-1","HMAC-SHA2-224","HMAC-SHA2-256",
            "HMAC-SHA2-384","HMAC-SHA2-512","CMAC-AES","ACVP-AES-ECB","ACVP-AES-CBC","ACVP-AES-CBC-CS3",
            "ACVP-AES-CTR","ACVP-AES-XTS","ACVP-AES-GCM"
        )
        println(vectors.map{ "$it.bz2" }.toTypedArray())
        batch_install("/data/local/tmp/vectors/",RES_PATH+"vectors/",
                      vectors.map{ "$it.bz2" }.toTypedArray()
        )
        batch_install("/data/local/tmp/expected/",RES_PATH+"expected/",
                      vectors.map{ "$it.bz2" }.toTypedArray()
        )
    }
    @Test
    fun nativeUnitTestInstall() {
        runBlocking {
            testInstall()
            //val OUT_PATH = "build/intermediates/cmake/debug/obj"
            //val GO_PATH  = "src/go"

            val acvp_tool_path = File(Paths.get(RES_PATH,"acvptool").toUri());
            val acvp_kh_path = File(Paths.get(RES_PATH,"acvp_kernel_harness_arm7").toUri());
            val fileConfig  = File(Paths.get(RES_PATH,"af_alg_config.txt").toUri());
            val fileConfig2  = File(Paths.get(RES_PATH,"config.json").toUri());

            pushFileToTmp(acvp_tool_path, "775");
            pushFileToTmp(acvp_kh_path, "775");
            pushFileToTmp(fileConfig, "555");
            pushFileToTmp(fileConfig2, "555");

            val updates = client.execute(
                request =
                ChanneledShellCommandRequest
                    ("LD_LIBRARY_PATH=/data/local/tmp "+
                       "/data/local/tmp/acvptool "),
                scope = GlobalScope,
                serial = adb.deviceSerial
            );
            var line: String = ""
            for (lines in updates) {
                println(lines)
                line = lines;
            }
            //assertThat(line).contains("PASSED");
        }
    }

}