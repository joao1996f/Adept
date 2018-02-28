# Makefile to generate Adept's verilog and test certain modules. The default
# verilog generation folder is 'verilog' in the root of the project. Test files
# are in the 'test_run_dir'.
MODULE?=core.Adept

verilog:
	sbt 'runMain adept.$(MODULE) --target-dir verilog --top-name $(MODULE)'

test-verilator:
	sbt 'testOnly adept.$(MODULE) -- -z verilator'

test-basic:
	sbt 'testOnly adept.$(MODULE) -- -z Basic'
