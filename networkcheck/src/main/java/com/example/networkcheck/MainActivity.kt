package com.example.networkcheck

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager


//The module simply record Unique Id to the configuration file
class MainActivity : AppCompatActivity() {
  val TAG:String = "FTP_ITC_EXT1";
  private var workManager: WorkManager? =null;

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    //

   /* val intent = Intent(Intent.ACTION_VIEW)
    intent.component = ComponentName("com.emanuelef.remote_capture",
                                     "com.emanuelef.remote_capture.activities.CaptureCtrl")
    //intent.addFlags(Intent.)
    intent.putExtra("action","start")
    intent.putExtra("capture_auto","true")
    intent.putExtra("pcap_dump_mode","pcap_file")
    intent.putExtra("pcap_name","traffic_pcap")

    startActivity(intent)*/

    var type:String? = intent.getStringExtra("type")
    var url:String? = intent.getStringExtra("url")
    if(type.isNullOrBlank()) type = "http"
    if(url.isNullOrBlank()) url = "https://tls-v1-2.badssl.com:1012/"

    val data = Data.Builder().putString("url",url).putString("type",type).build();

    workManager = WorkManager.getInstance(applicationContext);
    val request = OneTimeWorkRequest.Builder(NetworkWorker::class.java).setInputData(data)
    workManager?.enqueue(request.build())
  }

}