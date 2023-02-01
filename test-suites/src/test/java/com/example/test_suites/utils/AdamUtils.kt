package com.example.test_suites.utils

import com.example.test_suites.rule.AdbDeviceRule
import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.logcat.ChanneledLogcatRequest
import com.malinskiy.adam.request.logcat.LogcatSinceFormat
import com.malinskiy.adam.request.pkg.InstallRemotePackageRequest
import com.malinskiy.adam.request.prop.GetSinglePropRequest
import com.malinskiy.adam.request.shell.v1.ShellCommandResult
import com.malinskiy.adam.request.sync.v1.PushFileRequest
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.TimeZone
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class AdamUtils {
  companion object{

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

    fun InstallApk(apkFile: File, reinstall: Boolean = false, adb:AdbDeviceRule): ShellCommandResult {
      var stdio: ShellCommandResult
      var client:AndroidDebugBridgeClient = adb.adb;

      runBlocking {
        val fileName = apkFile.name
        val channel = client.execute(PushFileRequest(apkFile, "/data/local/tmp/$fileName"),
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
      return stdio
    }
  }
}



