//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <BufferedWriter.java Tue 2004/04/06 11:44:18 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app.io;

import java.io.IOException;
import jist.runtime.JistAPI;

/**
 * A functionally identical port of java.io.BufferedWriter, primarily brought
 * into jist.swans.app.io package so that it could be dynamically rewritten.
 *
 * @author Sun Microsystems
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: BufferedWriter.java,v 1.4 2004-04-06 16:07:46 barr Exp $
 * @since SWANS1.0
 */

public class BufferedWriter extends java.io.BufferedWriter 
{
  /** underlying writer. */
  private Writer out;
  /** buffer. */
  private char[] cb;
  /** indices. */
  private int nChars, nextChar;
  /** buffer size. */
  private static int defaultCharBufferSize = 8192;
  /** end-of-line. */
  private String lineSeparator;

  /** @see java.io.BufferedWriter */
  public BufferedWriter(Writer out) 
  {
    this(out, defaultCharBufferSize);
  }

  /** @see java.io.BufferedWriter */
  public BufferedWriter(Writer out, int sz) 
  {
    super(out);
    if (sz <= 0)
      throw new IllegalArgumentException("Buffer size <= 0");
    this.out = out;
    cb = new char[sz];
    nChars = sz;
    nextChar = 0;

    lineSeparator = (String) java.security.AccessController.doPrivileged(
        new sun.security.action.GetPropertyAction("line.separator"));
  }

  /** @see java.io.BufferedWriter */
  private void ensureOpen() throws IOException 
  {
    if (out == null)
      throw new IOException("Stream closed");
  }

  /** @see java.io.BufferedWriter */
  void flushBuffer() throws IOException, JistAPI.Continuable 
  {
    synchronized (lock) 
    {
      ensureOpen();
      if (nextChar == 0)
        return;
      out.write(cb, 0, nextChar);
      nextChar = 0;
    }
  }

  /** @see java.io.BufferedWriter */
  public void write(int c) throws IOException, JistAPI.Continuable 
  {
    synchronized (lock) 
    {
      ensureOpen();
      if (nextChar >= nChars)
        flushBuffer();
      cb[nextChar++] = (char) c;
    }
  }

  /** @see java.io.BufferedWriter */
  private int min(int a, int b) 
  {
    if (a < b) return a;
    return b;
  }

  /** @see java.io.BufferedWriter */
  public void write(char[] cbuf, int off, int len) throws IOException, JistAPI.Continuable 
  {
    synchronized (lock) 
    {
      ensureOpen();
      if ((off < 0) || (off > cbuf.length) || (len < 0) ||
          ((off + len) > cbuf.length) || ((off + len) < 0)) 
      {
        throw new IndexOutOfBoundsException();
      } 
      else if (len == 0) 
      {
        return;
      } 

      if (len >= nChars) 
      {
        flushBuffer();
        out.write(cbuf, off, len);
        return;
      }

      int b = off, t = off + len;
      while (b < t) 
      {
        int d = min(nChars - nextChar, t - b);
        System.arraycopy(cbuf, b, cb, nextChar, d);
        b += d;
        nextChar += d;
        if (nextChar >= nChars)
          flushBuffer();
      }
    }
  }

  /** @see java.io.BufferedWriter */
  public void write(String s, int off, int len) throws IOException, JistAPI.Continuable 
  {
    synchronized (lock) 
    {
      ensureOpen();

      int b = off, t = off + len;
      while (b < t) 
      {
        int d = min(nChars - nextChar, t - b);
        s.getChars(b, b + d, cb, nextChar);
        b += d;
        nextChar += d;
        if (nextChar >= nChars)
          flushBuffer();
      }
    }
  }

  /** @see java.io.BufferedWriter */
  public void newLine() throws IOException 
  {
    write(lineSeparator);
  }

  /** @see java.io.BufferedWriter */
  public void flush() throws IOException, JistAPI.Continuable 
  {
    synchronized (lock) 
    {
      flushBuffer();
      out.flush();
    }
  }

  /** @see java.io.BufferedWriter */
  public void close() throws IOException 
  {
    synchronized (lock) 
    {
      if (out == null)
        return;
      flushBuffer();
      out.close();
      out = null;
      cb = null;
    }
  }

} // class: BufferedWriter

