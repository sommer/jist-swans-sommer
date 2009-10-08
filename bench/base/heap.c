//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <heap.c Wed 2003/09/17 21:58:34 barr pompom.cs.cornell.edu>
//
// Author: Rimon Barr <barr+jist@cs.cornell.edu>
//

/**
 * Heap implementation
 */

#include <stdlib.h>
#include <assert.h>
#include "heap.h"

heap *heap_alloc(heapComparator comparator)
{
  heap *h;

  h = (heap *)malloc(sizeof(heap));
  assert(h);
  h->capacity = HEAP_INITIAL_LENGTH;
  h->items = (heapItem *)malloc(sizeof(heapItem)*h->capacity);
  assert(h->items);
  h->size = 0;
  h->comparator = comparator;
  return h;
}

void heap_free(heap *h)
{
  assert(!h->size);
  free(h->items);
  free(h);
}

int heap_size(heap *h)
{
  assert(h);
  return h->size;
}

void heap_doubleCapacity(heap *h)
{
  assert(h);
  h->capacity << 1;
  h->items = (heapItem *)realloc(h, sizeof(heapItem)*h->capacity);
}

int heap_insert(heap *h, heapItem hi)
{
  int i, parent;
  assert(h);
  if(h->size==h->capacity) heap_doubleCapacity(h);
  // bubble opening to right spot
  i = h->size;
  h->size++;
  while(i>0 && h->comparator(hi, h->items[parent = (i-1)/2])<0)
  {
    h->items[i] = h->items[parent];
    i = parent;
    parent = (i-1)/2;
  }
  // insert item
  h->items[i] = hi;
  return i;
}

void heap_heapify(heap *h, int i)
{
  int left, right, largest;
  heapItem tmp;
  assert(h);
  // left and right children
  left = (i*2)+1;
  right = left+1;
  // find "largest" element
  if(left<h->size && h->comparator(h->items[left], h->items[i])<0)
    largest = left;
  else
    largest = i;
  if(right<h->size && h->comparator(h->items[right], h->items[largest])<0)
    largest = right;
  // swap and recurse, if necessary
  if(largest!=i)
  {
    tmp = h->items[i];
    h->items[i] = h->items[largest];
    h->items[largest] = tmp;
    heap_heapify(h, largest);
  }
}

heapItem heap_delete(heap *h, int i)
{
  heapItem hi;
  assert(h);
  assert(h->size>i);
  // get item
  hi = h->items[i];
  // fill in the gap
  h->size--;
  h->items[i] = h->items[h->size];
  // reorder heap array
  heap_heapify(h, i);
  return hi;
}

heapItem heap_deleteFirst(heap *h)
{
  assert(h);
  return heap_delete(h, 0);
}

