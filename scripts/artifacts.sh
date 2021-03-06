#!/usr/bin/env bash

# Echo On
set -x

# Group artifacts and send them to our servers
mkdir artifacts
mv logs/ verilog/ artifacts/
export ARTIFACT_NAME=Adept-Travis-$(date +%d-%m-%Y--%H-%M-%S)
tar -czf $ARTIFACT_NAME.tgz artifacts

stat $ARTIFACT_NAME.tgz

export SSHPASS=$DEPLOY_PASS1
sshpass -e scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -P $DEPLOY_PORT1 $ARTIFACT_NAME.tgz $DEPLOY_USER1@$DEPLOY_HOST:$DEPLOY_PATH1 &

export PID=$!
while [[ `ps -p $PID | tail -n +2` ]]; do
    echo 'Sending Artifacts to Server 1'
    sleep 540
done

export SSHPASS=$DEPLOY_PASS2
sshpass -e scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -P $DEPLOY_PORT2 $ARTIFACT_NAME.tgz $DEPLOY_USER2@$DEPLOY_HOST:$DEPLOY_PATH2 &

export PID=$!
while [[ `ps -p $PID | tail -n +2` ]]; do
    echo 'Sending Artifacts to Server 2'
    sleep 540
done
