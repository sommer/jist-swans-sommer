//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <MacInterface.java Tue 2004/04/06 11:32:16 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.mac;

import jist.swans.misc.Message;

import jist.runtime.JistAPI;

/**
 * Defines the interface of all Link layer entity implementations.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: MacInterface.java,v 1.19 2004-04-06 16:07:48 barr Exp $
 * @since SWANS1.0
 */

public interface MacInterface extends JistAPI.Proxiable
{

  //////////////////////////////////////////////////
  // from radio layer
  //

  /**
   * Update mac regarding new mode of its radio.
   *
   * @param mode new radio mode
   */
  void setRadioMode(byte mode);

  /**
   * Radio has locked onto a packet signal; mac may have a peek.
   *
   * @param msg packet currently in flight
   */
  void peek(Message msg);

  /**
   * Radio has received a packet for mac to process.
   *
   * @param msg packet received
   */
  void receive(Message msg);

  //////////////////////////////////////////////////
  // from network layer
  //

  /**
   * Network layer would like to send the following packet. Should be called
   * only after Mac has notified that it is wants a packet.
   *
   * @param msg packet to send
   * @param nextHop destination mac
   */
  void send(Message msg, MacAddress nextHop);


  //////////////////////////////////////////////////
  // 802.11 interface
  //

  /**
   * Extends the default Mac interface with 802_11 functions.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  public static interface Mac802_11 extends MacInterface
  {

    /**
     * Initiate a timer event. Note that only one timer event can be active at a
     * given time.
     *
     * @param delay timer duration
     * @param mode new mode
     */
    void startTimer(long delay, byte mode);

    /**
     * Process mac timeout.
     *
     * @param timerId timer identifier
     */
    void timeout(int timerId);

    /**
     * Collision free send sequence complete.
     *
     * @param backoff is a backoff required
     * @param delPacket is processing for this packet complete
     */
    void cfDone(boolean backoff, boolean delPacket);

  } // interface: 802_11

} // interface: MacInterface

