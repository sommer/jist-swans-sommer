//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <TransUdp.java Sun 2005/03/13 11:03:15 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.trans;

import jist.swans.mac.MacAddress;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.misc.Message;
import jist.swans.misc.Pickle;
import jist.swans.Constants;

import jist.runtime.JistAPI;

import org.apache.log4j.*;

import java.util.HashMap;

/**
 * UDP implementation. Does not perform fragmentation.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: TransUdp.java,v 1.30 2005-03-13 16:11:56 barr Exp $
 * @since SWANS1.0
 */
public class TransUdp implements TransInterface.TransUdpInterface
{
  /**
   * UDP logger.
   */
  public static final Logger log = Logger.getLogger(TransUdp.class.getName());


  /**
   * UDP packet structure.
   * <code>
   *   UDP packet: (size = 8 + payload)
   *     src port         size: 2
   *     dst port         size: 2
   *     payload length   size: 2
   *     checksum         size: 2
   *     payload          size: variable
   * </code>
   */
  public static class UdpMessage extends TransMessage
  {
    //////////////////////////////////////////////////
    // constants
    //

    /**
     * UDP packet header size.
     */
    public static final int HEADER_SIZE = 8;

    //////////////////////////////////////////////////
    // locals
    //

    /**
     * packet source address.
     */
    private int srcPort; 

    /**
     * packet destination address.
     */
    private int dstPort;

    /**
     * packet payload.
     */
    private Message payload;

    //////////////////////////////////////////////////
    // initialization
    //

    /**
     * Create an new UDP packet.
     *
     * @param srcPort message source port
     * @param dstPort message destination port
     * @param payload message payload
     */
    public UdpMessage(int srcPort, int dstPort, Message payload)
    {
      this.srcPort = srcPort;
      this.dstPort = dstPort;
      this.payload = payload;
    }

    //////////////////////////////////////////////////
    // accessors
    //

    /**
     * Return packet source port.
     *
     * @return packet source port
     */
    public int getSrcPort()
    {
      return srcPort;
    }

    /**
     * Return packet destination port.
     *
     * @return packet destination port
     */
    public int getDstPort()
    {
      return dstPort;
    }

    /**
     * Return packet payload.
     *
     * @return packet payload
     */
    public Message getPayload()
    {
      return payload;
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return "udp(srcPort="+srcPort+" dstPort="+dstPort+" data="+payload+")";
    }

    /** {@inheritDoc} */
    public int getSize()
    {
      return HEADER_SIZE + payload.getSize();
    }

    /** {@inheritDoc} */
    public void getBytes(byte[] msg, int offset)
    {
      // source port (unsigned short)
      Pickle.ushortToArray(srcPort, msg, offset);
      // destination port (unsigned short)
      Pickle.ushortToArray(dstPort, msg, offset+2);
      // payload length (unsigned short)
      Pickle.ushortToArray(payload.getSize(), msg, offset+4);
      // checksum (unsigned short)
      // don't compute!
      // todo: payload length
      // payload
      payload.getBytes(msg, offset+8);
    }

  } // class: UdpMessage


  //////////////////////////////////////////////////
  // locals
  //

  /** network layer entity. */
  private NetInterface netEntity;
  /** self-referencing proxy entity. */
  private TransInterface.TransUdpInterface self;

  /** socket handlers. */
  private HashMap handlers; // Integer(port) -> SocketHandler

  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Initialize UDP transport implementation.
   */
  public TransUdp()
  {
    self = (TransInterface.TransUdpInterface)JistAPI.proxy(
        this, TransInterface.TransUdpInterface.class);
    handlers = new HashMap();
  }

  //////////////////////////////////////////////////
  // hookup entities
  //

  /**
   * Return self-referencing proxy entity.
   *
   * @return self-referencing proxy entity
   */
  public TransInterface.TransUdpInterface getProxy()
  {
    return self;
  }

  /**
   * Set network layer entity.
   *
   * @param netEntity network layer entity
   */
  public void setNetEntity(NetInterface netEntity)
  {
    if(!JistAPI.isEntity(netEntity)) throw new IllegalArgumentException("expected entity");
    this.netEntity = netEntity;
  }

  /** {@inheritDoc} */
  public void addSocketHandler(int port, SocketHandler handler)
  {
    if(log.isInfoEnabled())
    {
      log.info("add socket handler: "+port);
    }
    handlers.put(new Integer(port), handler);
  }

  /** {@inheritDoc} */
  public void delSocketHandler(int port)
  {
    if(log.isInfoEnabled())
    {
      log.info("delete socket handler: "+port);
    }
    handlers.remove(new Integer(port));
  }

  //////////////////////////////////////////////////
  // send/receive
  //

  /** {@inheritDoc} */
  public void send(Message msg, NetAddress dst, int dstPort, 
      int srcPort, byte priority)
  {
    UdpMessage udp = new UdpMessage(srcPort, dstPort, msg);
    JistAPI.sleep(Constants.TRANS_DELAY);
    if(log.isInfoEnabled())
    {
      log.info("send: t="+JistAPI.getTime()+" dst="+dst+":"+dstPort+" srcPort="+srcPort+" data="+msg);
    }
    netEntity.send(udp, dst, Constants.NET_PROTOCOL_UDP, 
        priority, Constants.TTL_DEFAULT);
  }

  /** {@inheritDoc} */
  public void receive(Message msg, NetAddress src, MacAddress lastHop, 
      byte macId, NetAddress dst, byte priority, byte ttl)
  {
    UdpMessage udp = (UdpMessage)msg;
    int dstPort = udp.getDstPort();
    SocketHandler handler = 
      (SocketHandler)handlers.get(new Integer(dstPort));
    if(handler==null) return;
    JistAPI.sleep(Constants.TRANS_DELAY);
    msg = udp.getPayload();
    int srcPort = udp.getSrcPort();
    if(log.isInfoEnabled())
    {
      log.info("receive: t="+JistAPI.getTime()+" src="+src+":"+srcPort+" dstPort="+dstPort+" from="+lastHop+" data="+msg);
    }
    handler.receive(msg, src, srcPort);
  }

} // class: TransUdp

