//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <events.c Wed 2003/09/17 22:08:43 barr pompom.cs.cornell.edu>
//
// Author: Rimon Barr <barr+jist@cs.cornell.edu>
//

/**
 * Baseline event throughput performance test
 */

#include <stdio.h>
#include <assert.h>
#include "events.h"
#include "heap.h"
#include "pool.h"
#include "time.h"

heap *HEAP;
pool *EVENT_POOL;
#define EVENT_POOL_SIZE (10)
int TIME, ENDTIME;
int pooling = 0;
clock_t start_clock;

void foo()
{
  event *ev;
  if(pooling)
    ev = (event *)pool_get(EVENT_POOL);
  else
    ev = (event *)malloc(sizeof(event));
  assert(ev);
  ev->time = TIME+1;
  ev->data = (void *)foo;
  heap_insert(HEAP, ev);
}

void finish()
{
  printf("time in ms: %.0f\n", (clock()-start_clock)*1000.0/CLOCKS_PER_SEC);
  exit(1);
}

int event_compare(event* e1, event* e2)
{
  return e1->time - e2->time;
}

poolItem *createEvent()
{
  poolItem *event;
  event = (poolItem *)malloc(sizeof(event));
  assert(event);
  return event;
}

void destroyEvent(poolItem *event)
{
  assert(event);
  free(event);
}

void events(int num)
{
  int i;
  event *ev;
  // allocate heap
  HEAP = heap_alloc((heapComparator)event_compare);
  assert(HEAP);
  // allocate event pool
  EVENT_POOL = pool_alloc(EVENT_POOL_SIZE, createEvent, destroyEvent);
  assert(EVENT_POOL);
  // initialize simulation time
  TIME = 0;
  ENDTIME = num;
  // insert final event
  if(pooling)
    ev = (event*)pool_get(EVENT_POOL);
  else
    ev = (event*)malloc(sizeof(event));
  assert(ev);
  ev->time = ENDTIME;
  ev->data = (void *)finish;
  heap_insert(HEAP, ev);
  // insert first event
  if(pooling)
    ev = (event*)pool_get(EVENT_POOL);
  else
    ev = (event*)malloc(sizeof(event));
  assert(ev);
  ev->time = TIME;
  ev->data = (void *)foo;
  heap_insert(HEAP, ev);
  // process num events
  while(heap_size(HEAP)>0)
  {
    ev = heap_deleteFirst(HEAP);
    TIME = ev->time;
    ((void (*)())ev->data)();
    if(pooling)
      pool_put(EVENT_POOL, (poolItem *)ev);
    else
      free(ev);
  }
  ev = heap_deleteFirst(HEAP);
  // free heap
  heap_free(HEAP);
  if(pooling)
    pool_free(EVENT_POOL);
  HEAP = NULL;
  finish();
}

int main(int argc, char** argv)
{
  int num;
  if(argc<2)
  {
    printf("usage: events <events> <pooling>\n");
    exit(1);
  }
  num = atoi(argv[1]);
  if(argc>2)
    pooling = atoi(argv[2]);
  start_clock = clock();
  events(num);
  return 0;
}

