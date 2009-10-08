//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <unthreaded.java Fri 2005/02/25 09:48:35 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.minisim;

import jist.runtime.JistAPI;

/**
 * An example to show why you don't always need threads 
 * in an event-oriented environment. Run this code under
 * both java and jist to see how jist provides simulation
 * time 'parallelism' without threads.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: unthreaded.java,v 1.1 2005-03-03 13:05:39 barr Exp $
 * @since JIST1.0
 */
public class unthreaded implements JistAPI.Entity
{
  /** 'Thread' processing. */
  public void process(int id)
  {
    for(int i=1; i<=5; i++)
    {
      System.out.println("@t="+JistAPI.getTime()+
          " 'thread'="+id+": i="+i);
      JistAPI.sleepBlock(1);
    }
  }

  /**
   * Program entry point: show difference between Java and JiST
   * execution models.
   *
   * @param args command-line parameters
   */
  public static void main(String args[])
  {
    unthreaded t = new unthreaded();
    t.process(1);
    t.process(2);
    t.process(3);
  }

} // class: unthreaded
