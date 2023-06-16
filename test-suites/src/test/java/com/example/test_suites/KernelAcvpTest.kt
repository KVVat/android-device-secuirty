package com.example.test_suites

import com.example.test_suites.rule.AdbDeviceRule
import com.example.test_suites.utils.ADSRPTestWatcher
import com.example.test_suites.utils.AdamUtils
import com.example.test_suites.utils.SFR
import com.example.test_suites.utils.TestAssertLogger
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.flipkart.zjsonpatch.DiffFlags
import com.flipkart.zjsonpatch.JsonDiff
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.sync.v1.PushFileRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.runBlocking
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.utils.FileNameUtils
import org.hamcrest.MatcherAssert
import org.hamcrest.core.IsEqual
import org.hamcrest.core.StringStartsWith
import org.jsfr.json.JsonPathListener
import org.jsfr.json.JsonSurfer
import org.jsfr.json.JsonSurferJackson
import org.jsfr.json.SurfingConfiguration
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import org.junit.rules.TestName
import org.junit.rules.TestWatcher
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date
import java.util.EnumSet


@SFR("Kernel ACVP Test Case", """
FIPS 140-2 test case
""")
class KernelAcvpTest {

  @Rule
  @JvmField
  val adb = AdbDeviceRule()
  val client = adb.adb

  @Rule @JvmField
  var errs: ErrorCollector = ErrorCollector()
  @Rule @JvmField
  var watcher:TestWatcher = ADSRPTestWatcher(adb)
  @Rule @JvmField
  var name: TestName = TestName()

  //Asset Log
  var a: TestAssertLogger = TestAssertLogger(name)

  @Before
  fun setup() {
    runBlocking {}
  }

  @After
  fun teardown() {
    runBlocking {

    }
  }

  val RES_PATH  = "src/test/resources"
  val OUT_PATH  = "../results/kernelacvp/"
  @OptIn(ExperimentalCoroutinesApi::class)
  fun pushFileToTmp(objFile: File, permission:String="", destdir:String="/data/local/tmp/") {
    runBlocking {

      //if(client.execute())

      val fileName = objFile.name
      val channel = client.execute(
        PushFileRequest(objFile, "$destdir$fileName"),
        GlobalScope,
        serial = adb.deviceSerial)

      var done=false
      while (!channel.isClosedForReceive) {
        val progress: Double? =
          channel.tryReceive().onClosed {
            Thread.sleep(1)
          }.getOrNull()
        if(progress!==null && progress==1.0 && !done) {
          println("Push file $fileName completed")
          done=true
        }
      }

      if(permission != ""){
        client.execute(request = ShellCommandRequest("chmod $permission $destdir$fileName"),
                       serial = adb.deviceSerial)
      }
    }
    return
  }

  //https://boringssl.googlesource.com/boringssl/+archive/refs/heads/master/util/fipstools/acvp/acvptool/test/expected.tar.gz
  //https://boringssl.googlesource.com/boringssl/+archive/refs/heads/master/util/fipstools/acvp/acvptool/test/vectors.tar.gz

  fun batch_install(source_:String,dest_:String,files:Array<String>):Boolean{
    runBlocking {
      files.forEach{
        //println(it)
        var target:String = it
        var mode:String = "555"
        if(it.indexOf(":")>-1){
          var targetarg = it.split(":")
          target = targetarg[0]
          mode = targetarg[1]
        }
        println("Process(Push):"+Paths.get(source_,target).toUri()+"=>"+dest_)
        pushFileToTmp(File(Paths.get(source_,target).toUri()),mode,dest_)
      }
    }
    return true
  }

