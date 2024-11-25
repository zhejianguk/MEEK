MEEK: Re-thinking Heterogeneous Parallel Error Detection Architecture for Real-World OoO Superscalar Processors
==================================================

This repository contains artefacts and workflows (Parsec 3.0) to reproduce experiments from the DAC 2025 submission 1436

"MEEK: A Generalized Microarchitecture for Fine-Grained Security Analysis on OoO Superscalar Cores"

Platform pre-requisities
========================
* An x86-64 system (more cores will improve simulation time).
* Linux operating system (we used Ubuntu 20.04 and Ubuntu 22.04)
* An AMD U280 FPGA (for FPGA simulation)

Dependencies Installation
========================
Building simulation platform, RISC-V toolchain, Chisel toolchain, and other dependencies: 

```
git clone https://github.com/ucb-bar/chipyard.git
cd chipyard
git checkout 1.7.0
./scripts/init-submodules-no-riscv-tools.sh
./scripts/build-toolchains.sh riscv-tools # for a normal risc-v toolchain 
```

Hardware
========================
Setting environments:

```
export MEEK=$(dirname $(pwd))
export PLATFORM=$(pwd)
. ./env.sh
```

Updating the source code, ensuring to achieve the latest version:
```
$MEEK/Scripts/update_src.sh
```

Building hardware for MEEK:
```
cd $PLATFORM/sims/verilator
make config=RocketConfig
```

(**OUTPUT I**) After a few hours, a software simulator and the corresponding Verilog code will be generated:
```
./simulator-chipyard-RocketConfig # Software Simulator
./generated-src/chipyard.TestHarness.RocketConfig/chipyard.TestHarness.RocketConfig.top.v # Verilog code
```


Software
========================
To run Parsec, both Linux kernel and Parsec are required to be compiled:

For Linux, downloading the kernel code:
```
git clone https://github.com/firesim/linux
git checkout firesim-v57
```
(**OUTPUT II**) With that, compiling the kernel using following steps: [link](https://firemarshal.readthedocs.io/en/latest/index.html)

For Parsec, compiling checker thread first:
```
cd $MEEK/Software/checker_thread
make checker_thread
```
After the compilation, a checker_thread.o is generated. 

Now, compiling Parsec and link the checker_thread.o:
```
cd $MEEK/Software/parsec/pkgs
./build_parsec.sh
```

(**OUTPUT III** ) After a few minutes, the Parsec is compiled with the checker_thread.o:
```
./app # Parsec benchmark
./run_parsec.sh # Scripts to run all Parsec benchmark
```

FPGA Simulation
========================
With the above steps, hardware and software are generated.

Deploying the hardware on the U280 FPGA using standard Xilinx steps or FireSim: [link](https://docs.fires.im/en/latest/Getting-Started-Guides/On-Premises-FPGA-Getting-Started/Running-Simulations/Running-Single-Node-Simulation-Xilinx-Alveo-U280.html)

The Verilog is generated in (**OUTPUT I**), and the OS kernel and workload are in (**OUTPUT II & III**).

(The SPEC06 is reproduced in the same way.)

