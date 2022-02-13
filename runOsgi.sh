#!/bin/bash

set -e

PROJECT=""
if [ -z "$1" ]; then
  PROJECT="simplest-build"
else
  PROJECT="$1"
fi

echo "---------- Installing osgi-run-core  --------------"
cd osgi-run-core
./gradlew clean publishToMavenLocal
echo "----------   Running $PROJECT   --------------"
cd ../osgi-run-test
./gradlew :$PROJECT:clean :$PROJECT:runOsgi
cd ..
