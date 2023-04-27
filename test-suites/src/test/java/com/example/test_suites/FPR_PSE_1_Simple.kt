package com.example.test_suites


import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import com.example.test_suites.rule.AdbDeviceRule
import com.example.test_suites.utils.ADSRPTestWatcher
import com.example.test_suites.utils.AdamUtils
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import java.io.File
import java.io.StringReader
import java.nio.file.Paths
import java.text.DecimalFormat
import java.time.LocalDateTime
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.not
import org.hamcrest.core.IsEqual
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import org.junit.rules.TestName
import org.junit.rules.TestWatcher
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.hamcrest.CoreMatchers.`is` as Is

//FPR_PSE.1
class FPR_PSE_1_Simple {

  private val TEST_PACKAGE = "com.example.uniqueid"
  private val TEST_MODULE = "uniqueid-debug.apk"
  private val LONG_TIMEOUT = 5000L
  private val SHORT_TIMEOUT = 1000L

  @Rule @JvmField
  public var errs:ErrorCollector = ErrorCollector()
  @Rule @JvmField
  public var watcher:TestWatcher = ADSRPTestWatcher()
  @Rule @JvmField
  public var name:TestName  = TestName();

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

      println("> The test verifies that the apis which generate unique ids return expected values.")
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
      println("Values of each api results : "+dictA.toString())

      //kill process (am force-stop com.package.name)
      client.execute(ShellCommandRequest("am force-stop $TEST_PACKAGE"), adb.deviceSerial);

      //launch application
      client.execute(ShellCommandRequest("am start -n $TEST_PACKAGE/$TEST_PACKAGE.MainActivity"), adb.deviceSerial);
      Thread.sleep(SHORT_TIMEOUT*5);

      //Store preference into map B/check prefernce and compare included values against A
      response =
        client.execute(ShellCommandRequest("run-as ${TEST_PACKAGE} cat /data/data/$TEST_PACKAGE/shared_prefs/UniqueID.xml"), adb.deviceSerial)

      val dictB:Map<String,String> = fromPrefMapListToDictionary(response.output.trimIndent())
      println("Values of each api results (after reboot) : "+dictB.toString());
      println("Check all api values are maintained.");

      var i = 1

      //Expected : All unique id values should be maintained
      //Note : Each test should not interrupt execution of the test case
      //errs.checkThat(getAssertMsg(i++),"A",IsEqual("B"))
      //errs.checkThat(getAssertMsg(i++),"B",IsEqual("B"))
      //errs.checkThat(getAssertMsg(i++),"C",IsEqual("B"))

      errs.checkThat(getAssertMsg(i++),dictA["UUID"],IsEqual(dictB["UUID"]))
      errs.checkThat(getAssertMsg(i++),dictA["ADID"],IsEqual(dictB["ADID"]))
      errs.checkThat(getAssertMsg(i++),dictA["AID"],IsEqual(dictB["AID"]))
      errs.checkThat(getAssertMsg(i++),dictA["WIDEVINE"],IsEqual(dictB["WIDEVINE"]))
      errs.checkThat(getAssertMsg(i++),dictA["IMEI1"],IsEqual(""))
      errs.checkThat(getAssertMsg(i++),dictA["IMEI2"],IsEqual(""))

      println(">Uninstall/Install again the target apk.");
      //uninstall application =>
      response = client.execute(UninstallRemotePackageRequest(TEST_PACKAGE), adb.deviceSerial)
      Thread.sleep(SHORT_TIMEOUT*2);
      //println(response.output)
      //install application again
      var respstring = AdamUtils.InstallApk(file_apk, false,adb);
      Thread.sleep(SHORT_TIMEOUT*2);
      //println(respstring)
      //launch application
      client.execute(ShellCommandRequest("am start -n $TEST_PACKAGE/$TEST_PACKAGE.MainActivity"), adb.deviceSerial);
      Thread.sleep(SHORT_TIMEOUT*5);
      //check preference and compare included values against A and B
      response =
        client.execute(ShellCommandRequest("run-as ${TEST_PACKAGE} cat /data/data/$TEST_PACKAGE/shared_prefs/UniqueID.xml"), adb.deviceSerial)
      val dictC:Map<String,String> = fromPrefMapListToDictionary(response.output.trimIndent())

      println(">Check the api values except UUID should be maintained.");
      //Expected : UUID should be changed. Others should be maintained
      //You should set allowbackup option in module's androidmanifest.xml to false
      //for passing this test.(the option makes application a bit vulnerable to attack)
      //Note : Each test should not interrupt execution of the test case
      errs.checkThat(getAssertMsg(i++),dictA["UUID"],Is(not(dictC["UUID"])))
      errs.checkThat(getAssertMsg(i++),dictA["ADID"],IsEqual(dictC["ADID"]))
      errs.checkThat(getAssertMsg(i++),dictA["AID"],IsEqual(dictC["AID"]))
      errs.checkThat(getAssertMsg(i++),dictA["WIDEVINE"],IsEqual(dictB["WIDEVINE"]))
      errs.checkThat(getAssertMsg(i++),dictA["IMEI1"],IsEqual(""))
      errs.checkThat(getAssertMsg(i++),dictA["IMEI2"],IsEqual(""))

    }
  }
  private fun getAssertMsg(idx: Int): String? {
    val id = DecimalFormat("000").format(idx);
    println(">Check values:"+id)
    return name.methodName + " : step=" + DecimalFormat("000").format(idx)
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