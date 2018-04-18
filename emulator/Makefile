default: all

# Set env variables. Verilator must be installed
include ../common.mk

TOP=V$(MODULE)
BASE_DIR=$(abspath ..)
V_FLAGS=--assert -Wno-fatal -Wno-WIDTH -Wno-STMTDLY --trace -O1 --top-module $(MODULE) \
	+define+TOP_TYPE=$(TOP) +define+PRINTF_COND=!$(MODULE).reset \
	+define+STOP_COND=!$(MODULE).reset
C_FLAGS=-CFLAGS "-Wno-undefined-bool-conversion -O1 -DTOP_TYPE=$(TOP) -DVL_USER_FINISH -include $(TOP).h"
OUT_DIR=dump
CHISEL_SRCS=$(addprefix $(BASE_DIR),$(CHISEL_SRCS))

.PHONY: gen-harness directories verilog

all: gen-harness

directories:
	mkdir -p $(OUT_DIR)

verilog:
	cd $(BASE_DIR) && $(MAKE) verilog

gen-harness: directories verilog
	verilator --cc $(BASE_DIR)/verilog/$(PROJECT).$(PACKAGE).$(MODULE).v $(V_FLAGS) $(C_FLAGS) -Mdir $(OUT_DIR) --exe emulator.cpp
	$(MAKE) -C $(OUT_DIR) -f $(TOP).mk
	mv dump/$(TOP) .

clean:
	rm -rf dump $(TOP) $(MODULE).vcd

