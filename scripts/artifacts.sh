#!/usr/bin/env bash

# Group artifacts and send them to our servers
mkdir artifacts
mv logs/ test_run_dir/ verilog/ artifacts/
export ARTIFACT_NAME=$(date +%d-%m-%Y--%H-%M-%S)
tar -czf $ARTIFACT_NAME.tgz artifacts
export SSHPASS=$DEPLOY_PASS1
sshpass -e scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -P $DEPLOY_PORT1 $ARTIFACT_NAME.tgz $DEPLOY_USER1@$DEPLOY_HOST:$DEPLOY_PATH1
export SSHPASS=$DEPLOY_PASS2
sshpass -e scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -P $DEPLOY_PORT2 $ARTIFACT_NAME.tgz $DEPLOY_USER2@$DEPLOY_HOST:$DEPLOY_PATH2
