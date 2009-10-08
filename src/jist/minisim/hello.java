//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <hello.java Wed 2004/06/09 14:45:51 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.minisim;

import jist.runtime.JistAPI;

/**
 * Hello World of simulations.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: hello.java,v 1.7 2004-06-09 18:54:16 barr Exp $
 * @since JIST1.0
 */
public class hello implements JistAPI.Entity
{
  /**
   * Hello event.
   */
  public void myEvent()
  {
    JistAPI.sleep(1);
    myEvent();
    System.out.println("hello world, t="
        +JistAPI.getTime());
    // delay execution
    try 
    { 
      Thread.sleep(500); 
    }
    catch(InterruptedException e) 
    { 
    }
  }

  /**
   * Program entry point: show difference between Java and JiST
   * execution models with a "hello world!" example.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    System.out.println("starting simulation.");
    hello h = new hello();
    h.myEvent();
  }

} // class: hello

