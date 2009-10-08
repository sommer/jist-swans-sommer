//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <JistAPI_Impl.java Thu 2005/02/24 23:11:11 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;

/** 
 * The concrete implementation of the JistAPI application interface stub.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: JistAPI_Impl.java,v 1.44 2005-02-25 04:43:58 barr Exp $
 * @since JIST1.0
 */

public final class JistAPI_Impl extends JistAPI
{
  /** Invalid system call error message. */
  public static final String INVALID_CALL_MSG = "invalid system call! is the JiST rewriter enabled?";

  //////////////////////////////////////////////////
  // Method stubs
  //

  /**
   * Method stub field for retrieving simulation time.
   */
  public static Method method_getTime;

  /**
   * Method stub field for advancing simulation time.
   */
  public static Method method_sleep;

  /**
   * Method stub field for advancing simulation time (with blocking).
   */
  public static Method method_sleepBlock;

  /**
   * API Method stub field for advancing simulation time (with blocking).
   */
  public static Method method_sleepBlockAPI;

  /**
   * Method stub field for converting an entity into an entity reference.
   */
  public static Method method_ref;

  /**
   * Method stub field for ending simulation now.
   */
  public static Method method_end;

  /**
   * Method stub field for ending simulation at given time.
   */
  public static Method method_endAt;

  /**
   * Method stub field for invoking a static method at some simulation time.
   */
  public static Method method_callStaticAt;

  /**
   * Method stub field for running a Runnable object at some simulation time.
   */
  public static Method method_runAt;

  /**
   * Method stub field for creating proxy entity.
   */
  public static Method method_proxy;

  /**
   * Method stub field for creating proxy entity with multiple interfaces.
   */
  public static Method method_proxyMany;

  /**
   * Method stub field for creating blocking channel.
   */
  public static Method method_createChannel;

  /**
   * Method stub field for installing a new rewriting step at top of chain.
   */
  public static Method method_installRewrite;

  /**
   * Method stub field for bootstrapping a new simulation.
   */
  public static Method method_run;

  /**
   * Method stub field for setting simulation time units.
   */
  public static Method method_setSimUnits;

  /**
   * Method stub field for getting simulation time string.
   */
  public static Method method_getTimeString;

  /**
   * Method stub field for setting logger.
   */
  public static Method method_setLog;

  /**
   * Method stub field for logging.
   */
  public static Method method_log;

  /**
   * Method stub field for determining whether a reference is an entity reference.
   */
  public static Method method_isEntity;

  /**
   * Method stub field for toString method.
   */
  public static Method method_toString;

  static
  {
    try
    {
      method_getTime = JistAPI_Impl.class.getDeclaredMethod(
          "getTime", 
          new Class[] { });
      method_sleep = JistAPI_Impl.class.getDeclaredMethod(
          "sleep",
          new Class[] { Long.TYPE });
      method_sleepBlock = JistAPI_Impl.class.getDeclaredMethod(
          "sleepBlock",
          new Class[] { Long.TYPE });
      method_sleepBlockAPI = JistAPI.class.getDeclaredMethod(
          "sleepBlock",
          new Class[] { Long.TYPE });
      method_ref = JistAPI_Impl.class.getDeclaredMethod(
          "ref",
          new Class[] { Object.class });
      method_end = JistAPI_Impl.class.getDeclaredMethod(
          "end",
          new Class[] { });
      method_endAt = JistAPI_Impl.class.getDeclaredMethod(
          "endAt",
          new Class[] { Long.TYPE });
      method_callStaticAt = JistAPI_Impl.class.getDeclaredMethod(
          "callStaticAt",
          new Class[] { Method.class, Object[].class, Long.TYPE });
      method_runAt = JistAPI_Impl.class.getDeclaredMethod(
          "runAt",
          new Class[] { Runnable.class, Long.TYPE });
      method_proxy = JistAPI_Impl.class.getDeclaredMethod(
          "proxy",
          new Class[] { Object.class, Class.class });
      method_proxyMany = JistAPI_Impl.class.getDeclaredMethod(
          "proxyMany",
          new Class[] { Object.class, Class[].class });
      method_createChannel = JistAPI_Impl.class.getDeclaredMethod(
          "createChannelImpl",
          new Class[] { });
      method_installRewrite = JistAPI_Impl.class.getDeclaredMethod(
          "installRewrite",
          new Class[] { ClassTraversal.Visitor.class });
      method_run = JistAPI_Impl.class.getDeclaredMethod(
          "run",
          new Class[] { Integer.TYPE, String.class, String[].class, Object.class });
      method_setSimUnits = JistAPI_Impl.class.getDeclaredMethod(
          "setSimUnits",
          new Class[] { Long.TYPE, String.class });
      method_getTimeString = JistAPI_Impl.class.getDeclaredMethod(
          "getTimeString",
          new Class[] { });
      method_setLog = JistAPI_Impl.class.getDeclaredMethod(
          "setLog",
          new Class[] { JistAPI.Logger.class });
      method_log = JistAPI_Impl.class.getDeclaredMethod(
          "log",
          new Class[] { String.class });
      method_isEntity = JistAPI_Impl.class.getDeclaredMethod(
          "isEntity",
          new Class[] { Object.class });
      method_toString = JistAPI_Impl.class.getDeclaredMethod(
          "toString",
          new Class[] { Object.class });
    }
    catch(NoSuchMethodException e)
    {
      throw new JistException("should not happen", e);
    }
  }

