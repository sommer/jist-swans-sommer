//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <FilterInputStream.java Tue 2004/04/06 11:44:49 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app.io;

import java.io.IOException;
import jist.runtime.JistAPI;

/**
 * A functionally identical port of java.io.FilterInputStream, primarily
 * brought into jist.swans.app.io package so that it could be dynamically
 * rewritten.
 *
 * @author Sun Microsystems
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: FilterInputStream.java,v 1.3 2004-04-06 16:07:46 barr Exp $
 * @since SWANS1.0
 */

public class FilterInputStream extends InputStream 
{

  /** @see java.io.FilterInputStream */
  protected InputStream in;

  /** @see java.io.FilterInputStream */
  protected FilterInputStream(InputStream in) 
  {
    this.in = in;
  }

  /** @see java.io.FilterInputStream */
  public int read() throws IOException, JistAPI.Continuation 
  {
    return in.read();
  }

  /** @see java.io.FilterInputStream */
  public int read(byte[] b) throws IOException, JistAPI.Continuation 
  {
    return read(b, 0, b.length);
  }

  /** @see java.io.FilterInputStream */
  public int read(byte[] b, int off, int len) throws IOException, JistAPI.Continuation 
  {
    return in.read(b, off, len);
  }

  /** @see java.io.FilterInputStream */
  public long skip(long n) throws IOException, JistAPI.Continuation 
  {
    return in.skip(n);
  }

  /** @see java.io.FilterInputStream */
  public int available() throws IOException, JistAPI.Continuation 
  {
    return in.available();
  }

  /** @see java.io.FilterInputStream */
  public void close() throws IOException, JistAPI.Continuation 
  {
    in.close();
  }

  /** @see java.io.FilterInputStream */
  public synchronized void mark(int readlimit) throws JistAPI.Continuation 
  {
    in.mark(readlimit);
  }

  /** @see java.io.FilterInputStream */
  public synchronized void reset() throws IOException, JistAPI.Continuation 
  {
    in.reset();
  }

  /** @see java.io.FilterInputStream */
  public boolean markSupported() throws JistAPI.Continuation 
  {
    return in.markSupported();
  }

}

