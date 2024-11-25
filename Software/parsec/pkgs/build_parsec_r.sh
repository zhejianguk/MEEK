#!/bin/bash

gc_kernel=none

rm -f *.o
rm -f *.riscv

# Input flags
while getopts k: flag
do
	case "${flag}" in
		k) gc_kernel=${OPTARG};;
	esac
done

input_type=simmedium

export PATH_GC_KERNELS="/home/centos/r-kernel_firesim/"
export PATH_PKGS=$PWD

cd $PATH_GC_KERNELS
make clean
make main_${gc_kernel}

make initialisation_${gc_kernel}
cp initialisation_${gc_kernel}.riscv $PATH_PKGS


# BENCHMARKS=(blackscholes bodytrack swaptions x264)
BENCHMARKS=(blackscholes bodytrack dedup ferret fluidanimate streamcluster freqmine swaptions x264)

cmd="parsecmgmt -a clean -p all"
eval ${cmd}
cmd="parsecmgmt -a fulluninstall -p all"
eval ${cmd}


for benchmark in ${BENCHMARKS[@]}; do

    cmd="parsecmgmt -a build -p ${benchmark} -c gcc-serial"
    eval ${cmd}
    
    cmd="parsecmgmt -a run -p ${benchmark} -i ${input_type} -c gcc-serial"
    eval ${cmd}

done

echo ""
echo "All Done!"