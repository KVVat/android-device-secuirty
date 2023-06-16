package com.example.test_suites.utils

import com.example.test_suites.rule.AdbDeviceRule
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import kotlinx.coroutines.runBlocking
import org.dom4j.Element
import org.dom4j.Node
import org.dom4j.io.SAXReader
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

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

                        if (response.exitCode != 0) {
                            println("touch action failure:" + response.output)
                        }
                    }
                }
            }
        }
    }
}