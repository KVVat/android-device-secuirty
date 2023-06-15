package com.example.test_suites


import android.graphics.Point
import com.example.test_suites.rule.AdbDeviceRule
import com.example.test_suites.utils.ADSRPTestWatcher
import com.example.test_suites.utils.AdamUtils
import com.example.test_suites.utils.HostShellHelper
import com.example.test_suites.utils.LogcatResult
import com.example.test_suites.utils.TestAssertLogger
import com.example.test_suites.utils.UIAutomatorSession
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import org.dom4j.Document
import org.dom4j.Element
import org.dom4j.Node
import org.dom4j.io.SAXReader
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import org.junit.rules.TestName
import org.junit.rules.TestWatcher
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathFactory
//import org.w3c.dom.Document
//import org.w3c.dom.NodeList
import java.io.IOException
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
//import java.util.Date
//import org.w3c.dom.Node
import javax.xml.xpath.XPathConstants

class FTP_ITC_EXT_1 {

  private val TEST_PACKAGE = "com.example.networkcheck"
  private val TEST_MODULE = "networkcheck-debug.apk"

  @Rule
  @JvmField
  val adb = AdbDeviceRule()
  private val client = adb.adb

  @Rule @JvmField
  var watcher: TestWatcher = ADSRPTestWatcher(adb)
  @Rule @JvmField
  var testname: TestName = TestName();
  //Asset Log
  var a: TestAssertLogger = TestAssertLogger(testname)
  @Rule @JvmField
  var errs: ErrorCollector = ErrorCollector()

