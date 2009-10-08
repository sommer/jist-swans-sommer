//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Channel.java Tue 2004/04/06 11:22:35 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.lang.reflect.Method;
import java.rmi.RemoteException;

/**
 * Implements a single-slot channel ala Communicating Sequential Processes
 * (CSP) by Hoare. Other synchronization primitives can be built atop this
 * structure, or directly using the same idea. This Channel implementation 
 * may not block the sender, and may drop excessive sends by setting
 * flags accordingly.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Channel.java,v 1.16 2004-04-06 16:07:43 barr Exp $
 * @since JIST1.0
 */

public class Channel extends Entity.Empty implements JistAPI.Entity
{

  //////////////////////////////////////////////////
  // entity method stubs
  //

  /**
   * Jist method stub for receive method.
   */
  public static Method _jistMethodStub_receive_28_29Ljava_2elang_2eObject_3b;

  /**
   * Jist method stub for send(Object) method.
   */
  public static Method _jistMethodStub_send_28Ljava_2elang_2eObject_3b_29V;

  /**
   * Jist method stub for send(Object) method.
   */
  public static Method _jistMethodStub_send_28Ljava_2elang_2eObject_3bZZ_29V;

  /**
   * Jist method stub for sendNonBlock(Object).
   */
  public static Method _jistMethodStub_sendNonBlock_28Ljava_2elang_2eObject_3b_29V;

  /**
   * Jist method stub for sendNonBlock(Object, boolean).
   */
  public static Method _jistMethodStub_sendNonBlock_28Ljava_2elang_2eObject_3bZZ_29V;

  static 
  {
    try
    {
      _jistMethodStub_receive_28_29Ljava_2elang_2eObject_3b =
        Channel.class.getDeclaredMethod(
            "receive",
            new Class[] { });
      _jistMethodStub_send_28Ljava_2elang_2eObject_3b_29V =
        Channel.class.getDeclaredMethod(
            "send",
            new Class[] { Object.class });
      _jistMethodStub_send_28Ljava_2elang_2eObject_3bZZ_29V =
        Channel.class.getDeclaredMethod(
            "send",
            new Class[] { Object.class, Boolean.TYPE, Boolean.TYPE });
      _jistMethodStub_sendNonBlock_28Ljava_2elang_2eObject_3b_29V = 
        Channel.class.getDeclaredMethod(
            "sendNonBlock",
            new Class[] { Object.class });
      _jistMethodStub_sendNonBlock_28Ljava_2elang_2eObject_3bZZ_29V =
        Channel.class.getDeclaredMethod(
            "sendNonBlock",
            new Class[] { Object.class, Boolean.TYPE, Boolean.TYPE });
    }
    catch(NoSuchMethodException e)
    {
      throw new JistException("should not happen", e);
    }
  }

  //////////////////////////////////////////////////
  // locals
  //

  /**
   * Continuation event of blocked sender.
   */ 
  private Event blockedSender;

  /**
   * Continuation event of blocked receiver.
   */
  private Event blockedReceiver;

  /**
   * Single data object passed through channel.
   */
  private Object data;

  /**
   * Whether channel is holding data.
   */
  private boolean hasData;

  /**
   * Create new Channel.
   * @see JistAPI
   */
  // intentionally prevent out-of-package initialization; use JistAPI
  Channel()
  {
  }

  //////////////////////////////////////////////////
  // channel functions
  //

  /**
   * Blocking send call implementation.
   *
   * @param data object to transmit
   * @param shouldDropIfFull whether over-sent channel should throw exception or silently drop
   * @param shouldDropIfNotReceiveWaiting whether send should be dropped if no receive is waiting
   * @param shouldBlockSender whether sender should be blocked
   */
  private void send(Object data, boolean shouldDropIfFull, 
      boolean shouldDropIfNotReceiveWaiting, boolean shouldBlockSender)
  {
    try
    {
      if(shouldDropIfNotReceiveWaiting && blockedReceiver==null) return;
      if(hasData)
      {
        if(shouldDropIfFull) return;
        throw new RuntimeException("only one send at a time");
      }
      Controller c = Controller.getActiveController();
      if(shouldBlockSender)
      {
        blockedSender = c.switchCaller(null);
      }
      setData(data);
      if(blockedReceiver!=null)
      {
        // schedule receive callback with result
        blockedReceiver.time = c.getSimulationTime();
        blockedReceiver.cont.result = clearData();
        blockedReceiver.ref.getController().addEvent(blockedReceiver);
        // return to sender
        if(shouldBlockSender)
        {
          c.switchCaller(blockedSender);
        }
        // clear
        blockedReceiver = null;
        blockedSender = null;
      }
    }
    catch(RemoteException e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
   * Blocking send call.
   *
   * @param data object to transmit
   * @param shouldDropIfFull whether over-sent channel should throw exception or silently drop
   * @param shouldDropIfNoReceiveWaiting whether send should be dropped if no receive is waiting
   * @throws JistAPI.Continuation never (merely a rewriter tag)
   */
  public void send(Object data, boolean shouldDropIfFull,
      boolean shouldDropIfNoReceiveWaiting) throws JistAPI.Continuation
  {
    send(data, shouldDropIfFull, shouldDropIfNoReceiveWaiting, true);
  }

  /**
   * Blocking non-dropping send call.
   *
   * @param data object to transmit
   * @throws JistAPI.Continuation never (merely a rewriter tag)
   */
  public void send(Object data) throws JistAPI.Continuation
  {
    send(data, false, false);
  }

  /**
   * Non-blocking send call.
   *
   * @param data object to transmit
   * @param shouldDropIfFull whether over-sent channel should throw exception or silently drop
   * @param shouldDropIfNoReceiveWaiting whether send should be dropped if no receive is waiting
   */
  public void sendNonBlock(Object data, boolean shouldDropIfFull,
      boolean shouldDropIfNoReceiveWaiting)
  {
    send(data, shouldDropIfFull, shouldDropIfNoReceiveWaiting, false);
  }

  /**
   * Non-blocking non-dropping send call.
   *
   * @param data object to transmit
   */
  public void sendNonBlock(Object data)
  {
    send(data, false, false);
  }

  /**
   * Blocking receive call.
   *
   * @return transmitted data
   * @throws JistAPI.Continuation never (merely a rewriter tag)
   */
  public Object receive() throws JistAPI.Continuation
  {
    
    try
    {
      if(blockedReceiver!=null)
      {
        throw new RuntimeException("only one receive at a time");
      }
      Controller c = Controller.getActiveController();
      blockedReceiver = c.switchCaller(null);
      if(hasData)
      {
        // schedule send callback
        if(blockedSender!=null)
        {
          blockedSender.time = c.getSimulationTime();
          blockedSender.ref.getController().addEvent(blockedSender);
        }
        // return to receiver
        c.switchCaller(blockedReceiver);
        // clear data and return result
        blockedReceiver = null;
        blockedSender = null;
        return clearData();
      }
      return null; // won't return anywhere
    }
    catch(RemoteException e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
   * Put data in the channel.
   *
   * @param data data to insert into the channel
   */
  private void setData(Object data)
  {
    this.data = data;
    this.hasData = true;
  }

  /**
   * Clear data from the channel.
   *
   * @return data in the channel, if any.
   */
  private Object clearData()
  {
    Object data = this.data;
    this.data = null;
    this.hasData = false;
    return data;
  }

} // class: Channel

