//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RouteZrp.java Sun 2005/03/13 11:07:19 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.route;

import jist.swans.mac.MacAddress;
import jist.swans.net.NetMessage;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.misc.Message;
import jist.swans.misc.Timer;
import jist.swans.Constants;

import jist.runtime.JistAPI;

import org.apache.log4j.Logger;

/** 
 * Zone Routing Protocol Implementation.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: RouteZrp.java,v 1.41 2005-03-13 16:11:55 barr Exp $
 * @since SWANS1.0
 */

public class RouteZrp implements RouteInterface.Zrp
{
  /** logger for ZRP events. */
  public static final Logger logZRP = Logger.getLogger(RouteZrp.class.getName());

  //////////////////////////////////////////////////
  // data structures
  //


  /**
   * Data structure to collect ZRP statistics.
   */
  public static class ZrpStats
  {
    /** zrp send counters. */
    public final ZrpPacketStats send = new ZrpPacketStats();
    /** zrp receive counters. */
    public final ZrpPacketStats recv = new ZrpPacketStats();

    /** Reset statistics. */
    public void clear()
    {
      send.clear();
      recv.clear();
    }
  }

  /**
   * Data structure to collect ZRP packet statistics.
   */
  public static class ZrpPacketStats
  {
    /** zrp counter. */
    public long ndpPackets;
    /** zrp counter. */
    public long ndpBytes;
    /** zrp counter. */
    public long iarpPackets;
    /** zrp counter. */
    public long iarpBytes;
    /** zrp counter. */
    public long brpPackets;
    /** zrp counter. */
    public long brpBytes;
    /** zrp counter. */
    public long ierpPackets;
    /** zrp counter. */
    public long ierpBytes;
    /** zrp counter. */
    public long dataPackets;
    /** zrp counter. */
    public long dataBytes;

    /** Reset statistics. */
    public void clear()
    {
      ndpPackets = 0;
      ndpBytes = 0;
      iarpPackets = 0;
      iarpBytes = 0;
      brpPackets = 0;
      brpBytes = 0;
      ierpPackets = 0;
      ierpBytes = 0;
      dataPackets = 0;
      dataBytes = 0;
    }
  }

  //////////////////////////////////////////////////
  // helper functions
  //

  /**
   * Whether first sequence number is after the second (with wrap-around).
   *
   * @param seq1 first sequence number
   * @param seq2 second sequence number
   * @return whether first sequence number is after the second (with wrap-around)
   */
  public static boolean seqAfter(int seq1, int seq2)
  {
    if(seq1<0 && seq1>Short.MAX_VALUE) throw new IllegalArgumentException("invalid seq number: seq1");
    if(seq2<0 && seq2>Short.MAX_VALUE) throw new IllegalArgumentException("invalid seq number: seq2");
    final int WINDOW = 10;
    return (seq1>seq2 && seq1-seq2<WINDOW) ||
      (seq1+Short.MAX_VALUE>seq2 && seq1+Short.MAX_VALUE-seq2<WINDOW);
  }

  /**
   * Replace end of route with route to destination.
   *
   * @param route route in format: (src ... partial route ... dst)
   * @param finder node that knows route to destination
   * @param remainder route to destination from finder
   * @return complete route from source to destination
   */
  public static NetAddress[] replaceDest(NetAddress[] route, NetAddress finder, NetAddress[] remainder)
  {
    NetAddress[] route2 = new NetAddress[route.length+remainder.length];
    System.arraycopy(route, 0, route2, 0, route.length-1);
    route2[route.length-1] = finder;
    System.arraycopy(remainder, 0, route2, route.length, remainder.length);
    return route2;
  }

  //////////////////////////////////////////////////
  // locals
  //

  // entity hookup
  /** network entity. */
  private NetInterface netEntity;
  /** self-referencing proxy entity. */
  private RouteInterface.Zrp self;

