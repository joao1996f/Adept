#!/usr/bin/env bash

if [ -z "${RISCV+x}" ]; then
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
cd "$TOP"

for test in $HEXS/*; do
    TEST_NAME=${test##*/}
    LOG_FOLDER=logs/"$TEST_NAME"_log
    mkdir -p "$LOG_FOLDER"

    echo "Testing $TEST_NAME instruction"

    OUTPUT_TEST_FILE="$LOG_FOLDER/verilator_$test_$(date +%d-%m-%Y)"
    OUTPUT_RESULT_FILE="$LOG_FOLDER/verilator_result_$TEST_NAME_$(date +%d-%m-%Y)"
    # Run test in verilator
    make test-verilator PROG="$test" > "$OUTPUT_TEST_FILE"
    # Cut the log and take just the output that we want
    tac "$OUTPUT_TEST_FILE" | grep "PC = " -m 1 -B32 | sed 's/\[.*\] \[.*\] //g' | tac > "$OUTPUT_RESULT_FILE"
    diff -w "$OUTPUT_RESULT_FILE" "$RESULTS_FOLDER/${TEST_NAME%.hex}/verilator"
done
