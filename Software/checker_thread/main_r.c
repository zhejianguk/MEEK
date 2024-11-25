#ifndef _GNU_SOURCE
	#define _GNU_SOURCE             /* See feature_test_macros(7) */
#endif
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>
#include <sys/syscall.h>
#define gettid() syscall(SYS_gettid)
#include "libraries/ght.h"
#include "libraries/ghe.h"
#include "libraries/gc_top.h"
#include <time.h>
#include "libraries/spin_lock.h"



void rStartup (void) __attribute__ ((constructor));
void rCleanup (void) __attribute__ ((destructor));

pthread_t threads[NUM_CORES-1];
struct timespec start, end;
int uart_lock;

void* thread_r_checker(void* args){
    uint64_t hart_id = (uint64_t) args;
    //================== Initialisation ==================//
	if (gc_pthread_setaffinity(hart_id) != 0){
		printf ("[Rocket-C%x]: pthread_setaffinity failed.", hart_id);
	}
	
	ghe_asR();
	ght_set_satp_priv();

	while (ghe_checkght_status() != 0x04) {
	}

	ghe_go();
	ghe_initailised(1);

	// ROCC_INSTRUCTION_S (1, 0X01, 0x69);
	//======================= Perf ========================//
	ghe_perf_ctrl(0x01);
  	ghe_perf_ctrl(0x00);

	//===================== Execution =====================//
	ROCC_INSTRUCTION (1, 0x75); // Record context
	ROCC_INSTRUCTION (1, 0x73); // Store context from main core
	ROCC_INSTRUCTION (1, 0x64); // Record PC

	/*
	for (int sel_elu = 0; sel_elu < 2; sel_elu ++) {
    	ROCC_INSTRUCTION_S (1, sel_elu, 0x65);

		while (elu_checkstatus() != 0){
			// printf("C%x: Error detected for ELU %x.\r\n", hart_id, sel_elu);
			ROCC_INSTRUCTION_S (1, sel_elu, 0x63);
		}
  	}
	*/

	while (ghe_checkght_status() != 0x02) {
		if ((ghe_rsur_status() & 0x08) == 0x08) {
			ROCC_INSTRUCTION (1, 0x60);
			R_INSTRUCTION_JLR (3, 0x00);
		}
  	}
	//=================== Post execution ===================//
	__asm__ volatile("nop");
	__asm__ volatile("nop");
	__asm__ volatile("nop");
	__asm__ volatile("nop");

	ROCC_INSTRUCTION (1, 0x72); // Store context from checker core
	ROCC_INSTRUCTION (1, 0x60);

	__asm__ volatile("nop");
	__asm__ volatile("nop");
	__asm__ volatile("nop");
	__asm__ volatile("nop");

	ghe_perf_ctrl(0x07<<1);
	uint64_t perf_val_NCP = ghe_perf_read();
	ghe_perf_ctrl(0x01<<1);
	uint64_t perf_val_NC = ghe_perf_read();
	ghe_perf_ctrl(0x02<<1);
	uint64_t perf_val_NPC = ghe_perf_read();
	ghe_perf_ctrl(0x03<<1);
	uint64_t perf_val_NOT = ghe_perf_read();
	ghe_perf_ctrl(0x04<<1);
	uint64_t perf_val_NNC = ghe_perf_read();
	ghe_perf_ctrl(0x05<<1);
	uint64_t Nonchecking_OtherThreads = ghe_perf_read();
	ghe_perf_ctrl(0x06<<1);
	uint64_t Nonchecking_MOtherThreads = ghe_perf_read();
	ghe_perf_ctrl(0x08<<1);
	uint64_t Nonchecking_MCheck = ghe_perf_read();
	ghe_perf_ctrl(0x09<<1);
	uint64_t Insts = ghe_perf_read();
	ghe_perf_ctrl(0x0a<<1);
	uint64_t CPSTrans = ghe_perf_read();
	ghe_perf_ctrl(0x0b<<1);
	uint64_t Nonchecking_Sched = ghe_perf_read();
	


	/*
	lock_acquire(&uart_lock);
	printf("================ PERF: C%x ================\r\n", hart_id);
	printf("[Rocket-C%x]: Perf: N.CheckPoints = %ld \r\n", hart_id, perf_val_NCP);
	printf("[Rocket-C%x]: Perf: N.CheckingState = %ld \r\n", hart_id, perf_val_NC);
	printf("[Rocket-C%x]: Perf: N.PostcheckingState = %ld \r\n", hart_id, perf_val_NPC);
	printf("[Rocket-C%x]: Perf: N.OtherThreads = %ld \r\n", hart_id, perf_val_NOT);
	printf("[Rocket-C%x]: Perf: N.Idle = %ld \r\n", hart_id, perf_val_NNC);
	printf("[Rocket-C%x]: Perf: N.Nonchecking_OtherThreads = %ld \r\n", hart_id, Nonchecking_OtherThreads);
	printf("[Rocket-C%x]: Perf: N.Nonchecking_MOtherThreads = %ld \r\n", hart_id, Nonchecking_MOtherThreads);
	printf("[Rocket-C%x]: Perf: N.Nonchecking_MCheck = %ld \r\n", hart_id, Nonchecking_MCheck);
	printf("[Rocket-C%x]: Perf: N.Nonchecking_Sched = %ld \r\n", hart_id, Nonchecking_Sched);
	printf("[Rocket-C%x]: Perf: N.CPSTrans = %ld \r\n", hart_id, CPSTrans);
	printf("[Rocket-C%x]: Perf: N.Insts = %ld \r\n", hart_id, Insts);
	lock_release(&uart_lock);
	*/

	lock_acquire(&uart_lock);
	printf("================ PERF: C%x ================\r\n", hart_id);
	printf("[Rocket-C%x]: Perf: N.Debug_perf_blocking_CP = %ld \r\n", hart_id, perf_val_NCP);
	printf("[Rocket-C%x]: Perf: N.debug_perf_blocking_id_ex_h = %ld \r\n", hart_id, perf_val_NC);
	printf("[Rocket-C%x]: Perf: N.debug_perf_blocking_id_mem_h = %ld \r\n", hart_id, perf_val_NPC);
	printf("[Rocket-C%x]: Perf: N.debug_perf_blocking_id_wb_h = %ld \r\n", hart_id, perf_val_NOT);
	printf("[Rocket-C%x]: Perf: N.debug_perf_blocking_id_sb_h = %ld \r\n", hart_id, perf_val_NNC);
	printf("[Rocket-C%x]: Perf: N.debug_perf_blocking_CSR_S = %ld \r\n", hart_id, Nonchecking_OtherThreads);
	printf("[Rocket-C%x]: Perf: N.debug_perf_blocking_FP = %ld \r\n", hart_id, Nonchecking_MOtherThreads);
	printf("[Rocket-C%x]: Perf: N.debug_perf_blocking_MEM = %ld \r\n", hart_id, Nonchecking_MCheck);
	printf("[Rocket-C%x]: Perf: N.debug_perf_blocking_DIV = %ld \r\n", hart_id, Insts);
	printf("[Rocket-C%x]: Perf: N.debug_perf_blocking_ST = %ld \r\n", hart_id, CPSTrans);
	printf("[Rocket-C%x]: Perf: N.debug_perf_blocking_FENCE = %ld \r\n", hart_id, Nonchecking_Sched);
	lock_release(&uart_lock);
	
	ghe_initailised(0);
	ghe_release();
  	ght_unset_satp_priv();
	ghe_asG();

	__asm__ volatile("nop");

	return NULL;
}



