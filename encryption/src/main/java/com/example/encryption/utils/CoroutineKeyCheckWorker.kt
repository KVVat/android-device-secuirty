package com.example.encryption.utils

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.IOException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableKeyException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey
import javax.security.cert.CertificateException

class CoroutineKeyCheckWorker(
  private val context: Context,
  params: WorkerParameters
) : Worker(context, params) {

  private val TAG = "FCS_CKH_EXT_TEST"
  private val PREF_NAME:String = "FCS_CKH_EXT_PREF"

  override  fun doWork(): Result {
    repeat(50){
      try {
        tryEncrypt(AuthUtils.KEY_NAME)
        writePrefValue("UNLOCKDEVICE","OK")
        Thread.sleep(100)
      } catch (e:Exception){
        writePrefValue("UNLOCKDEVICE","NG")
        Thread.sleep(100)
        return Result.failure()
      }

      val pf: SharedPreferences =
        context.getSharedPreferences(PREF_NAME,Context.MODE_PRIVATE)
      val resultAuth = pf.getString("AUTHREQUIRED","")
      val resultUnlock = pf.getString("UNLOCKDEVICE","")

      Log.d(TAG,"AUTHREQUIRED:$resultAuth,UNLOCKDEVICE:$resultUnlock")
    }
    return Result.success()
  }
  private fun writePrefValue(label:String, value:String):String{
    val sharedPref = applicationContext.getSharedPreferences(
      PREF_NAME, Context.MODE_PRIVATE)

    val ret = sharedPref.getString(label,"")
    sharedPref.edit().putString(label,value).apply()

    return if(ret=="") value else ret!!
  }

  private fun tryEncrypt(key_name:String): Boolean {
    try {
      val keyStore = KeyStore.getInstance("AndroidKeyStore")
      keyStore.load(null)
      val secretKey: SecretKey = keyStore.getKey(key_name, null) as SecretKey
      val cipher: Cipher = Cipher.getInstance(
        KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_GCM + "/"
                + KeyProperties.ENCRYPTION_PADDING_NONE
      )
      // Try encrypting something, it will only work if the user authenticated within
      // the last AUTHENTICATION_DURATION_SECONDS seconds.
      cipher.init(Cipher.ENCRYPT_MODE, secretKey)
      cipher.doFinal("test".toByteArray())
      return true
    } catch (e: UserNotAuthenticatedException) {
      // User is not authenticated, let's authenticate with device credentials.
      return false
    } catch (e: KeyPermanentlyInvalidatedException) {
      return false
    } catch (e: BadPaddingException) {
      throw java.lang.RuntimeException(e)
    } catch (e: IllegalBlockSizeException) {
      throw java.lang.RuntimeException(e)
    } catch (e: KeyStoreException) {
      throw java.lang.RuntimeException(e)
    } catch (e: CertificateException) {
      throw java.lang.RuntimeException(e)
    } catch (e: UnrecoverableKeyException) {
      throw java.lang.RuntimeException(e)
    } catch (e: IOException) {
      throw java.lang.RuntimeException(e)
    } catch (e: NoSuchPaddingException) {
      throw java.lang.RuntimeException(e)
    } catch (e: NoSuchAlgorithmException) {
      throw java.lang.RuntimeException(e)
    } catch (e: InvalidKeyException) {
      throw java.lang.RuntimeException(e)
    }
  }
}