package com.example.test_suites.utils

import com.example.test_suites.rule.AdbDeviceRule
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.Feature
import com.malinskiy.adam.request.adbd.RestartAdbdRequest
import com.malinskiy.adam.request.adbd.RootAdbdMode
import com.malinskiy.adam.request.logcat.ChanneledLogcatRequest
import com.malinskiy.adam.request.logcat.LogcatSinceFormat
import com.malinskiy.adam.request.misc.FetchHostFeaturesRequest
import com.malinskiy.adam.request.pkg.InstallRemotePackageRequest
import com.malinskiy.adam.request.prop.GetSinglePropRequest
import com.malinskiy.adam.request.shell.v2.ShellCommandRequest
import com.malinskiy.adam.request.shell.v2.ShellCommandResult
import com.malinskiy.adam.request.shell.v2.ShellCommandInputChunk
import com.malinskiy.adam.request.sync.v2.PushFileRequest
import com.malinskiy.adam.request.sync.v2.PullFileRequest
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.TimeZone
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AdamUtils {
  companion object{


    fun root(adb:AdbDeviceRule):String{
      var ret:String
      runBlocking {
       ret = adb.adb.execute(
         request = RestartAdbdRequest(RootAdbdMode),
         serial = adb.deviceSerial)
      }

      println(ret)
      return ret;
    }
    fun shellRequest(shellCommand:String,adb:AdbDeviceRule):ShellCommandResult{
      var ret:ShellCommandResult

      runBlocking {

        ret = adb.adb.execute(
          ShellCommandRequest(shellCommand),
          adb.deviceSerial)
      }
      println(ret.exitCode)
      return ret
    }
    fun waitLogcatLine(waitTime:Int,tagWait:String,adb:AdbDeviceRule):LogcatResult? {
      var found = false
      var text:String = ""
      var tag:String = ""
      runBlocking {
        val deviceTimezoneString = adb.adb.execute(GetSinglePropRequest("persist.sys.timezone"), adb.deviceSerial).trim()
        val deviceTimezone = TimeZone.getTimeZone(deviceTimezoneString)
        val nowInstant = Instant.now()
        val request = ChanneledLogcatRequest(LogcatSinceFormat.DateString(nowInstant, deviceTimezoneString), modes = listOf())
        val channel = adb.adb.execute(request, this, adb.deviceSerial)

        // Receive logcat for max several seconds, wait and find certain tag text
        for (i in 1..waitTime) {
          var lines:List<LogLine> = channel.receive()
            .split("\n")
            .mapNotNull { LogLine.of(it, deviceTimezone) }
            .filterIsInstance<LogLine.Log>()
            .filter { it.level == 'D'}
            .filter {
              it.tag.equals(tagWait)
            }

          if(!lines.isEmpty()){
            //println(lines);
            tag = lines.get(0).tag
            text = lines.get(0).text
            found = true
            break;
          }
          delay(100)
        }
        channel.cancel()
      }
      if(found) {
        return LogcatResult(tag, text)
      } else {
        return null
      }
    }
    fun pullfile(sourcePath:String,destDir:String,adb: AdbDeviceRule){
      runBlocking {
        var p: Path = Paths.get(sourcePath);
        val destPath: Path = Paths.get(destDir, p.fileName.toString());
        val features: List<Feature> = adb.adb.execute(request = FetchHostFeaturesRequest())
        val channel = adb.adb.execute(
          PullFileRequest(sourcePath,destPath.toFile(),
                          supportedFeatures = features,null,coroutineContext),
          this,
          adb.deviceSerial);
        var percentage = 0
        for (percentageDouble in channel) {
          percentage = (percentageDouble * 100).toInt()
          println(percentage)
        }
      }
    }
    fun InstallApk(apkFile: File, reinstall: Boolean = false, adb:AdbDeviceRule): String {
      var stdio: com.malinskiy.adam.request.shell.v1.ShellCommandResult
      var client:AndroidDebugBridgeClient = adb.adb;

      runBlocking {
        val features: List<Feature> = adb.adb.execute(request = FetchHostFeaturesRequest())
        val fileName = apkFile.name
        val channel = client.execute(PushFileRequest(apkFile, "/data/local/tmp/$fileName",features),
                                     GlobalScope,
                                     serial = adb.deviceSerial)
        while (!channel.isClosedForReceive) {
          val progress: Double? =
            channel.tryReceive().onClosed {
            }.getOrNull()
        }
        //add -g option to permit all exisitng runtime option
        stdio = client.execute(InstallRemotePackageRequest(
          "/data/local/tmp/$fileName", reinstall, listOf("-g")), serial = adb.deviceSerial)
      }
      return stdio.output;
    }
  }
}



