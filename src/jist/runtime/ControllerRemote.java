//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <ControllerRemote.java Tue 2004/04/06 11:22:51 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.lang.reflect.*;
import java.rmi.*;

//////////////////////////////////////////////////
// Remote Jist Controller
//

/** 
 * Remote (RMI) interface that Controllers implement and use to interoperate.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: ControllerRemote.java,v 1.19 2004-04-06 16:07:43 barr Exp $
 * @since JIST1.0
 */

public interface ControllerRemote extends Remote
{

  /**
   * Start the Controller processing thread with given rewriter instance. The thread 
   * will die when there are no more events to process, or simulation time limit is 
   * reached, whichever comes first.
   *
   * @throws RemoteException rpc failure
   */
  void start() throws RemoteException;

  /**
   * End simulation after given time-step.
   *
   * @param time simulation time to end
   * @throws RemoteException rpc failure
   */
  void endAt(long time) throws RemoteException;

  /**
   * Set the simulation time units.
   *
   * @param ticks number of simulation quanta
   * @param name unit of time name
   * @throws RemoteException rpc failure
   */
  void setSimUnits(long ticks, String name) throws RemoteException;

  /**
   * Insert event in into event queue.
   * 
   * @param ev event to schedule
   * @throws RemoteException rpc failure
   */
  void addEvent(Event ev) throws RemoteException;

  /**
   * Insert event in into event queue (performance: avoids need to
   * get controller instance to create event - shaves off around 10%).
   * 
   * @param meth method of event to schedule
   * @param ref entity reference of event to schedule
   * @param params parameters of event to schedule
   * @throws RemoteException rpc failure
   */
  void addEvent(Method meth, EntityRef ref, Object[] params) throws RemoteException;

  /**
   * Insert event in into event queue (performance: avoids need to
   * get controller instance to create event - shaves off around 10%).
   * 
   * @param meth method of event to schedule
   * @param ref entity reference of event to schedule
   * @param params parameters of event to schedule
   * @param time event invocation time
   * @throws RemoteException rpc failure
   */
  void addEvent(Method meth, EntityRef ref, Object[] params, long time) throws RemoteException;

  /**
   * Return type of specific entity at this Controller.
   *
   * @param index local entity identifier
   * @return request entity object type
   * @throws RemoteException rpc failure
   */
  Class getEntityClass(int index) throws RemoteException;

  /**
   * Return toString of a specific entity at this Controller.
   *
   * @param index local entity identifier
   * @return toString of given entity
   * @throws RemoteException rpc failure
   */
  String toStringEntity(int index) throws RemoteException;

  /**
   * Emit message in JisT log.
   *
   * @param s string to log
   * @throws RemoteException rpc failure
   */
  void log(String s) throws RemoteException;

} // interface: ControllerRemote

