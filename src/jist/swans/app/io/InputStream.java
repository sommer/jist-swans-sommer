//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <InputStream.java Tue 2004/04/06 11:44:56 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app.io;

import java.io.IOException;
import jist.runtime.JistAPI;

/**
 * A functionally identical port of java.io.InputStream, primarily brought into
 * jist.swans.app.io package so that it could be dynamically rewritten.
 *
 * @author Sun Microsystems
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: InputStream.java,v 1.4 2004-04-06 16:07:46 barr Exp $
 * @since SWANS1.0
 */

public abstract class InputStream extends java.io.InputStream 
{
  /** SKIP_BUFFER_SIZE is used to determine the size of skipBuffer. */
  private static final int SKIP_BUFFER_SIZE = 2048;
  /** skipBuffer is initialized in skip(long), if needed. */
  private static byte[] skipBuffer;

  /** @see java.io.InputStream */
  public abstract int read() throws IOException, JistAPI.Continuable;

  /** @see java.io.InputStream */
  public int read(byte[] b) throws IOException 
  {
    return read(b, 0, b.length);
  }

  /** @see java.io.InputStream */
  public int read(byte[] b, int off, int len) throws IOException 
  {
    if (b == null) 
    {
      throw new NullPointerException();
    } 
    else if ((off < 0) || (off > b.length) || (len < 0) ||
        ((off + len) > b.length) || ((off + len) < 0)) 
    {
      throw new IndexOutOfBoundsException();
    } 
    else if (len == 0) 
    {
      return 0;
    }

    int c = read();
    if (c == -1) 
    {
      return -1;
    }
    b[off] = (byte)c;

    int i = 1;
    try 
    {
      for (; i < len ; i++) 
      {
        c = read();
        if (c == -1) 
        {
          break;
        }
        if (b != null) 
        {
          b[off + i] = (byte)c;
        }
      }
    } catch (IOException ee) 
    { 
    }
    return i;
  }

  /** @see java.io.InputStream */
  public long skip(long n) throws IOException 
  {
    long remaining = n;
    int nr;
    if (skipBuffer == null)
      skipBuffer = new byte[SKIP_BUFFER_SIZE];

    byte[] localSkipBuffer = skipBuffer;

    if (n <= 0) 
    {
      return 0;
    }

    while (remaining > 0) 
    {
      nr = read(localSkipBuffer, 0,
          (int) Math.min(SKIP_BUFFER_SIZE, remaining));
      if (nr < 0) 
      {
        break;
      }
      remaining -= nr;
    }

    return n - remaining;
  }

  /** @see java.io.InputStream */
  public int available() throws IOException 
  {
    return 0;
  }

  /** @see java.io.InputStream */
  public void close() throws IOException 
  {
  }

  /** @see java.io.InputStream */
  public synchronized void mark(int readlimit) 
  {
  }

  /** @see java.io.InputStream */
  public synchronized void reset() throws IOException 
  {
    throw new IOException("mark/reset not supported");
  }

  /** @see java.io.InputStream */
  public boolean markSupported() 
  {
    return false;
  }

} // class: InputStream

