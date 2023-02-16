package com.example.encryption

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.MasterKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.IOException
import java.security.*
import javax.crypto.*
import javax.security.cert.CertificateException


//The module simply record Unique Id to the configuration file
class MainActivity : AppCompatActivity() {
  lateinit var mKeyGuardservice:KeyguardManager
  var keyLockEnabled = true;
  var TAG = "ADSRP_ENCRYPTION"
  var TAG_TEST = "FCS_CKH_EXT1_HIGH"

  lateinit var keyGenParameterSpec1: KeyGenParameterSpec;
  lateinit var keyGenParameterSpec2: KeyGenParameterSpec;
  private val REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1


  class CoroutineKeyCheckWorker(
    context: Context,
    params: WorkerParameters
  ) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

      return Result.success()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val btn:Button = findViewById<Button>(R.id.test_button)

    mKeyGuardservice =
      getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager;
    if (!mKeyGuardservice.isKeyguardSecure()) {
      keyLockEnabled = false;
      btn.isEnabled = false
      Log.w(TAG,"KeyGuard Secure is disabled. we can not try testing keys relate to it.")
      Log.d(TAG_TEST,"KeyFeature:setAuthenticationRequired=true => disabled")
      return; //画面のロックが設定されていない
    }

    keyGenParameterSpec1 =
    keyGenParameterSpec("key_1",true,false)
    createKey(keyGenParameterSpec1)

    keyGenParameterSpec2 =
      keyGenParameterSpec("key_2",false,true)
    createKey(keyGenParameterSpec2)

    btn.setOnClickListener {
      Log.i(TAG,"Button Clicked!")
      tryEncrypt("key_1")
    }

    testUnlockedDeviceRequired()

    /* Application should run ke_2 check in background
    val request = OneTimeWorkRequestBuilder<MyWorkTestable>()
      .build()

    // Enqueue and wait for result. This also runs the Worker synchronously
    // because we are using a SynchronousExecutor.
    workManager.enqueue(request).result.get()
    // Get WorkInfo
    val workInfo = workManager.getWorkInfoById(request.id).get()
    */
  }

  private fun testUnlockedDeviceRequired():Boolean
  {
    try {
      tryEncrypt("key_2")
      Log.d(TAG_TEST,"KeyFeature:setUnlockedDeviceRequired=true => success")
      return true
    } catch (e:java.lang.RuntimeException){
      Log.d(TAG_TEST,"KeyFeature:setUnlockedDeviceRequired=true => failed")
      return false
    }
  }

  override fun onStart() {
    super.onStart()
  }

  private fun createKey(keyGenParameterSpec: KeyGenParameterSpec) {
    try {
      val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
      keyStore.load(null)
      val keyGenerator: KeyGenerator = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
      )
      keyGenerator.init(keyGenParameterSpec)
      keyGenerator.generateKey()
    } catch (e: NoSuchAlgorithmException) {
      throw RuntimeException("Failed to create a symmetric key", e)
    } catch (e: NoSuchProviderException) {
      throw RuntimeException("Failed to create a symmetric key", e)
    } catch (e: InvalidAlgorithmParameterException) {
      throw RuntimeException("Failed to create a symmetric key", e)
    } catch (e: KeyStoreException) {
      throw RuntimeException("Failed to create a symmetric key", e)
    } catch (e: CertificateException) {
      throw RuntimeException("Failed to create a symmetric key", e)
    } catch (e: IOException) {
      throw RuntimeException("Failed to create a symmetric key", e)
    }
  }
  fun keyGenParameterSpec(keyNameAlias:String,authRequired:Boolean,unlockDeviceRequired:Boolean): KeyGenParameterSpec {
    try {
      return KeyGenParameterSpec.Builder(keyNameAlias,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setKeySize(256).setEncryptionPaddings(
          KeyProperties.ENCRYPTION_PADDING_NONE)
        .setUnlockedDeviceRequired(unlockDeviceRequired)
        .setUserAuthenticationRequired(authRequired) // Require that the user has unlocked in the last 30 seconds
        .setUserAuthenticationValidityDurationSeconds(1)
        .build()
    } catch (e: NoSuchAlgorithmException) {
      throw java.lang.RuntimeException("Failed to create a symmetric key", e)
    } catch (e: NoSuchProviderException) {
      throw java.lang.RuntimeException("Failed to create a symmetric key", e)
    } catch (e: InvalidAlgorithmParameterException) {
      throw java.lang.RuntimeException("Failed to create a symmetric key", e)
    } catch (e: KeyStoreException) {
      throw java.lang.RuntimeException("Failed to create a symmetric key", e)
    } catch (e: CertificateException) {
      throw java.lang.RuntimeException("Failed to create a symmetric key", e)
    } catch (e: IOException) {
      throw java.lang.RuntimeException("Failed to create a symmetric key", e)
    }
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
      Log.d(TAG_TEST,"KeyFeature:setAuthenticationRequired=true => success")
      Toast.makeText(this, ("Authed"), Toast.LENGTH_LONG).show()
      return true
    } catch (e: UserNotAuthenticatedException) {
      // User is not authenticated, let's authenticate with device credentials.
      Log.d(TAG_TEST,"KeyFeature:setAuthenticationRequired=true => failed")
      showAuthenticationScreen()
      return false
    } catch (e: KeyPermanentlyInvalidatedException) {
      // This happens if the lock screen has been disabled or reset after the key was
      // generated after the key was generated.
      Log.d(TAG_TEST,"KeyFeature:setAuthenticationRequired=true => failed (keys are invalidated after created)")
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
  private fun showAuthenticationScreen() {
    // Create the Confirm Credentials screen. You can customize the title and description. Or
    // we will provide a generic one for you if you leave it null
    val intent: Intent = mKeyGuardservice.createConfirmDeviceCredentialIntent(null, null)
    if (intent != null) {
      startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
      // Challenge completed, proceed with using cipher
      if (resultCode == Activity.RESULT_OK) {
        if (tryEncrypt("key_1")) { }
      } else {
        // The user canceled or didn’t complete the lock screen
        // operation. Go to error/cancellation flow.
        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
      }
    }
  }
}