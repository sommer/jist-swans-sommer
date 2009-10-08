//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <BufferedReader.java Tue 2004/04/06 11:44:12 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app.io;

import java.io.IOException;
import jist.runtime.JistAPI;

/**
 * A functionally identical port of java.io.BufferedReader, primarily brought
 * into jist.swans.app.io package so that it could be dynamically rewritten.
 *
 * @author Sun Microsystems
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: BufferedReader.java,v 1.5 2004-04-06 16:07:46 barr Exp $
 * @since SWANS1.0
 */

public class BufferedReader extends java.io.BufferedReader 
{

  /** underlying reader. */
  private Reader in;
  /** buffer. */
  private char[] cb;
  /** indices. */
  private int nChars, nextChar;

  /** marking constant. */
  private static final int INVALIDATED = -2;
  /** marking constant. */
  private static final int UNMARKED = -1;
  /** marking status. */
  private int markedChar = UNMARKED;
  /** limit for reading ahead; valid only when markedChar greater than 0. */
  private int readAheadLimit = 0;

  /** If the next character is a line feed, skip it. */
  private boolean skipLF = false;
  /** The skipLF flag when the mark was set. */
  private boolean markedSkipLF = false;
  /** buffer size. */
  private static int defaultCharBufferSize = 8192;
  /** expected input line length. */
  private static int defaultExpectedLineLength = 80;

  /** @see java.io.BufferedReader */
  public BufferedReader(Reader in, int sz) 
  {
    super(in);
    if (sz <= 0)
      throw new IllegalArgumentException("Buffer size <= 0");
    this.in = in;
    cb = new char[sz];
    nextChar = 0;
    nChars = 0;
  }

  /** @see java.io.BufferedReader */
  public BufferedReader(Reader in) 
  {
    this(in, defaultCharBufferSize);
  }

  /** @see java.io.BufferedReader */
  private void ensureOpen() throws IOException 
  {
    if (in == null)
      throw new IOException("Stream closed");
  }

  /** @see java.io.BufferedReader */
  private void fill() throws IOException 
  {
    int dst;
    if (markedChar <= UNMARKED) 
    {
      /* No mark */
      dst = 0;
    } 
    else 
    {
      /* Marked */
      int delta = nextChar - markedChar;
      if (delta >= readAheadLimit) 
      {
        /* Gone past read-ahead limit: Invalidate mark */
        markedChar = INVALIDATED;
        readAheadLimit = 0;
        dst = 0;
      } 
      else 
      {
        if (readAheadLimit <= cb.length) 
        {
          /* Shuffle in the current buffer */
          System.arraycopy(cb, markedChar, cb, 0, delta);
          markedChar = 0;
          dst = delta;
        } 
        else 
        {
          /* Reallocate buffer to accomodate read-ahead limit */
          char[] ncb = new char[readAheadLimit];
          System.arraycopy(cb, markedChar, ncb, 0, delta);
          cb = ncb;
          markedChar = 0;
          dst = delta;
        }
        nextChar = delta;
        nChars = delta;
      }
    }

    int n;
    do 
    {
      n = in.read(cb, dst, cb.length - dst);
    } while (n == 0);
    if (n > 0) 
    {
      nChars = dst + n;
      nextChar = dst;
    }
  }

  /** @see java.io.BufferedReader */
  public int read() throws IOException, JistAPI.Continuable 
  {
    synchronized (lock) 
    {
      ensureOpen();
      for (;;) 
      {
        if (nextChar >= nChars) 
        {
          fill();
          if (nextChar >= nChars)
            return -1;
        }
        if (skipLF) 
        {
          skipLF = false;
          if (cb[nextChar] == '\n') 
          {
            nextChar++;
            continue;
          }
        }   
        return cb[nextChar++];
      }
    }   
  }

  /** @see java.io.BufferedReader */
  private int read1(char[] cbuf, int off, int len) throws IOException, JistAPI.Continuable 
  {
    if (nextChar >= nChars) 
    {
      if (len >= cb.length && markedChar <= UNMARKED && !skipLF) 
      {
        return in.read(cbuf, off, len);
      }
      fill();
    }
    if (nextChar >= nChars) return -1;
    if (skipLF) 
    {
      skipLF = false;
      if (cb[nextChar] == '\n') 
      {
        nextChar++;
        if (nextChar >= nChars)
          fill();
        if (nextChar >= nChars)
          return -1;
      }
    }
    int n = Math.min(len, nChars - nextChar);
    System.arraycopy(cb, nextChar, cbuf, off, n);
    nextChar += n;
    return n;
  }

