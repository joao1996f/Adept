MODULE?=Adept
PACKAGE?=core
PROJECT=adept
export SBT_OPTS=-Xss4M -Xmx2G
V_OUT=verilog
CHISEL_SRCS=$(wildcard src/main/scala/**/*.scala)

$(V_OUT)/$(MODULE).v: $(CHISEL_SRCS)
	sbt 'runMain $(PROJECT).$(PACKAGE).$(MODULE) --target-dir $(V_OUT) --top-name $(PROJECT).$(PACKAGE).$(MODULE)'
