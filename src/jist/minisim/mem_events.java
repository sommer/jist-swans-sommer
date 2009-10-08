//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <mem_events.java Tue 2004/04/06 11:28:38 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.minisim;

import java.io.*;
import jist.runtime.JistAPI;


/**
 * Measures memory overhead of events in JiST.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: mem_events.java,v 1.6 2004-04-06 16:07:42 barr Exp $
 */

public class mem_events implements JistAPI.Entity
{

  /**
   * Benchmark entry point: schedule events and measure memory consumption.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    // command-line arguments
    if(args.length<1)
    {
      System.out.println("usage: jist mem_events <events>");
      return;
    }
    int num = Integer.parseInt(args[0]);

    // create entity
    mem_events e = new mem_events();

    // schedule events
    for(int i=0; i<num; i++)
    {
      e.process();
    }

    // report internal memory use
    System.gc();
    System.out.println("freemem:  "+Runtime.getRuntime().freeMemory());
    System.out.println("maxmem:   "+Runtime.getRuntime().maxMemory());
    System.out.println("totalmem: "+Runtime.getRuntime().totalMemory());
    System.out.println("used:     "+
        (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));

    // report system memory numbers
    try
    {
      byte[] b = new byte[5000];
      FileInputStream fin = new FileInputStream("/proc/self/status");
      int readbytes = fin.read(b);
      System.out.write(b, 0, readbytes);
    }
    catch(IOException ex)
    {
      JistAPI.end();
    }
  }

  /** dummy event to schedule. */
  public void process()
  {
  }

} // class mem_events