  private fun bz2reader(fileURI: URI): String {
    try {
      FileInputStream(File(fileURI)).use {
        BufferedInputStream(it).use {
          CompressorStreamFactory().createCompressorInputStream(it).use {
            return BufferedReader(InputStreamReader(it)).readText()
          }
        }
      }
    } catch (ex:java.lang.Exception){
      throw ex
    }
  }
  private fun targz_reader(fileURI: URI, callback: (String, String) -> Unit)  {
    try {
      FileInputStream(File(fileURI)).use { fin ->
        GzipCompressorInputStream(fin).use { gzin->
          TarArchiveInputStream(gzin).use {tar->
            var break_ = false
            while(!break_){
              try {
                val entry = tar.nextEntry
                //println(entry.name)
                if (entry.isDirectory) continue
                if (!tar.canReadEntryData(entry)){
                  break_=true
                } else {
                  val br = BufferedReader(InputStreamReader(tar))
                  callback(entry.name,br.readText())
                }
              } catch(ex:IOException){
                ex.printStackTrace()
                break
              }
            }
          }
        }
      }
    } catch (ex:Exception){

    }
  }
  private fun evaluateResultFiles(expectedDir:String)
  {
    //Compare output file against the expected files.

    targz_reader(Paths.get(OUT_PATH,"actual.tar.gz").toUri()) { name, tartext ->
      val fname: String = Paths.get(name).fileName.toString()
      var result = true
      val uri:URI = Paths.get(RES_PATH+expectedDir,"$fname.bz2").toUri()
      try {
        val br2text = bz2reader(uri)//.readText()
        if (br2text !== null) {
          //Ignore case due to the variant of the output format ...
          val br2text_ = br2text.lowercase()
          val tartext_ = tartext.lowercase()
          val jsonB: JsonNode = jacksonObjectMapper().readTree(br2text_)//expected
          val jsonT: JsonNode = jacksonObjectMapper().readTree(tartext_)//actual

          //analyze test stats
          //val surfer: JsonSurfer = JsonSurferJackson.INSTANCE

          val flags: EnumSet<DiffFlags> = DiffFlags.dontNormalizeOpIntoMoveAndCopy().clone()
          val patch: JsonNode = JsonDiff.asJson(jsonT, jsonB,flags)
          //to suppress the differences of the format due to the revison of the files.
          val ignoreList = mutableListOf<String>("/1/revision","/1/issample")
          if(patch.isArray){

            patch.forEach {
              jsonNode ->
              val nodepath = jsonNode.get("path").textValue()
              if(!ignoreList.contains(nodepath)){
                val resp = jsonNode.toString()
                println("Found unmatches in $fname:$resp")
                result = false
              }
            }
          } else {
            println("??? patch is blank ???")
            result = false
          }
        }
      } catch (ex:IOException){
        println("[error to read]: $fname")
        result = false
      } catch (ex:java.lang.Exception){
        println("[process error]: $fname (${ex.message})")
        result = false
      }
      errs.checkThat(a.Msg("Evaluate $fname"),result, IsEqual(true))
    }
  }


