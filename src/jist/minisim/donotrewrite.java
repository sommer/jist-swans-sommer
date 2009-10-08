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
 * Hello World of simulations, but with rewritting DISABLED. In other words,
 * none of the JiST API functions will work. They will throw exceptions.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: donotrewrite.java,v 1.2 2004-12-02 16:08:54 barr Exp $
 * @since JIST1.0
 */
public class donotrewrite implements JistAPI.Entity, JistAPI.DoNotRewrite
{
  public void myEvent()
  {
    ///////////////////////////////////////////////////////
    // this line would throw an exception, because the 
    // rewriter has been turned off for this class!
    ///////////////////////////////////////////////////////
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

  public static void main(String[] args)
  {
    System.out.println("starting simulation.");
    donotrewrite h = new donotrewrite();
    ///////////////////////////////////////////////////////
    // this line will not actually schedule an event...
    // it is a regular function call, because the rewriter
    // has been turned off for this class!
    ///////////////////////////////////////////////////////
    h.myEvent();
  }

}

