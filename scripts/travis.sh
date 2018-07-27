#!/usr/bin/env bash
sudo apt -qq update
sudo apt install -y sshpass

# Get RISCV Toolchain
mkdir riscv-tools
export SSHPASS=$DEPLOY_PASS2
sshpass -e scp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -P $DEPLOY_PORT2 $DEPLOY_USER2@$DEPLOY_HOST:~/toolchain.tar.gz riscv-tools/

cd riscv-tools/
tar -xvzf toolchain.tar.gz
cd ..

# Install dependencies
./scripts/install-deps.sh
