//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RouteZrpIerp.java Sun 2005/03/13 11:07:49 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.route;

import jist.swans.net.NetAddress;
import jist.swans.net.NetMessage;
import jist.swans.misc.Timer;
import jist.swans.misc.Util;
import jist.swans.Constants;
import jist.swans.Main;

import jist.runtime.JistAPI;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/** 
 * Zone Routing Protocol: IntErzone Routing (sub)Protocol: Default implementation.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: RouteZrpIerp.java,v 1.12 2005-03-13 16:11:55 barr Exp $
 * @since SWANS1.0
 */
public class RouteZrpIerp implements RouteInterface.Zrp.Ierp, Timer
{

  /** logger for IERP events. */
  public static final Logger logIERP = Logger.getLogger(RouteZrpIerp.class.getName());

  //////////////////////////////////////////////////
  // constants
  //

  /** packet queue expiration. */
  public static final long QUEUE_LIFETIME = 30 * Constants.SECOND;
  /** packet queue refresh timer (check for expiration). */
  public static final long QUEUE_REFRESH = 30 * Constants.SECOND;

  //////////////////////////////////////////////////
  // data structures
  //

  /**
   * IERP routing entry.
   */
  private static class RouteEntry
  {
    /** inter-zone route. */
    public final NetAddress[] route;
    /** entry insertion time. */
    public final long time;

    /**
     * Create new IERP route entry.
     *
     * @param route inter-zone route
     * @param time entry insertion time
     */
    public RouteEntry(NetAddress[] route, long time)
    {
      this.route = route;
      this.time = time;
    }

  } // class: RouteEntry

  /**
   * Packet queued and waiting to be sent.
   */
  private static class QueuedPacket
  {
    /** packet to be sent. */
    public final NetMessage.Ip packet;
    /** queue insertion time. */
    public final long time;

    /**
     * Create new queued packet holder.
     *
     * @param packet packet to be sent
     */
    public QueuedPacket(NetMessage.Ip packet)
    {
      this.packet = packet;
      time = JistAPI.getTime();
    }

  } // class: QueuedPacket

  /**
   * Queue of message waiting for route replies.
   */
  public static class MessageQueue 
  {
    /** internal list of messages. */
    private List list;

    /**
     * Create new, empty message queue.
     */
    public MessageQueue()
    {
      list = new ArrayList();
    }

    /**
     * Add message to queue.
     *
     * @param msg outgoing message
     */
    public void add(NetMessage.Ip msg)
    {
      list.add(new QueuedPacket(msg));
    }

    /**
     * Send off all messages destined for a certain address.
     *
     * @param dst destination address
     * @param zrp zrp reference
     * @param route route to destination
     */
    public void sendAll(NetAddress dst, RouteZrp zrp, NetAddress[] route)
    {
      Iterator it = list.iterator();
      while(it.hasNext())
      {
        NetMessage.Ip ip = ((QueuedPacket)it.next()).packet;
        if(ip.getDst().equals(dst))
        {
          // source route packet
          if(ip.isFrozen()) ip = ip.copy();
          ip.setSourceRoute(new NetMessage.IpOptionSourceRoute(route));
          // send it off
          if(logIERP.isInfoEnabled())
          {
            logIERP.info("sending off data t="+JistAPI.getTime()+" msg="+ip);
          }
          zrp.send(ip, route[0]);
          it.remove();
        }
      }
    }

    /**
     * Flush all expired packets.
     */
    public void flush()
    {
      final long expireCutoff = JistAPI.getTime() - QUEUE_LIFETIME;
      Iterator it = list.iterator();
      while(it.hasNext())
      {
        QueuedPacket packet = (QueuedPacket)it.next();
        if(packet.time < expireCutoff) it.remove();
      }
    }

  } // class: MessageQueue


  //////////////////////////////////////////////////
  // packet structures
  //

  /**
   * IERP (IntErzone Routing Protocol) packet.
   *  <pre>
   *   type (req, rep)               size: 1
   *   length                        size: 1  -- computed
   *   node pointer                  size: 1
   *   RESERVED                      size: 1
   *   query ID                      size: 2
   *   RESERVED                      size: 2
   *   src .. route .. dst           size: 4 * n
   *  </pre>
   */
  private static class MessageIerp implements RouteInterface.Zrp.MessageIerp
  {
    /** fixed ierp packet size. */
    public static final int FIXED_SIZE = 8;
    /** incremental ierp packet size. */
    public static final int INC_SIZE   = 4;

    /** ierp packet type constant: request. */
    public static final byte TYPE_RREQ = 1;
    /** ierp packet type constant: reply. */
    public static final byte TYPE_RREP = 2;

