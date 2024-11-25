#!/bin/bash

# Input flags
gc_kernel=none
parallel=none

while getopts k:p: flag
do
	case "${flag}" in
		k) gc_kernel=${OPTARG};;
        p) parallel=${OPTARG};;
	esac
done

input_type=simmedium
path_firesim=/home/centos/firesim
path_parsec=/home/centos/parsec_gc
workload_name=parsec

path_firesim_sw=${path_firesim}/sw/FireMarshal
path_firesim_sw_workloads=${path_firesim_sw}/gc-${workload_name}-workloads/gc-${workload_name}/overlay/root
path_firesim_workloads=${path_firesim}/deploy/workloads/gc-parsec
path_parsec_pkgs=${path_parsec}/pkgs

BENCHMARKS=(bodytrack)

cd ${path_firesim_sw_workloads}
if [ -r "pkgs" ]; then
    cmd="rm -rf ${path_firesim_sw_workloads}/pkgs"
    echo "${cmd}"
    eval ${cmd}
fi

cd ${path_firesim_sw}/images
cmd="rm -rf gc-parsec*"
echo "${cmd}"
eval ${cmd}

cd ${path_firesim_workloads}
cmd="rm -rf ${path_firesim_workloads}/*"
echo "${cmd}"
eval ${cmd}

cd ${path_parsec_pkgs}
cmd="./build_parsec_r.sh -k ${gc_kernel}"
echo "${cmd}"
eval ${cmd}

cd ${path_firesim_sw_workloads}
cmd="rm -rf pkgs"
echo "${cmd}"
eval ${cmd}

cd ${path_parsec}
cmd="cp -rf pkgs ${path_firesim_sw_workloads}"
echo "${cmd}"
eval ${cmd}

cd ${path_firesim_sw_workloads}
cmd="./simplify_parsec.sh"
echo "${cmd}"
eval ${cmd}

cd ../../
cmd="cp run_parsec.sh ./overlay/root/pkgs"
echo "${cmd}"
eval ${cmd}

cd ${path_firesim_sw}
cmd="./marshal clean gc-${workload_name}-workloads/gc-${workload_name}.json"
echo "${cmd}"
eval ${cmd}

cmd="./marshal build gc-${workload_name}-workloads/gc-${workload_name}.json"
echo "${cmd}"
eval ${cmd}

# cmd="./marshal launch gc-${workload_name}-workloads/gc-${workload_name}.json"
# echo "${cmd}"
# eval ${cmd}

if [[ $parallel != none ]]; then
    cd images
    for benchmark in ${BENCHMARKS[@]}; do
        cd gc-${workload_name}-${benchmark}
        cmd="cp -rf gc-${workload_name}-${benchmark}-bin gc-${workload_name}-${benchmark}.img ${path_firesim_workloads}"
        echo "${cmd}"
        eval ${cmd}
        cd ..
    done
fi

if [[ $parallel == none ]]; then
cd images 
cd gc-${workload_name}-all
cmd="cp -rf gc-${workload_name}-all-bin gc-${workload_name}-all.img ${path_firesim_workloads}"
echo "${cmd}"
eval ${cmd}
fi

cmd="firesim launchrunfarm && firesim infrasetup && firesim runworkload"
echo "${cmd}"
eval ${cmd}

cmd="echo yes | firesim terminaterunfarm"
echo "${cmd}"
eval ${cmd}