  var PKG_PCAPDROID ="com.emanuelef.remote_capture"
  @Before
  fun setup() {
    runBlocking {
      //client.execute(UninstallRemotePackageRequest(PKG_PCAPDROID), adb.deviceSerial)
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




  /*Node.selectChildNodes(){

  }*/
  fun Node.attrib(attrib:String="showname"):String{
    //if(this == null) return ""
    //val node = this.selectSingleNode(".//field[@name='$key']")
    val elem = this as Element
    return elem.attributeValue(attrib).toString()
  }
  fun Node.selectChild(key:String):Node?
  {
    //if(this == null) return null;
    return this.selectSingleNode(".//descendant::field[@name='${key}']");
  }
  fun Node.selectChildren(key:String):List<Node>?
  {
    //if(this == null) return null;
    return this.selectNodes(".//descendant::field[@name='${key}']");
  }
  fun _showname(n:Node?):String{
    return n?.attrib() ?: "N/A"
  }
  fun _value(n:Node?):String {
    return n?.attrib("value") ?: "0"
  }
  companion object {

  }

  val REQUIRED_CIPHERS_IN_SFR = arrayOf(
    "TLS_RSA_WITH_AES_256_GCM_SHA384",
    "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
    "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
    "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384",
    "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
    "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
    "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384")
  @Test
  fun anaylzeCertainPdml()
  {
    var p:Path = Paths.get("../results/capture/20230614133107-normal.pcap.xml")
    val document:Document = SAXReader().read(File(p.toUri()))

    //check dns.qry.name and find a connection to examine
    //make sure to ignore the packets before querying the target host ....
    //document.
    val nodes = document.selectNodes("/pdml/packet/proto[@name='tls']")

    if(nodes.size == 0) {
      //there are no tls records...exit
      return
    }

    var helloLookupDone = false;
    var certLookupDone = false;
    for(tlsp in nodes){
      val records = tlsp.selectChildren("tls.record")//multiple tls.records can be exist in a proto tag
      if(records !== null) {
        var i=1;
        for (record in records) {
          println(record!!.attrib()+"[$i]")
          println("\t" + _showname(record.selectChild("tls.record.version")))
          println("\t" + _showname(record.selectChild("tls.handshake.type")))
          println("\t\t>" + _value(record.selectChild("tls.handshake.type")))
          val hsType = _value(record.selectChild("tls.handshake.type")).toInt(16)
          if(hsType>0){
            //test for client hello
            if(hsType == 1 && !helloLookupDone){ //Client Hello
              //test 1: client need to support some certain ciphersuite listed in SFR
              val ciphers = record.selectChildren("tls.handshake.ciphersuite")
              if(ciphers !== null){
                println("\t\ttest 1:ciphers>")
                var matches:MutableList<String> = mutableListOf()
                for(c in ciphers) {
                  println("\t\t\t>"+_showname(c))
                  val cipherName = _showname(c)
                  REQUIRED_CIPHERS_IN_SFR.forEach { it->
                    if(cipherName.indexOf(it)>=0){
                      matches.add(it)
                    }
                  }
                }
                //should support one of the ciphersuite listed in SFR:
                if(matches.size>=1){
                  println("supported ciphers in SFR requirement:"+matches.toString())
                } else {
                  //should assert
                  println("found no ciphers which is required to implement.")
                }
              } else {
                //should assert
                println("found no ciphers block in this tls packet")
              }
              //test 2
              val tlsversions = record.selectChildren("tls.handshake.extensions.supported_version")
              if(tlsversions !== null){
                //implement TLS v1.2 [7], TLS v1.3 [11] or higher version of TLS;
                println("\t\ttest 2:versions>")//0x.0304,0303
                //var matches:MutableList<String> = mutableListOf()
                var supported:Boolean = false
                for(ver in tlsversions) {
                  var found = _value(ver).toInt()
                  if(found == 0x0304 || found == 0x0303){
                    supported = true;
                    break;
                  }
                }
                if(supported){
                  println("The client supports tls v1.2 or later")
                } else {
                  //should assert
                  println("The client does not support tls v1.2 or later")
                }
              } else {
                //should assert
                println("found no tlsversion block in this tls packet")
              }
              helloLookupDone = true
            }
            else if(hsType == 11 && !certLookupDone){ //Certificate
              //check : x509af.version

              //Check validity of certificate : expiration date

              certLookupDone = true
            }
          }
          i++;
        }
      }
    //if("tls.handshake.type"==1) //client hello
      //{
      //list and eval supported tls versions
      //list and eval support cipher suites
      //}

      //if(record.selectChild("tls.handshake.type"))
    }
    println(nodes.size)
  }

  @Test
  fun testNormalHost(){
    val resp:Pair<String,Path> =
      tlsCapturePacket("normal","https://tls-v1-2.badssl.com:1012/")

    println(resp)
  }

  @Test
  fun testExpiredHost(){
    val resp:Pair<String,Path> =
      tlsCapturePacket("expired","https://expired.badssl.com/")

    println(resp)
  }

  @Test
  fun testInvalidHost(){
    val resp:Pair<String,Path> =
      tlsCapturePacket("invalid","https://wrong.host.badssl.com/")

    println(resp)
  }


  val OUT_PATH  = "../results/capture/"
  fun copyPcapToOutPath(pcap:Path,testlabel:String):Path
  {
    val outdir = File(Paths.get(OUT_PATH).toUri());
    if(!outdir.exists()){
     outdir.mkdir()
    } else if(!outdir.isDirectory){
      outdir.delete()
      outdir.mkdir()
    }
    val tstmp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now())
    val to = Paths.get(OUT_PATH,"${tstmp}-${testlabel}.pcap")
    try {
      Files.copy(pcap, to)
    } catch (e: IOException) {
      e.printStackTrace()
    }
    return to
  }
  fun tlsCapturePacket(testlabel:String,testurl:String):Pair<String,Path> {

    var pcap:Path
    var http_resp:String

    runBlocking {
      //prerequite module check
      var cmdret = HostShellHelper.executeCommands("compgen -ac tshark")
      if(!cmdret.equals(0)){
        println("tshark is not found. please install the command to the environment")
        Assert.assertTrue(false)
        exitProcess(1)
      }
      val serial = adb.deviceSerial
      //Install prerequisite modules
      val pcap_apk=
        File(Paths.get("src", "test", "resources", "pcapdroid-debug.apk").toUri())
      var ret = AdamUtils.InstallApk(pcap_apk, true,adb)
      Assert.assertTrue(ret.startsWith("Success"))
      val browser_apk=
        File(Paths.get("src", "test", "resources", "openurl-debug.apk").toUri())
      ret = AdamUtils.InstallApk(browser_apk, false,adb)
      Assert.assertTrue(ret.startsWith("Success"))

      var response =
        client.execute(ShellCommandRequest(
          "am start -n com.emanuelef.remote_capture/.activities.CaptureCtrl"+
                  " -e action start"+
                  " -e pcap_dump_mode pcap_file"+
                  " -e pcap_name traffic.pcap"
          ),serial)

      println(response)

      //Launch packet capture software with uiautomator session
      //if it's first time we should say 'OK' to 3 dialogues,
      //after that we only need to say once.
      UIAutomatorSession(adb,PKG_PCAPDROID).run {
        val label0= "${PKG_PCAPDROID}:id/allow_btn"
        if(exists(label0)){ tap(label0) } else return@run
        Thread.sleep(2000)
        UIAutomatorSession(adb,PKG_PCAPDROID).run level2@{
          val label1= "android:id/button1"
          if(exists(label1)){ tap(label1) } else return@level2
          Thread.sleep(2000)
          UIAutomatorSession(adb,"com.android.vpndialogs").run level3@{
            val label2= "android:id/button1"
            if(exists(label2)){ tap(label2) } else return@level3
          }
        }
      }
      Thread.sleep(1000);
      //Launch openurl app to access a certain website!
      client.execute(ShellCommandRequest(
        "am start -a android.intent.action.VIEW -n com.example.openurl/.MainActivity"+
                " -e openurl $testurl"
      ),serial)

      //Wait worker response on logcat and get return code from that
      val res:LogcatResult? =
        AdamUtils.waitLogcatLine(100,"worker@return",adb)

      if(res !== null){
        println("worker@return=>"+res.text)
        //evaluate the return value
      } else {
        //res == null break *panic*
        println("we can't grab the return value from worker.")
        Assert.assertTrue(false)
      }
      //return value
      http_resp = res!!.text
      //
      Thread.sleep(500);
      //Open a connection(?) on the URL(??) and cast the response(???)
      //kill processes
      client.execute(ShellCommandRequest("am force-stop com.emanuelf.remote_capture"),serial)
      client.execute(ShellCommandRequest("am force-stop com.example.openurl"),serial)
      Thread.sleep(500);
      //pull a pdml file
      val src = "/storage/emulated/0/Download/PCAPdroid/traffic.pcap"
      var pcap0: Path = kotlin.io.path.createTempFile("t", ".pcap")
      AdamUtils.pullfile(src, pcap0.toString(), adb, true)
      //
      pcap = copyPcapToOutPath(pcap0,testlabel)
      Thread.sleep(3000)

      //convert pcap to pdml file to analyze
      val cmd="""\
tshark -r ${pcap.toAbsolutePath()} -o tls.debug_file:ssldebug.log \
-o tls.desegment_ssl_records:TRUE \
-o tls.desegment_ssl_application_data:TRUE -V -T pdml > ${pcap.toAbsolutePath()}.xml
"""
      cmdret = HostShellHelper.executeCommands(cmd)

      println(cmdret)
      Thread.sleep(1000)
      //return Pair<String,Path>(res!!.text,pcap)
    }
    return Pair(http_resp,pcap)
  }
}


/*
fun selectNodesValues(target:Node,name:String):List<String>{
  val nodes = target.selectNodes(".//field[@name='"+name+"']")
  return selectNodesValues(nodes,name)
}

fun selectNodesValues(nodes:List<Node>,name:String="<internal>"):List<String>{
  //val nodes = node.selectNodes(".//field[@name='"+name+"']")
  var ret =  mutableListOf<String>()
  for(ii in 0..nodes.size-1) {
    val record = (nodes[ii] as Node) as Element
    val showname = record.attributeValue("showname").toString()
    println("$name[$ii]:" + showname)
    ret.add(showname)
  }
  return ret
}
fun selectNodesValuePairs(target:Node,name:String):List<Pair<String,String>>{
  val nodes = target.selectNodes(".//field[@name='"+name+"']")
  return selectNodesValuePairs(nodes,name)
}
fun selectNodesValuePairs(nodes:List<Node>,name:String="<internal>"):List<Pair<String,String>>{
  //val nodes = nodes.selectNodes(".//field[@name='"+name+"']")
  var ret =  mutableListOf<Pair<String,String>>()
  for(ii in 0..nodes.size-1) {
    val record = (nodes[ii] as Node) as Element
    val showname = record.attributeValue("showname").toString()
    val show = record.attributeValue("show").toString()
    println("$name[$ii]:" + showname+","+show)
    ret.add(Pair<String,String>(showname,show))
  }
  return ret
}

fun testPcapReader() {
  val file_pcap: File =
    File(Paths.get("src", "test", "resources", "traffic.pcap").toUri());
  val filePath = file_pcap.toString()
  println(filePath)

  var ret:Int = HostShellHelper.executeCommands("compgen -ac tshark")
  if(!ret.equals(0)){
    println("tshark is not found. please install the command to the environment")
    Assert.assertTrue(false)
    exitProcess(1)
  }
  //convert from pcap to pdml
  val cmd:String="""\
  tshark -r $filePath -o tls.debug_file:ssldebug.log \
-o tls.desegment_ssl_records:TRUE \
-o tls.desegment_ssl_application_data:TRUE -V -T pdml > pdml_analysis.xml
"""
  ret = HostShellHelper.executeCommands(cmd)
  println(ret)
  var listSupportedAlgorithm = listOf<String>()

  if(ret.equals(0)){
    val pdml = File(Paths.get("pdml_analysis.xml").toUri())
    val document = SAXReader().read(pdml)
    val nodes = document.selectNodes("/pdml/packet/proto[@name='tls']")
    if(nodes.size == 0) return
    println(nodes.size)
    var i=1
    for(tlspacket in nodes){
      println(">"+i+"==============================")
      val handshakes = selectNodesValuePairs(tlspacket,"tls.handshake.type")
      val records = selectNodesValues(tlspacket,"tls.record");
      //val handshakes = getNodeValuesPair(tlspacket,"tls.handshake.type")
      println(handshakes.size)
      if(handshakes.size>0){
        //val handshakes:List<Pair<String,String>> = selectNodesValuePairs(handshakes_)
        for(ii in 0..handshakes.size-1) {
          var p:Pair<String,String> = handshakes[ii]
          val showname = p.first
          val step = Integer.valueOf(p.second)
          //val step: Int = Integer.valueOf(handshake.attributeValue("value"), 16)
          //val showname = handshake.attributeValue("showname").toString()
          println("" + showname + " step:" + step);
          if(step.equals(0)){
            //Pick data for evaluation//
            //<field name="tls.handshake.extensions_server_name" showname="Server Name: expired.badssl.com" size="18" pos="165" show="expired.badssl.com" value="657870697265642e62616473736c2e636f6d"/>
            selectNodesValuePairs(tlspacket,"tls.handshake.ciphersuite")
            selectNodesValuePairs(tlspacket,"tls.handshake.sig_hash_alg")
            selectNodesValuePairs(tlspacket,"tls.handshake.extensions.supported_version")
            selectNodesValuePairs(tlspacket,"tls.handshake.extensions_server_name")
            //tlspacket.selectNodes(".//field[@name='tls.handshake.ciphersuite']")
            //tlspacket.selectNodes(".//field[@name='tls.handshake.sig_hash_alg']")
            //tlspacket.selectNodes(".//field[@name='tls.handshake.extensions.supported_version']")
            //tlspacket.selectNodes(".//field[@name='tls.handshake.extensions_server_name']")//Connected Host
          }
        }
      } else {
        println("handshake element not found")
        val alerts = tlspacket.selectNodes(".//field[@name='tls.alert_message']")
        if(alerts.size>0){
          for(ii in 0..alerts.size-1) {
            val alert = alerts[ii] as Node
            val alml = alert.selectNodes(".//field[@name='tls.alert_message.level']")
            val almd = alert.selectNodes(".//field[@name='tls.alert_message.desc']")
            if(alml.size>0 && almd.size>0){
              val alml_ = (alml[0] as Node) as Element
              val almd_ = (almd[0] as Node) as Element
              println("TLS Alert:"+alml_.attributeValue("showname")+
                      ","+almd_.attributeValue("showname"))
            }
          }
        }
      }
      i++
    }
  }
}*/