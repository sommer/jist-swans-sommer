
EVENTS_OBJ = events.o heap.o pool.o
CC_OPT = -O3

all: events

clean:
	rm -f events
	rm -f $(EVENTS_OBJ)

events: $(EVENTS_OBJ)
	gcc -o events $(EVENTS_OBJ)

%.o: %.c
	gcc $(CC_OPT) -c $<