  // sub-protocols
  /** reference to NDP sub-protocol. */
  private RouteInterface.Zrp.Ndp  ndp;
  /** reference to IARP sub-protocol. */
  private RouteInterface.Zrp.Iarp iarp;
  /** reference to BRP sub-protocol. */
  private RouteInterface.Zrp.Brp brp;
  /** reference to IERP sub-protocol. */
  private RouteInterface.Zrp.Ierp ierp;

  // zrp
  /** zone radius. */
  private byte radius;
  /** local network address. */
  private NetAddress localAddr;

  // statistics
  /** statistics accumulator. */
  private ZrpStats stats;

  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Create and initialize new ZRP instance.
   *
   * @param localAddr local node address
   * @param radius zone radius
   */
  public RouteZrp(NetAddress localAddr, int radius)
  {
    init(localAddr, radius);
  }

  /**
   * Create and initialize new ZRP instance.
   *
   * @param localAddr local node address
   * @param config configuration string
   */
  public RouteZrp(NetAddress localAddr, String config)
  {
    String[] data = config.split("x|,");
    byte radius = Byte.parseByte(data[0]);
    init(localAddr, radius);
  }

  /**
   * Initialize new ZRP instance.
   *
   * @param localAddr local node address
   * @param radius zone radius
   */
  private void init(NetAddress localAddr, int radius)
  {
    if(radius<1 || radius>Byte.MAX_VALUE) throw new IllegalArgumentException("invalid radius value");
    this.radius = (byte)radius;
    this.localAddr = localAddr;
    // self
    self = (RouteInterface.Zrp)JistAPI.proxy(this, RouteInterface.Zrp.class);
  }

  //////////////////////////////////////////////////
  // entity hookup
  //

  /**
   * Set network entity. Also starts timers. Should be called only once.
   *
   * @param netEntity network entity
   */
  public void setNetEntity(NetInterface netEntity)
  {
    if(!JistAPI.isEntity(netEntity)) throw new IllegalArgumentException("expected entity");
    if(this.netEntity!=null) throw new IllegalStateException("net entity already set");
    this.netEntity = netEntity;
  }

  /**
   * Return self-referencing proxy entity.
   *
   * @return self-referencing proxy entity
   */
  public RouteInterface.Zrp getProxy()
  {
    return self;
  }

  //////////////////////////////////////////////////
  // accessors
  //

  /**
   * Return local network address.
   *
   * @return local network address
   */
  public NetAddress getLocalAddr()
  {
    return localAddr;
  }

  /**
   * Return ZRP zone radius.
   *
   * @return ZRP zone radius
   */
  public byte getRadius()
  {
    return radius;
  }

  /**
   * Set zrp statistics object.
   *
   * @param stats zrp statistics object
   */
  public void setStats(ZrpStats stats)
  {
    this.stats = stats;
  }

  /**
   * Return zrp statistics.
   *
   * @return zrp statistics
   */
  public ZrpStats getStats()
  {
    return stats;
  }

  //////////////////////////////////////////////////
  // sub-protocols
  //

  /**
   * Set NDP sub-protocol implementation.
   *
   * @param ndp NDP sub-protocol implementation
   */
  public void setNdp(RouteInterface.Zrp.Ndp ndp)
  {
    this.ndp = ndp;
  }

  /**
   * Return NDP sub-protocol implementation.
   *
   * @return NDP sub-protocol implementation
   */
  public RouteInterface.Zrp.Ndp getNdp()
  {
    return ndp;
  }

  /**
   * Set IARP sub-protocol implementation.
   *
   * @param iarp IARP sub-protocol implementation
   */
  public void setIarp(RouteInterface.Zrp.Iarp iarp)
  {
    this.iarp = iarp;
  }

  /**
   * Return IARP sub-protocol implementation.
   *
   * @return IARP sub-protocol implementation
   */
  public RouteInterface.Zrp.Iarp getIarp()
  {
    return iarp;
  }

