//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Reader.java Tue 2004/04/06 11:45:26 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app.io;

import java.io.IOException;

/**
 * A functionally identical port of java.io.Reader, primarily brought into
 * jist.swans.app.io package so that it could be dynamically rewritten.
 *
 * @author Sun Microsystems
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Reader.java,v 1.4 2004-04-06 16:07:46 barr Exp $
 * @since SWANS1.0
 */
public abstract class Reader extends java.io.Reader 
{
  /** maximum skip buffer size. */
  private static final int maxSkipBufferSize = 8192;

  /** read lock. */
  protected Object lock;
  /** skipped content. */
  private char[] skipBuffer = null;

  /** @see java.io.Reader */
  protected Reader() 
  {
    this.lock = this;
  }

  /** @see java.io.Reader */
  protected Reader(Object lock) 
  {
    if (lock == null) 
    {
      throw new NullPointerException();
    }
    this.lock = lock;
  }

  /** @see java.io.Reader */
  public int read() throws IOException 
  {
    char[] cb = new char[1];
    if (read(cb, 0, 1) == -1)
      return -1;
    else
      return cb[0];
  }

  /** @see java.io.Reader */
  public int read(char[] cbuf) throws IOException 
  {
    return read(cbuf, 0, cbuf.length);
  }

  /** @see java.io.Reader */
  public abstract int read(char[] cbuf, int off, int len) throws IOException;

  /** @see java.io.Reader */
  public long skip(long n) throws IOException 
  {
    if (n < 0L) 
      throw new IllegalArgumentException("skip value is negative");
    int nn = (int) Math.min(n, maxSkipBufferSize);
    synchronized (lock) 
    {
      if ((skipBuffer == null) || (skipBuffer.length < nn))
        skipBuffer = new char[nn];
      long r = n;
      while (r > 0) 
      {
        int nc = read(skipBuffer, 0, (int)Math.min(r, nn));
        if (nc == -1)
          break;
        r -= nc;
      }
      return n - r;
    }
  }

  /** @see java.io.Reader */
  public boolean ready() throws IOException 
  {
    return false;
  }

  /** @see java.io.Reader */
  public boolean markSupported() 
  {
    return false;
  }

  /** @see java.io.Reader */
  public void mark(int readAheadLimit) throws IOException 
  {
    throw new IOException("mark() not supported");
  }

  /** @see java.io.Reader */
  public void reset() throws IOException 
  {
    throw new IOException("reset() not supported");
  }

  /** @see java.io.Reader */
  public abstract void close() throws IOException;

} // class: Reader