void rStartup (void) {
    //================== Initialisation ==================//
    // Bound current thread to BOOM
    if (gc_pthread_setaffinity(BOOM_ID) != 0) {
		printf ("[Boom-C%x]: pthread_setaffinity failed.", BOOM_ID);
	}

    // GC threads
    for (uint64_t i = 0; i < NUM_CORES - 1; i++) {
		pthread_create(&threads[i], NULL, thread_r_checker, (void *) (i+1));
	}

	ROCC_INSTRUCTION (1, 0x34);
	while (ght_get_initialisation() == 0){
 	}

	ght_set_satp_priv();
	// printf("[Boom-%x]: Test is now started: \r\n", BOOM_ID);

	//======================= Perf ========================//
	ghe_perf_ctrl(0x01);
  	ghe_perf_ctrl(0x00); 
	clock_gettime(CLOCK_MONOTONIC_RAW, &start); // get start time

    // ROCC_INSTRUCTION_S (1, 0x3, 0x69);
   	ROCC_INSTRUCTION (1, 0x31); // start monitoring
   	ROCC_INSTRUCTION_S (1, 0x01, 0x70); // ISAX_Go
    //===================== Execution =====================//

}


void rCleanup (void){
	//=================== Post execution ===================//
	ROCC_INSTRUCTION (1, 0x32); // stop monitoring
	ROCC_INSTRUCTION_S (1, 0X02, 0x70); // ISAX_Stop
	__asm__ volatile("nop");
	__asm__ volatile("nop");
	__asm__ volatile("nop");
	__asm__ volatile("nop");
	
	clock_gettime(CLOCK_MONOTONIC_RAW, &end);

	ghe_perf_ctrl(0x07<<1);
	uint64_t perf_val_CC = ghe_perf_read();
	ghe_perf_ctrl(0x01<<1);
	uint64_t perf_val_SB = ghe_perf_read();
	ghe_perf_ctrl(0x02<<1);
	uint64_t perf_val_SS = ghe_perf_read();
	ghe_perf_ctrl(0x03<<1);
	uint64_t perf_val_CS = ghe_perf_read();
	ghe_perf_ctrl(0x04<<1);
	uint64_t perf_val_SS_OT = ghe_perf_read();
	ghe_perf_ctrl(0x05<<1);
	uint64_t perf_val_SS_AB = ghe_perf_read();
	ghe_perf_ctrl(0x06<<1);
	uint64_t perf_val_OT = ghe_perf_read();

	uint64_t bp_checker  = debug_bp_checker();
    uint64_t bp_cdc = debug_bp_cdc();
    uint64_t bp_filter = debug_bp_filter();

	
	uint64_t status;
  	while (ght_get_initialisation() != 0) {

  	}

    // GC threads.
	for (uint64_t i = 0; i < NUM_CORES-1; i++) {
		pthread_join(threads[i], NULL);
	}

	lock_acquire(&uart_lock);
	printf("================ PERF: BOOM%x ================\r\n", BOOM_ID);
	printf("[Boom-%x]: Perf: ExecutionTime = %ld \r\n", BOOM_ID, perf_val_CC);
	printf("[Boom-%x]: Perf: SchedulingStateTime = %ld \r\n", BOOM_ID, perf_val_SS);
	printf("[Boom-%x]: Perf: SchedulingBlockTime-AllBusy = %ld \r\n", BOOM_ID, perf_val_SS_AB);
	printf("[Boom-%x]: Perf: SchedulingBlockTime-OtherThread = %ld \r\n", BOOM_ID, perf_val_SS_OT);
	printf("[Boom-%x]: Perf: SchedulingBlockTime = %ld \r\n", BOOM_ID, perf_val_SB);
	printf("[Boom-%x]: Perf: CheckingStateTime = %ld \r\n", BOOM_ID, perf_val_CS);
	printf("[Boom-%x]: Perf: OtherThreadTime = %ld \r\n", BOOM_ID, perf_val_OT);
	printf("[Boom-%x]: Perf-BPChecker: %ld cycles; \r\n",BOOM_ID, bp_checker);
 	printf("[Boom-%x]: Perf-BPCDC: %ld cycles; \r\n", BOOM_ID, bp_cdc);
 	printf("[Boom-%x]: Perf-BP-Filter: %ld cycles. \r\n", BOOM_ID, bp_filter);
	lock_release(&uart_lock);

	printf("[Boom-%x]: Test is now completed: \r\n", BOOM_ID);
	double elapsed = (end.tv_sec - start.tv_sec) + (end.tv_nsec - start.tv_nsec) / 1e9;
    printf("==== Execution time: %f seconds ==== \r\n", elapsed);


	ght_unset_satp_priv();
	ROCC_INSTRUCTION (1, 0x30); // reset monitoring
	
	// ROCC_INSTRUCTION_S (1, 0X00, 0x69);
}
