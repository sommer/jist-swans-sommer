//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <pool.c Wed 2003/09/17 21:26:18 barr pompom.cs.cornell.edu>
//
// Author: Rimon Barr <barr+jist@cs.cornell.edu>
//

/**
 * Pool implementation
 */

#include <stdlib.h>
#include <assert.h>
#include "pool.h"

// create new heap
pool *pool_alloc(int size, poolItemCreateFunction create, poolItemDestroyFunction destroy)
{
  pool *p;
  p = (pool*)malloc(sizeof(pool));
  assert(p);
  p->items = (poolItem*)malloc(size*sizeof(poolItem*));
  assert(p->items);
  p->capacity = size;
  p->size = 0;
  assert(create);
  p->create = create;
  assert(destroy);
  p->destroy = destroy;
  return p;
}

// destroy empty heap
void pool_free(pool* p)
{
  assert(p);
  assert(p->items);
  while(p->size)
  {
    p->destroy(pool_get(p));
  }
  free(p->items);
  free(p);
}

// insert item into heap
poolItem* pool_get(pool *p)
{
  assert(p);
  if(p->size)
  {
    assert(p->items);
    p->size--;
    return p->items[p->size];
  }
  else
  {
    assert(p->create);
    return p->create();
  }
}

// delete item from heap
void pool_put(pool *p, poolItem *item)
{
  if(p->size < p->capacity)
  {
    p->items[p->size] = item;
    p->size++;
  }
  else
  {
    assert(p->destroy);
    p->destroy(item);
  }
}

