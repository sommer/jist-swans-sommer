//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <JistAPI.java Fri 2005/02/25 00:12:21 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.lang.reflect.Method;

/** 
 * Represents the JIST application interface to the JIST system. A simulation
 * application should not have any references into the jist.runtime package
 * other than to the JistAPI class.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: JistAPI.java,v 1.52 2005-02-25 05:12:28 barr Exp $
 * @since JIST1.0
 */

public class JistAPI
{
  /**
   * Tags a simulation object as an Timeless. Usually used for messages,
   * indicating that they do not change over time and need not be copied.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */
  public static interface Timeless
  {
  }

  /**
   * Tags a simulation object as an Entity. Invocations on entity objects
   * follow "simulation time" semantics.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */
  public static interface Entity extends Timeless
  {
  }

  /**
   * Tags an object as a target for a Proxy Entity.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */
  public static interface Proxiable extends Timeless
  {
  }

  /**
   * Tags a method as call-with-continuation (blocking).
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */
  public static class Continuation extends Error
  {
  }

  /**
   * Tags a method (explicitly) as continuable.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */
  public static class Continuable extends Error
  {
  }

  /**
   * Interface for custom rewrite pass.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */
  public static interface CustomRewriter
  {
    /**
     * Perform rewriter pass of BCEL JavaClass object.
     *
     * @param jcl BCEL JavaClass object to rewrite
     * @return rewritten/transformed BCEL JavaClass object
     */
    org.apache.bcel.classfile.JavaClass process(org.apache.bcel.classfile.JavaClass jcl) throws ClassNotFoundException;
  }

  /**
   * Do not rewrite tagged class.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0.3
   */
  public static interface DoNotRewrite
  {
  }

  /**
   * Interface for custom logger.
   */
  public static interface Logger
  {
    /**
     * Insert log entry.
     *
     * @param o object to log, often a string
     */
    void log(Object o);
  }

  /**
   * Entity reference for 'this'.
   */
  public static JistAPI.Entity THIS;

  /**
   * Simulation end time constant. Greater than any legal simulation time.
   */
  public static long END = Long.MAX_VALUE;

  /**
   * Return the current simulation time.
   *
   * @return current simulation time
   */
  public static long getTime()
  {
    if(Main.isRunning()) throw new RuntimeException(JistAPI_Impl.INVALID_CALL_MSG);
    return 0;
  }

  /**
   * Advance the current simulation time, but do not block the current method.
   *
   * @param i number of time steps to advance simulation time
   */
  public static void sleep(long i)
  {
    if(Main.isRunning()) throw new RuntimeException(JistAPI_Impl.INVALID_CALL_MSG);
  }

  /**
   * Advance the current simulation time, and block current method
   * (using simulation time continuations) until that time. Note 
   * that sleep() is far less expensive in the current implementation.
   * SleepBlock requires two events and to store the continuation... a
   * considerable overhead in comparison.
   *
   * @param i number of time steps to advance simulation time
   */
  public static void sleepBlock(long i) throws JistAPI.Continuable
  {
    if(Main.isRunning()) throw new RuntimeException(JistAPI_Impl.INVALID_CALL_MSG);
  }

  /**
   * Get Entity reference for given Entity.
   *
   * @param entity entity to convert into entity reference
   * @return entity reference of given entity
   */
  public static JistAPI.Entity ref(JistAPI.Entity entity)
  {
    if(Main.isRunning()) throw new RuntimeException(JistAPI_Impl.INVALID_CALL_MSG);
    return null;
  }

  /**
   * End simulation at current time.
   */
  public static void end()
  {
    if(Main.isRunning()) JistAPI_Impl.end();
  }

  /**
   * End simulation at given (absolute) time.
   *
   * @param time simulation time at which to end simulation
   */
  public static void endAt(long time)
  {
    if(Main.isRunning()) JistAPI_Impl.endAt(time);
  }

  /**
   * Invoke a static method at given simulation time.
   *
   * @param meth method to invoke
   * @param params parameters of invocation
   * @param time simulation invocation time
   */
  public static void callStaticAt(Method meth, Object[] params, long time)
  {
    if(Main.isRunning()) throw new RuntimeException(JistAPI_Impl.INVALID_CALL_MSG);
  }

