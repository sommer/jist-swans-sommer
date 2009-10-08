//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <AppJava.java Sun 2005/03/13 10:26:09 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app;

import jist.swans.trans.TransInterface;
import jist.swans.app.lang.SimtimeThread;
import jist.runtime.JistAPI;

import java.lang.reflect.*;

/**
 * Entity harness for standard Java applications.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: AppJava.java,v 1.11 2005-03-13 15:55:01 barr Exp $
 * @since SWANS1.0
 */
public class AppJava implements AppInterface, AppInterface.TcpApp, AppInterface.UdpApp, AppInterface.ThreadedApp
{

  public static interface Runnable extends java.lang.Runnable, JistAPI.Proxiable
  {
    void run() throws JistAPI.Continuation;
  }

  //////////////////////////////////////////////////
  // locals
  //

  /** self-referencing proxy entity. */
  private AppInterface self;
  /** application class. */
  private Class app;
  /** UDP entity. */
  private TransInterface.TransUdpInterface udp;
  /** TCP entity. */
  private TransInterface.TransTcpInterface tcp;
  /** current thread. */
  private SimtimeThread thread;

  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Create new Java application harness.
   *
   * @param app main class of Java application
   * @throws NoSuchMethodException unable to find main method in application class
   */
  public AppJava(Class app) throws NoSuchMethodException
  {
    this.app = app;
    findMain(app);
    self = (AppInterface)JistAPI.proxy(this, AppInterface.class);
  }

  /**
   * Create new Java application harness.
   *
   * @param app name of Java application class
   * @throws ClassNotFoundException unable to load application class
   * @throws NoSuchMethodException unable to find main method in application class
   */
  public AppJava(String app) throws ClassNotFoundException, NoSuchMethodException
  {
    this(Class.forName(app));
  }

  /**
   * Find main method in class.
   *
   * @param c main class
   * @return main method in class, if it exists
   * @throws NoSuchMethodException if class does not contain a main method
   */
  private Method findMain(Class c) throws NoSuchMethodException
  {
    return c.getDeclaredMethod("main", new Class[] { String[].class });
  }

  //////////////////////////////////////////////////
  // entity hookup
  //

  /**
   * Return self-referencing entity proxy.
   *
   * @return self-referencing entity proxy
   */
  public AppInterface getProxy()
  {
    return self;
  }

  /**
   * Set application UDP entity.
   *
   * @param udp udp entity
   */
  public void setUdpEntity(TransInterface.TransUdpInterface udp)
  {
    if(!JistAPI.isEntity(udp)) throw new IllegalArgumentException("expected entity");
    this.udp = udp;
  }

  /** {@inheritDoc} */
  public TransInterface.TransUdpInterface getUdpEntity()
  {
    if(udp==null) throw new NullPointerException("UDP entity has not been set in AppJava application context.");
    return udp;
  }

  /**
   * Set application TCP entity.
   *
   * @param tcp tcp entity
   */
  public void setTcpEntity(TransInterface.TransTcpInterface tcp)
  {
    if(!JistAPI.isEntity(tcp)) throw new IllegalArgumentException("expected entity");
    this.tcp = tcp;
  }

  /** {@inheritDoc} */
  public TransInterface.TransTcpInterface getTcpEntity()
  {
    if(tcp==null) throw new NullPointerException("TCP entity has not been set in AppJava application context.");
    return tcp;
  }

  /** {@inheritDoc} */
  public void setCurrentThread(SimtimeThread thread)
  {
    if(!JistAPI.isEntity(thread)) throw new IllegalArgumentException("expected entity");
    this.thread = thread;
  }

  /** {@inheritDoc} */
  public SimtimeThread getCurrentThread()
  {
    if(thread==null) throw new NullPointerException("current thread has not been set in AppJava application context.");
    return thread;
  }

  //////////////////////////////////////////////////
  // run
  //

  /** {@inheritDoc} */
  public void run(String[] args)
  {
    try
    {
      findMain(app).invoke(null, new Object[] { args });
    }
    catch(NoSuchMethodException e)
    {
      throw new RuntimeException("can not occur; checked in constructor");
    }
    catch(IllegalAccessException e)
    {
      throw new RuntimeException(e);
    }
    catch(InvocationTargetException e)
    {
      Throwable t = e.getTargetException();
      if(t instanceof Error) throw (Error)t;
      throw new RuntimeException(t);
    }
  }

  /** {@inheritDoc} */
  public void run()
  {
    run(null);
  }

} // class: AppJava
