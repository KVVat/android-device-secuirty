package com.example.test_suites.utils

import java.text.MessageFormat
import java.time.LocalDateTime
import org.junit.Rule
import org.junit.rules.TestWatcher;
import org.junit.runner.Description

class ADSRPTestWatcher():TestWatcher() {
  override fun starting(desc: Description?) {
    println(MessageFormat.format("==========================================\n[Test Start] : {0} on {1}", desc, LocalDateTime.now()))
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