package com.example.adamsample.utils

import com.example.adamsample.rule.AdbDeviceRule
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

// These regex comes from https://cs.android.com/android/platform/superproject/+/master:development/tools/bugreport/src/com/android/bugreport/logcat/LogcatParser.java
private val BUFFER_BEGIN_RE = Pattern.compile("--------- beginning of (.*)")
private val LOG_LINE_RE = Pattern.compile(
  "((?:(\\d\\d\\d\\d)-)?(\\d\\d)-(\\d\\d)\\s+(\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d\\d\\d)\\s+(\\d+)\\s+(\\d+)\\s+(.)\\s+)(.*?):\\s(.*)",
  Pattern.MULTILINE
)
private val sinceFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")
  .withZone(ZoneId.systemDefault())

data class LogcatResult(var tag:String,var text:String)

@Suppress("HasPlatformType", "MemberVisibilityCanBePrivate")
sealed class LogLine(val matcher: Matcher) {
  abstract val tag: String
  abstract val text:String
  class BufferLine(rawText: String) : LogLine(BUFFER_BEGIN_RE.matcher(rawText).also { it.find() }) {
    val bufferBegin = matcher.group(1)
    override val tag="[Blank]"
    override val text = bufferBegin
    override fun toString() = "[BufferLine] $bufferBegin"
  }

  class Log(rawText: String, val timeZone: TimeZone) : LogLine(LOG_LINE_RE.matcher(rawText).also { it.find() }) {
    val date = Calendar.getInstance(timeZone).apply {
      set(Calendar.MONTH, matcher.group(3).toInt() - 1)
      set(Calendar.DAY_OF_MONTH, matcher.group(4).toInt())
      set(Calendar.HOUR_OF_DAY, matcher.group(5).toInt())
      set(Calendar.MINUTE, matcher.group(6).toInt())
      set(Calendar.SECOND, matcher.group(7).toInt())
      set(Calendar.MILLISECOND, matcher.group(8).toInt())
    }

    val pid = matcher.group(9)
    val tid = matcher.group(10)
    val level = matcher.group(11)[0]
    override val tag = matcher.group(12)
    override val text = matcher.group(13)

    //val instant get() = ZonedDateTime.ofInstant(date.toInstant(), timeZone.toZoneId())

    override fun toString() = "[LogLine] $tag: $text"

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as Log

      if (date != other.date) return false
      if (pid != other.pid) return false
      if (tid != other.tid) return false
      if (level != other.level) return false
      if (tag != other.tag) return false
      if (text != other.text) return false

      return true
    }

    override fun hashCode(): Int {
      var result = date?.hashCode() ?: 0
      result = 31 * result + (pid?.hashCode() ?: 0)
      result = 31 * result + (tid?.hashCode() ?: 0)
      result = 31 * result + level.hashCode()
      result = 31 * result + (tag?.hashCode() ?: 0)
      result = 31 * result + (text?.hashCode() ?: 0)
      return result
    }
  }

  companion object {
    fun of(rawText: String, timeZone: TimeZone): LogLine? = when {
      BUFFER_BEGIN_RE.matcher(rawText).matches() -> BufferLine(rawText)
      LOG_LINE_RE.matcher(rawText).matches() -> Log(rawText, timeZone)
      else -> null
    }
  }
}

