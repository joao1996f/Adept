# The Adept RISC-V Processor

The Adept processor is a low power and size optimized RISC-V CPU, which only
implements the RV32I instruction set and only supports bare metal applications.
Adept targets both ASIC and FPGA, and has been implemented in a USMC 130nm
technology.

## Quick Start Guide

1. Make sure you have the RISC-V tool chain installed. If not follow the instructions in [here](https://github.com/riscv/riscv-tools) to install the 32-bit Newlib toolchain.

2. Make sure you have the Chisel HDL installed. If not follow the instructions in [here](https://github.com/freechipsproject/chisel3/wiki/Installation%20Preparation).

3. In the project root run

```git submodule update --init```

4. To create an Adept Verilog model, from the project root run

```make verilog```

5. Import the Verilog model into your favorite FPGA/ASIC tools


## Generate Verilog for a Module

If you want the
verilog for a specific module run the previous command with the environment
variable `MODULE` with the package name of the specific module. For example, to
generate verilog just for the ALU run 

```MODULE=alu.ALU make verilog```

The generated verilog will be in a verilog folder in the root of the project.


## Testing Framework

There are two testing backends the FIRRTL interpreter and Verilator. To test the
core using FIRRTL run 

```make test-basic PROG=path/to/your/program.hex``` 

and to test in Verilator run 

```make test-verilator PROG=Adept-TestFiles/path/to/your/program.hex```


Similarly to the verilog generation you can specify which
module to test with the `MODULE` environment variable. For example, to test the
ALU run 

```MODULE=alu.ALU make test-verilator```

The generated waveforms will be in the `test-run-dir` folder in the root of the project.

There is also a target which tests all components within Adept separately: 

```make test-all```


## License

The hardware description provided herein can be licensed under the Apache 2.0
license. You can read about it [here](LICENSE).
