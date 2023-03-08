#!/usr/bin/env bash

echo "Choose a test case to run:"
echo "1: ** All automatable JUnit cases **"
echo "2: ** All automatable AndroidJUnit cases **"
echo "3: FCS_CKH_EXT1_Low (DirectBoot)"
echo "4: FCS_CKH_EXT1_High (ScreenLock)"
echo "5: FIA_AFL_1 (ScreenLock)"
echo -n ">"
read -r NUM

if [ $NUM -eq 1 ]; then
  ./gradlew testDebug --tests com.example.test_suites.\*_Simple
elif [ $NUM -eq 2 ]; then
  echo 2
elif [ $NUM -eq 3 ]; then
  echo "This test case automtically reboot the target device. "
  echo "Please ensure booting device as fast as possible.     "
  read -r WARN
  ./gradlew testDebug --tests com.example.test_suites.FCS_CKH_EXT1_Manual
elif [ $NUM -eq 4 ]; then
  echo 4
elif [ $NUM -eq 5 ]; then
  echo 5
else
  echo "Please input value from 1 to 5."
  exit 1
fi

exit 0