    /** packet type. */
    private byte type;
    /** unique source-sequence request identifier. */
    private short seq;
    /** route. */
    private NetAddress[] route;

    /**
     * Create new IERP route request packet.
     *
     * @param seq source-sequence request number
     * @param dst route request destination
     */
    public MessageIerp(short seq, NetAddress dst)
    {
      this(TYPE_RREQ, seq, new NetAddress[] { dst });
    }

    /**
     * Create new IERP packet.
     *
     * @param type ierp packet type
     * @param seq source-sequence request number
     * @param route route
     */
    private MessageIerp(byte type, short seq, NetAddress[] route)
    {
      this.type = type;
      this.seq = seq;
      this.route = route;
    }

    /**
     * Return packet type.
     *
     * @return packet type
     */
    public byte getType()
    {
      return type;
    }

    /**
     * Return packet length.
     *
     * @return packet length
     */
    public byte getLength()
    {
      return (byte)(2+route.length);
    }

    /** {@inheritDoc} */
    public short getID()
    {
      return seq;
    }

    /** {@inheritDoc} */
    public NetAddress getSrc()
    {
      return route[0];
    }

    /** {@inheritDoc} */
    public NetAddress getDst()
    {
      return route[route.length-1];
    }

    /**
     * Return route.
     *
     * @return route (do not modify)
     */
    public NetAddress[] getRoute()
    {
      return route;
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return "ierp(t="+(type==TYPE_RREQ?"q":"a")+" id="+seq+" route=["+Util.stringJoin(route, ",")+"])";
    }

    /** {@inheritDoc} */
    public int getSize()
    {
      return FIXED_SIZE+INC_SIZE*route.length;
    }

    /** {@inheritDoc} */
    public void getBytes(byte[] b, int offset)
    {
      throw new RuntimeException("not implemented");
    }

    /**
     * Helper method: append hop to route.
     *
     * @param route existing route (source .. route .. destination)
     * @param nextHop next hop to append (before destination)
     * @return new route
     */
    private static NetAddress[] routeAppend(NetAddress[] route, NetAddress nextHop)
    {
      NetAddress[] route2 = new NetAddress[route.length+1];
      System.arraycopy(route, 0, route2, 0, route.length-1); // prefix
      route2[route.length-1] = nextHop;                      // new hop
      route2[route.length] = route[route.length-1];          // dst
      if(Main.ASSERT) Util.assertion(!routeDupCheck(route2));
      return route2;
    }

    /**
     * Return whether is a cycle in the route.
     *
     * @param route route to check for cycle
     * @return whether there is a cycle in the route
     */
    private static boolean routeDupCheck(NetAddress[] route)
    {
      HashSet check = new HashSet();
      for(int i=0; i<route.length; i++)
      {
        if(!check.add(route[i])) return true;
      }
      return false;
    }

    /**
     * Create new packet and append hop to packet route.
     *
     * @param nextHop next hop to append to route
     * @return new message with next hop appended
     */
    public MessageIerp appendHop(NetAddress nextHop)
    {
      return new MessageIerp(type, seq, routeAppend(route, nextHop));
    }

    /**
     * Create a reply IERP packet.
     *
     * @param finder address of node that found destination
     * @param routeToDst remaining route to destination
     * @return reply ierp packet
     */
    public MessageIerp makeReply(NetAddress finder, NetAddress[] routeToDst)
    {
      return new MessageIerp(TYPE_RREP, seq, RouteZrp.replaceDest(route, finder, routeToDst));
    }

  } // class: MessageIerp

  //////////////////////////////////////////////////
  // locals
  //

  /** reference to zrp routing framework. */
  private RouteZrp zrp;
  /** inter-zone routes: NetAddress - RouteEntry. */
  private HashMap routes;
  /** ierp identifier. */
  private short routeSeq;
  /** outgoing message queue. */
  private MessageQueue mq;

  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Create new "default" IERP (ZRP sub-protocol) handler.
   *
   * @param zrp object reference to zrp routing framework
   */
  public RouteZrpIerp(RouteZrp zrp)
  {
    if(JistAPI.isEntity(zrp)) throw new IllegalArgumentException("expecting object reference");
    this.zrp = zrp;
    routes = new HashMap();
    routeSeq = 0;
    mq = new MessageQueue();
  }

  /**
   * Create new "default" IERP (ZRP sub-protocol) handler.
   *
   * @param zrp object reference to zrp routing framework
   * @param config configuration string
   */
  public RouteZrpIerp(RouteZrp zrp, String config)
  {
    this(zrp);
  }

