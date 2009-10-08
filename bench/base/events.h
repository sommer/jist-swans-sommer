//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <events.h Tue 2003/04/22 22:46:37 barr pompom.cs.cornell.edu>
//
// Author: Rimon Barr <barr+jist@cs.cornell.edu>
//

#ifndef __EVENTS_H__
#define __EVENTS_H__

// event structure
typedef struct
{
  int time;
  void *data;
} event;

// event comparator
int event_compare(event* e1, event* e2);

#endif