  @Test
  fun testKernelAcvp() {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm XXX")

    //Check kernel settings
    //If the system does not require  the test should be fail.

    //install test modules
    val ret = AdamUtils.root(adb)
    //If we can not to be root the device fail test
    Thread.sleep(5000)//Wait system reponse for a while
    println(ret)
    //
    batch_install(RES_PATH,"/data/local/tmp/", arrayOf(
      "acvptool:775","acvp_kernel_harness_arm64:775","af_alg_config.txt",
      "config.json"
    ))

    // Test Set 1
    //
    //val vectors:List<String> = listOf(
    // "SHA-1","SHA2-224","SHA2-256","SHA2-384","SHA2-512","HMAC-SHA-1","HMAC-SHA2-224","HMAC-SHA2-256",
    // "HMAC-SHA2-384","HMAC-SHA2-512","CMAC-AES","ACVP-AES-ECB","ACVP-AES-CBC-CS3",
    // "ACVP-AES-CTR","ACVP-AES-XTS","ACVP-AES-GCM","ctrDRBG","hmacDRBG"
    // )

    // Test Set 2
    // val vectors:List<String> = listOf(
    // "1044265_SHA2-224",            "1044271_HMAC-SHA2-256",       "1044266_SHA2-256",            "1044263_CMAC-AES",
    // "1044258_ACVP-AES-ECB",        "1044272_HMAC-SHA2-384",       "1044270_HMAC-SHA2-224",       "1044267_SHA2-384",
    // "1044259_ACVP-AES-CBC",        "1044261_ACVP-AES-CTR",        "1044269_HMAC-SHA-1",          "1044268_SHA2-512",
    // "1044274_hmacDRBG",
    // "1044273_HMAC-SHA2-512",       "1044264_SHA-1",               "1044262_ACVP-AES-XTS"       /*,"1044275_hmacDRBG"*/
    // )

    val vectors:List<String> =
      listOf("1633593-ACVP-AES-ECB",	"1633602-SHA2-384", "1633594-ACVP-AES-CBC",	"1633603-SHA2-512",
        "1633595-ACVP-AES-CBC-CS3",	"1633604-HMAC-SHA-1", "1633596-ACVP-AES-CTR",	"1633605-HMAC-SHA2-224",
        "1633597-ACVP-AES-XTS",	"1633606-HMAC-SHA2-256", "1633598-CMAC-AES","1633607-HMAC-SHA2-384",
        "1633599-SHA-1","1633608-HMAC-SHA2-512", "1633600-SHA2-224","1633609-hmacDRBG", "1633601-SHA2-256")

    val vectorsDir  = "/vectors-20230509/"
    val expectedDir = "/expected-20230509/"

    val fnames:Array<String> = vectors.map{ "$it.bz2" }.toTypedArray()
    //
    batch_install(RES_PATH+vectorsDir,"/data/local/tmp/vectors/",fnames)

    // For in case not be configured :
    // Because the key and DRBG entropy are set with setsockopt,
    // tests can fail on certain inputs if sysctl_optmem_max is too low.
    AdamUtils.shellRequest("sysctl -w net.core.optmem_max=204800",adb)
    //extract datas
    AdamUtils.shellRequest("bzip2 -dk /data/local/tmp/vectors/*.bz2",adb)
    AdamUtils.shellRequest("cd /data/local/tmp/;rm -rf actual;mkdir actual",adb)

    vectors.forEach {
      val sr = AdamUtils.shellRequest("cd /data/local/tmp/;./acvptool -json vectors/$it -wrapper ./acvp_kernel_harness_arm64 > actual/$it",adb)
      val line:String
      errs.checkThat(a.Msg("Execute acvptool $it"),sr.exitCode, IsEqual(0))
      if(sr.exitCode!=0) {
        line = "\""+dateFormat.format(Date())+" *** processing $it ... failure ***\""+sr.toString()
      } else {
        line = "\""+dateFormat.format(Date())+" *** processing $it ... ok ***\""+sr.toString()
      }

      AdamUtils.shellRequest("cd /data/local/tmp/;echo $line >> acvptest.log",adb)
    }
    AdamUtils.shellRequest("cd /data/local/tmp/;tar -zcvf actual.tar.gz actual",adb)
    //Pull worklog, actual, diff file into results dir from device

    //AdamUtils.pullfile("/data/local/tmp/acvptest.log","../results/kernelacvp/",adb)
    AdamUtils.pullfile("/data/local/tmp/actual.tar.gz","../results/kernelacvp/",adb)

    evaluateResultFiles(expectedDir)
  }

