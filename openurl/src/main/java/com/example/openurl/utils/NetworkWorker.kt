package com.example.openurl.utils

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters

class NetworkWorker (context: Context,
                     params: WorkerParameters
) : Worker(context, params) {

  override  fun doWork(): Result {
    val url:String = inputData.getString("url")!!;
    val type:String = inputData.getString("type")!!;

    var ret: Int = 0;
    //setProgress(firstUpdate)
    setProgressAsync(Data.Builder().putString("progress","... Initialize $type").build())
    if (type.equals("http")) {
      ret = NetworkUtils.testHttpURLConnection(url)
    } else if (type.equals("okhttp3")) {
      ret = NetworkUtils.testOkHttp3(url)
    }

    //setProgressAsync(Data.Builder().putString("return_code","$type").build())

    if(ret == 200){
      return Result.success(Data.Builder().putString("progress","... Success(${ret})").putString("return",ret.toString()).build())
    } else {
      return Result.failure(Data.Builder().putString("progress","... Failure(${ret})").putString("return",ret.toString()).build())
    }
  }
}


