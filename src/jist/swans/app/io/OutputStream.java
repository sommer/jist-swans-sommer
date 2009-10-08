//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <OutputStream.java Tue 2004/04/06 11:45:09 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app.io;

import java.io.IOException;

import jist.runtime.JistAPI;

/**
 * A functionally identical port of java.io.OutputStream, primarily brought
 * into jist.swans.app.io package so that it could be dynamically rewritten and
 * tagged with the appropriate blocking invocation semantics.
 *
 * @author Sun Microsystems
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: OutputStream.java,v 1.4 2004-04-06 16:07:46 barr Exp $
 * @since SWANS1.0
 */

public abstract class OutputStream 
{

  /** @see java.io.OutputStream */
  public abstract void write(int b) throws IOException, JistAPI.Continuable;

  /** @see java.io.OutputStream */
  public void write(byte[] b) throws IOException 
  {
    write(b, 0, b.length);
  }

  /** @see java.io.OutputStream */
  public void write(byte[] b, int off, int len) throws IOException 
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
      return;
    }
    for (int i = 0 ; i < len ; i++) 
    {
      write(b[off + i]);
    }
  }

  /** @see java.io.OutputStream */
  public void flush() throws IOException 
  {
  }

  /** @see java.io.OutputStream */
  public void close() throws IOException 
  {
  }

} // class: OutputStream

