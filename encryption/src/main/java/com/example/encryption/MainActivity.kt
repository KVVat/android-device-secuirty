package com.example.encryption


import android.os.Bundle
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.encryption.utils.AuthUtils
import com.example.encryption.utils.CoroutineKeyCheckWorker
import java.nio.charset.Charset
import java.security.InvalidKeyException
import java.util.Arrays
import java.util.concurrent.Executor
import javax.crypto.Cipher

//https://developer.android.com/training/sign-in/biometric-auth?hl=ja
class MainActivity : AppCompatActivity() {

  private val TAG = "FCS_CKH_EXT_TEST"

  private var workManager: WorkManager? =null

  private lateinit var executor: Executor
  private lateinit var biometricPrompt: BiometricPrompt
  private lateinit var promptInfo: BiometricPrompt.PromptInfo

  //Options for the application behaviour
  private var authRequired = true
  private var unlockDeviceRequired = true
  private var useBiometricAuth = false
  private var tryBackgroundKeyChainAccess = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    ///////////////////////////////////////////////////////
    //Android X Biometric authenticators
    executor = ContextCompat.getMainExecutor(this)

    //take 4 parameters from intent options (by default the app works as tap-button-to-auth test mode)
    authRequired = intent.getBooleanExtra("authRequired",true)
    unlockDeviceRequired = intent.getBooleanExtra("unlockDeviceRequired",true)
    useBiometricAuth = intent.getBooleanExtra("useBiometricAuth",false)
    tryBackgroundKeyChainAccess = intent.getBooleanExtra("tryBackgroundKeyChainAccess",false)

    val keyGenBuilder = AuthUtils.defaultKeyGenParametersBuilder(
      authRequired = authRequired,
      unlockDeviceRequired = unlockDeviceRequired,
      type = if(useBiometricAuth){ KeyProperties.AUTH_DEVICE_CREDENTIAL} else {KeyProperties.AUTH_BIOMETRIC_STRONG}
    )

    if(useBiometricAuth){
      keyGenBuilder.setInvalidatedByBiometricEnrollment(true)
    }
    val keyGenParameterSpec = keyGenBuilder.build()
    //Generate a key here
    AuthUtils.generateSecretKey(keyGenParameterSpec)

    biometricPrompt = BiometricPrompt(this, executor,
      object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int,
                                           errString: CharSequence) {
          super.onAuthenticationError(errorCode, errString)
          Toast.makeText(applicationContext,
            "Authentication error: $errString", Toast.LENGTH_SHORT)
            .show()
          Log.d(TAG,"Auth:Error")

        }

        override fun onAuthenticationSucceeded(
          result: BiometricPrompt.AuthenticationResult) {
          super.onAuthenticationSucceeded(result)

          encryptSecretInformationTest()

          Toast.makeText(applicationContext,
            "Authentication succeeded!", Toast.LENGTH_SHORT)
            .show()
          Log.d(TAG,"Auth:Success")
        }

        override fun onAuthenticationFailed() {
          super.onAuthenticationFailed()
          Log.d(TAG,"Auth:Failed")
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

    if(useBiometricAuth)
      promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Device Authentication")
        .setNegativeButtonText("Negative")
        .setAllowedAuthenticators(BIOMETRIC_STRONG)
        .build()

    val authTestButton =
      findViewById<Button>(R.id.auth_test_button)

    authTestButton.setOnClickListener {
      biometricPrompt.authenticate(promptInfo)
    }


    ///////////////////////////////////////////////////////
    //Key Check With WorkManager : Start it if it's called.
    //
    if(tryBackgroundKeyChainAccess)
    {
      workManager = WorkManager.getInstance(applicationContext)
      val request = OneTimeWorkRequest.from(CoroutineKeyCheckWorker::class.java)
      workManager?.enqueue(request)
    }
  }

  fun encryptSecretInformationTest() {
    // Exceptions are unhandled for getCipher() and getSecretKey().
    val cipher = AuthUtils.getCipher()
    val secretKey = AuthUtils.getSecretKey()
    try {
      cipher.init(Cipher.ENCRYPT_MODE, secretKey)
      val encryptedInfo: ByteArray = cipher.doFinal(
        // plaintext-string text is whatever data the developer would
        // like to encrypt. It happens to be plain-text in this example,
        // but it can be anything
        "plaintext-string".toByteArray(Charset.defaultCharset())
      )
      Log.d(
        TAG, "Encrypted information: " +
                Arrays.toString(encryptedInfo)
      )
    } catch (e: InvalidKeyException) {
      Log.e(TAG, "Key is invalid.")
    } catch (e: UserNotAuthenticatedException) {
      Log.d(TAG, "The key's validity timed out.")
      biometricPrompt.authenticate(promptInfo)
    }
  }

}