#!/usr/bin/env bash

SCRIPTS_DIR="`dirname \"$0\"`"
SCRIPTS_DIR="`( cd \"$SCRIPTS_DIR\" && pwd )`"

CWD=$(pwd)

cd $SCRIPTS_DIR/..

mvn clean install

cd $SCRIPTS_DIR

./docker-build.sh

cd $CWD
