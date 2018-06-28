# Makefile to generate Adept's verilog and test certain modules. The default
# verilog generation folder is 'verilog' in the root of the project. Test files
# are in the 'test_run_dir'.
MODULE?=core.Adept
PACKAGE?=adept
export SBT_OPTS=-Xss4M -Xmx2G
PROG?=

verilog:
	sbt 'runMain $(PACKAGE).$(MODULE) --target-dir verilog --top-name $(MODULE)'

test-verilator:
	sbt 'test:runMain $(PACKAGE).$(MODULE)Main --backend-name verilator --program-file=$(PROG)'

test-basic:
	sbt 'test:runMain $(PACKAGE).$(MODULE)Main --program-file=$(PROG)'

repl:
	sbt 'test:runMain $(PACKAGE).$(MODULE)Repl'

test-all:
	sbt testOnly
	./scripts/run_instr_tests.sh

.PHONY: clean clean-verilog verilog

clean:
	rm -rf verilog
	rm -rf test_run_dir
	rm -rf logs

clean-verilog:
	rm -rf verilog/$(MODULE)*
