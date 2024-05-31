#!/usr/bin/env bash

SCRIPTS_DIR="`dirname \"$0\"`"
SCRIPTS_DIR="`( cd \"$SCRIPTS_DIR\" && pwd )`"

source $SCRIPTS_DIR/settings.sh

CWD=$(pwd)

cd $SCRIPTS_DIR/..

$GS_HOME/bin/gs.sh service deploy processor-pu processor/target/*.jar

cd $CWD

