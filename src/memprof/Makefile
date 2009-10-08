CC = gcc
CC_OPT = -g -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux
CLASSPATH=../../libs/jargs.jar:..

all: libmemprof.so memprof.class

clean:
	rm -f *.so
	rm -f *.o
	rm -f *.class
	rm -f memprof.h

libmemprof.so: memprof.o
	ld -o $@ -shared $< 

memprof.o: memprof.c
	$(CC) $(CC_OPT) -o $@ -c $<

memprof.c: memprof.h
	touch memprof.c

memprof.h: memprof.class
	javah -classpath .. -o $@ memprof.memprof

%.class: %.java
	javac -classpath $(CLASSPATH):$$CLASSPATH $<
