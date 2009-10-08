//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <BufferedInputStream.java Tue 2004/04/06 11:44:40 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app.io;

import java.io.IOException;
import jist.runtime.JistAPI;

/**
 * A functionally identical port of java.io.BufferedInputStream, primarily
 * brought into jist.swans.app.io package so that it could be dynamically
 * rewritten.
 *
 * @author Sun Microsystems
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: BufferedInputStream.java,v 1.3 2004-04-06 16:07:46 barr Exp $
 * @since SWANS1.0
 */

public class BufferedInputStream extends FilterInputStream 
{

  /** @see java.io.BufferedInputStream */
  private static int defaultBufferSize = 2048;

  /** @see java.io.BufferedInputStream */
  protected byte[] buf;

  /** @see java.io.BufferedInputStream */
  protected int count;

  /** @see java.io.BufferedInputStream */
  protected int pos;

  /** @see java.io.BufferedInputStream */
  protected int markpos = -1;

  /** @see java.io.BufferedInputStream */
  protected int marklimit;

  /** @see java.io.BufferedInputStream */
  private void ensureOpen() throws IOException 
  {
    if (in == null)
      throw new IOException("Stream closed");
  }

  /** @see java.io.BufferedInputStream */
  public BufferedInputStream(InputStream in) 
  {
    this(in, defaultBufferSize);
  }

  /** @see java.io.BufferedInputStream */
  public BufferedInputStream(InputStream in, int size) 
  {
    super(in);
    if (size <= 0) 
    {
      throw new IllegalArgumentException("Buffer size <= 0");
    }
    buf = new byte[size];
  }

  /** @see java.io.BufferedInputStream */
  private void fill() throws IOException 
  {
    if (markpos < 0)
      pos = 0;    /* no mark: throw away the buffer */
    else if (pos >= buf.length) /* no room left in buffer */
      if (markpos > 0) 
      { /* can throw away early part of the buffer */
        int sz = pos - markpos;
        System.arraycopy(buf, markpos, buf, 0, sz);
        pos = sz;
        markpos = 0;
      } 
      else if (buf.length >= marklimit) 
      {
        markpos = -1; /* buffer got too big, invalidate mark */
        pos = 0;  /* drop buffer contents */
      } 
      else 
      {   /* grow buffer */
        int nsz = pos * 2;
        if (nsz > marklimit)
          nsz = marklimit;
        byte[] nbuf = new byte[nsz];
        System.arraycopy(buf, 0, nbuf, 0, pos);
        buf = nbuf;
      }
    count = pos;
    int n = in.read(buf, pos, buf.length - pos);
    if (n > 0)
      count = n + pos;
  }

  /** @see java.io.BufferedInputStream */
  public synchronized int read() throws IOException, JistAPI.Continuation 
  {
    ensureOpen();
    if (pos >= count) 
    {
      fill();
      if (pos >= count)
        return -1;
    }
    return buf[pos++] & 0xff;
  }

  /** @see java.io.BufferedInputStream */
  private int read1(byte[] b, int off, int len) throws IOException 
  {
    int avail = count - pos;
    if (avail <= 0) 
    {
      /* If the requested length is at least as large as the buffer, and
         if there is no mark/reset activity, do not bother to copy the
         bytes into the local buffer.  In this way buffered streams will
         cascade harmlessly. */
      if (len >= buf.length && markpos < 0) 
      {
        return in.read(b, off, len);
      }
      fill();
      avail = count - pos;
      if (avail <= 0) return -1;
    }
    int cnt = (avail < len) ? avail : len;
    System.arraycopy(buf, pos, b, off, cnt);
    pos += cnt;
    return cnt;
  }

  /** @see java.io.BufferedInputStream */
  public synchronized int read(byte[] b, int off, int len)
    throws IOException, JistAPI.Continuation
  {
    ensureOpen();
    if ((off | len | (off + len) | (b.length - (off + len))) < 0) 
    {
      throw new IndexOutOfBoundsException();
    } 
    else if (len == 0) 
    {
      return 0;
    }

    int n = read1(b, off, len);
    if (n <= 0) return n;
    while ((n < len) && (in.available() > 0)) 
    {
      int n1 = read1(b, off + n, len - n);
      if (n1 <= 0) break;
      n += n1;
    }
    return n;
  }

  /** @see java.io.BufferedInputStream */
  public synchronized long skip(long n) throws IOException, JistAPI.Continuation 
  {
    ensureOpen();
    if (n <= 0) 
    {
      return 0;
    }
    long avail = count - pos;

    if (avail <= 0) 
    {
      // If no mark position set then don't keep in buffer
      if (markpos <0) 
        return in.skip(n);

      // Fill in buffer to save bytes for reset
      fill();
      avail = count - pos;
      if (avail <= 0)
        return 0;
    }

    long skipped = (avail < n) ? avail : n;
    pos += skipped;
    return skipped;
  }

  /** @see java.io.BufferedInputStream */
  public synchronized int available() throws IOException, JistAPI.Continuation 
  {
    ensureOpen();
    return (count - pos) + in.available();
  }

  /** @see java.io.BufferedInputStream */
  public synchronized void mark(int readlimit) throws JistAPI.Continuation 
  {
    marklimit = readlimit;
    markpos = pos;
  }

  /** @see java.io.BufferedInputStream */
  public synchronized void reset() throws IOException, JistAPI.Continuation 
  {
    ensureOpen();
    if (markpos < 0)
      throw new IOException("Resetting to invalid mark");
    pos = markpos;
  }

  /** @see java.io.BufferedInputStream */
  public boolean markSupported() throws JistAPI.Continuation 
  {
    return true;
  }

  /** @see java.io.BufferedInputStream */
  public void close() throws IOException, JistAPI.Continuation 
  {
    if (in == null)
      return;
    in.close();
    in = null;
    buf = null;
  }

}

