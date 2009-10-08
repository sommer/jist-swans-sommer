//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RouteZrpNdp.java Tue 2004/04/06 11:35:51 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.route;

import jist.swans.mac.MacAddress;
import jist.swans.net.NetAddress;
import jist.swans.misc.Util;
import jist.swans.misc.Timer;
import jist.swans.Constants;

import jist.runtime.JistAPI;

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;


/** 
 * Zone Routing Protocol: Node Discovery (sub)Protocol: Default implementation.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: RouteZrpNdp.java,v 1.14 2004-04-06 16:07:51 barr Exp $
 * @since SWANS1.0
 */
public class RouteZrpNdp implements RouteInterface.Zrp.Ndp, Timer
{
  /** logger for NDP events. */
  public static final Logger logNDP = Logger.getLogger(RouteZrpNdp.class.getName());

  //////////////////////////////////////////////////
  // constants
  //

  /** heartbeat period. */
  public static final long PERIOD = 10 * Constants.SECOND;
  /** jitter. */
  public static final long JITTER = 2 * Constants.SECOND;
  /** heartbeats until link expiration. */
  public static final short LIFETIME_BEATS = 3;

  //////////////////////////////////////////////////
  // data structures
  //

  /** 
   * NDP neighbour entry.
   */
  private static class NeighbourState
  {
    /** neigbour mac address. */
    public MacAddress macAddr;
    /** incoming mac interface. */
    public byte macId;
    /** number of heartbeats to expiration. */
    public int beats;
  } // class: NeighbourState


  //////////////////////////////////////////////////
  // packet structures
  //

  /** 
   * NDP (Node Discovery Protocol) packet.
   * <pre>
   *   (nothing)
   * </pre>
   */
  private static class MessageNdp implements RouteInterface.Zrp.MessageNdp
  {
    /** {@inheritDoc} */
    public int getSize()
    {
      return 0;
    }

    /** {@inheritDoc} */
    public void getBytes(byte[] b, int offset)
    {
      throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return "ndp";
    }
  } // class: MessageNDP

  //////////////////////////////////////////////////
  // locals
  //

  /** reference to zrp routing framework. */
  private RouteZrp zrp;
  /** list of neighbours: NetAddress - NeighbourState. */
  private HashMap neighbours;

  //////////////////////////////////////////////////
  // initialization
  //

  /**
   * Create new "default" NDP (ZRP sub-protocol) handler.
   *
   * @param zrp object reference to zrp routing framework
   */
  public RouteZrpNdp(RouteZrp zrp)
  {
    if(JistAPI.isEntity(zrp)) throw new IllegalArgumentException("expecting object reference");
    this.zrp = zrp;
    neighbours = new HashMap();
  }

  /**
   * Create new "default" NDP (ZRP sub-protocol) handler.
   *
   * @param zrp object reference to zrp routing framework
   * @param config configuration string
   */
  public RouteZrpNdp(RouteZrp zrp, String config)
  {
    this(zrp);
  }

  //////////////////////////////////////////////////
  // RouteInterface.Zrp.Ndp implementation
  //

  /** {@inheritDoc} */
  public void start()
  {
    zrp.getProxy().timeout(this);
  }

  /** {@inheritDoc} */
  public void receive(RouteInterface.Zrp.MessageNdp msg, NetAddress src, MacAddress macAddr, byte macId)
  {
    if(logNDP.isDebugEnabled())
    {
      logNDP.debug("receive t="+JistAPI.getTime()+" at="+zrp.getLocalAddr()+" from="+src+" msg="+msg);
    }
    NeighbourState n = (NeighbourState)neighbours.get(src);
    boolean changed = false;
    if(n==null)
    {
      // neighbour found
      n = new NeighbourState();
      neighbours.put(src, n);
      changed = true;
      if(logNDP.isInfoEnabled())
      {
        logNDP.info("t="+JistAPI.getTimeString()+" "+zrp.getLocalAddr()+" found neighbour "+src);
      }
    }
    n.macAddr = macAddr;
    n.beats = LIFETIME_BEATS;
    n.macId = macId;
    if(changed) zrp.getIarp().linkinfo(new Link(src, zrp.getLocalAddr()), false);
  }

  /** {@inheritDoc} */
  public MacAddress getMacAddress(NetAddress addr)
  {
    NeighbourState ns = (NeighbourState)neighbours.get(addr);
    if(ns==null) return null;
    return ns.macAddr;
  }

  /** {@inheritDoc} */
  public byte getMacId(NetAddress addr)
  {
    NeighbourState ns = (NeighbourState)neighbours.get(addr);
    if(ns==null) return Constants.NET_INTERFACE_INVALID;
    return ns.macId;
  }

  /** {@inheritDoc} */
  public NetAddress[] getNeighbours()
  {
    return (NetAddress[])neighbours.keySet().toArray(NetAddress.EMPTY_ARRAY);
  }

  /** {@inheritDoc} */
  public int getNumNeighbours()
  {
    return neighbours.size();
  }

  /** {@inheritDoc} */
  public boolean isNeighbour(NetAddress addr)
  {
    return neighbours.containsKey(addr);
  }

  //////////////////////////////////////////////////
  // timers
  //

  /** {@inheritDoc} */
  public void timeout()
  {
    // send heartbeat
    MessageNdp msg = new MessageNdp();
    if(logNDP.isDebugEnabled())
    {
      logNDP.debug("send t="+JistAPI.getTime()+" from="+zrp.getLocalAddr()+" msg="+msg);
    }
    zrp.broadcast(msg);
    // process neighbour set
    Iterator it = neighbours.entrySet().iterator();
    while(it.hasNext())
    {
      Map.Entry e = (Map.Entry)it.next();
      NeighbourState n = (NeighbourState)e.getValue();
      n.beats--;
      if(n.beats<0)
      {
        // neighbour lost
        if(logNDP.isInfoEnabled())
        {
          logNDP.info("t="+JistAPI.getTimeString()+" "+zrp.getLocalAddr()+" lost neighbour "+e.getKey());
        }
        zrp.getIarp().linkinfo(new Link((NetAddress)e.getKey(), zrp.getLocalAddr()), true);
        it.remove();
      }
    }
    // schedule next
    JistAPI.sleep(PERIOD-JITTER+Util.randomTime(2*JITTER));
    zrp.getProxy().timeout(this);
  } // timeout

} // class: RouteZrpNdp

