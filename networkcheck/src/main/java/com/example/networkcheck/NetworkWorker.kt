package com.example.networkcheck

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class NetworkWorker (context: Context,
                     params: WorkerParameters
) : Worker(context, params) {

  override  fun doWork(): Result {
    val url:String = inputData.getString("url")!!;
    val type:String = inputData.getString("type")!!;

    var ret: Int = 0;
    if (type.equals("http")) {
      ret = NetworkUtils.testHttpURLConnection(url)
    } else if (type.equals("okhttp3")) {
      ret = NetworkUtils.testOkHttp3(url)
    }

    Log.d("","${type} return code=>"+ret);
    if(ret == 200){
      return Result.success()
    } else {
      return Result.failure()
    }
  }
}


