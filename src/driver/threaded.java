//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <threaded.java Sun 2005/03/13 10:54:30 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package driver;

import jist.runtime.JistAPI;
import jist.swans.app.AppJava;

/**
 * An example to show the transparent conversion of threads 
 * to simulation time under the AppJava.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: threaded.java,v 1.2 2005-03-13 15:55:01 barr Exp $
 * @since JIST1.0
 */
public class threaded
{

  public static class worker implements AppJava.Runnable
  {
    private int threadid, count;

    public worker(int id, int count)
    {
      this.threadid = id;
      this.count = count;
    }

    public void run()
    {
      for(int i=0; i<count; i++)
      {
        try
        {
          Thread.sleep(50);
        }
        catch(InterruptedException e) { }
        System.out.println("t="+JistAPI.getTime()+" run "+threadid+": "+i);
      }
    }
  } // class: worker

  public static void main(String args[])
  {
    Thread threads[] = new Thread[3];
    for(int id=0; id<threads.length; id++)
    {
      System.out.println("Spawning thread: "+id);
      // note: Java threads call runnable objects; 
      //       SWANS simulation time threads call runnable entities
      AppJava.Runnable worker = new worker(id, id+3);
      AppJava.Runnable workerEntity = (AppJava.Runnable)JistAPI.proxy(worker, AppJava.Runnable.class);
      threads[id] = new Thread(workerEntity);
    }
    for(int id=0; id<threads.length; id++) threads[id].start();
    try
    {
      for(int id=0; id<threads.length; id++)
      {
        System.out.println("t="+JistAPI.getTime()+" joining "+threads[id]);
        threads[id].join();
        System.out.println("t="+JistAPI.getTime()+" "+threads[id]+" done.");
      }
    }
    catch(InterruptedException e) { }
  }

} // class: threaded

