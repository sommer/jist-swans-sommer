//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <MacDumb.java Sun 2005/03/13 11:06:58 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.mac;

import jist.swans.radio.RadioInterface;
import jist.swans.radio.RadioInfo;
import jist.swans.net.NetInterface;
import jist.swans.misc.Message;
import jist.swans.Constants;

import jist.runtime.JistAPI;

/**
 * A dumb, pass-through mac implementation.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: MacDumb.java,v 1.17 2005-03-13 16:11:55 barr Exp $
 * @since SWANS1.0
 */

public class MacDumb implements MacInterface
{
  //////////////////////////////////////////////////
  // messages
  //

  /**
   * MacDumbMessage is the packet sent out by the MacDumb mac.
   * <pre>
   *   src address:    size=6
   *   dst address:    size=6
   *   size:           size=2
   *   body:           0-65535
   * </pre>
   */
  private static class MacDumbMessage implements Message
  {
    /** fixed mac packet header length. */
    public static int HEADER_SIZE = 14;

    /** mac message source address. */
    private MacAddress src;
    /** mac message destination address. */
    private MacAddress dst;
    /** mac message payload. */
    private Message body;

    /**
     * Create new mac packet.
     *
     * @param src source mac address
     * @param dst destination mac address
     * @param body mac packet payload
     */
    public MacDumbMessage(MacAddress src, MacAddress dst, Message body)
    {
      this.src = src;
      this.dst = dst;
      this.body = body;
    }

    //////////////////////////////////////////////////
    // accessors
    //

    /**
     * Return mac message source.
     *
     * @return mac message source
     */
    public MacAddress getSrc()
    {
      return src;
    }

    /**
     * Return mac message destination.
     *
     * @return mac message destination
     */
    public MacAddress getDst()
    {
      return dst;
    }

    /**
     * Return mac message payload.
     *
     * @return mac message payload
     */
    public Message getPayload()
    {
      return body;
    }

    //////////////////////////////////////////////////
    // message interface 
    //

    /** {@inheritDoc} */
    public int getSize()
    {
      int size = body.getSize();
      if(size==Constants.ZERO_WIRE_SIZE)
      {
        return Constants.ZERO_WIRE_SIZE;
      }
      return HEADER_SIZE+size;
    }

    /** {@inheritDoc} */
    public void getBytes(byte[] msg, int offset)
    {
      throw new RuntimeException("todo: not implemented");
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return "macdumb(payload="+body+")";
    }
  } // class: MacDumbMessage


  //////////////////////////////////////////////////
  // locals
  //

  // entities
  /** radio entity. */
  private RadioInterface radioEntity;
  /** network entity. */
  private NetInterface netEntity;
  /** network interface identifier. */
  private byte netId;
  /** self-referencing proxy entity. */
  private final MacInterface self;

  // state
  /** radio mode: transmit, receive, etc. */
  private byte radioMode;
  /** local mac address. */
  private MacAddress localAddr;
  /** link bandwidth. */
  private final int bandwidth;
  /** whether in promiscuous mode. */
  private boolean promisc;

  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Create a new "dumb" mac entity. Does not perform any collision
   * avoidance or detection. Simply does not transmit (drops) packet
   * if it is current receiving.
   *
   * @param addr local mac address
   * @param radioInfo radio information
   */
  public MacDumb(MacAddress addr, RadioInfo radioInfo)
  {
    this.localAddr = addr;
    bandwidth = radioInfo.getShared().getBandwidth() / 8;
    radioMode = Constants.RADIO_MODE_IDLE;
    promisc = Constants.MAC_PROMISCUOUS_DEFAULT;
    self = (MacInterface)JistAPI.proxy(this, MacInterface.class);
  }

  //////////////////////////////////////////////////
  // accessors
  //

  /**
   * Set promiscuous mode (whether to pass all packets through).
   *
   * @param promisc promiscuous flag
   */
  public void setPromiscuous(boolean promisc)
  {
    this.promisc = promisc;
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
   * Hook up with the radio entity.
   *
   * @param radio radio entity
   */
  public void setRadioEntity(RadioInterface radio)
  {
    if(!JistAPI.isEntity(radio)) throw new IllegalArgumentException("expected entity");
    this.radioEntity = radio;
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

  /** {@inheritDoc} */
  public String toString()
  {
    return "MacDumb:"+localAddr;
  }

  //////////////////////////////////////////////////
  // MacInterface methods
  //

  /** {@inheritDoc} */
  public void setRadioMode(byte mode)
  {
    this.radioMode = mode;
  }

  /** {@inheritDoc} */
  public void peek(Message msg)
  {
  }

  /** {@inheritDoc} */
  public void receive(Message msg)
  {
    MacDumbMessage mdm = (MacDumbMessage)msg;
    JistAPI.sleep(Constants.LINK_DELAY);
    if(MacAddress.ANY.equals(mdm.getDst()))
    {
      if(netEntity!=null) netEntity.receive(mdm.getPayload(), mdm.getSrc(), netId, false);
    }
    else if(localAddr.equals(mdm.getDst()))
    {
      if(netEntity!=null) netEntity.receive(mdm.getPayload(), mdm.getSrc(), netId, false);
    }
    else if(promisc)
    {
      if(netEntity!=null) netEntity.receive(mdm.getPayload(), mdm.getSrc(), netId, true);
    }
  }

  /**
   * Compute packet transmission time at current bandwidth.
   *
   * @param msg packet to transmit
   * @return time to transmit given packet at current bandwidth
   */
  private long transmitTime(Message msg)
  {
    int size = msg.getSize();
    if(size==Constants.ZERO_WIRE_SIZE)
    {
      return Constants.EPSILON_DELAY;
    }
    return size * Constants.SECOND/bandwidth;
  }

  /** {@inheritDoc} */
  public void send(Message msg, MacAddress nextHop)
  {
    JistAPI.sleep(Constants.LINK_DELAY);
    if(radioMode==Constants.RADIO_MODE_IDLE)
    {
      MacDumbMessage mdm = new MacDumbMessage(this.localAddr, nextHop, msg);
      final long transmitTime = transmitTime(mdm);
      radioEntity.transmit(mdm, 0, transmitTime);
      JistAPI.sleep(transmitTime+Constants.EPSILON_DELAY);
    }
    if(netEntity!=null) netEntity.pump(netId);
  }
}
