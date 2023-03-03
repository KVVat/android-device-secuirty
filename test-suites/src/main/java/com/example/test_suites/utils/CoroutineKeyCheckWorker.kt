package com.example.test_suites.utils

import android.content.Context
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import androidx.work.CoroutineWorker
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
  context: Context,
  params: WorkerParameters
) : Worker(context, params) {

  var TAG_TEST = "FCS_CKH_EXT1_HIGH_UNLOCK"

  override  fun doWork(): Result {
    repeat(10){
      try {
        tryEncrypt("key_2")
        Log.d(TAG_TEST,"UNLOCKDEVICE:OK")
        Thread.sleep(1000)
      } catch (e:Exception){
        Log.d(TAG_TEST,"UNLOCKDEVICE:NG")
        return Result.failure();
      }

    }

    return Result.success()
  }

  private fun tryEncrypt(keyname:String): Boolean {
    try {
      val keyStore = KeyStore.getInstance("AndroidKeyStore")
      keyStore.load(null)
      val secretKey: SecretKey = keyStore.getKey(keyname, null) as SecretKey
      val cipher: Cipher = Cipher.getInstance(
        KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_GCM + "/"
          + KeyProperties.ENCRYPTION_PADDING_NONE
      )
      // Try encrypting something, it will only work if the user authenticated within
      // the last AUTHENTICATION_DURATION_SECONDS seconds.
      cipher.init(Cipher.ENCRYPT_MODE, secretKey)
      cipher.doFinal("test".toByteArray())
      // If the user has recently authenticated, you will reach here.
      //showAlreadyAuthenticated()
      return true
    } catch (e: UserNotAuthenticatedException) {
      // User is not authenticated, let's authenticate with device credentials.
      return false
    } catch (e: KeyPermanentlyInvalidatedException) {
      // This happens if the lock screen has been disabled or reset after the key was
      // generated after the key was generated.
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