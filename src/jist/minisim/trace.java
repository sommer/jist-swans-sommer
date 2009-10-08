//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <trace.java Tue 2004/04/06 11:28:53 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.minisim;

import jist.runtime.JistAPI;

/**
 * Test trace facility, by throwing an exception.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: trace.java,v 1.3 2004-04-06 16:07:42 barr Exp $
 * @since JIST1.0
 */

public class trace implements JistAPI.Entity
{
  /** an event. */
  public void abort()
  {
    throw new RuntimeException("abort");
  }

  /** an event. */
  public void foo()
  {
    abort();
  }

  /** 
   * Simulation entry point.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    trace t = new trace();
    t.foo();
  }

} // class: trace
