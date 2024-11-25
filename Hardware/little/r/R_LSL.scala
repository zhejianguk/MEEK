package freechips.rocketchip.r

import chisel3._
import chisel3.util._
import chisel3.experimental.{BaseModule}
import freechips.rocketchip.guardiancouncil._

case class R_LSLParams(
  nEntries: Int,
  xLen: Int
)

class R_LSLIO(params: R_LSLParams) extends Bundle {
  val m_ld_valid = Input(UInt(1.W))
  val m_st_valid = Input(UInt(1.W))
  val m_ldst_data = Input(UInt((params.xLen).W))
  val m_ldst_addr = Input(UInt((params.xLen).W))
  val m_csr_valid = Input(UInt(1.W))
  val m_csr_data = Input(UInt((params.xLen).W))

  val m_ld_valid1 = Input(UInt(1.W))
  val m_st_valid1 = Input(UInt(1.W))
  val m_ldst_data1 = Input(UInt((params.xLen).W))
  val m_ldst_addr1 = Input(UInt((params.xLen).W))
  val m_csr_valid1 = Input(UInt(1.W))
  val m_csr_data1 = Input(UInt((params.xLen).W))

  val cdc_ready = Output(UInt(1.W))


  val req_ready = Output(UInt(1.W))
  val req_valid = Input(UInt(1.W))
  val req_addr = Input(UInt(40.W)) // Later used for comparision
  val req_tag = Input(UInt(8.W))
  val req_cmd = Input(UInt(2.W)) // 01: load; 10: store; 11: load & store
  val req_data = Input(UInt(params.xLen.W)) // Later used for comparision
  val req_size = Input(UInt(2.W))
  val req_kill = Input(UInt(1.W))
  val req_valid_csr = Input(UInt(1.W))
  val req_ready_csr = Output(UInt(1.W))

  val resp_valid = Output(UInt(1.W))
  val resp_tag = Output(UInt(8.W))
  val resp_size = Output(UInt(2.W))
  val resp_data = Output(UInt(params.xLen.W))
  val resp_has_data = Output(UInt(1.W))
  val resp_addr = Output(UInt(40.W))
  val resp_replay = Output(UInt(1.W))
  val near_full = Output(UInt(1.W))
  val resp_data_csr = Output(UInt(params.xLen.W))
  val if_empty = Output(UInt(1.W))
  val lsl_highwatermark = Output(UInt(1.W))
  // val resp_replay_csr = Output(UInt(1.W))
  val st_deq = Output(UInt(1.W))
  val ld_deq = Output(UInt(1.W))
}

trait HasR_RLSLIO extends BaseModule {
  val params: R_LSLParams
  val io = IO(new R_LSLIO(params))
}

class R_LSL(val params: R_LSLParams) extends Module with HasR_RLSLIO {
  val fifowidth               = 2*params.xLen + 2 // extra two bits added to indicate inst type
  val u_channel               = Seq.fill(2) {Module(new GH_MemFIFO(FIFOParams (fifowidth, params.nEntries)))}


  val has_data                = RegInit(false.B)
  val scala_ptr               = RegInit(0.U(1.W))
  val scala_num_reqs          = RegInit(0.U(2.W))
  /* Enqueue */
  val enq_data                = RegInit(0.U(fifowidth.W))
  val channel_enq_valid       = WireInit(false.B)
  val channel_enq_data        = WireInit(0.U(fifowidth.W))
  val channel_deq_ready       = WireInit(false.B)
  val channel_deq_data        = WireInit(0.U(fifowidth.W))
  val channel_empty           = WireInit(true.B)

  val enq_data1               = RegInit(0.U(fifowidth.W))
  val channel_enq_valid1      = WireInit(false.B)
  val channel_enq_data1       = WireInit(0.U(fifowidth.W))
  val channel_deq_ready1      = WireInit(false.B)
  val channel_deq_data1       = WireInit(0.U(fifowidth.W))
  val channel_empty1          = WireInit(true.B)

  has_data                   := io.m_st_valid.asBool || io.m_ld_valid.asBool || io.m_st_valid1.asBool || io.m_ld_valid1.asBool
  scala_ptr                  := Mux(has_data && (scala_num_reqs === 1.U), scala_ptr + 1.U, scala_ptr)
  scala_num_reqs             := Mux(io.m_st_valid.asBool || io.m_ld_valid.asBool || io.m_st_valid1.asBool || io.m_ld_valid1.asBool, 
                                Mux(((io.m_st_valid.asBool || io.m_ld_valid.asBool)) && (io.m_st_valid1.asBool || io.m_ld_valid1.asBool), 2.U, 1.U), 0.U)
  
  enq_data                   := Cat(io.m_st_valid, io.m_ld_valid, io.m_ldst_data, io.m_ldst_addr)
  enq_data1                  := Cat(io.m_st_valid1, io.m_ld_valid1, io.m_ldst_data1, io.m_ldst_addr1)

