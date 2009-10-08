//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <pool.h Wed 2003/09/17 19:59:10 barr pompom.cs.cornell.edu>
//
// Author: Rimon Barr <barr+jist@cs.cornell.edu>
//

#ifndef __POOL_H__
#define __POOL_H__

/**
 * Header for pool implementation
 */

// pool item type
typedef void* poolItem;

// pool item create
typedef poolItem* (*poolItemCreateFunction)();
// pool item destroy
typedef void (*poolItemDestroyFunction)(poolItem*);

// pool type
typedef struct
{
  // array of items in pool
  poolItem *items;
  // number items in pool
  int size;
  // pool capacity
  int capacity;
  // item create/destroy functions
  poolItemCreateFunction create;
  poolItemDestroyFunction destroy;
} pool;

// create new heap
pool *pool_alloc(int size, poolItemCreateFunction create, poolItemDestroyFunction destroy);

// destroy empty heap
void pool_free(pool*);

// insert item into heap
poolItem* pool_get(pool *);

// delete item from heap
void pool_put(pool *, poolItem *);

#endif
