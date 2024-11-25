CC = riscv64-unknown-linux-gnu-gcc
CPP = riscv64-unknown-linux-gnu-g++

OFLAGS := -O3 -DRISCV -march=rv64imafd -DUSE_PUBLIC_MALLOC_WRAPPERS
CFLAGS:= -O3 -DRISCV -march=rv64imafd

CFLAGS_CM:= -O3 -DRISCV -march=rv64imafd -Wl,--allow-multiple-definition


#CC = gcc
#CFLAGS := -O3 -std=gnu99
initialisation_r: initialisation_r.c
	${CC} ${CFLAGS} -o initialisation_r.riscv initialisation_r.c -lpthread

initialisation_none: initialisation_none.c
	${CC} ${CFLAGS} -o initialisation_none.riscv initialisation_none.c -lpthread

main_none: main_none.c
	${CC} ${CFLAGS_CM} -c main_none.c -lpthread


checker_thread: main_r.c
	${CC} ${CFLAGS_CM} -c main_r.c -o checker_thread.o -lpthread

clean:
	rm -f *.o *.riscv *.so

