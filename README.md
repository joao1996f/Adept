# The Adept RISC-V Processor

## How to generate the Verilog
Run `make verilog` to generate the verilog for the entire core. If you want the
verilog for a specific module run the previous command with the environment
variable `MODULE` with the package name of the specific module. For example, to
generate verilog just for the ALU run `MODULE=alu.ALU make verilog`. The
generated verilog will be in a verilog folder in the root of the project.

## How to test each component
There are two testing backends the FIRRTL interpreter and Verilator. To test the
core using FIRRTL run `make test-basic`, and to test in Verilator run `make
test-verilator`. Similarly to the verilog generation you can specify which
module to test with the `MODULE` environment variable. For example, to test the
ALU run `MODULE=alu.ALU make test-verilator`. The generated waveforms will be in
the `test-run-dir` folder in the root of the project.

## License
The hardware description provided here in can be licensed under the Apache 2.0
license. You can read about it [here](LICENSE).
