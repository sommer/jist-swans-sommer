//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Writer.java Tue 2004/04/06 11:45:32 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app.io;

import java.io.IOException;

/**
 * A functionally identical port of java.io.Writer, primarily brought into
 * jist.swans.app.io package so that it could be dynamically rewritten.
 *
 * @author Sun Microsystems
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Writer.java,v 1.4 2004-04-06 16:07:46 barr Exp $
 * @since SWANS1.0
 */
public abstract class Writer extends java.io.Writer 
{
  /** outgoing buffer. */
  private char[] writeBuffer;
  /** buffer limit. */
  private final int writeBufferSize = 1024;
  /** write lock. */
  protected Object lock;


  /** @see java.io.Writer */
  protected Writer() 
  {
    this.lock = this;
  }

  /** @see java.io.Writer */
  protected Writer(Object lock) 
  {
    if (lock == null) 
    {
      throw new NullPointerException();
    }
    this.lock = lock;
  }

  /** @see java.io.Writer */
  public void write(int c) throws IOException 
  {
    synchronized (lock) 
    {
      if (writeBuffer == null)
      {
        writeBuffer = new char[writeBufferSize];
      }
      writeBuffer[0] = (char) c;
      write(writeBuffer, 0, 1);
    }
  }

  /** @see java.io.Writer */
  public void write(char[] cbuf) throws IOException 
  {
    write(cbuf, 0, cbuf.length);
  }

  /** @see java.io.Writer */
  public abstract void write(char[] cbuf, int off, int len) throws IOException;

  /** @see java.io.Writer */
  public void write(String str) throws IOException 
  {
    write(str, 0, str.length());
  }

  /** @see java.io.Writer */
  public void write(String str, int off, int len) throws IOException 
  {
    synchronized (lock) 
    {
      char[] cbuf;
      if (len <= writeBufferSize) 
      {
        if (writeBuffer == null) 
        {
          writeBuffer = new char[writeBufferSize];
        }
        cbuf = writeBuffer;
      } 
      else 
      { 
        // Don't permanently allocate very large buffers.
        cbuf = new char[len];
      }
      str.getChars(off, (off + len), cbuf, 0);
      write(cbuf, 0, len);
    }
  }

  /** @see java.io.Writer */
  public abstract void flush() throws IOException;

  /** @see java.io.Writer */
  public abstract void close() throws IOException;

} // class: Writer

