package com.example.test_suites

import com.example.test_suites.rule.AdbDeviceRule
import com.example.test_suites.utils.AdamUtils
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.misc.FetchHostFeaturesRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.v2.PullFileRequest
import com.malinskiy.adam.request.sync.v1.PushFileRequest
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

class KernelAcvpTest {

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

  val RES_PATH  = "src/test/resources"
  fun pushFileToTmp(objFile: File, permission:String="",destdir:String="/data/local/tmp/") {
    runBlocking {

      //if(client.execute())

      val fileName = objFile.name
      val channel = client.execute(
        PushFileRequest(objFile, "$destdir$fileName"),
        GlobalScope,
        serial = adb.deviceSerial);

      while (!channel.isClosedForReceive) {
        val progress: Double? =
          channel.tryReceive().onClosed {
          }.getOrNull()
      }

      if(permission != ""){
        client.execute(request = ShellCommandRequest("chmod $permission $destdir$fileName"),
                       serial = adb.deviceSerial);
      }
    }
    return;
  }

  //https://boringssl.googlesource.com/boringssl/+archive/refs/heads/master/util/fipstools/acvp/acvptool/test/expected.tar.gz
  //https://boringssl.googlesource.com/boringssl/+archive/refs/heads/master/util/fipstools/acvp/acvptool/test/vectors.tar.gz

  fun batch_install(source_:String,dest_:String,files:Array<String>):Boolean{
    runBlocking {
      files.forEach{
        //println(it)
        var target:String = it;
        var mode:String = "555"
        if(it.indexOf(":")>-1){
          var targetarg = it.split(":");
          target = targetarg[0]
          mode = targetarg[1]
        }
        pushFileToTmp(File(Paths.get(source_,target).toUri()),mode,dest_)
      }
    }
    return true
  }




  @Test
  fun testKernelAcvp() {

    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm XXX")

    //Check kernel settings

    //If the system does not require  the test should be fail.

    //install test modules
    val ret = AdamUtils.root(adb)
    //If we can not to be root the device fail test
    Thread.sleep(5000);//Wait system reponse for a while
    println(ret)
    //
    batch_install(RES_PATH,"/data/local/tmp/", arrayOf(
      "acvptool:775","acvp_kernel_harness_arm64:775","af_alg_config.txt",
      "config.json"
    ))
    //based on a recent test cases.
    val vectors:List<String> = listOf(
      "SHA-1","SHA2-224","SHA2-256","SHA2-384","SHA2-512","HMAC-SHA-1","HMAC-SHA2-224","HMAC-SHA2-256",
      "HMAC-SHA2-384","HMAC-SHA2-512","CMAC-AES","ACVP-AES-ECB","ACVP-AES-CBC","ACVP-AES-CBC-CS3",
      "ACVP-AES-CTR","ACVP-AES-XTS","ACVP-AES-GCM"
    )
    //
    val fnames:Array<String> = vectors.map{ "$it.bz2" }.toTypedArray()
    //
    batch_install(RES_PATH+"/vectors/","/data/local/tmp/vectors/",fnames)
    batch_install(RES_PATH+"/expected/","/data/local/tmp/expected/",fnames)
    // For in case not be configured :
    // Because the key and DRBG entropy are set with setsockopt,
    // tests can fail on certain inputs if sysctl_optmem_max is too low.
    AdamUtils.shellRequest("sysctl -w net.core.optmem_max=204800",adb)
    //extract datas
    AdamUtils.shellRequest("bzip2 -dk /data/local/tmp/vectors/*.bz2",adb)
    AdamUtils.shellRequest("bzip2 -dk /data/local/tmp/expected/*.bz2",adb)
    AdamUtils.shellRequest("cd /data/local/tmp/;mkdir actual;mkdir diffs",adb)

    var foundError = false;
    vectors.forEach(){
      AdamUtils.shellRequest("cd /data/local/tmp/;./acvptool -json vectors/$it -wrapper ./acvp_kernel_harness_arm64 > actual/$it",adb)
      var diffResult = AdamUtils.shellRequest("cd /data/local/tmp/;diff actual/$it expected/$it > diffs/$it.diff",adb)
      if(diffResult.exitCode !== 0){
        foundError = true
        val line = "\""+dateFormat.format(Date())+" *** result for $it does not match expected ***\""
        AdamUtils.shellRequest("cd /data/local/tmp/;echo $line >> acvptest.log",adb)
        println(line)
      }
    }
    //File("results","kernelacvp").mkdirs()
    //Archive diff,actual dirs
    AdamUtils.shellRequest("cd /data/local/tmp/;tar -zcvf actual.tar.gz actual",adb)
    AdamUtils.shellRequest("cd /data/local/tmp/;tar -zcvf diffs.tar.gz diffs",adb)
    //Pull worklog, actual, diff file into results dir from device

    AdamUtils.pullfile("/data/local/tmp/acvptest.log","../results/kernelacvp/",adb)
    AdamUtils.pullfile("/data/local/tmp/diffs.tar.gz","../results/kernelacvp/",adb)
    AdamUtils.pullfile("/data/local/tmp/actual.tar.gz","../results/kernelacvp/",adb)

    assertEquals(false,foundError)

    //Clean the system
  }
}
