#!/usr/bin/env bash

# relative path
$(dirname $0)
# full path
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
### prepare a result directory to the current dir
rdir="$DIR/results"
### Check for dir, if not found create it using the mkdir ##
[ ! -d "$rdir" ] && mkdir -p "$rdir"

clone_output () {
  echo $1 $2 #arg $1=test key $2=test type
  dstdir="$rdir/$1"
  srcdir="$DIR/test-suites/build/reports/$2"
  if [ -d "$dstdir" ]; then rm -Rf $dstdir; fi
  mkdir -p "$dstdir"
  cp -r $srcdir $dstdir
}

# dialogue
echo "*** Android Security Test Stacks ***"
echo "Choose a test case to run:"
echo "1: ** All automatable JUnit cases **"
echo "2: ** All automatable AndroidJUnit cases **"
echo "3: FCS_CKH_EXT1_Low (DirectBoot)"
echo "4: FCS_CKH_EXT1_High (File Encryption)"
echo "5: FIA_AFL_1 (ScreenLock Password)"
echo "9: Clean test"
echo "Results will be stored in $rdir"
echo -n ">"
read -r NUM

if [ $NUM -eq 1 ]; then
  ./gradlew testDebug --tests com.example.test_suites.\*_Simple
  # copy test results to the result directory
  clone_output $NUM tests/testDebugUnitTest
elif [ $NUM -eq 2 ]; then
  ./gradlew -Pandroid.testInstrumentationRunnerArguments.class=com.example.test_suites.FCS_CKH_EXT1_High connectedAndroidTest
  clone_output $NUM androidTests
elif [ $NUM -eq 3 ]; then
  echo "This test case automtically reboot the target device. "
  echo "Please ensure booting the target device as fast as possible.(otherwise it fails)"
  echo "If we can see a log cat line says 'des=Success,ces=Failed'. The test was succeeded"
  echo "*** any key to start ***"
  read -r WARN
  ./gradlew testDebug --tests com.example.test_suites.FCS_CKH_EXT1
  clone_output $NUM tests/testDebugUnitTest
elif [ $NUM -eq 4 ]; then
  echo " - The test automatically operate the target device with UIAutomator"
  echo " - For running this test you need to set the Screenlock setting to 'None'. "
  echo "*** any key to start ***"
  read -r WARN
  ./gradlew -Pandroid.testInstrumentationRunnerArguments.class=com.example.test_suites.FCS_CKH_EXT1_High2 connectedAndroidTest
elif [ $NUM -eq 5 ]; then
  echo " - The test depends on the device/system environment, and we test it on the pixel devices"
  echo " - The test automatically operate the target device with UIAutomator"
  echo " - For running this test you need to set the Screenlock setting to 'None'. "
  echo " - For running this test you need to set the System navigation setting to 'Gesture Navigation'. "
  echo "Start This Test Case? (y/n)"
  read -r WARN
  if [ $WARN = "y" ] || [ $WARN = "Y" ]; then
    ./gradlew -Pandroid.testInstrumentationRunnerArguments.class=com.example.test_suites.FIA_AFL_1 connectedAndroidTest
  fi
elif [ $NUM -eq 9 ]; then
  ./gradlew clean
else
  echo "Please input a value included in the choices."
  exit 1
fi

exit 0