package com.example.encryption

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

//The module simply record Unique Id to the configuration file
class MainActivity : AppCompatActivity() {
  val TAG:String = "MainActivity";
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    
  }

  fun getPrefValueOrWrite(label:String,value:String):String{
    val sharedPref = getSharedPreferences("UniqueID", Context.MODE_PRIVATE)
    val ret = sharedPref.getString(label,"")
    if(ret==""){
      if(!ret.equals(value)){
        Log.d(TAG, "New API Value=>"+value+" assigned.")
      } else {
        Log.d(TAG, "It's a panic case. Both api value and existing value are blank.")
      }
      sharedPref.edit().putString(label,value).apply()
      return value;
    } else {
      Log.d(TAG, "ID:"+label+" API Value:"+value+" Existing Value:"+ret!!+" Constancy:"+(ret.equals(value)))
      return ret!!;
    }
  }
}