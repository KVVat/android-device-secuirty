package com.example.test_suites.utils

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.malinskiy.adam.request.shell.v1.ShellCommandRequest

public class UIAutomatorHelper(c:Context,d:UiDevice) {
    private val mDevice: UiDevice = d
    private var mContext: Context? = c

    fun safeObjectClick(objectLabel:String,timeout:Long){
        //Ignore exception in case object is not found to suppress unintentional/varying behaviour
        try {
            mDevice.wait(Until.findObject(By.text(objectLabel)),timeout).click();
        } catch(ex:java.lang.NullPointerException){
            Log.d("TAG", "Click $objectLabel ignored")
        }
    }

    fun setScreenLockText(label:String,PIN:String){
        launchSettings(Settings.ACTION_SECURITY_SETTINGS);
        swipeUp()
        Thread.sleep(500);
        safeObjectClick("Screen lock",2000)
        safeObjectClick(label,2000)
        for(i in 0..1) {
            //client.execute(ShellCommandRequest("input text ${PIN}"))
            Thread.sleep(1000);
            mDevice.executeShellCommand("input text ${PIN}")
            mDevice.pressEnter()
            Thread.sleep(1000);
        }
        Thread.sleep(2000);
        safeObjectClick("DONE",2000)
        safeObjectClick("Done",2000)
    }

    fun resetScreenLockText(PIN: String) {
        launchSettings(Settings.ACTION_SECURITY_SETTINGS);
        swipeUp()
        Thread.sleep(500);
        safeObjectClick("Screen lock",2000)
        Thread.sleep(1000);
        mDevice.executeShellCommand("input text ${PIN}")
        mDevice.pressEnter()
        Thread.sleep(1000);
        safeObjectClick("None",2000)
        safeObjectClick("Delete",2000)
        safeObjectClick("DONE",2000)
    }
    fun launchSettings(page:String){
        val intent = Intent(page)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        mContext!!.startActivity(intent)
        Thread.sleep(5000 )
    }
    fun isLockScreenEnbled():Boolean{
        val km = mContext!!.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return km.isKeyguardSecure
    }

    /**
     * screenlock
     */
    fun sleepAndWakeUpDevice() {
        mDevice.sleep()
        Thread.sleep(100)
        mDevice.wakeUp()
    }
    fun swipeUp(){
        mDevice.swipe(mDevice.getDisplayWidth() / 2, mDevice.getDisplayHeight(),
            mDevice.getDisplayWidth() / 2, 0, 30);
        Thread.sleep(1000);
    }


}