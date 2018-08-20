#!/bin/bash

ndkpath=/home/vlad805/Android/Sdk/ndk-bundle/build/
abi=armeabi-v7a
nowdate=$(date +'%Y-%m-%d')
apkname="Spirit3-$nowdate"

export KEYSTORE_FILE="/home/vlad805/git/androidkeystore.jks"
export KEYSTORE_PASS="myNadusik"
export KEY_NAME="alias"
export PROJ="sf"

android update project --target android-21 --path . --name $PROJ
echo $ndkpath/ndk-build start
time $ndkpath/ndk-build #2>&1 | grep "error:"

mv libs/$abi/s2d libs/$abi/libs2d.so
mv libs/$abi/ssd libs/$abi/libssd.so

ant -q clean release

jarsigner -storepass $KEYSTORE_PASS -sigalg MD5withRSA -digestalg SHA1 -keystore $KEYSTORE_FILE -signedjar bin/$apkname-unaligned.apk bin/$apkname-unsigned.apk $KEY_NAME

zipalign -f 4 bin/$apkname-unaligned.apk bin/$apkname.apk

#adb install -r bin/$PROJ-release.apk
#adb shell am start -n fm.a2d.sf/fm.a2d.sf.MainActivity