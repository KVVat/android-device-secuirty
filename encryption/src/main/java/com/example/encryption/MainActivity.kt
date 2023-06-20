package com.example.encryption

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.encryption.utils.CoroutineKeyCheckWorker
import androidx.biometric.BiometricPrompt


import java.io.IOException
import java.security.*
import java.util.concurrent.Executor
import javax.crypto.*
import javax.security.cert.CertificateException
//https://developer.android.com/training/sign-in/biometric-auth?hl=ja
class MainActivity : AppCompatActivity() {

  lateinit var mKeyGuardservice: KeyguardManager
  var keyLockEnabled = true;

  private val TAG = "FCS_CKH_EXT_TEST"
  private val REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1
  private val PREF_NAME:String = "FCS_CKH_EXT_PREF"

  private var workManager: WorkManager? =null;

  lateinit var keyGenParameterSpec1: KeyGenParameterSpec;
  lateinit var keyGenParameterSpec2: KeyGenParameterSpec;


  private lateinit var executor: Executor
  private lateinit var biometricPrompt: BiometricPrompt
  private lateinit var promptInfo: BiometricPrompt.PromptInfo

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val btn: Button = findViewById<Button>(R.id.test_button)
    val biometric_btn: Button = findViewById<Button>(R.id.biometric_test)

    mKeyGuardservice =
      getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager;

    if (!mKeyGuardservice.isKeyguardSecure()) {
      keyLockEnabled = false
      btn.isEnabled = false
      biometric_btn.isEnabled = false
      Log.w(TAG,"KeyGuard Secure is disabled. we can not try testing keys relate to it.")
      writePrefValue("AUTHREQUIRED","NG")

      return; //画面のロックが設定されていない
    }

    keyGenParameterSpec1 =
      keyGenParameterSpec("key_auth",true,false)
    createKey(keyGenParameterSpec1)

    keyGenParameterSpec2 =
      keyGenParameterSpec("key_unlock",false,true)
    createKey(keyGenParameterSpec2)

    btn.setOnClickListener {
      Log.i(TAG,"Button Clicked!")
      tryEncrypt("key_auth",true)
    }

    workManager = WorkManager.getInstance(applicationContext);
    val request = OneTimeWorkRequest.from(CoroutineKeyCheckWorker::class.java)
    workManager?.enqueue(request)

    //biometric authenticators
    executor = ContextCompat.getMainExecutor(this)
    biometricPrompt = BiometricPrompt(this, executor,
      object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int,
                                           errString: CharSequence) {
          super.onAuthenticationError(errorCode, errString)
          Toast.makeText(applicationContext,
            "Authentication error: $errString", Toast.LENGTH_SHORT)
            .show()
        }

        override fun onAuthenticationSucceeded(
          result: BiometricPrompt.AuthenticationResult) {
          super.onAuthenticationSucceeded(result)
          Toast.makeText(applicationContext,
            "Authentication succeeded!", Toast.LENGTH_SHORT)
            .show()
        }

        override fun onAuthenticationFailed() {
          super.onAuthenticationFailed()
          Toast.makeText(applicationContext, "Authentication failed",
            Toast.LENGTH_SHORT)
            .show()
        }
      })

    promptInfo =
      BiometricPrompt.PromptInfo.Builder()
        .setTitle("Device Authentication")
        .setAllowedAuthenticators(DEVICE_CREDENTIAL)
        .build()
    biometricPrompt.authenticate(promptInfo)
    val biometricLoginButton =
      findViewById<Button>(R.id.biometric_test)

    biometricLoginButton.setOnClickListener {
      biometricPrompt.authenticate(promptInfo)
    }
  }

  fun writePrefValue(label:String,value:String):String{
    val sharedPref = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val ret = sharedPref.getString(label,"")
    sharedPref.edit().putString(label,value).apply()
    Log.d(TAG,"${label}:${value}")
    if(ret==""){
      return value;
    } else {
      Log.d(TAG, "ID:"+label+" API Value:"+value+" Existing Value:"+ret!!)
      return ret;
    }
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
  private fun tryEncrypt(keyname:String,click_check:Boolean=false): Boolean {
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
      if(keyname.equals("key_auth") && !click_check) {
        //Log.d(TAG_TEST_AUTH,"AUTHREQUIRED:OK")
        writePrefValue("AUTHREQUIRED","OK")
        Toast.makeText(this, ("Authed"), Toast.LENGTH_LONG).show()
      }
      return true
    } catch (e: UserNotAuthenticatedException) {
      // User is not authenticated, let's authenticate with device credentials.

      if(keyname.equals("key_auth") ) {
        if(!click_check) {
          writePrefValue("AUTHREQUIRED","NG")
          Toast.makeText(this, ("NG - User Not Authed"), Toast.LENGTH_LONG).show()
        }
        showAuthenticationScreen()
      }

      return false
    } catch (e: KeyPermanentlyInvalidatedException) {
      // This happens if the lock screen has been disabled or reset after the key was
      // generated after the key was generated.
      if(keyname.equals("key_auth") && !click_check) {
        writePrefValue("AUTHREQUIRED","NG")
        Toast.makeText(this, ("NG - Key Permanently Invalidate"), Toast.LENGTH_LONG).show()
      }
      //e.printStackTrace()
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
    startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
      // Challenge completed, proceed with using cipher
      if (resultCode == Activity.RESULT_OK) {
        tryEncrypt("key_auth",false)
      } else {
        // The user canceled or didn’t complete the lock screen
        // operation. Go to error/cancellation flow.
        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
        writePrefValue("AUTHREQUIRED","NG")
      }
    }
  }



}