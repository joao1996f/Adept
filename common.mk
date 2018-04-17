MODULE?=Adept
PACKAGE?=core
PROJECT=adept
export SBT_OPTS=-Xss4M -Xmx2G
PROG?=
V_OUT=verilog

$(V_OUT)/$(MODULE).v:
	sbt 'runMain $(PROJECT).$(PACKAGE).$(MODULE) --target-dir $(V_OUT) --top-name $(MODULE)'
