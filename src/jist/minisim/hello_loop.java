//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <hello_loop.java Tue 2004/04/06 11:27:51 barr pompom.cs.cornell.edu>
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
 * @version $Id: hello_loop.java,v 1.4 2004-04-06 16:07:42 barr Exp $
 * @since JIST1.0
 */

public class hello_loop implements JistAPI.Entity
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
  
    hello_loop hl = new hello_loop ();
    for (int i = 0; i < 10; i++)
    {
      JistAPI.sleep (1);
      hl.HelloWorld ("0");
    }
    hl.HelloWorld1("1"); // non-blocking
    hl.HelloWorld1("2"); // non-blocking
    hl.HelloWorld2("3"); // blocking
    hl.HelloWorld2("4"); // blocking
  }
  
  /** 
   * Initialize hello_loop entity.
   */
  public hello_loop()
  {
    System.out.println ("Creating new object hello_loop at t = " + JistAPI.getTime());
  }
  
  /**
   * A non-blocking hello event.
   *
   * @param msg message to display with hello
   */
  public void HelloWorld(String msg) 
  {
    System.out.println (msg + ": hello world at t = " + JistAPI.getTime());
  }    
  
  /**
   * non-blocking event that schedules 10 hellos.
   *
   * @param msg message to display with hello
   */
  public void HelloWorld1(String msg) 
  {
    for(int i = 0; i < 10; i++)
    {
      JistAPI.sleep(1);
      HelloWorld(msg);
    }
  }    
  
  /**
   * blocking event that schedules 10 hellos.
   *
   * @param msg message to display with hello
   * @throws JistAPI.Continuation never; blocking event
   */
  public void HelloWorld2(String msg) throws JistAPI.Continuation
  {
    for(int i = 0; i < 10; i++)
    {
      JistAPI.sleep(1);
      HelloWorld(msg);
    }
  }    
  
} // class: hello_loop

