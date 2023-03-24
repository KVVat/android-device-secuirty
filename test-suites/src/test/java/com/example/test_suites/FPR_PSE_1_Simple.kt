package com.example.test_suites


import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import com.example.test_suites.rule.AdbDeviceRule
import com.example.test_suites.utils.AdamUtils
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import java.io.File
import java.io.StringReader
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource

import java.time.LocalDateTime

//FPR_PSE.1
class FPR_PSE_1_Simple {

  private val TEST_PACKAGE = "com.example.uniqueid"
  private val TEST_MODULE = "uniqueid-debug.apk"
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
    println("** A Junit test case for FPR_PSE_1 started on "+ LocalDateTime.now()+" **")

  }

  @After
  fun teardown() {
    runBlocking {
      client.execute(ShellCommandRequest("rm /data/local/tmp/$TEST_MODULE"),
                     adb.deviceSerial)
    }
  }

  @Test
  fun testUniqueIDs()
  {
    runBlocking {
      val file_apk: File =
        File(Paths.get("src", "test", "resources", TEST_MODULE).toUri());

      println("** A junit test case for FPR_PSE_1 started on "+LocalDateTime.now()+" **")

      AdamUtils.InstallApk(file_apk, false,adb);

      Thread.sleep(SHORT_TIMEOUT*2);

      var response: ShellCommandResult

      //launch application (am start -n com.package.name/com.package.name.ActivityName)
      response =   client.execute(ShellCommandRequest("am start -n $TEST_PACKAGE/$TEST_PACKAGE.MainActivity"), adb.deviceSerial);
      Thread.sleep(LONG_TIMEOUT);
      response =
        client.execute(ShellCommandRequest("run-as ${TEST_PACKAGE} cat /data/data/$TEST_PACKAGE/shared_prefs/UniqueID.xml"), adb.deviceSerial)
      //store preference into map A
      //the map contains unique ids below : ADID,UUID,AID,WIDEVINE (see application code)
      val dictA:Map<String,String> = fromPrefMapListToDictionary(response.output.trimIndent())
      //

      //kill process (am force-stop com.package.name)
      client.execute(ShellCommandRequest("am force-stop $TEST_PACKAGE"), adb.deviceSerial);

      //launch application
      client.execute(ShellCommandRequest("am start -n $TEST_PACKAGE/$TEST_PACKAGE.MainActivity"), adb.deviceSerial);
      Thread.sleep(SHORT_TIMEOUT*5);

      //Store preference into map B/check prefernce and compare included values against A
      response =
        client.execute(ShellCommandRequest("run-as ${TEST_PACKAGE} cat /data/data/$TEST_PACKAGE/shared_prefs/UniqueID.xml"), adb.deviceSerial)

      val dictB:Map<String,String> = fromPrefMapListToDictionary(response.output.trimIndent())
      //println(dictB);

      //Expected : All unique id values should be maintained
      assertThat(dictA["UUID"]).isEqualTo(dictB["UUID"])
      assertThat(dictA["ADID"]).isEqualTo(dictB["ADID"])
      assertThat(dictA["AID"]).isEqualTo(dictB["AID"])
      assertThat(dictA["WIDEVINE"]).isEqualTo(dictB["WIDEVINE"])

      //uninstall application =>
      client.execute(UninstallRemotePackageRequest(TEST_PACKAGE), adb.deviceSerial)
      Thread.sleep(SHORT_TIMEOUT*2);

      //install application again
      AdamUtils.InstallApk(file_apk, false,adb);
      Thread.sleep(SHORT_TIMEOUT*2);

      //launch application
      client.execute(ShellCommandRequest("am start -n $TEST_PACKAGE/$TEST_PACKAGE.MainActivity"), adb.deviceSerial);
      Thread.sleep(SHORT_TIMEOUT*5);
      //check preference and compare included values against A and B
      response =
        client.execute(ShellCommandRequest("run-as ${TEST_PACKAGE} cat /data/data/$TEST_PACKAGE/shared_prefs/UniqueID.xml"), adb.deviceSerial)
      val dictC:Map<String,String> = fromPrefMapListToDictionary(response.output.trimIndent())

      //Expected : UUID should be changed. Others should be maintained
      assertThat(dictA["UUID"]).isNotEqualTo(dictC["UUID"])
      assertThat(dictA["ADID"]).isEqualTo(dictC["ADID"])
      assertThat(dictA["AID"]).isEqualTo(dictC["AID"])
      assertThat(dictA["WIDEVINE"]).isEqualTo(dictC["WIDEVINE"])
    }
  }

  fun fromPrefMapListToDictionary(xml:String):Map<String,String>{
    val source = InputSource(StringReader(xml))

    val dbf: DocumentBuilderFactory = DocumentBuilderFactory.newInstance()
    val db: DocumentBuilder = dbf.newDocumentBuilder()
    val document: Document = db.parse(source)

    val nodes: NodeList = document.getElementsByTagName("string");
    var  ret = mutableMapOf<String,String>();
    for(i in 0 .. nodes.length-1){
      var node: Node = nodes.item(i);
      val key:String = node.attributes.getNamedItem("name").nodeValue;
      val value:String = node.textContent
      ret.put(key,value);
    }
    return ret;
  }
}