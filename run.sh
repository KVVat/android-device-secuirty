#!/usr/bin/env bash

# relative path
echo $(dirname $0)
# full path
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
### prepare a result directory to the current dir
rdir="$DIR/results"
xmldir="$DIR/xml-patches"
### Check for dir, if not found create it using the mkdir ##
if [ -d "$xmldir" ]; then rm -Rf $xmldir; fi
[ ! -d "$rdir" ] && mkdir -p "$rdir"
[ ! -d "$xmldir" ] && mkdir -p "$xmldir"


clone_output () {
  echo $1 $2 #arg $1=test key $2=test type
  dstdir="$rdir/$1"
  srcdir="$DIR/$2"
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
# echo "5: FIA_AFL_1 (ScreenLock Password)"
echo "6: FCS_COP_1 (Kernel Acvp Test)"
echo "9: Clean test"
echo "Results will be stored in $rdir"
echo -n ">"
read -r NUM

if [ $NUM -eq 1 ]; then
  ./gradlew testDebug --tests com.example.test_suites.\*_Simple
  ./gradlew task xmlPatchAfterExecute
  # copy test results to the result directory
  #clone_output $NUM test-results
elif [ $NUM -eq 2 ]; then
   echo "Those test cases run on the target device. please check logcat to confirm the process of the test."
  ./gradlew -Pandroid.testInstrumentationRunnerArguments.class=com.example.test_suites.FCS_CKH_EXT1_High connectedAndroidTest
  clone_output $NUM instrumentation-results
elif [ $NUM -eq 3 ]; then
  echo "This test case automtically reboot the target device. "
  echo "Please ensure booting the target device as fast as possible.(otherwise it fails)"
  echo "Note : If a logcat line says 'des=Success,ces=Failed'. The test was succeeded"
  echo "*** any key to start ***"
  read -r WARN
  ./gradlew testDebug --tests com.example.test_suites.FCS_CKH_EXT1
  ./gradlew task xmlPatchAfterExecute
elif [ $NUM -eq 4 ]; then
  echo " - The test automatically operate the target device with UIAutomator"
  echo " - For running this test you need to set the Screenlock setting to 'None'. "
  echo "*** any key to start ***"
  read -r WARN
  ./gradlew -Pandroid.testInstrumentationRunnerArguments.class=com.example.test_suites.FCS_CKH_EXT1_High2 connectedAndroidTest
  clone_output $NUM instrumentation-results
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
elif [ $NUM -eq 6 ]; then
  echo " - This test runs ACVP harness for FIPS 140-2 certifications."
  echo " - The test require a device with custom android build which enables certain kernel features.(see comment or design doc)"
  echo " - The target device os need to be rooted , and it should be singing with debug key. "
  echo "Start This Test Case? (y/n)"
  read -r WARN
  if [ $WARN = "y" ] || [ $WARN = "Y" ]; then
   ./gradlew testDebug --tests com.example.test_suites.KernelAcvpTest.testKernelAcvp
   ./gradlew task xmlPatchAfterExecute
   #./gradlew task singleTargetPatchExample -Poutdir=$NUM
   #clone_output $NUM test-results
  fi
elif [ $NUM -eq 9 ]; then
  ./gradlew clean
else
  echo "Please input a value included in the choices."
  exit 1
fi

exit 0

# For acvpkernel test you should compile kernel with the settings below...
  # CONFIG_CRYPTO_FIPS140_MOD=y
  # CONFIG_CRYPTO_DRBG_HASH=y
  # CONFIG_CRYPTO_DRBG_CTR=y
  # CONFIG_CRYPTO_USER_API_HASH=y
  # CONFIG_CRYPTO_USER_API_SKCIPHER=y
  # CONFIG_CRYPTO_USER_API_RNG=y
  # CONFIG_CRYPTO_USER_API_RNG_CAVP=y
  # CONFIG_CRYPTO_USER_API_AEAD=y