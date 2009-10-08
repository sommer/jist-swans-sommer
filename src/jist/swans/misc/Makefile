CC = gcc
CC_OPT = -g -O3 -ffast-math -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux
CP=../../..:$(CLASSPATH)

all: libswansutil.so Util.class

clean:
	@rm -f *.so
	@rm -f *.o
	@rm -f Util.class
	@rm -f native.h

libswansutil.so: native.o
	@-ld -lm -o $@ -shared $<  &> /dev/null

native.o: native.c
	@-$(CC) $(CC_OPT) -o $@ -c $< &> /dev/null

native.c: native.h
	touch native.c

native.h: Util.class
	javah -classpath $(CP) -o $@ jist.swans.misc.Util
	touch native.h

%.class: %.java
	javac -classpath $(CP) $<