  /**
   * Set BRP sub-protocol implementation.
   *
   * @param brp BRP sub-protocol implementation
   */
  public void setBrp(RouteInterface.Zrp.Brp brp)
  {
    this.brp = brp;
  }

  /**
   * Return BRP sub-protocol implementation.
   *
   * @return BRP sub-protocol implementation
   */
  public RouteInterface.Zrp.Brp getBrp()
  {
    return brp;
  }

  /**
   * Set IERP sub-protocol implementation.
   *
   * @param ierp IERP sub-protocol implementation
   */
  public void setIerp(RouteInterface.Zrp.Ierp ierp)
  {
    this.ierp = ierp;
  }

  /**
   * Return IERP sub-protocol implementation.
   *
   * @return IERP sub-protocol implementation
   */
  public RouteInterface.Zrp.Ierp getIerp()
  {
    return ierp;
  }

  /**
   * Set all the ZRP sub-protocol implementations.
   *
   * @param ndp NDP sub-protocol implementation
   * @param iarp IARP sub-protocol implementation
   * @param brp BRP sub-protocol implementation
   * @param ierp IERP sub-protocol implementation
   */
  public void setSubProtocols(RouteInterface.Zrp.Ndp ndp, RouteInterface.Zrp.Iarp iarp, 
      RouteInterface.Zrp.Brp brp, RouteInterface.Zrp.Ierp ierp)
  {
    setNdp(ndp);
    setIarp(iarp);
    setBrp(brp);
    setIerp(ierp);
  }

  /**
   * Set all the ZRP sub-protocols to default implementations.
   */
  public void setSubProtocolsDefault()
  {
    setSubProtocols(new RouteZrpNdp(this),
        new RouteZrpIarp(this),
        new RouteZrpBrp(this),
        new RouteZrpIerp(this));
  }

  //////////////////////////////////////////////////
  // timers
  //

  /**
   * Process timer expiration.
   *
   * @param t timer that expired
   */
  public void timeout(Timer t)
  {
    t.timeout();
  }

  //////////////////////////////////////////////////
  // Protocol implementation
  //

  /** {@inheritDoc} */
  public void start()
  {
    // start sub-protocols
    if(ndp!=null) ndp.start();
    if(iarp!=null) iarp.start();
    if(brp!=null) brp.start();
    if(ierp!=null) ierp.start();
  }

  //////////////////////////////////////////////////
  // RouteInterface functions
  //

  /** {@inheritDoc} */
  public void peek(NetMessage msg, MacAddress lastHop)
  {
    if(logZRP.isDebugEnabled())
    {
      logZRP.debug("peek from="+lastHop+" data="+msg);
    }
    // todo: collect passing information
  }

  /** {@inheritDoc} */
  public void send(NetMessage msg)
  {
    if(!(msg instanceof NetMessage.Ip)) throw new IllegalArgumentException("illegal packet type");
    NetMessage.Ip ip = (NetMessage.Ip)msg;
    if(logZRP.isInfoEnabled())
    {
      logZRP.info("t="+JistAPI.getTime()+" at="+localAddr+" route "+ip);
    }
    NetAddress nextHop = null;
    // compute next hop
    if(iarp.hasRoute(ip.getDst()))
    {
      // route found inside zone
      nextHop = iarp.getRoute(ip.getDst())[0];
    }
    else if(ip.hasSourceRoute())
    {
      // packet source routed
      NetAddress[] route = ip.getSourceRoute();
      // look within source route for first node inside zone
      int i=route.length-1;
      for(i=route.length-1; i>=ip.getSourceRoutePointer() && !iarp.hasRoute(route[i]); i--)
      {
      }
      if(i<ip.getSourceRoutePointer())
      {
        // todo: route failure
        return;
      }
      nextHop = iarp.getRoute(route[i])[0];
      // adjust source route pointer
      if(nextHop.equals(route[i])) i++;
      if(i!=ip.getSourceRoutePointer())
      {
        if(ip.isFrozen()) ip = ip.copy();
        ip.setSourceRoute(new NetMessage.IpOptionSourceRoute(ip.getSourceRoute(), i));
      }
    }

    if(nextHop!=null)
    {
      // send off packet
      netEntity.send(ip, ndp.getMacId(nextHop), ndp.getMacAddress(nextHop));
    }
    else
    {
      // request routing from ierp
      ierp.send(ip);
    }
  }

