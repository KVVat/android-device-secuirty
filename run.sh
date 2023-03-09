#!/usr/bin/env bash

echo "Choose a test case to run:"
echo "1: ** All automatable JUnit cases **"
echo "2: ** All automatable AndroidJUnit cases **"
echo "3: FCS_CKH_EXT1_Low (DirectBoot)"
echo "4: FCS_CKH_EXT1_High (File Encryption)"
echo "5: FIA_AFL_1 (ScreenLock Password)"
echo -n ">"
read -r NUM

if [ $NUM -eq 1 ]; then
  ./gradlew testDebug --tests com.example.test_suites.\*_Simple
elif [ $NUM -eq 2 ]; then
  ./gradlew -Pandroid.testInstrumentationRunnerArguments.class=com.example.test_suites.FCS_CKH_EXT1_High connectedAndroidTest
elif [ $NUM -eq 3 ]; then
  echo "This test case automtically reboot the target device. "
  echo "Please ensure booting device as fast as possible.     "
  read -r WARN
  ./gradlew testDebug --tests com.example.test_suites.FCS_CKH_EXT1
elif [ $NUM -eq 4 ]; then
    ./gradlew -Pandroid.testInstrumentationRunnerArguments.class=com.example.test_suites.FCS_CKH_EXT1_High2 connectedAndroidTest
elif [ $NUM -eq 5 ]; then
    echo " - The test depends on the device/system environment, and we test it on the pixel devices"
    echo " - The test autmatically operate the target device with UIAutomator"
    echo " - For running this test you need to set the screenlock setting to 'None'. "
    echo "Start Test Cases? (Y/N)"
    read -r WARN
    ./gradlew -Pandroid.testInstrumentationRunnerArguments.class=com.example.test_suites.FIA_AFL_1 connectedAndroidTest
else
  echo "Please input a value included in the choices."
  exit 1
fi

exit 0