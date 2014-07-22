#!/bin/bash

PROJECT="ipojo-example"
if [ -z "$2" ]; then
  PROJECT="$1"
fi

echo "---------- Installing osgi-run-core  --------------"
cd osgi-run-core
./gradlew clean publishToMavenLocal
echo "----------   Running $PROJECT   --------------"
cd ../osgi-run-test
./gradlew $PROJECT:clean $PROJECT:runOsgi
cd ..
