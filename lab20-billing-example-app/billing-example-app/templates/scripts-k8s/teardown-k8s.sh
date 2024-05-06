#!/usr/bin/env bash

SCRIPTS_DIR="`dirname \"$0\"`"
SCRIPTS_DIR="`( cd \"$SCRIPTS_DIR\" && pwd )`"

CWD=$(pwd)

cd $SCRIPTS_DIR/yaml

kubectl delete -f mydeployment.yaml
kubectl delete -f myjob.yaml
kubectl delete -f np.yaml


helm delete manager
helm delete operator
helm delete processor


cd $CWD