# Makefile to generate Adept's verilog and test certain modules. The default
# verilog generation folder is 'verilog' in the root of the project. Test files
# are in the 'test_run_dir'.
MODULE?=core.Adept
PACKAGE?=adept
export SBT_OPTS=-Xss4M -Xmx2G
PROG?=
V_OUT=verilog

verilog: $(V_OUT)/$(MODULE).v

$(V_OUT)/$(MODULE).v:
	sbt 'runMain $(PACKAGE).$(MODULE) --target-dir $(V_OUT) --top-name $(MODULE)'

test-verilator:
	sbt 'test:runMain $(PACKAGE).$(MODULE)Main --backend-name verilator --program-file=$(PROG)'

test-basic:
	sbt 'test:runMain $(PACKAGE).$(MODULE)Main --program-file=$(PROG)'

repl:
	sbt 'test:runMain $(PACKAGE).$(MODULE)Repl'

test-all:
	sbt 'test:runMain $(PACKAGE).alu.ALUMain'
	sbt 'test:runMain $(PACKAGE).mem.MemoryMain'
	sbt 'test:runMain $(PACKAGE).pc.PcMain'
	sbt 'test:runMain $(PACKAGE).registerfile.RegisterFileMain'

test-all-verilator:
	sbt 'test:runMain $(PACKAGE).alu.ALUMain --backend-name verilator'
	sbt 'test:runMain $(PACKAGE).mem.MemoryMain --backend-name verilator'
	sbt 'test:runMain $(PACKAGE).pc.PcMain --backend-name verilator'
	sbt 'test:runMain $(PACKAGE).registerfile.RegisterFileMain --backend-name verilator'

.PHONY: clean clean-verilog verilog

clean:
	rm -rf $(V_OUT)
	rm -rf test_run_dir

clean-verilog:
	rm -rf $(V_OUT)/$(MODULE)*
