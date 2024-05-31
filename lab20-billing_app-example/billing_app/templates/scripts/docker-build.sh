#!/usr/bin/env bash

SCRIPTS_DIR="`dirname \"$0\"`"
SCRIPTS_DIR="`( cd \"$SCRIPTS_DIR\" && pwd )`"

CWD=$(pwd)

docker login -u {{docker.username}}

# build the processor
echo "########################################"
echo "# Building the processor for Docker..."
echo "########################################"
cd $SCRIPTS_DIR/../processor && \
docker buildx build --platform linux/amd64 --push --no-cache -t {{docker.username}}/processor:{{app.docker.version}} .

#docker build --no-cache -t {{docker.username}}/processor:{{app.docker.version}} .
#docker push {{docker.username}}/processor:{{app.docker.version}}

# build the feeder
echo "########################################"
echo "# Building the payment-feeder for Docker..."
echo "########################################"
cd $SCRIPTS_DIR/../payment-feeder && \
docker buildx build --platform linux/amd64 --push --no-cache -t {{docker.username}}/payment-feeder:{{app.docker.version}} .

#docker build --no-cache -t {{docker.username}}/payment-feeder:{{app.docker.version}} .
#docker push {{docker.username}}/payment-feeder:{{app.docker.version}}

# build the rest-app
echo "########################################"
echo "# Building the rest-application for Docker..."
echo "########################################"
cd $SCRIPTS_DIR/../rest-application && \
docker buildx build --platform linux/amd64 --push --no-cache -t {{docker.username}}/rest-app:{{app.docker.version}} .

#docker build --no-cache -t {{docker.username}}/rest-app:{{app.docker.version}} .
#docker push {{docker.username}}/rest-app:{{app.docker.version}}

cd $CWD
