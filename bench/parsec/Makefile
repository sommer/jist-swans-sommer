DST = context events mem_event mem_entity

all: $(DST)

clean:
	rm -f *.o *.pi
	rm -f $(DST)

context: context.pc
	pcc -O3 -o context context.pc

events: events.pc
	pcc -O3 -o events events.pc

mem_event: mem_event.pc
	pcc -O3 -stack 20000 -o mem_event mem_event.pc

mem_entity: mem_entity.pc
	pcc -O3 -stack 20000 -o mem_entity mem_entity.pc


