# Makefile to generate Adept's verilog and test certain modules. The default
# verilog generation folder is 'verilog' in the root of the project. Test files
# are in the 'test_run_dir'.
include common.mk

verilog: $(V_OUT)/$(MODULE).v

test-verilator:
	sbt 'test:runMain $(PROJECT).$(PACKAGE).$(MODULE)Main --backend-name verilator --program-file=$(PROG)'

test-basic:
	sbt 'test:runMain $(PROJECT).$(PACKAGE).$(MODULE)Main --program-file=$(PROG)'

repl:
	sbt 'test:runMain $(PROJECT).$(PACKAGE).$(MODULE)Repl'

test-all:
	sbt 'test:runMain $(PROJECT).alu.ALUMain'
	sbt 'test:runMain $(PROJECT).mem.MemoryMain'
	sbt 'test:runMain $(PROJECT).pc.PcMain'
	sbt 'test:runMain $(PROJECT).registerfile.RegisterFileMain'

test-all-verilator:
	sbt 'test:runMain $(PROJECT).alu.ALUMain --backend-name verilator'
	sbt 'test:runMain $(PROJECT).mem.MemoryMain --backend-name verilator'
	sbt 'test:runMain $(PROJECT).pc.PcMain --backend-name verilator'
	sbt 'test:runMain $(PROJECT).registerfile.RegisterFileMain --backend-name verilator'

.PHONY: clean clean-verilog verilog

clean:
	rm -rf $(V_OUT)
	rm -rf test_run_dir

clean-verilog:
	rm -rf $(V_OUT)/$(MODULE)*
