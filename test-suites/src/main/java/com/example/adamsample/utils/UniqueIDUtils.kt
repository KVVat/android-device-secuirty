package com.example.adamsample.utils

import android.content.Context
import android.media.MediaDrm
import android.provider.Settings
import java.util.UUID


class UniqueIDUtils {
  companion object {
    fun generateUuid():String{
      return UUID.randomUUID().toString()
    }
    fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

    fun getWidevineId():String{
      val widevineUuid = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed")
      val ba = MediaDrm(widevineUuid).getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
      return ba.toHex()
    }
    // https://android-developers.googleblog.com/2017/04/changes-to-device-identifiers-in.html
    fun getAndroidId(ctx: Context):String{
      return Settings.Secure.getString(ctx.getContentResolver(), Settings.System.ANDROID_ID)
    }
  }
}