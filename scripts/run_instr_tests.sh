#!/usr/bin/env bash

# Turn echo on
set -x

if [ -z ${RISCV+x} ]; then
    echo "The RISCV environment variable is not set. Please set it and rerun script."
    exit 1
fi

# Create log folder
mkdir -p logs

TEST_FILE_FOLDER=Adept-TestFiles/instructions
HEXS=$TEST_FILE_FOLDER/hex
RESULTS_FOLDER=$TEST_FILE_FOLDER/results/
TOP=$(pwd)

cd $TEST_FILE_FOLDER && make clean && make
cd $TOP

for test in $(ls $HEXS); do
    LOG_FOLDER=logs/"$test"_log
    mkdir -p $LOG_FOLDER

    # Run test in verilator
    make test-verilator PROG=$HEXS/$test > $LOG_FOLDER/verilator_$test_$(date +%d-%m-%Y)
    # Cut the log and take just the output that we want
    cat $LOG_FOLDER/verilator_$test_$(date +%d-%m-%Y) | grep 0x | sed 's/\[.*\] \[.*\] //g' > $LOG_FOLDER/verilator_result_$test_$(date +%d-%m-%Y)
    diff $LOG_FOLDER/verilator_result_$test_$(date +%d-%m-%Y) $RESULTS_FOLDER/${test%.hex}/verilator || exit 1

    # Run test in FIRRTL
    make test-basic PROG=$HEXS/$test > $LOG_FOLDER/firrtl_$test_$(date +%d-%m-%Y)
    # Cut the log and take just the output that we want
    cat $LOG_FOLDER/firrtl_$test_$(date +%d-%m-%Y) | grep 0x | sed 's/\[.*\] \[.*\] //g' | sed -n 'H; /^PC*/h; ${g;p;}' > $LOG_FOLDER/firrtl_result_$test_$(date +%d-%m-%Y)
    diff $LOG_FOLDER/firrtl_result_$test_$(date +%d-%m-%Y) $RESULTS_FOLDER/${test%.hex}/firrtl || exit 1
done
