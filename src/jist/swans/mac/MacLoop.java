//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <MacLoop.java Tue 2004/04/06 11:32:19 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.mac;

import jist.runtime.JistAPI;

import jist.swans.net.NetInterface;
import jist.swans.misc.Message;
import jist.swans.Constants;

/**
 * A loopback mac implementation.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: MacLoop.java,v 1.5 2004-04-06 16:07:48 barr Exp $
 * @since SWANS1.0
 */

public class MacLoop implements MacInterface
{
  //////////////////////////////////////////////////
  // locals
  //

  // entities
  /** self-referencing proxy entity. */
  private final MacInterface self;
  /** network entity. */
  private NetInterface netEntity;
  /** network interface number. */
  private byte netId;

  /** 
   * Create new loopback interface.
   */
  public MacLoop()
  {
    self = (MacInterface)JistAPI.proxy(this, MacInterface.class);
  }

  //////////////////////////////////////////////////
  // entity hookup
  //

  /**
   * Hook up with the network entity.
   *
   * @param net network entity
   * @param netid network interface number
   */
  public void setNetEntity(NetInterface net, byte netid)
  {
    if(!JistAPI.isEntity(net)) throw new IllegalArgumentException("expected entity");
    this.netEntity = net;
    this.netId = netid;
  }

  /**
   * Return self-referencing proxy entity.
   *
   * @return proxy entity
   */
  public MacInterface getProxy()
  {
    return self;
  }

  //////////////////////////////////////////////////
  // MacInterface methods
  //

  /** {@inheritDoc} */
  public void peek(Message msg)
  {
    // no radio
  }

  /** {@inheritDoc} */
  public void setRadioMode(byte mode)
  {
    // no radio
  }

  /** {@inheritDoc} */
  public void send(Message msg, MacAddress nextHop)
  {
    JistAPI.sleep(Constants.LINK_DELAY);
    netEntity.receive(msg, MacAddress.LOOP, (byte)Constants.NET_INTERFACE_LOOPBACK, false);
    JistAPI.sleep(Constants.EPSILON_DELAY);
    netEntity.pump(netId);
  }

  /** {@inheritDoc} */
  public void receive(Message msg)
  {
    // performed in send
  }

}
