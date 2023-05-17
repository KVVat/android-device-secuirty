package com.example.test_suites.utils

import java.io.File
import java.text.MessageFormat
import java.time.LocalDateTime
import org.junit.rules.TestWatcher;
import org.junit.runner.Description

class ADSRPTestWatcher():TestWatcher() {
  override fun starting(desc: Description?) {
    println(MessageFormat.format("==========================================\n[Test Start] : {0} on {1}", desc, LocalDateTime.now()))
    //get class annotation
    /*val myClassKClass = desc!!.testClass
    println(">"+myClassKClass)
    val sfr = myClassKClass.getAnnotation(SFR::class.java)
    println(">"+sfr.description)
    println(">"+sfr.title)
    //access the outputfile. confirm if it's oka
    val packageName = desc!!.testClass.canonicalName
    val fname = String.format("../instrumentation-results/connected/TEST-Pixel 6 - 13-_test-suites-.xml",packageName)
    val f: File? = File(fname)

    println(fname)
    println(f.toString()+","+f!!.exists())*/

  }

  override fun succeeded(desc: Description?) {
    println(MessageFormat.format("[Test Succeeded] : {0}", desc))
  }

  override fun failed(e: Throwable, desc: Description?) {
    System.err.println(
      MessageFormat.format(
        "[Test Failed] : {0} \r\n*** Exception : {1}.", desc, e.message
      )
    )
  }

  override fun finished(desc: Description?) {
    println(MessageFormat.format("[Test Finished] : {0}", desc))
    //postprocess
  }
}