//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <heap.h Sun 2003/09/07 22:42:19 barr pompom.cs.cornell.edu>
//
// Author: Rimon Barr <barr+jist@cs.cornell.edu>
//

#ifndef __HEAP_H__
#define __HEAP_H__

/**
 * Header for heap implementation
 */

#define HEAP_INITIAL_LENGTH 8

// heap item type
typedef void* heapItem;

// heap comparator function type
typedef int (*heapComparator)(heapItem, heapItem);

typedef struct
{
  // array of items in heap
  heapItem *items;
  // number of items in heap
  int size;
  // current array size
  int capacity;
  // heap item comparator function
  heapComparator comparator;
} heap;

// create new heap
heap *heap_alloc(heapComparator);

// destroy empty heap
void heap_free(heap*);

// heap size
int heap_size(heap*);

// insert item into heap
int heap_insert(heap*, heapItem);

// delete item from heap
heapItem heap_delete(heap *h, int i);

// delete first item for heap
heapItem heap_deleteFirst(heap *h);

#endif
