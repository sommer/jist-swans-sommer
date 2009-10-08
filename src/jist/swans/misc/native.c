//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <native.c Wed 2003/09/10 13:24:21 barr pompom.cs.cornell.edu>
// Author: Rimon Barr <barr+jist@cs.cornell.edu>
//
// Library to for misc SWANS native routines:
//   - faster Math.log
//

#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <assert.h>
#include <jvmpi.h>
#include <jvmdi.h>
#include <math.h>
#include <assert.h>

#define DEBUG 0

// See: http://mail.flightgear.org/pipermail/simgear-cvslogs/2003-June/000533.html
//  or: http://www.flipcode.com/cgi-bin/msg.cgi?showThread=Tip-Fastlogfunction&forum=totd&id=-1

float fast_log2(float val)
{
  int * const    exp_ptr = (int*)(&val);
  int            x = *exp_ptr;
  const int      log_2 = ((x >> 23) & 255) - 128;
  //assert(sizeof(float)==sizeof(int));
  x &= ~(255 << 23);
  x += 127 << 23;
  *exp_ptr = x;
  val = ((-1.0f/3) * val + 2) * val - 2.0f/3;   // (1)
  return (val + log_2);
}

/** Error of ~0.01% */
float fast_log(float val)
{
  return (fast_log2 (val) * 0.69314718f);
}

//////////////////////////////////////////////////
// native functions
//

JNIEXPORT jfloat JNICALL Java_jist_swans_misc_Util_fast_1log
  (JNIEnv *env, jclass cl, jfloat n)
{
  return fast_log(n);
}

int main(int argv, char *argc)
{
  int i;
  for(i=0; i<10000000; i++)
  {
    log(20.0);
  }
}

