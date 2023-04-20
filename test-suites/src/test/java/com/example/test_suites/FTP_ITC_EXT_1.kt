package com.example.test_suites

import assertk.assertions.support.show
import com.example.test_suites.rule.AdbDeviceRule
import com.example.test_suites.utils.HostShellHelper
import com.malinskiy.adam.request.pkg.UninstallRemotePackageRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import com.malinskiy.adam.request.sync.v1.PullFileRequest
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import kotlin.system.exitProcess
import org.junit.Assert
import kotlinx.coroutines.runBlocking
import org.dom4j.Element
import org.dom4j.Node
import org.dom4j.io.SAXReader
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test



class FTP_ITC_EXT_1 {

  private val TEST_PACKAGE = "com.example.networkcheck"
  private val TEST_MODULE = "networkcheck-debug.apk"

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
    println("** A Junit test case for FTP_ITC_EXT1 started on "+ LocalDateTime.now()+" **")
  }
  @After
  fun teardown() {
    runBlocking {
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
      File(Paths.get("src", "test", "resources", "traffic_expire.pcap").toUri());
    val filePath = file_pcap.toString()
    println(filePath)

    var ret:Int = HostShellHelper.executeCommands("compgen -ac tshark")
    if(!ret.equals(0)){
      println("tshark is not found. please install the command to the environment")
      Assert.assertTrue(false)
      exitProcess(1)
    }

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

  @Test
  fun testTlsCapture() {
    runBlocking {
      //need to execute target instrumented test at least once
      var response: ShellCommandResult
      //Need to install instrumented test before testing
      //Launch packet capture software
      response =   client.execute(ShellCommandRequest(
        "am instrument -w -e class com.example.test_suites.FTP_ITC_EXT"+
          " com.example.test_suites.test/androidx.test.runner.AndroidJUnitRunner"
      ),adb.deviceSerial)
      Thread.sleep(1000*10);
      println(response.output);
      //adb pull /storage/emulated/0/Download/PCAPdroid/traffic.pcap traffic.pcap
      var p: Path = kotlin.io.path.createTempFile("t", ".pcap")
      //"/storage/emulated/0/Download/PCAPdroid/traffic.pcap"
      //client.execute(PullFileRequest(
      //  "/sdcard/Download/PCAPdroid/traffic_expire.pcap",
      //            p.toFile()),scope=this,adb.deviceSerial);
      //  ShellCommandRequest("am force-stop com.emanuelf.remote_capture"),ser);
      println(p.toAbsolutePath());
    }
  }
}
