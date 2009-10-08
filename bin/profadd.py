#!/usr/bin/env python

# Short utility to display cumulative profile times per location
# Standard Java profiler outputs times by unique stack trace. This
# script takes this output and simply adds up all times for different
# stack frames that end up in the same location.

import sys

fin = sys.stdin
totals = {}

l = fin.readline()
while l:
  if l.find('rank')==0: break
  l = fin.readline()
l = fin.readline()
while l:
  if l.find('CPU')==0: break
  _, time, _, cnt, _, loc = l.split()
  time = float(time[:-1])
  cnt = int(cnt)
  try:
    tot_time, tot_cnt = totals[loc]
  except:
    tot_time, tot_cnt = 0, 0
  tot_time += time
  tot_cnt += cnt
  totals[loc] = tot_time, tot_cnt
  l = fin.readline()

output = []
for loc in totals.keys():
  tot_time, tot_cnt = totals[loc]
  output.append( (tot_time, tot_cnt, loc) )
output.sort()
output.reverse()
try:
  for l in output:
    print "%2.1f%% %3d %s" % l
except IOError:
  pass

