//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RadioInterface.java Tue 2004/04/13 18:22:52 barr glenlivet.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.radio;

import jist.swans.misc.Message;

import jist.runtime.JistAPI;

/**
 * Defines the interface of all Radio entity implementations.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: RadioInterface.java,v 1.16 2004-04-13 22:28:55 barr Exp $
 * @since SWANS1.0
 */

public interface RadioInterface extends JistAPI.Proxiable
{

  //////////////////////////////////////////////////
  // communication (mac)
  //

  /**
   * Start transmitting message. Puts radio into transmission mode and contacts
   * other radios that receive the signal. Called from mac entity.
   *
   * @param msg message object to transmit
   * @param delay time to the wire
   * @param duration time on the wire
   */
  void transmit(Message msg, long delay, long duration);

  /**
   * End message transmission. Putting the radio back into idle (or possibly
   * receiving) mode. Called from mac entity.
   */
  void endTransmit();

  //////////////////////////////////////////////////
  // communication (field)
  //

  /**
   * Start receiving message. Puts radio into receive or sensing mode depending
   * on the message power and the state of the radio. A radio that is currently
   * transmitting will ignore incoming messages. Called from field entity.
   *
   * @param msg incoming message
   * @param power signal strength of incoming message (units: mW)
   * @param duration time until end of transmission (units: simtime)
   */
  void receive(Message msg, Double power, Long duration);

  /**
   * End message reception. Puts the radio back into sensing or idle mode, and
   * sends the received message to upper layers for processing, if no error has
   * occurred during the reception. Called from field entity.
   *
   * @param power signal strength of incoming message (units: mW)
   */
  void endReceive(Double power);


  //////////////////////////////////////////////////
  // sleep (application)
  //

  /**
   * Put radio you in sleep/awake mode.
   *
   * @param sleep sleep/awake switch
   */
  void setSleepMode(boolean sleep);

} // interface: RadioInterface