  /** @see java.io.BufferedReader */
  public int read(char[] cbuf, int off, int len) throws IOException, JistAPI.Continuable
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
        return 0;
      }

      int n = read1(cbuf, off, len);
      if (n <= 0) return n;
      while ((n < len) && in.ready()) 
      {
        int n1 = read1(cbuf, off + n, len - n);
        if (n1 <= 0) break;
        n += n1;
      }
      return n;
    }
  }

  /** @see java.io.BufferedReader */
  String readLine(boolean ignoreLF) throws IOException, JistAPI.Continuable 
  {
    StringBuffer s = null;
    int startChar;
    boolean omitLF = ignoreLF || skipLF;

    synchronized (lock) 
    {
      ensureOpen();

bufferLoop:
      for (;;) 
      {

        if (nextChar >= nChars)
          fill();
        if (nextChar >= nChars) 
        { /* EOF */
          if (s != null && s.length() > 0)
            return s.toString();
          else
            return null;
        }
        boolean eol = false;
        char c = 0;
        int i;

        /* Skip a leftover '\n', if necessary */
        if (omitLF && (cb[nextChar] == '\n')) 
          nextChar++;
        skipLF = false;
        omitLF = false;

charLoop:
        for (i = nextChar; i < nChars; i++) 
        {
          c = cb[i];
          if ((c == '\n') || (c == '\r')) 
          {
            eol = true;
            break charLoop;
          }
        }

        startChar = nextChar;
        nextChar = i;

        if (eol) 
        {
          String str;
          if (s == null) 
          {
            str = new String(cb, startChar, i - startChar);
          } 
          else 
          {
            s.append(cb, startChar, i - startChar);
            str = s.toString();
          }
          nextChar++;
          if (c == '\r') 
          {
            skipLF = true;
          }
          return str;
        }

        if (s == null) 
          s = new StringBuffer(defaultExpectedLineLength);
        s.append(cb, startChar, i - startChar);
      }
    }
  }

  /** @see java.io.BufferedReader */
  public String readLine() throws IOException, JistAPI.Continuable
  {
    return readLine(false);
  }

  /** @see java.io.BufferedReader */
  public long skip(long n) throws IOException, JistAPI.Continuable
  {
    if (n < 0L) 
    {
      throw new IllegalArgumentException("skip value is negative");
    }
    synchronized (lock) 
    {
      ensureOpen();
      long r = n;
      while (r > 0) 
      {
        if (nextChar >= nChars)
          fill();
        if (nextChar >= nChars) /* EOF */
          break;
        if (skipLF) 
        {
          skipLF = false;
          if (cb[nextChar] == '\n') 
          {
            nextChar++;
          }
        }
        long d = nChars - nextChar;
        if (r <= d) 
        {
          nextChar += r;
          r = 0;
          break;
        }
        else 
        {
          r -= d;
          nextChar = nChars;
        }
      }
      return n - r;
    }
  }

  /** @see java.io.BufferedReader */
  public boolean ready() throws IOException, JistAPI.Continuable
  {
    synchronized (lock) 
    {
      ensureOpen();

      if (skipLF) 
      {
        if (nextChar >= nChars && in.ready()) 
        {
          fill();
        }
        if (nextChar < nChars) 
        {
          if (cb[nextChar] == '\n') 
            nextChar++;
          skipLF = false;
        } 
      }
      return (nextChar < nChars) || in.ready();
    }
  }

  /** @see java.io.BufferedReader */
  public boolean markSupported() throws JistAPI.Continuable
  {
    return true;
  }

  /** @see java.io.BufferedReader */
  public void mark(int readAheadLimit) throws IOException, JistAPI.Continuable
  {
    if (readAheadLimit < 0) 
    {
      throw new IllegalArgumentException("Read-ahead limit < 0");
    }
    synchronized (lock) 
    {
      ensureOpen();
      this.readAheadLimit = readAheadLimit;
      markedChar = nextChar;
      markedSkipLF = skipLF;
    }
  }

  /** @see java.io.BufferedReader */
  public void reset() throws IOException, JistAPI.Continuable
  {
    synchronized (lock) 
    {
      ensureOpen();
      if (markedChar < 0)
        throw new IOException((markedChar == INVALIDATED)
            ? "Mark invalid"
            : "Stream not marked");
      nextChar = markedChar;
      skipLF = markedSkipLF;
    }
  }

  /** @see java.io.BufferedReader */
  public void close() throws IOException, JistAPI.Continuable
  {
    synchronized (lock) 
    {
      if (in == null)
        return;
      in.close();
      in = null;
      cb = null;
    }
  }

} // class: BufferedReader

