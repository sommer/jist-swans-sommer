#! /bin/env python

__doc__ = 'Semicolon counter'
__version__ = '0.1'
import getopt, sys, string, glob, os, re

def showVersion():
  print 'scc v'+__version__+', by Rimon Barr:',
  print 'Semicolon counter.'

def showUsage():
  print
  showVersion()
  print
  print 'Usage: scc            <-- filter mode'
  print '       scc [file(s)]  <-- read file names from command-line'
  print '       scc -f=<file>  <-- read file names from file'
  print '       scc -h | -v'
  print 
  print '  -h, -?, --help      display this help information'
  print '  -v, --version       display version'
  print '  -f= | --file=       read file names from file; - = stdin'
  print '  -t | --total        show only total'
  print
  print 'Send comments, suggestions and bug reports to <barr+scc@cs.cornell.edu>.'
  print

def usageError():
  print 'scc command syntax error'
  print 'Try `scc --help\' for more information.'

def numSemi(s):
  return len(string.split(s, ';'))-1

def display(num, name):
  print '%6d  %s' % (num, name)

def processFilename(fname, show):
  if not os.path.exists(fname):
    print 'not found: %s' % fname
    return 0
  f = None
  try:
    f = open(fname, 'r')
    s = f.read()
    f.close()
  except IOError:
    print 'ioerror: %s' % fname
    try:
      if f: f.close()
    except IOError: pass
    return 0
  num = numSemi(s)
  if show: display(num, fname)
  return num

def main():
  filemode = None
  totalsmode = 0
  try:
    opts, args = getopt.getopt(sys.argv[1:], 'hv?f:t',
      ['help', 'version', 'file=', 'total'])
  except getopt.error: 
    usageError()
    return -1
  for o, a in opts:
    if o in ("-h", "--help", "-?"):
      showUsage()
      return
    if o in ("-v", "--version"):
      showVersion()
      return
    if o in ('-t', '--total'):
      totalsmode = 1
    if o in ('-f', '--file'):
      filemode=re.match('=(.+)', a).group(1)
  totalsString = 'TOTAL'
  if totalsmode: totalString=''
  if filemode:
    if filemode=='-':
      f = sys.stdin
    else:
      f = open(filemode, 'r')
    fname = f.readline()
    total = 0
    while fname:
      fname = string.strip(fname)
      total = total + processFilename(fname, not totalsmode)
      fname = f.readline()
    f.close()
    display(total, totalString)

  else:
    if not len(args):
      print numSemi(sys.stdin.read())
    else:
      total = 0
      for globname in args:
        files = glob.glob(globname)
        if not files: files = [globname,]
        for fname in files:
          total = total + processFilename(fname, not totalsmode)
      display(total, totalString)


if __name__ == '__main__':
  try:
    main()
  except KeyboardInterrupt:
    pass