  u_channel(0).io.enq_valid  := channel_enq_valid
  u_channel(0).io.enq_bits   := channel_enq_data
  u_channel(0).io.deq_ready  := channel_deq_ready
  channel_deq_data           := u_channel(0).io.deq_bits
  channel_empty              := u_channel(0).io.empty

  u_channel(1).io.enq_valid  := channel_enq_valid1
  u_channel(1).io.enq_bits   := channel_enq_data1
  u_channel(1).io.deq_ready  := channel_deq_ready1
  channel_deq_data1          := u_channel(1).io.deq_bits
  channel_empty1             := u_channel(1).io.empty

  channel_enq_valid          := Mux(has_data, Mux(scala_num_reqs === 2.U, 1.U, Mux(scala_ptr === 0.U, 1.U, 0.U)), 0.U)
  channel_enq_valid1         := Mux(has_data, Mux(scala_num_reqs === 2.U, 1.U, Mux(scala_ptr === 1.U, 1.U, 0.U)), 0.U)
  channel_enq_data           := Mux(has_data, Mux(scala_num_reqs === 2.U, Mux(scala_ptr === 0.U, enq_data, enq_data1), Mux(scala_ptr === 0.U, enq_data, 0.U)), 0.U)
  channel_enq_data1          := Mux(has_data, Mux(scala_num_reqs === 2.U, Mux(scala_ptr === 0.U, enq_data1, enq_data), Mux(scala_ptr === 0.U, 0.U, enq_data)), 0.U)


  val scala_ptr_core          = RegInit(0.U(1.W))
  /* Dequeue */
  val req_valid_reg           = RegInit(0.U(1.W))
  val resp_valid_reg          = RegInit(0.U(1.W))
  val resp_kill_reg           = RegInit(0.U(1.W))
  val resp_tag                = RegInit(0.U(8.W))
  val cmd                     = RegInit(0.U(2.W))
  val req_size_reg            = RegInit(0.U(2.W))
  resp_kill_reg              := io.req_kill // already in the replay procedure....

  val if_lsl_empty            = Mux(scala_ptr_core === 0.U, channel_empty, channel_empty1)
  scala_ptr_core             := Mux((io.req_valid & !if_lsl_empty & !io.req_kill).asBool, scala_ptr_core + 1.U, scala_ptr_core)
  req_valid_reg              := io.req_valid
  resp_valid_reg             := io.req_valid & !if_lsl_empty & !io.req_kill
  resp_tag                   := io.req_tag
  cmd                        := io.req_cmd
  req_size_reg               := io.req_size

  io.req_ready               := !if_lsl_empty
  io.resp_valid              := resp_valid_reg
  io.resp_tag                := resp_tag
  // Revisit
  io.resp_size               := Mux((resp_valid_reg === 1.U), req_size_reg, 0.U)
  io.resp_data               := Mux((resp_valid_reg === 1.U), Mux(scala_ptr_core === 0.U, channel_deq_data1(127, 64), channel_deq_data(127, 64)), 0.U)
  io.resp_addr               := Mux((resp_valid_reg === 1.U), Mux(scala_ptr_core === 0.U, channel_deq_data1(63, 0), channel_deq_data(63, 0)), 0.U)
  io.resp_has_data           := Mux((resp_valid_reg === 1.U) && (cmd(0) === 1.U), 1.U, 0.U)
  io.resp_replay             := req_valid_reg & !resp_valid_reg & !resp_kill_reg

  channel_deq_ready          := io.req_valid & !if_lsl_empty & !io.req_kill & (scala_ptr_core === 0.U)
  channel_deq_ready1         := io.req_valid & !if_lsl_empty & !io.req_kill & (scala_ptr_core === 1.U)

  io.ld_deq                  := Mux(channel_deq_ready || channel_deq_ready1, Mux(io.req_cmd === 0x01.U, 1.U, 0.U), 0.U)
  io.st_deq                  := Mux(channel_deq_ready || channel_deq_ready1, Mux(io.req_cmd === 0x02.U, 1.U, 0.U), 0.U)


  val has_data_csr            = RegInit(false.B)
  val scala_ptr_csr           = RegInit(0.U(1.W))
  val scala_num_reqs_csr      = RegInit(0.U(2.W))

  val u_channel_csr           = Seq.fill(2) {Module(new GH_FIFO(FIFOParams (params.xLen, 11)))}
  
  val csr_enq_data            = RegInit(0.U(fifowidth.W))
  val csr_channel_enq_valid   = WireInit(false.B)
  val csr_channel_enq_data    = WireInit(0.U(params.xLen.W))
  val csr_channel_deq_ready   = WireInit(false.B)
  val csr_channel_deq_data    = WireInit(0.U(params.xLen.W))
  val csr_channel_empty       = WireInit(true.B)
  val csr_channel_nearfull    = WireInit(0.U(1.W))

