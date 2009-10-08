//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <memprof.c Wed 2003/07/23 15:27:31 barr glengoyne>
// Author: Rimon Barr <barr+jist@cs.cornell.edu>
//
// Library to produce memory profile of Java application using JVMPI
//

#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <assert.h>
#include <jvmpi.h>
#include <jvmdi.h>

#define DEBUG 0
#define FOUT stderr

//////////////////////////////////////////////////
// prototypes
//

void notifyEvent(JVMPI_Event *event);
void doHeapDump();

//////////////////////////////////////////////////
// globals
//

static JavaVM *Gjvm;
static JVMPI_Interface *Gjvmpi;
static JNIEnv *Gjni;

static int GonShutDown, GnumDumps;
static char *Gdumpname;
static jobjectID Gobject;
static int Gsize;
static const char *Gclass;
static jobjectID Gclazz;
static char buf[1024];
static FILE *Gout;

//////////////////////////////////////////////////
// initialization
//

JNIEXPORT jint JNICALL 
JVM_OnLoad(
    JavaVM *jvm, char *options, void *reserved) 
{
  jvmdiError err;
  // open output stream
  if(options && strlen(options))
  {
    Gout = fopen(options, "w");
    if(Gout==NULL)
    {
      fprintf(FOUT, "MEMPROF: # unable to open file '%s' for output\n", options);
      return JNI_ERR;
    }
  }
  else
  {
    Gout = FOUT;
  }
  // get JVMPI pointer
  Gjvm = jvm;
  err=(*Gjvm)->GetEnv(Gjvm,(void**)&Gjvmpi, JVMPI_VERSION_1);
  if (err != 0) {
    fprintf(stderr, "MEMPROF: # error in obtaining jvmpi interface pointer\n");
    return JNI_ERR;
  }
  // initialize dump variables
  GonShutDown = GnumDumps = 0;
  // register notifications
  Gjvmpi->NotifyEvent = notifyEvent;
  Gjvmpi->EnableEvent(JVMPI_EVENT_DATA_DUMP_REQUEST, NULL);
  Gjvmpi->EnableEvent(JVMPI_EVENT_JVM_SHUT_DOWN, NULL); 
  // return ok
  if(DEBUG) fprintf(Gout, "MEMPROF: # Initialized.\n");
  return JNI_OK;
}

//////////////////////////////////////////////////
// native
//

JNIEXPORT void JNICALL Java_memprof_memprof__1dumpHeap
  (JNIEnv *env, jclass jcl, jstring UTFname)
{
  jboolean iscopy;
  const char *name = (*env)->GetStringUTFChars(
      env, UTFname, &iscopy);
  Gdumpname = strdup(name);
  (*env)->ReleaseStringUTFChars(env, UTFname, name);
  doHeapDump();
}

JNIEXPORT void JNICALL Java_memprof_memprof__1dumpOnShutdown
  (JNIEnv *env, jclass jcl)
{
  GonShutDown = 1;
}

//////////////////////////////////////////////////
// heap dump
//

void doHeapDump()
{
  JVMPI_HeapDumpArg arg;
  // garbage collection sweep
  Gjvmpi->RunGC();
  sleep(1);
  Gjvmpi->RunGC();
  //(*Gjvm)->AttachCurrentThread(Gjvm, &Gjni, NULL);
  arg.heap_dump_level = JVMPI_DUMP_LEVEL_0;
  Gjvmpi->RequestEvent(JVMPI_EVENT_HEAP_DUMP, &arg);
  //(*Gjvm)->DetachCurrentThread(Gjvm);
  GnumDumps++;
}

void load_classname() {
  if(Gclazz == NULL) 
  {
    Gclass = "UNKNOWN";
    return;
  }
  Gclass = "NULL";
  Gjvmpi->RequestEvent(JVMPI_EVENT_CLASS_LOAD, Gclazz);
}

jobjectID asObject(char *curr) {
  char ptr[4];
  memcpy(ptr, curr, 4);
  return *((jobjectID*)ptr);
}

//////////////////////////////////////////////////
// profile event notification
//

void notifyEvent(JVMPI_Event *event) 
{
  switch(event->event_type) 
  {
    case JVMPI_EVENT_DATA_DUMP_REQUEST:
      Gdumpname = "DUMP_REQUEST";
      doHeapDump();
      break;

    case JVMPI_EVENT_JVM_SHUT_DOWN:
      Gdumpname = "JVM_SHUTDOWN";
      if(!GnumDumps || GonShutDown)
      {
        doHeapDump();
      }
      if(Gout!=FOUT)
      {
        fclose(Gout);
      }
      break;

    case JVMPI_EVENT_HEAP_DUMP | JVMPI_REQUESTED_EVENT: 
    {
      int count, type;
      char *curr;
      if(DEBUG) fprintf(Gout, "MEMPROF: # Dump begin.\n");
      fprintf(Gout, "MEMPROF: BEGIN %s\n", Gdumpname);
      count = 0;
      curr = event->u.heap_dump.begin;
      while(curr < event->u.heap_dump.end) 
      {
        count++;
        type = *(curr++);
        Gobject = asObject(curr);
        if(Gobject==NULL) fprintf(Gout, "MEMPROF: # object null\n");
        curr+=4;
        Gsize = -1;
        Gclass = "NULL";
        Gjvmpi->RequestEvent(JVMPI_EVENT_OBJECT_ALLOC, Gobject);
        fprintf(Gout, "MEMPROF: DATA %d %s\n", Gsize, Gclass);
      }
      fprintf(Gout, "MEMPROF: END %s\n", Gdumpname);
      if(DEBUG) fprintf(Gout, "MEMPROF: # Dump end.\n");
      fflush(Gout);
      break;
    }

    case JVMPI_EVENT_CLASS_LOAD  | JVMPI_REQUESTED_EVENT:
      if(DEBUG) fprintf(Gout, "MEMPROF: # Class load request.\n");
      Gclass = event->u.class_load.class_name;
      if(Gclass==NULL) Gclass = "UNKNOWN";
      break;

    case JVMPI_EVENT_OBJECT_ALLOC  | JVMPI_REQUESTED_EVENT:
      if(DEBUG) fprintf(Gout, "MEMPROF: # Object allocation request.\n");
      Gsize = event->u.obj_alloc.size;
      switch(event->u.obj_alloc.is_array) 
      {
        case JVMPI_BOOLEAN: 
          Gclass = "[boolean"; 
          break;
        case JVMPI_FLOAT:
          Gclass = "[float";
          break;
        case JVMPI_DOUBLE:
          Gclass = "[double";
          break;
        case JVMPI_BYTE:
          Gclass = "[byte";
          break;
        case JVMPI_SHORT:
          Gclass = "[short";
          break;
        case JVMPI_INT:
          Gclass = "[int";
          break;
        case JVMPI_LONG:
          Gclass = "[long";
          break;
        case JVMPI_CHAR : 
          Gclass = "[char";
          break;
        case JVMPI_NORMAL_OBJECT: 
          Gclazz = event->u.obj_alloc.class_id;
          load_classname();
          break;
        case JVMPI_CLASS:
          Gclazz = event->u.obj_alloc.class_id;
          load_classname();
          sprintf(buf, "[%s", Gclass);
          Gclass = buf;
          break;
        default :
          fprintf(Gout, "MEMPROF: # invalid event type");
          assert(0);
      }
      break;
  }
}

