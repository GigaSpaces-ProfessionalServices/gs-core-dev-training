#!/usr/bin/env bash

SCRIPTS_DIR="`dirname \"$0\"`"
SCRIPTS_DIR="`( cd \"$SCRIPTS_DIR\" && pwd )`"

CWD=$(pwd)


cd $SCRIPTS_DIR/yaml

kubectl apply -f mydeployment.yaml

cd $CWD