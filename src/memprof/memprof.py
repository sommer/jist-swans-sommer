#!/usr/bin/env python

##################################################
## JIST (Java In Simulation Time) Project
## Timestamp: <memprof.py Sun 2003/08/10 22:42:40 barr pompom.cs.cornell.edu>
## Author: Rimon Barr <barr+jist@cs.cornell.edu>
##
## Process memory profiles

import sys, os, thread, string

DEBUG = 1

MEMPROF_IDENT = 'MEMPROF:'
MIN_SIZE = 0.02 # 2%
DUMP_FILE = 'dump.memprof'

# global tally variables
TYPES = {}

def processLine(l):
  if(l[0]=='#'): return
  try:
    l = string.split(l)
    cmd, rest = l[0], l[1:]
    {
      'BEGIN': doBegin,
      'END': doEnd,
      'DATA': doData,
    }[cmd](rest)
  except:
    print 'Error processing line: %s' % `l`
    #raise

def doBegin(info):
  print '# BEGIN', info[0]
  TYPES = {}

def doEnd(info):
  output = []
  total_count = total_size = 0
  for t in TYPES.keys():
    count, size = TYPES[t]
    total_count = total_count + count
    total_size  = total_size + size
    output.append( (size, count, t) )
  output.append( (total_size, total_count, 'TOTAL') )
  output.sort()
  output.reverse()
  for size, count, t in output:
    if size/float(total_size)<MIN_SIZE: break
    frac = "%3.1f%%" % (size/float(total_size)*100)
    print "%8s %10s (%6s) %s" % (count, size, frac, t)
  print '# END', info[0]

def doData(info):
  size, type = info
  try:
    count, bytes = TYPES[type]
  except:
    count, bytes = 0, 0
  count = count + 1
  bytes = bytes + int(size)
  TYPES[type] = count, bytes

def processInput(input):
  l = input.readline()
  while l:
    if l[:len(MEMPROF_IDENT)]==MEMPROF_IDENT:
      l = string.strip(l[len(MEMPROF_IDENT):])
      processLine(l)
    l = input.readline()


def processFile(filename):
  f = open(filename)
  processInput(f)
  f.close()

if __name__=='__main__':
  try:
    processFile(sys.argv[1])
  except KeyboardInterrupt:
    print 'Abort!'

