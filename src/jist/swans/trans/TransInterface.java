//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <TransInterface.java Tue 2004/04/06 11:37:58 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.trans;

import jist.swans.net.NetInterface;
import jist.swans.net.NetAddress;
import jist.swans.misc.Message;

import jist.runtime.JistAPI;

/**
 * Defines the interface of all Transport layer entity implementations.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: TransInterface.java,v 1.22 2004-04-06 16:07:51 barr Exp $
 * @since SWANS1.0
 */
public interface TransInterface extends JistAPI.Proxiable, NetInterface.NetHandler
{

  /**
   * Send message (from APPLICATON).
   *
   * @param msg packet payload (usually from application layer)
   * @param dst packet destination address
   * @param dstPort packet destination port
   * @param srcPort packet source port
   * @param priority packet priority
   */
  void send(Message msg, NetAddress dst, int dstPort, int srcPort, byte priority);

  /**
   * Register socket handler.
   *
   * @param port bound socket port
   * @param socketCallback callback handler
   * @throws JistAPI.Continuation never; blocking event
   */
  void addSocketHandler(int port, SocketHandler socketCallback) throws JistAPI.Continuation;

  /**
   * Unregister socket handler.
   *
   * @param port bound socket port
   * @throws JistAPI.Continuation never; blocking event
   */
  void delSocketHandler(int port) throws JistAPI.Continuation;

  /**
   * Super-class of all transport layer packets.
   */
  public abstract static class TransMessage implements Message
  {
  }

  /**
   * Socket callback handler.
   */
  public interface SocketHandler
  {

    /**
     * Receive transport layer packet.
     *
     * @param msg incoming packet
     * @param src packet source address
     * @param srcPort packet source port
     * @throws JistAPI.Continuation never; blocking event
     */
    void receive(Message msg, NetAddress src, int srcPort) throws JistAPI.Continuation;

    /**
     * Defines the interface for TCP socket callback.
     */
    public interface TcpHandler extends SocketHandler
    {
    }
  }

  /**
   * Defines the Transport sub-interface for UDP entities.
   */
  public static interface TransUdpInterface extends TransInterface
  {
  }

  /**
   * Defines the Transport sub-interface for TCP entities.
   */
  public static interface TransTcpInterface extends TransInterface
  {
    /**
     * Return whether socket bound to port.
     *
     * @param port port to check
     * @return whether socket bound to port
     * @throws JistAPI.Continuation never; blocking event
     */
    boolean checkSocketHandler(int port) throws JistAPI.Continuation;
  }

} // interfacE: TransInterface

