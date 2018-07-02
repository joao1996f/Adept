////////////////////////////////////////////////////////////////////////
//
// Memory Implementations for different FPGA Technologies and ASIC
//
// Author: Luis Fiolhais
//
////////////////////////////////////////////////////////////////////////
module SimBRAM #(
                 parameter MEM_SIZE = 1024,
                 parameter XILINX = 1,
                 parameter ALTERA = 0
                 )(
                   input        clock,

                   // Write Mask
                   input        io_mask_0,
                   input        io_mask_1,
                   input        io_mask_2,
                   input        io_mask_3,

                   // Port Enables
                   input        io_en_A,
                   input        io_en_B,

                   // Data address
                   input [31:0] io_data_addr,
                   input        io_data_we,

                   // Data Write Bus
                   input [7:0]  io_data_in_0,
                   input [7:0]  io_data_in_1,
                   input [7:0]  io_data_in_2,
                   input [7:0]  io_data_in_3,

                   // Data Read Bus
                   output [7:0] io_data_out_0,
                   output [7:0] io_data_out_1,
                   output [7:0] io_data_out_2,
                   output [7:0] io_data_out_3,

                   // Program Loading Bus
                   input [7:0]  io_load_data_in_0,
                   input [7:0]  io_load_data_in_1,
                   input [7:0]  io_load_data_in_2,
                   input [7:0]  io_load_data_in_3,

                   input        io_load_we,

                   // Instruction Read Bus
                   input [31:0] io_instr_addr,

                   output [7:0] io_instr_out_0,
                   output [7:0] io_instr_out_1,
                   output [7:0] io_instr_out_2,
                   output [7:0] io_instr_out_3
                   );

   // Memory Banks
   reg [7:0]                    mem_0 [0:MEM_SIZE-1];
   reg [7:0]                    mem_1 [0:MEM_SIZE-1];
   reg [7:0]                    mem_2 [0:MEM_SIZE-1];
   reg [7:0]                    mem_3 [0:MEM_SIZE-1];

   reg [7:0]                    data_out_0;
   reg [7:0]                    data_out_1;
   reg [7:0]                    data_out_2;
   reg [7:0]                    data_out_3;

   reg [7:0]                    instr_out_0;
   reg [7:0]                    instr_out_1;
   reg [7:0]                    instr_out_2;
   reg [7:0]                    instr_out_3;

   wire                         data_we_0;
   wire                         data_we_1;
   wire                         data_we_2;
   wire                         data_we_3;

   assign data_we_0 = io_mask_0 & io_data_we;
   assign data_we_1 = io_mask_1 & io_data_we;
   assign data_we_2 = io_mask_2 & io_data_we;
   assign data_we_3 = io_mask_3 & io_data_we;

   generate
      if (XILINX == 1) begin
          ////////////////////////////////////////////////////////////////////////////////
          // Memory 0
          ////////////////////////////////////////////////////////////////////////////////

          // Data Port 0
          always @ (posedge clock) begin
             if (io_en_A) begin
                if (data_we_0)
                   mem_0[io_data_addr] <= io_data_in_0;

                data_out_0 <= mem_0[io_data_addr];
             end
          end

          // Instruction Port 0
          always @ (posedge clock) begin
              if (io_en_B) begin
                if (io_load_we)
                    mem_0[io_instr_addr] <= io_load_data_in_0;

                instr_out_0 <= mem_0[io_instr_addr];
                end
          end

          ////////////////////////////////////////////////////////////////////////////////
          // Memory 1
          ////////////////////////////////////////////////////////////////////////////////

          // Data Port 1
          always @ (posedge clock) begin
              if (io_en_A) begin
                if (data_we_1)
                    mem_1[io_data_addr] <= io_data_in_1;

                data_out_1 <= mem_1[io_data_addr];
              end
          end

          // Instruction Port 1
          always @ (posedge clock) begin
              if (io_en_B) begin
                if (io_load_we)
                    mem_1[io_instr_addr] <= io_load_data_in_1;

                instr_out_1 <= mem_1[io_instr_addr];
              end
          end

          ////////////////////////////////////////////////////////////////////////////////
          // Memory 2
          ////////////////////////////////////////////////////////////////////////////////

          // Data Port 2
          always @ (posedge clock) begin
              if (io_en_A) begin
                if (data_we_2)
                  mem_2[io_data_addr] <= io_data_in_2;

                data_out_2 <= mem_2[io_data_addr];
              end
          end

          // Instruction Port 2
          always @ (posedge clock) begin
              if (io_en_B) begin
                if (io_load_we)
                    mem_2[io_instr_addr] <= io_load_data_in_2;

                instr_out_2 <= mem_2[io_instr_addr];
              end
          end

         ////////////////////////////////////////////////////////////////////////////////
         // Memory 3
         ////////////////////////////////////////////////////////////////////////////////

         // Data Port 3
         always @ (posedge clock) begin
             if (io_en_A) begin
               if (data_we_3)
                 mem_3[io_data_addr] <= io_data_in_3;

               data_out_3 <= mem_3[io_data_addr];
             end
         end

         // Instruction Port 3
         always @ (posedge clock) begin
             if (io_en_B) begin
               if (io_load_we)
                 mem_3[io_instr_addr] <= io_load_data_in_3;

               instr_out_3 <= mem_3[io_instr_addr];
             end
         end

      end // if (XILINX == 1)
   endgenerate

   assign io_data_out_0 = data_out_0;
   assign io_data_out_1 = data_out_1;
   assign io_data_out_2 = data_out_2;
   assign io_data_out_3 = data_out_3;

   assign io_instr_out_0 = instr_out_0;
   assign io_instr_out_1 = instr_out_1;
   assign io_instr_out_2 = instr_out_2;
   assign io_instr_out_3 = instr_out_3;

endmodule