  /**
   * Send out a message for a sub-protocol.
   *
   * @param msg packet payload
   */
  public void broadcast(MessageZrp msg)
  {
    if(stats!=null)
    {
      if(msg instanceof RouteInterface.Zrp.MessageNdp)
      {
        stats.send.ndpPackets++;
        stats.send.ndpBytes+=Math.max(msg.getSize(),0);
      }
      else if(msg instanceof RouteInterface.Zrp.MessageIarp)
      {
        stats.send.iarpPackets++;
        stats.send.iarpBytes+=Math.max(msg.getSize(),0);
      }
      else if(msg instanceof RouteInterface.Zrp.MessageBrp)
      {
        stats.send.brpPackets++;
        stats.send.brpBytes+=Math.max(msg.getSize(),0);
      }
      else if(msg instanceof RouteInterface.Zrp.MessageIerp)
      {
        stats.send.ierpPackets++;
        stats.send.ierpBytes+=Math.max(msg.getSize(),0);
      }
      else
      {
        throw new RuntimeException("invalid packet type");
      }
    }
    netEntity.send(msg, NetAddress.ANY, Constants.NET_PROTOCOL_ZRP,
        Constants.NET_PRIORITY_NORMAL, (byte)1);
  }

  /**
   * Send out a data packet.
   *
   * @param msg data packet
   * @param dst packet destination
   */
  public void send(NetMessage.Ip msg, NetAddress dst)
  {
    MacAddress mac = ndp.getMacAddress(dst);
    if(mac!=null) netEntity.send(msg, ndp.getMacId(dst), mac);
  }

  /** {@inheritDoc} */
  public void receive(Message msg, NetAddress src, MacAddress lastHop, byte macId, NetAddress dst, byte priority, byte ttl)
  {
    if(logZRP.isDebugEnabled())
    {
      logZRP.debug("receive at="+localAddr+" from="+src+" msg="+msg);
    }
    if(msg instanceof RouteInterface.Zrp.MessageNdp)
    {
      if(stats!=null) 
      {
        stats.recv.ndpPackets++;
        stats.recv.ndpBytes+=Math.max(msg.getSize(),0);
      }
      ndp.receive((RouteInterface.Zrp.MessageNdp)msg, src, lastHop, macId);
    }
    else if(msg instanceof RouteInterface.Zrp.MessageIarp)
    {
      if(stats!=null)
      {
        stats.recv.iarpPackets++;
        stats.recv.iarpBytes+=Math.max(msg.getSize(),0);
      }
      iarp.receive((RouteInterface.Zrp.MessageIarp)msg, src);
    }
    else if(msg instanceof RouteInterface.Zrp.MessageBrp)
    {
      if(stats!=null)
      {
        stats.recv.brpPackets++;
        stats.recv.brpBytes+=Math.max(msg.getSize(),0);
      }
      brp.receive((RouteInterface.Zrp.MessageBrp)msg, src);
    }
    else if(msg instanceof RouteInterface.Zrp.MessageIerp)
    {
      if(stats!=null)
      {
        stats.recv.ierpPackets++;
        stats.recv.ierpBytes+=Math.max(msg.getSize(),0);
      }
      ierp.receive((RouteInterface.Zrp.MessageIerp)msg);
    }
    else
    {
      throw new RuntimeException("invalid packet type received");
    }
  }

} // class: RouteZrp