  //////////////////////////////////////////////////
  // helpers
  //

  /**
   * Return new IERP sequence number.
   *
   * @return new IERP sequence number.
   */
  private short incSeq()
  {
    routeSeq++;
    if(routeSeq==Short.MAX_VALUE) routeSeq = 0;
    return routeSeq;
  }

  /**
   * Return whether IERP table has a route.
   *
   * @param dst destination node
   * @return whether IERP knows route to given destination
   */
  private boolean hasRoute(NetAddress dst)
  {
    return routes.containsKey(dst);
  }

  /**
   * Return IERP route.
   *
   * @param dst destination node
   * @return IERP route to given destination, nor NullPointerException if it
   *   does not exist
   */
  private NetAddress[] getRoute(NetAddress dst)
  {
    RouteEntry re = (RouteEntry)routes.get(dst);
    if(re==null) return null;
    return re.route;
  }


  //////////////////////////////////////////////////
  // RouteZrpIerp implementation
  //

  /** {@inheritDoc} */
  public void start()
  {
    zrp.getProxy().timeout(this);
  }

  /** {@inheritDoc} */
  public void zoneChanged()
  {
    // todo: implement me!
  }

  /** {@inheritDoc} */
  public void receive(RouteInterface.Zrp.MessageIerp msg)
  {
    if(logIERP.isDebugEnabled())
    {
      logIERP.debug("received t="+JistAPI.getTime()+" node="+zrp.getLocalAddr()+" msg="+msg);
    }
    MessageIerp msgImpl = (MessageIerp)msg;
    switch(msgImpl.getType())
    {
      // route request
      case MessageIerp.TYPE_RREQ:
        if(zrp.getIarp().hasRoute(msgImpl.getDst()))
        {
          // destination in zone, send back reply
          MessageIerp reply = msgImpl.makeReply(zrp.getLocalAddr(), zrp.getIarp().getRoute(msgImpl.getDst()));
          if(logIERP.isInfoEnabled())
          {
            logIERP.info("dst found: t="+JistAPI.getTime()+" at="+zrp.getLocalAddr()+" reply="+reply);
          }
          receive(reply);
        }
        else
        {
          // destination not found in zone, rebordercast
          zrp.getBrp().send(msgImpl.appendHop(zrp.getLocalAddr()));
        }
        break;
        // route reply
      case MessageIerp.TYPE_RREP:
        NetAddress[] route = msgImpl.getRoute();
        if(zrp.getLocalAddr().equals(msgImpl.getSrc()))
        {
          // reply reached query source
          if(logIERP.isInfoEnabled())
          {
            logIERP.info("reply received: t="+JistAPI.getTime()+" reply="+msgImpl);
          }
          mq.sendAll(msgImpl.getDst(), zrp, (NetAddress[])Util.rest(route));
        }
        else
        {
          // reply en-route, find next hop
          NetAddress nextHop = null;
          for(int i=0; i<route.length-1; i++)
          {
            if(zrp.getLocalAddr().equals(route[i+1]))
            {
              nextHop = route[i];
              break;
            }
          }
          if(Main.ASSERT) Util.assertion(nextHop!=null);
          if(logIERP.isDebugEnabled())
          {
            logIERP.debug("forwarding reply: t="+JistAPI.getTime()+" from="+zrp.getLocalAddr()+" to="+nextHop+" reply="+msgImpl);
          }
          // forward reply
          if(zrp.getNdp().isNeighbour(nextHop))
          {
            zrp.send(new NetMessage.Ip(msgImpl, zrp.getLocalAddr(), nextHop, 
                  Constants.NET_PROTOCOL_ZRP, Constants.NET_PRIORITY_NORMAL, (byte)1), nextHop);
          }
        }
        break;
      default:
        throw new RuntimeException("unrecognized IERP packet type: "+msgImpl.getType());
    }
  } // receive

  /** {@inheritDoc} */
  public void send(NetMessage.Ip ip)
  {
    if(logIERP.isInfoEnabled())
    {
      logIERP.info("send: t="+JistAPI.getTime()+" msg="+ip);
    }
    // todo: check if this destination is among the outstanding queries
    // no route known, so add packet to outgoing queue
    mq.add(ip);
    // initiate discovery
    receive(new MessageIerp(incSeq(), ip.getDst()));
  }

  //////////////////////////////////////////////////
  // timers
  //

  /** {@inheritDoc} */
  public void timeout()
  {
    // flush packet queue
    mq.flush();
    // schedule another one
    JistAPI.sleep(Util.randomTime(2*QUEUE_REFRESH));
    zrp.getProxy().timeout(this);
  }

} // class: RouteZrpIerp