  //@Test
  fun testKernelAcvp_() {

    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm XXX")

    //Check kernel settings

    //If the system does not require  the test should be fail.

    //install test modules
    val ret = AdamUtils.root(adb)
    //If we can not to be root the device fail test
    Thread.sleep(5000)//Wait system reponse for a while
    println(ret)
    //
    batch_install(RES_PATH,"/data/local/tmp/", arrayOf(
      "acvptool:775","acvp_kernel_harness_arm64:775","af_alg_config.txt",
      "config.json"
    ))
    //based on a recent test cases.

    // val vectors:List<String> = listOf(
    //   "SHA-1","SHA2-224","SHA2-256","SHA2-384","SHA2-512","HMAC-SHA-1","HMAC-SHA2-224","HMAC-SHA2-256",
    //   "HMAC-SHA2-384","HMAC-SHA2-512","CMAC-AES","ACVP-AES-ECB","ACVP-AES-CBC","ACVP-AES-CBC-CS3",
    //   "ACVP-AES-CTR","ACVP-AES-XTS","ACVP-AES-GCM"
    // )

    // val vectors:List<String> = listOf(
    // "1044265_SHA2-224",            "1044271_HMAC-SHA2-256",       "1044266_SHA2-256",            "1044263_CMAC-AES",
    // "1044258_ACVP-AES-ECB",        "1044272_HMAC-SHA2-384",       "1044270_HMAC-SHA2-224",       "1044267_SHA2-384",
    // "1044259_ACVP-AES-CBC",        "1044261_ACVP-AES-CTR",        "1044269_HMAC-SHA-1",          "1044268_SHA2-512",
    // "1044274_hmacDRBG",
    // "1044273_HMAC-SHA2-512",       "1044264_SHA-1",               "1044262_ACVP-AES-XTS"       /*,"1044275_hmacDRBG"*/
    // )

    val vectors:List<String> = 
    listOf("1633593-ACVP-AES-ECB",	"1633602-SHA2-384", "1633594-ACVP-AES-CBC",	"1633603-SHA2-512",
    "1633595-ACVP-AES-CBC-CS3",	"1633604-HMAC-SHA-1", "1633596-ACVP-AES-CTR",	"1633605-HMAC-SHA2-224",
    "1633597-ACVP-AES-XTS",	"1633606-HMAC-SHA2-256", "1633598-CMAC-AES","1633607-HMAC-SHA2-384",
    "1633599-SHA-1","1633608-HMAC-SHA2-512", "1633600-SHA2-224","1633609-hmacDRBG", "1633601-SHA2-256")

    //
    val fnames:Array<String> = vectors.map{ "$it.bz2" }.toTypedArray()
    //
    batch_install(RES_PATH+"/vectors-20230509/","/data/local/tmp/vectors/",fnames)
    batch_install(RES_PATH+"/expected-20230509/","/data/local/tmp/expected/",fnames)
    // For in case not be configured :
    // Because the key and DRBG entropy are set with setsockopt,
    // tests can fail on certain inputs if sysctl_optmem_max is too low.
    AdamUtils.shellRequest("sysctl -w net.core.optmem_max=204800",adb)
    //extract datas
    AdamUtils.shellRequest("bzip2 -dk /data/local/tmp/vectors/*.bz2",adb)
    //AdamUtils.shellRequest("bzip2 -dk /data/local/tmp/expected/*.bz2",adb)
    AdamUtils.shellRequest("cd /data/local/tmp/;mkdir actual;mkdir diffs",adb)

    //var foundError = false;
    vectors.forEach {
      val sr = AdamUtils.shellRequest("cd /data/local/tmp/;./acvptool -json vectors/$it -wrapper ./acvp_kernel_harness_arm64 > actual/$it",adb)

      val line = if(sr.exitCode!=0) {
        "\""+dateFormat.format(Date())+" *** processing $it ... failure ***\""+sr.toString()
      } else {
        "\""+dateFormat.format(Date())+" *** processing $it ... ok ***\""+sr.toString()
      }
      AdamUtils.shellRequest("cd /data/local/tmp/;echo $line >> acvptest.log",adb)

      //var diffResult = AdamUtils.shellRequest("cd /data/local/tmp/;diff actual/$it expected/$it > diffs/$it.diff",adb)
      /*if(sr.exitCode !== 0){
        foundError = true
        val line = "\""+dateFormat.format(Date())+" *** result for $it does not match expected ***\""
        AdamUtils.shellRequest("cd /data/local/tmp/;echo $line >> acvptest.log",adb)
        println(line)
      }*/
    }
    //File("results","kernelacvp").mkdirs()
    //Archive diff,actual dirs
    AdamUtils.shellRequest("cd /data/local/tmp/;tar -zcvf actual.tar.gz actual",adb)
    //AdamUtils.shellRequest("cd /data/local/tmp/;tar -zcvf diffs.tar.gz diffs",adb)
    //Pull worklog, actual, diff file into results dir from device

    AdamUtils.pullfile("/data/local/tmp/acvptest.log","../results/kernelacvp/",adb)
    //AdamUtils.pullfile("/data/local/tmp/diffs.tar.gz","../results/kernelacvp/",adb)
    AdamUtils.pullfile("/data/local/tmp/actual.tar.gz","../results/kernelacvp/",adb)

    //Clean the system
  }
}