  /**
   * Call a runnable object at given simulation time.
   *
   * @param r runnable object
   * @param time invocation time
   */
  public static void runAt(Runnable r, long time)
  {
    if(Main.isRunning()) throw new RuntimeException(JistAPI_Impl.INVALID_CALL_MSG);
  }

  /**
   * Create proxy entity with given interface.
   *
   * @param proxyTarget target object of proxy entity
   * @param proxyInterface public interface of proxy entity
   * @return proxy entity object
   */
  public static Object proxy(Object proxyTarget, Class proxyInterface)
  {
    if(Main.isRunning()) throw new RuntimeException(JistAPI_Impl.INVALID_CALL_MSG);
    return proxyTarget;
  }

  /**
   * Create a entity proxy with multiple given interfaces.
   *
   * @param proxyTarget target object of proxy entity
   * @param proxyInterface public interfaces of proxy entity
   * @return proxy entity object
   */
  public static Object proxyMany(Object proxyTarget, Class[] proxyInterface)
  {
    if(Main.isRunning()) throw new RuntimeException(JistAPI_Impl.INVALID_CALL_MSG);
    return proxyTarget;
  }

  /**
   * Create a new Channel entity.
   *
   * @return new Channel entity
   */
  public static Channel createChannel()
  {
    if(Main.isRunning()) throw new RuntimeException(JistAPI_Impl.INVALID_CALL_MSG);
    return new Channel();
  }

  /**
   * Install a new rewriting step at top of chain.
   *
   * @param rewrite rewrite traversal object
   */
  public static void installRewrite(CustomRewriter rewrite)
  {
    if(Main.isRunning()) throw new RuntimeException(JistAPI_Impl.INVALID_CALL_MSG);
  }

  /** Java application constant. */
  public static final int RUN_CLASS = 0;
  /** Beanshell script constant. */
  public static final int RUN_BSH   = 1;
  /** Jython script constant. */
  public static final int RUN_JPY   = 2;

  /**
   * Bootstrap a new program or script.
   *
   * @param type bootstrap type
   * @param name script or application name
   * @param args command-line arguments
   * @param properties values passed to the bootstrap agent
   */
  public static void run(int type, String name, String[] args, Object properties)
  {
    if(Main.isRunning()) throw new RuntimeException(JistAPI_Impl.INVALID_CALL_MSG);
  }

  /**
   * Set simulation time units.
   *
   * @param ticks number of simulation ticks per unit
   * @param name time unit of measure name
   */
  public static void setSimUnits(long ticks, String name)
  {
    if(Main.isRunning()) JistAPI_Impl.setSimUnits(ticks, name);
  }

  /**
   * Return time string in simulation time units.
   *
   * @return time string in simulation time units
   */
  public static String getTimeString()
  {
    if(Main.isRunning()) throw new RuntimeException(JistAPI_Impl.INVALID_CALL_MSG);
    return null;
  }

  /**
   * Set the simulation logger.
   *
   * @param logger logger instance
   */
  public static void setLog(JistAPI.Logger logger)
  {
    if(Main.isRunning()) throw new RuntimeException(JistAPI_Impl.INVALID_CALL_MSG);
  }

  /**
   * Emit string to simulation log.
   *
   * @param s log information
   */
  public static void log(String s)
  {
    if(Main.isRunning()) throw new RuntimeException(JistAPI_Impl.INVALID_CALL_MSG);
  }

  /**
   * Determine whether given reference is an entity reference.
   *
   * @param o object reference to test
   * @return whether given reference is an entity reference
   */
  public static boolean isEntity(Object o)
  {
    if(Main.isRunning()) throw new RuntimeException(JistAPI_Impl.INVALID_CALL_MSG);
    return false;
  }

  /**
   * Determine the String representation of an object or entity.
   *
   * @param o object to stringify
   * @return string representation of given object
   */
  public static String toString(Object o)
  {
    if(Main.isRunning()) throw new RuntimeException(JistAPI_Impl.INVALID_CALL_MSG);
    return o.toString();
  }

} // interface: JistAPI

