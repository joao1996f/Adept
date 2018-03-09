# Makefile to generate Adept's verilog and test certain modules. The default
# verilog generation folder is 'verilog' in the root of the project. Test files
# are in the 'test_run_dir'.
MODULE?=core.Adept
PACKAGE?=adept
export SBT_OPTS=-Xss4M -Xmx2G

verilog:
	sbt 'runMain $(PACKAGE).$(MODULE) --target-dir verilog --top-name $(MODULE)'

test-verilator:
	sbt 'testOnly $(PACKAGE).$(MODULE)Tester -- -z verilator'

test-basic:
	sbt 'testOnly $(PACKAGE).$(MODULE)Tester -- -z Basic'

repl:
	sbt 'test:runMain $(PACKAGE).$(MODULE)Repl'

.PHONY: clean clean-verilog

clean:
	rm -rf verilog
	rm -rf test_run_dir

clean-verilog:
	rm -rf verilog/$(PACKAGE).$(MODULE)*
