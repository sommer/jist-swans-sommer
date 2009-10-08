//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <entity.java Tue 2004/04/06 11:27:23 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.minisim;

import java.io.*;
import jist.runtime.JistAPI;

/**
 * Measures infrastructure memory overhead of entities in JiST.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: entity.java,v 1.8 2004-04-06 16:07:42 barr Exp $
 * @since JIST1.0
 */

public class entity implements JistAPI.Entity
{

  /**
   * Benchmark entry point: create entities and measure memory consumption.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    // command-line arguments
    if(args.length<1)
    {
      System.out.println("usage: jist entity <entities>");
      return;
    }
    int num = Integer.parseInt(args[0]);

    // create entities
    entity[] e = new entity[num];
    for(int i=0; i<num; i++)
    {
      e[i] = new entity();
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

} // class entity

