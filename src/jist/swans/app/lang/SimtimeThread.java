//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <SimtimeThread.java Sun 2005/03/13 11:13:23 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app.lang;

import jist.runtime.JistAPI;
import jist.runtime.Channel;
import jist.swans.Constants;
import jist.swans.app.AppInterface;
import jist.swans.app.AppJava;

import java.util.Vector;

/**
 * SWANS simulation time implementation of a Java thread. Note that this thread
 * implementation differs from the Java specification. 
 *
 * 1. It is cooperative (not pre-emptive), since we are optimizing for
 * throughput, not concurrency. Use the yield() command. Theoretically, these
 * can be weaved into the code automatically, but at a performance cost. Again,
 * throughput performance was considered to be more important than transparency
 * in this case. 
 *
 * 2. Implementation of daemon modes, thread priority, class loader, security
 * contexts, interrupts, suspend/resume/stop, thread groups, and subclassing of
 * the base thread class are not supported in the current implementation. They
 * can be added later, or provided in alternative simulation time thread
 * implementation.
 *
 * 3. Note that in this implementation each sleep/yield performs a blocking
 * sleep, thus incurring two blocking simulation events.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: SimtimeThread.java,v 1.5 2005-03-13 16:13:10 barr Exp $
 * @since SWANS1.0
 */
public class SimtimeThread implements ThreadInterface, JistAPI.Proxiable
{

  //////////////////////////////////////////////////
  // state
  //

  private char name[];
  private AppJava.Runnable target;
  private ThreadInterface thread;
  private Vector joined;

  //////////////////////////////////////////////////
  // subset of public java.lang.Thread
  //

  public SimtimeThread() { init(null, null, "Thread-" + nextThreadNum()); }
  public SimtimeThread(Runnable target) { init(null, target, "Thread-" + nextThreadNum()); }
  public SimtimeThread(ThreadGroup group, Runnable target) { init(group, target, "Thread-" + nextThreadNum()); }
  public SimtimeThread(String name) { init(null, null, name); }
  public SimtimeThread(ThreadGroup group, String name) { init(group, null, name); }
  public SimtimeThread(Runnable target, String name) { init(null, target, name); }
  public SimtimeThread(ThreadGroup group, Runnable target, String name) { init(group, target, name); }

  private void init(ThreadGroup g, Runnable target, String name)
  {
    if(g!=null) throw new IllegalArgumentException("thread groups not supported");
    if(target==null) throw new IllegalArgumentException("must provide AppJava.RunnableEntity thread target");
    if(!JistAPI.isEntity(target)) throw new IllegalArgumentException("target of simulation time thread must be an Entity");
    if(!(target instanceof AppJava.Runnable)) throw new IllegalArgumentException("target of simulation time thread must implement AppJava.Runnable");
    this.target=(AppJava.Runnable)target;
    this.name=name.toCharArray();
  }

  // root AppJava thread
  public static void InitializeApplicationContext(AppInterface.ThreadedApp app)
  {
    app.setCurrentThread(new SimtimeThread(-1));
  }
  public static SimtimeThread currentThread() { return RootThread; }
  private static SimtimeThread RootThread = new SimtimeThread(-1);
  private SimtimeThread(int i) { }

  // life cycle
  public boolean isAlive() { return thread!=null; }
  public void start()
  {
    if(isAlive()) throw new IllegalThreadStateException("thread already started.");
    thread = ((ThreadInterface)JistAPI.proxy(this, ThreadInterface.class));
    joined = new Vector();
    thread.ThreadRun();
  }
  public void ThreadRun() // intentionally non-blocking!
  { 
    target.run(); // blocking
    // done
    for(int i=0; i<joined.size(); i++) ((Channel)joined.elementAt(i)).sendNonBlock(null, true, true);
    joined = null;
    thread = null;
  }

  // name
  public void setName(String name) { this.name = name.toCharArray(); }
  public String getName() { return String.valueOf(name); }
  public String toString() { return "SimtimeThread[" + getName() + "]"; }

  // context switch
  private static void sleep0(long ticks) { JistAPI.sleepBlock(ticks); }
  public static void yield() { sleep0(Constants.EPSILON_DELAY); }
  public static void sleep(long millis) { sleep0(millis*Constants.MILLI_SECOND); }
  public static void sleep(long millis, int nanos) { sleep0(millis*Constants.MILLI_SECOND + nanos*Constants.NANO_SECOND); }

  // blocking
  public void join()
  {
    if(isAlive()) thread.ThreadJoin();
  }
  public void ThreadJoin() throws JistAPI.Continuation
  {
    Channel c = JistAPI.createChannel();
    joined.add(c);
    c.receive();
  }

  // thread ids
  private static int threadInitNumber;
  private static int nextThreadNum() { return threadInitNumber++; }

}
