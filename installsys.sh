#!/usr/bin/env sh

adb push ./app/build/outputs/apk/debug/app-debug.apk /data/local/tmp
adb shell su 0 mount -o rw,remount /system
adb shell su 0 mkdir -p /system/priv-app/my-app/
adb shell su 0 chmod 755 /system/priv-app/my-app/
adb shell su 0 cp /data/local/tmp/app-debug.apk /system/priv-app/my-app
adb shell su 0 chmod 644 /system/priv-app/my-app/app-debug.apk
adb reboot