  //////////////////////////////////////////////////
  // JistAPI methods
  //

  /** @see JistAPI */
  public static long getTime()
  {
    return Controller.getActiveController().getSimulationTime();
  }

  /** @see JistAPI */
  public static void sleep(long i)
  {
    Controller.getActiveController().advanceSimulationTime(i);
  }

  /** @see JistAPI */
  public static void sleepBlock(long i) throws JistAPI.Continuable
  {
    try
    {
      Controller.entityInvocationCont(
          BlockingSleep._jistMethodStub_sleep_28J_29V, 
          Controller.getActiveController().entityBlockingSleep._jistMethod_Get__ref(),
          new Object[] { new Long(i) });
    }
    catch(Throwable e)
    {
      throw new JistException("unexpected exception during blocking sleep", e);
    }
  }

  /** @see JistAPI */
  public static Object ref(Object o)
  {
    return o instanceof jist.runtime.Entity
      ? ((jist.runtime.Entity)o)._jistMethod_Get__ref() 
      : o; // will always be EntityRef, we save the cast
  }

  /** @see JistAPI */
  public static void end()
  {
    endAt(getTime()+1);
  }

  /** @see JistAPI */
  public static void endAt(long time)
  {
    Controller.getActiveController().endAt(time);
  }

  /** @see JistAPI */
  public static void callStaticAt(Method meth, Object[] params, long time)
  {
    if(!Modifier.isStatic(meth.getModifiers()))
    {
      throw new RuntimeException("method must be static");
    }
    if(!Modifier.isPublic(meth.getModifiers()))
    {
      throw new RuntimeException("method must be public");
    }
    Controller c = Controller.getActiveController();
    c.addEvent(meth, c.getStaticEntityRef(), params, time);
  }

  /** @see JistAPI */
  public static void runAt(Runnable r, long time)
  {
    callStaticAt(Bootstrap.StaticRunnableCaller.method_run, new Object[] { r }, time);
  }

  /** @see JistAPI */
  public static Object proxy(Object proxyTarget, Class proxyInterface)
  {
    return proxyMany(proxyTarget, new Class[] { proxyInterface });
  }

  /** @see JistAPI */
  public static Object proxyMany(Object proxyTarget, Class[] proxyInterface)
  {
    if(proxyTarget==null) throw new NullPointerException();
    return ProxyEntity.create(proxyTarget, proxyInterface);
  }

  /** @see JistAPI */
  public static Channel createChannel()
  {
    return null;
  }

  /**
   * Actual implementation of <code>createChannel</code> method;
   * returns new Channel Entity reference.
   *
   * @return entity reference of new Channel Entity.
   */
  public static EntityRef createChannelImpl()
  {
    return (new Channel())._jistMethod_Get__ref();
  }

  /** @see JistAPI */
  public static void installRewrite(ClassTraversal.Visitor rewrite)
  {
    Rewriter r = (Rewriter)Thread.currentThread().getContextClassLoader();
    r.installPreRewriteTraversal(rewrite);
  }

  /** @see JistAPI */
  public static void run(int type, String name, String[] args, Object properties)
  {
    try
    {
      Bootstrap.create(type, Controller.getActiveController(), name, args, properties);
    }
    catch(RemoteException e)
    {
      throw new JistException("distributed simulation failure", e);
    }
  }

  /** @see JistAPI */
  public static void setSimUnits(long ticks, String name)
  {
    Controller.getActiveController().setSimUnits(ticks, name);
  }

  /** @see JistAPI */
  public static String getTimeString()
  {
    return Controller.getActiveController().getSimulationTimeString();
  }

  /** @see JistAPI */
  public static void setLog(JistAPI.Logger logger)
  {
    Controller.getActiveController().setLog(logger);
  }

  /** @see JistAPI */
  public static void log(String s)
  {
    Controller.getActiveController().log(s);
  }

  /** @see JistAPI */
  public static boolean isEntity(Object o)
  {
    try
    {
      return o instanceof EntityRef ||
        (Proxy.isProxyClass(o.getClass()) && Proxy.getInvocationHandler(o) instanceof EntityRef);
    }
    catch(NullPointerException e)
    {
      return false;
    }
  }

  /** @see JistAPI */
  public static String toString(Object o)
  {
    try
    {
      return Controller.toString(o);
    }
    catch(RemoteException e)
    {
      throw new RuntimeException("unable to execute toString", e);
    }
  }

} // class: JistAPI_Impl

