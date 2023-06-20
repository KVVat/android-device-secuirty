package com.example.test_suites.utils

import android.provider.Settings.ACTION_SECURITY_SETTINGS
import com.example.test_suites.rule.AdbDeviceRule
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.dom4j.Element
import org.dom4j.Node
import org.dom4j.io.SAXReader
import java.io.File
import java.net.URI
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

class UIAutomatorSession
    (var adb: AdbDeviceRule, var packageName: String) {

    private var document: org.dom4j.Document?
    private lateinit var xmlFile:File
    private var client:AndroidDebugBridgeClient = adb.adb
    private lateinit var outerBounds:Array<Int>
    init {
        runBlocking {
            val response: ShellCommandResult =
                adb.adb.execute(ShellCommandRequest("uiautomator dump"), adb.deviceSerial)
            val found = Regex("^.*([ ].*.xml)\$").find(response.output)
            //if dump command failure
            if (found?.groups != null) {
                val srcpath: String? = found.groups[1]?.value?.trim()
                //println("***$srcpath***")
                val temppath: Path = kotlin.io.path.createTempFile("ui", ".xml")
                AdamUtils.pullfile(srcpath!!.trim(), temppath.absolutePathString(), adb, true)
                xmlFile = File(temppath.toUri())
                document = SAXReader().read(xmlFile)
                //get display size from outer object
                if(document !== null){
                    val node = document!!.selectSingleNode(
                        "/hierarchy/node") as Element
                    val bounds = node.attributeValue("bounds")
                    outerBounds = boundsToArray(bounds)
                    //println("$outerBounds")
                }
            } else {
                document = null
            }
        }
    }
    private fun boundsToArray(literal:String):Array<Int>
    {
        val pp = literal.split("][")
        val pos = arrayOf(0, 0, 0, 0)
        var i = 0
        if (pp.size == 2) {
            for (ppp: String in pp) {
                val nums = ppp.replace("[", "")
                    .replace("]", "").split(",")
                if (nums.size == 2) {
                    pos[i] = nums[0].toInt()
                    pos[i + 1] = nums[1].toInt()
                }
                i += 2
            }
        }
        //println("$pos")
        return pos
    }
    suspend fun shellexec(cmd:String):ShellCommandResult
    {
        return client.execute(ShellCommandRequest(cmd), adb.deviceSerial)
    }
    suspend fun freezeRotation(){
        shellexec("settings put system accelerometer_rotation 0")
    }
    suspend fun unfreezeRotation(){
        shellexec("settings put system accelerometer_rotation 1")
    }

    //fun launchSettings(page:String){
    //        val intent = Intent(page)
    //        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    //        mContext!!.startActivity(intent)

    suspend fun showSettings(child:String=""){
        //display all available settings
        //adb shell dumpsys | grep "SETTINGS"
        if(child == "") {
            shellexec("am start -a android.settings.SETTINGS")
        } else if(child.equals("security")){
            shellexec("am start -a android.settings.SECURITY_SETTINGS")
        } else {
            shellexec("am start -a $child")
        }
        delay(1000)
    }

    suspend fun isScreenOff():Boolean
    {
        val r = shellexec("dumpsys deviceidle | grep mScreenOn")
        return r.output.contains("mScreenOn=false")
    }
    suspend fun turnon(){
        if(isScreenOff()) {
            shellexec("input keyevent 26")
        }
    }
    fun copyxml(path: Path){
        //println(xmlFile.absolutePath)
        Files.copy(Paths.get(xmlFile.absolutePath),path,REPLACE_EXISTING)
    }

    // adb shell dumpsys deviceidle | grep mScreenOn
    // adb shell input keyevent 26
    // adb shell dumpsys deviceidle | grep mScreenLocked

    fun enumPackageSymbols()
    {
        if(document != null){
            val nodes = document!!.selectNodes(
                "//node[contains(@package, '${packageName}')]"
            )
            for(n: Node in nodes){
                val target = n as Element
                val _1 = target.attributeValue("class")
                val _2 = target.attributeValue("package")
                val _3 = target.attributeValue("resource-id")
                val _4 = target.attributeValue("text")
                println("class=>$_1 \npackage=>$_2 \nres=> $_3 \ntext=>$_4")
            }
        }
    }
    /*fun labelToId(label:String){
        if(document != null){

        }
    }*/

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
    class UIClickable(x:Int,y:Int,h:Int,w:Int){
    }
    fun listAllClickables(){
        if(document != null){
            val nodes = document!!.selectNodes(
                "//node[@clickable='true']")
            for(n in nodes){
                val elem = n as Element
                val cls = elem.attributeValue("class")
                val res = elem.attributeValue("resource-id")
                val pkg = elem.attributeValue("package")
                var bounds = boundsToArray(elem.attributeValue("bounds"))

                if(cls.endsWith("Layout")){
                    //check if clickable layout contains text node

                } else if(cls.endsWith("Button")){
                    var name = elem.attributeValue("text")
                    if(name.isNullOrBlank()){
                        name = elem.attributeValue("content-desc")
                        if(name.isNullOrBlank()) name = "<not found>"
                    }
                }
            }
            println(nodes.size)
            //println(nodes);
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

                    val pos = boundsToArray(bounds)
                    val cx = ((pos[2] - pos[0]) / 2) + pos[0]
                    val cy = ((pos[3] - pos[1]) / 2) + pos[1]
                    //tap
                    println("tap $cx, $cy")
                    val response: ShellCommandResult = shellexec("input touchscreen tap $cx $cy")

                    if (response.exitCode != 0) {
                        println("touch action failure:" + response.output)
                    }
                }
            }
        }
    }
}