  val csr_enq_data1           = RegInit(0.U(fifowidth.W))
  val csr_channel_enq_valid1  = WireInit(false.B)
  val csr_channel_enq_data1   = WireInit(0.U(params.xLen.W))
  val csr_channel_deq_ready1  = WireInit(false.B)
  val csr_channel_deq_data1   = WireInit(0.U(params.xLen.W))
  val csr_channel_empty1      = WireInit(true.B)
  val csr_channel_nearfull1   = WireInit(0.U(1.W))


  has_data_csr               := io.m_csr_valid | io.m_csr_valid1
  scala_ptr_csr              := Mux(has_data_csr && (scala_num_reqs_csr === 1.U), scala_ptr_csr + 1.U, scala_ptr_csr)
  scala_num_reqs_csr         := Mux(io.m_csr_valid.asBool || io.m_csr_valid1.asBool, 
                                Mux((io.m_csr_valid.asBool && io.m_csr_valid1.asBool), 2.U, 1.U), 0.U)

  csr_enq_data               := Mux(io.m_csr_valid.asBool, io.m_csr_data, 0.U)
  csr_enq_data1              := Mux(io.m_csr_valid1.asBool, io.m_csr_data1, 0.U)

  u_channel_csr(0).io.enq_valid := csr_channel_enq_valid
  u_channel_csr(0).io.enq_bits  := csr_channel_enq_data
  u_channel_csr(0).io.deq_ready := csr_channel_deq_ready
  csr_channel_deq_data       := u_channel_csr(0).io.deq_bits
  csr_channel_empty          := u_channel_csr(0).io.empty
  csr_channel_nearfull       := u_channel_csr(0).io.status_threeslots

  u_channel_csr(1).io.enq_valid := csr_channel_enq_valid1
  u_channel_csr(1).io.enq_bits  := csr_channel_enq_data1
  u_channel_csr(1).io.deq_ready := csr_channel_deq_ready1
  csr_channel_deq_data1      := u_channel_csr(1).io.deq_bits
  csr_channel_empty1         := u_channel_csr(1).io.empty
  csr_channel_nearfull1      := u_channel_csr(1).io.status_threeslots

  csr_channel_enq_valid      := Mux(has_data_csr, Mux(scala_num_reqs_csr === 2.U, 1.U, Mux(scala_ptr_csr === 0.U, 1.U, 0.U)), 0.U)
  csr_channel_enq_valid1     := Mux(has_data_csr, Mux(scala_num_reqs_csr === 2.U, 1.U, Mux(scala_ptr_csr === 1.U, 1.U, 0.U)), 0.U)

  csr_channel_enq_data       := Mux(has_data_csr, Mux(scala_num_reqs_csr === 2.U, Mux(scala_ptr_csr === 0.U, csr_enq_data, csr_enq_data1), Mux(scala_ptr_csr === 0.U, csr_enq_data, 0.U)), 0.U)
  csr_channel_enq_data1      := Mux(has_data_csr, Mux(scala_num_reqs_csr === 2.U, Mux(scala_ptr_csr === 0.U, csr_enq_data1, csr_enq_data), Mux(scala_ptr_csr === 0.U, 0.U, csr_enq_data)), 0.U)

  val scala_ptr_core_csr      = RegInit(0.U(1.W))
  val if_csr_empty            = Mux(scala_ptr_core_csr === 0.U, csr_channel_empty, csr_channel_empty1)

  csr_channel_deq_ready      := (io.req_valid_csr === 1.U) && !if_csr_empty && (scala_ptr_core_csr === 0.U)
  csr_channel_deq_ready1     := (io.req_valid_csr === 1.U) && !if_csr_empty && (scala_ptr_core_csr === 1.U)

  io.resp_data_csr           := Mux((scala_ptr_core_csr === 0.U), csr_channel_deq_data, csr_channel_deq_data1)
  // io.resp_replay_csr         := (io.req_valid_csr === 1.U) && csr_channel_empty
  scala_ptr_core_csr         := Mux((io.req_valid_csr === 1.U) && !if_csr_empty, scala_ptr_core_csr + 1.U, scala_ptr_core_csr)


  io.cdc_ready               := has_data || has_data_csr
  io.near_full               := u_channel(0).io.status_fiveslots | u_channel(1).io.status_fiveslots | csr_channel_nearfull | csr_channel_nearfull1
  io.req_ready_csr           := !if_csr_empty
  io.if_empty                := csr_channel_empty & csr_channel_empty1 & channel_empty & channel_empty1
  io.lsl_highwatermark       := u_channel(0).io.high_watermark | u_channel(1).io.high_watermark | u_channel_csr(0).io.status_fiveslots | u_channel_csr(1).io.status_fiveslots
}