package com.example.uniqueid.encryption.utils

import android.content.Context
import android.media.MediaDrm
import android.provider.Settings
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.android.gms.ads.identifier.AdvertisingIdClient

class UniqueId {
  companion object {
    fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
    //Simple Unique ID
    fun generateUuid():String{
      return UUID.randomUUID().toString()
    }
    //
    fun getWidevineId():String{
      val widevineUuid = UUID.fromString("edef8ba9-79d6-4ace-a3c8-27dcd51d21ed")
      val ba = MediaDrm(widevineUuid).getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)
      return ba.toHex()
    }
    // https://android-developers.googleblog.com/2017/04/changes-to-device-identifiers-in.html
    fun getAndroidId(ctx: Context):String{
      return Settings.Secure.getString(ctx.getContentResolver(), Settings.System.ANDROID_ID)
    }
    //
    suspend fun getAdId(applicationContext:Context): String {
      return withContext(Dispatchers.Default) {
        try {
          AdvertisingIdClient.getAdvertisingIdInfo(applicationContext).id!!
        } catch (exception: Exception) {
          "" // there still can be an exception for other reasons but not for thread issue
        }
      }
    }
  }
}