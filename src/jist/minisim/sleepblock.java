//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <sleepblock.java Thu 2005/02/24 22:46:17 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.minisim;

import jist.runtime.JistAPI;

/**
 * A slightly more sophisticated Hello World of simulations to show
 * the difference between blocking and non-blocking events.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: sleepblock.java,v 1.1 2005-02-25 04:43:58 barr Exp $
 * @since JIST1.0
 */

public class sleepblock implements JistAPI.Entity
{
  /**
   * Program entry point: show difference between blocking
   * and non-blocking events with an extended "hello world!" example.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    System.out.println("starting simulation.");
    sleepblock hl = new sleepblock();
    hl.nonblock();
    hl.block();
  }
  
  /** 
   * Initialize sleepblock entity.
   */
  public sleepblock()
  {
    System.out.println ("Creating new object sleepblock at t = " + JistAPI.getTime());
  }

  public void hello(String msg)
  {
    System.out.println (msg+": hello world at t = " + JistAPI.getTime());
  }

  public void nonblock()
  {
    for (int i = 0; i < 3; i++)
    {
      System.out.println("nonblock i="+i);
      hello("nonblock i="+i);
      JistAPI.sleep(1);
    }
    System.out.println("nonblock DONE.");
  }
  
  public void block()
  {
    for (int i = 0; i < 3; i++)
    {
      System.out.println("   block i="+i);
      hello("   block i="+i);
      JistAPI.sleepBlock(1);
    }
    System.out.println("   block DONE.");
  }

} // class: sleepblock

