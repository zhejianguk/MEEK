#ifndef GC_TOP_H
#define GC_TOP_H

#define NUM_CORES 5
#define GC_DEBUG 0
#define FILTERWIDTH 0x00

#define BOOM_ID 0
#define CHK1_ID 1
#define CHK2_ID 2
#define CHK3_ID 3
#define CHK4_ID 4
#define CHK5_ID 5
#define CHK6_ID 6
#define CHK7_ID 7

#define AGG_CORE_ID NUM_CORES

#define read_csr_safe(reg) ({ register long __tmp asm("a0"); \
        asm volatile ("csrr %0, " #reg : "=r"(__tmp)); \
        __tmp; })


static inline  int gc_pthread_setaffinity(uint64_t phart_id){
	pthread_t thread_id = pthread_self();
	cpu_set_t cpu_id;
	int s;

	CPU_ZERO(&cpu_id);
	CPU_SET(phart_id, &cpu_id); 
	s = pthread_setaffinity_np(thread_id, sizeof(cpu_id), &cpu_id);
	
	return s;
} 


#endif