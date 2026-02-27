#!/usr/bin/env bash

JAR_NAME="TERIE-Fascript-1.0.0.jar"
JAR_PATH="build/libs/$JAR_NAME"
OUTPUT_PATH="bin"

gradle clean build --stacktrace
if [ $? -ne 0 ]; then
  echo -e "Build Failed"
  exit 1
fi

if [ ! -f "$JAR_PATH" ]; then
  echo -e "Build Failed: artifact not found at $JAR_PATH"
  exit 1
fi

mkdir -p "$OUTPUT_PATH"
cp -f "$JAR_PATH" "$OUTPUT_PATH/$JAR_NAME"
echo -e "Build Success"
