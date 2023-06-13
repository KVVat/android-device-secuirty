package com.example.test_suites


import android.graphics.Point
import com.example.test_suites.rule.AdbDeviceRule
import com.example.test_suites.utils.ADSRPTestWatcher
import com.example.test_suites.utils.AdamUtils
import com.example.test_suites.utils.HostShellHelper
import com.example.test_suites.utils.LogcatResult
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import org.dom4j.Element
import org.dom4j.Node
import org.dom4j.io.SAXReader
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathFactory
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import java.io.IOException
import java.nio.file.Files
import java.util.Date
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

      //val file_apk: File =
      //  File(Paths.get("src", "test", "resources", "pcapdroid-debug.apk").toUri())
      //client.execute(UninstallRemotePackageRequest(TEST_PACKAGE), adb.deviceSerial)
      client.execute(ShellCommandRequest("rm /data/local/tmp/$TEST_MODULE"),
                     adb.deviceSerial)
    }
  }

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

  @Test
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
  }


  class UIAutomatorSession
    (var adb: AdbDeviceRule, var packageName: String) {
    var document: org.dom4j.Document?;
    init {
      runBlocking {
        var response: ShellCommandResult =
          adb.adb.execute(ShellCommandRequest("uiautomator dump"), adb.deviceSerial)
        val found = Regex("^.*([ ].*.xml)\$").find(response.output)
        if (found?.groups != null) {
          val srcpath: String? = found.groups[1]?.value?.trim();
          //println("***$srcpath***")
          var temppath: Path = kotlin.io.path.createTempFile("ui", ".xml")
          AdamUtils.pullfile(srcpath!!.trim(), temppath.absolutePathString(), adb, true)
          val xmlFile = File(temppath.toUri())
          document = SAXReader().read(xmlFile)
        } else {
          document = null
        }
      }
    }
    fun enumPackageSymbols()
    {
      if(document != null){
        val nodes = document!!.selectNodes(
          "//node[contains(@package, '${packageName}')]"
        )
        for(n:Node in nodes){
          val target = n as Element
          val _1 = target.attributeValue("class")
          val _2 = target.attributeValue("package")
          val _3 = target.attributeValue("resource-id")
          val _4 = target.attributeValue("text")
          println("class=>$_1 \npackage=>$_2 \nres=> $_3 \ntext=>$_4")
        }
      }
    }
    fun labelToId(label:String){
      if(document != null){

      }
    }

    fun exists(id:String):Boolean{
      if(document != null) {
        val nodes = document!!.selectNodes(
          "//node[contains(@resource-id, '${id}') and contains(@package, '${packageName}')]"
        )
        return nodes.size != 0
      } else {
        return false
      }
    }
    //fun swipe()
    fun tap(id:String){
      if(document != null){
        runBlocking {
          val nodes = document!!.selectNodes(
            "//node[contains(@resource-id, '${id}') and contains(@package, '${packageName}')]"
          )
          if (nodes.size != 0) {
            val target = (nodes[0] as Node) as Element
            val bounds = target.attributeValue("bounds")
            val pp = bounds.split("][")
            val pos = arrayOf(0, 0, 0, 0)
            var i = 0;
            if (pp.size == 2) {
              for (ppp: String in pp) {
                var nums = ppp.replace("[", "")
                  .replace("]", "").split(",")
                if (nums.size == 2) {
                  pos[i] = nums[0].toInt()
                  pos[i + 1] = nums[1].toInt()
                }
                i += 2;
              }
              val cx = ((pos[2] - pos[0]) / 2) + pos[0]
              val cy = ((pos[3] - pos[1]) / 2) + pos[1]
              //tap
              println("tap $cx, $cy")
              var response: ShellCommandResult =
                adb.adb.execute(
                  ShellCommandRequest("input touchscreen tap $cx $cy"), adb.deviceSerial
                )

              if (response.exitCode !== 0) {
                println("touch action failure:" + response.output)
              }
            }
          }
        }
      }
    }
  }

  fun copyPcapToOutPath(pcap:Path,name:String):Path
  {
    val outdir = File(Paths.get(OUT_PATH).toUri());
    if(!outdir.exists()){
     outdir.mkdir()
    } else if(!outdir.isDirectory){
      outdir.delete()
      outdir.mkdir()
    }
    val tstmp = Date().time
    val to = Paths.get(OUT_PATH,"${tstmp}-${name}.pdml")
    try {
      Files.copy(pcap, to)
    } catch (e: IOException) {
      e.printStackTrace()
    }
    return to
  }
  val OUT_PATH  = "../results/capture/"
  @Test
  fun testTlsCapture() {
    runBlocking {
      //need to execute target instrumented test at least once

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
          ),adb.deviceSerial)

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
                " -e openurl https://www.google.com"
      ),adb.deviceSerial)

      //Wait worker response on logcat and get return code from that
      val res:LogcatResult? =
        AdamUtils.waitLogcatLine(100,"worker@return",adb)
      if(res !== null){
        println("worker@return=>"+res.text)
        //evaluate the return value
      } else {
        //res == null break *panic*
      }
      Thread.sleep(100);
      //Open a connection(?) on the URL(??) and cast the response(???)
      //kill processes
      client.execute(ShellCommandRequest("am force-stop com.emanuelf.remote_capture"),adb.deviceSerial)
      client.execute(ShellCommandRequest("am force-stop com.example.openurl"),adb.deviceSerial)
      //Thread.sleep(1000*5);
      //assertThat(ret).isEqualTo(400)
      // adb shell am force-stop com.emanuelf.remote_capture
      //mDevice.executeShellCommand(
      //  "am force-stop com.emanuelf.remote_capture")
      //Thread.sleep(1000*5);
      //pull a pdml file
      val src = "/storage/emulated/0/Download/PCAPdroid/traffic.pcap"
      var pcap0: Path = kotlin.io.path.createTempFile("t", ".pcap")
      AdamUtils.pullfile(src, pcap0.toString(), adb, true)
      val pcap = copyPcapToOutPath(pcap0,"dummy")
      Thread.sleep(3000)
      var cmdret = HostShellHelper.executeCommands("compgen -ac tshark")
      if(!cmdret.equals(0)){
        println("tshark is not found. please install the command to the environment")
        Assert.assertTrue(false)
        exitProcess(1)
      }

      //convert pcap to pdml file to analyze
      val cmd="""\
tshark -r ${pcap.toAbsolutePath()} -o tls.debug_file:ssldebug.log \
-o tls.desegment_ssl_records:TRUE \
-o tls.desegment_ssl_application_data:TRUE -V -T pdml > ${pcap.toAbsolutePath()}.xml
"""
      cmdret = HostShellHelper.executeCommands(cmd)
      Thread.sleep(3000)

      println(cmdret)


    }
  }
}
