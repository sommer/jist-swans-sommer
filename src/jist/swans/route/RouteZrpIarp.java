//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RouteZrpIarp.java Tue 2004/04/06 11:35:44 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.route;

import jist.swans.net.NetAddress;
import jist.swans.misc.Util;
import jist.swans.misc.Timer;
import jist.swans.Constants;

import jist.runtime.JistAPI;

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Vector;
import java.util.Collection;
import java.util.Iterator;
import java.util.Enumeration;
import java.lang.ref.SoftReference;

/** 
 * Zone Routing Protocol: IntrAzone Routing (sub)Protocol: Default implementation.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: RouteZrpIarp.java,v 1.24 2004-04-06 16:07:51 barr Exp $
 * @since SWANS1.0
 */
public class RouteZrpIarp implements RouteInterface.Zrp.Iarp, Timer
{

  /** logger for IARP events. */
  public static final Logger logIARP = Logger.getLogger(RouteZrpIarp.class.getName());


  //////////////////////////////////////////////////
  // constants
  //

  /** link state expiration time. */
  public static final long LINK_LIFETIME = 10 * Constants.MINUTE;
  /** iarp transmission jitter. */
  public static final long JITTER = 2 * Constants.SECOND;

  /** compression constant: no aggregation. */
  public static final byte COMPRESS_NONE = 0;
  /** compression constant: inifinite. */
  public static final byte COMPRESS_INFINITE = 1;

  //////////////////////////////////////////////////
  // data structures
  //

  /**
   * IARP Link state entry.
   */
  private static class LinkStateEntry
  {
    /** link source identifier. */
    public final short seq;
    /** entry insertion time. */
    public final long time;
    /** link endpoints. */
    public final NetAddress[] dst;

    /** 
     * Create new link state entry.
     *
     * @param seq link source identifier
     * @param dst link endpoints
     */
    public LinkStateEntry(short seq, NetAddress[] dst)
    {
      this.seq = seq;
      this.time = JistAPI.getTime();
      this.dst = dst;
    }
  } // class: LinkStateEntry


  /**
   * IARP route entry.
   */
  public static class RouteEntry
  {
    /** intra-zone route. */
    public final NetAddress[] route;

    /**
     * Create new IARP route entry.
     *
     * @param route intra-zone route
     */
    public RouteEntry(NetAddress[] route)
    {
      this.route = route;
    }
  } // class: RouteEntry

  //////////////////////////////////////////////////
  // packet structures
  //

  /**
   * IARP (IntrAzone Routing Protocol) packet.
   *  <pre>
   *   link source address           size: 4
   *   link state sequence number    size: 2
   *   zone radius                   size: 1  -- not impl.
   *   time-to-live (TTL)            size: 1
   *   RESERVED                      size: 2
   *   RESERVED                      size: 1
   *   link destination count        size: 1
   *   link destination address(es)  size: 4 * count
   *   link destination mask(s)      size: 4 * count  -- not impl.
   *   link metrics                  size: k * count  -- not impl.
   *  </pre>
   */
  private static class MessageIarp implements RouteInterface.Zrp.MessageIarp
  {
    /** fixed ierp packet size. */
    public static final int FIXED_SIZE = 8;
    /** incremental ierp packet size. */
    public static final int INC_SIZE = 4;

    /** link source. */
    private NetAddress src;
    /** link state sequence identifier. */
    private short seq;
    /** time-to-live. */
    private byte ttl;
    /** links destinations. */
    private NetAddress[] dst;
    /** compression mode. */
    private byte compress;

    /**
     * Create new IARP packet.
     *
     * @param src link source address
     * @param seq link state sequence identifier
     * @param ttl packet time-to-live
     * @param dst links destinations
     * @param compress packet compression mode/format
     */
    public MessageIarp(NetAddress src, short seq, byte ttl, NetAddress[] dst, byte compress)
    {
      this.src = src;
      this.seq = seq;
      this.ttl = ttl;
      this.dst = dst;
      this.compress = compress;
    }

    /**
     * Return link source address.
     *
     * @return link source address
     */
    public NetAddress getSrc()
    {
      return src;
    }

    /**
     * Return link state sequence identifier.
     *
     * @return link state sequence identifier
     */
    public short getSeq()
    {
      return seq;
    }

    /**
     * Return packet time-to-live.
     *
     * @return packet time-to-live.
     */
    public byte getTTL()
    {
      return ttl;
    }

    /**
     * Return links destination addresses.
     *
     * @return links destination addresses
     */
    public NetAddress[] getDst()
    {
      return dst;
    }

    /**
     * Create new IARP message with decremented TTL.
     *
     * @return new message with decremented TTL
     */
    public MessageIarp decTTL()
    {
      if(ttl>1)
      {
        return new MessageIarp(src, seq, (byte)(ttl-1), dst, compress);
      }
      return null;
    }

    /** {@inheritDoc} */
    public int getSize()
    {
      switch(compress)
      {
        case COMPRESS_NONE:
          return FIXED_SIZE + dst.length*INC_SIZE;
        case COMPRESS_INFINITE:
          return Constants.ZERO_WIRE_SIZE;
        default:
          throw new RuntimeException("invalid compression mode: "+compress);
      }
    }

    /** {@inheritDoc} */
    public void getBytes(byte[] b, int offset)
    {
      throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return "iarp(src="+src+" id="+seq+" ttl="+ttl+" dst=["+Util.stringJoin(dst,",")+"])";
    }
  } // class: MessageIarp

  //////////////////////////////////////////////////
  // locals
  //

  /** reference to zrp routing framework. */
  private RouteZrp zrp;
  /** zone link state: NetAddress - LinkStateEntry. */
  private HashMap linkState;
  /** iarp identifier. */
  private short linkStateSeq;
  /** intra-zone routes: NetAddress - RouteEntry. */
  private SoftReference computedRoutes;
  /** compression mode. */
  private byte compress;

  //////////////////////////////////////////////////
  // initialization
  //

  /**
   * Create a new "default" IARP (ZRP sub-protocol) handler.
   *
   * @param zrp object reference to zrp routing framework
   */
  public RouteZrpIarp(RouteZrp zrp)
  {
    if(JistAPI.isEntity(zrp)) throw new IllegalArgumentException("expecting object reference");
    this.zrp = zrp;
    linkState = new HashMap();
    linkStateSeq = 0;
    compress = COMPRESS_NONE;
  }

  /**
   * Create a new "default" IARP (ZRP sub-protocol) handler.
   *
   * @param zrp object reference to zrp routing framework
   * @param config configuration string
   */
  public RouteZrpIarp(RouteZrp zrp, String config)
  {
    this(zrp);
    String[] data = config.split("x|,");
    if(data[0].equalsIgnoreCase("none"))
    {
      setCompress(COMPRESS_NONE);
    }
    else if(data[0].equalsIgnoreCase("inf"))
    {
      setCompress(COMPRESS_INFINITE);
    }
    else
    {
      throw new RuntimeException("invalid configuration string");
    }
  }

  //////////////////////////////////////////////////
  // accessors
  //

  /**
   * Set packet compression mode.
   *
   * @param compress packet compression mode
   */
  public void setCompress(byte compress)
  {
    this.compress = compress;
  }

  //////////////////////////////////////////////////
  // helpers
  //

  /**
   * Return new link state sequence number.
   *
   * @return new link state sequence number.
   */
  private short incLinkStateSeq()
  {
    linkStateSeq++;
    if(linkStateSeq==Short.MAX_VALUE) linkStateSeq = 0;
    return linkStateSeq;
  }

  /**
   * Neighbours have changed.
   */
  private void neighboursChanged()
  {
    NetAddress[] n = zrp.getNdp().getNeighbours();
    if(logIARP.isDebugEnabled())
    {
      logIARP.debug("t="+JistAPI.getTime()+" "+zrp.getLocalAddr()+"==>"+Util.stringJoin(n, ","));
    }
    receive(new MessageIarp(zrp.getLocalAddr(), incLinkStateSeq(), (byte)(zrp.getRadius()), n, compress), null);
  }

  /**
   * Update link state with new information.
   *
   * @param src link state information source
   * @param seq link state source sequence number
   * @param dst list of links from source
   * @return whether local link state changed
   */
  private boolean updateLinkState(NetAddress src, short seq, NetAddress[] dst)
  {
    LinkStateEntry entry = (LinkStateEntry)linkState.get(src);
    if(entry==null || RouteZrp.seqAfter(seq, entry.seq))
    {
      linkState.put(src, new LinkStateEntry(seq, dst));
      return true;
    }
    return false;
  }

  /**
   * Flush out IARP link state that has expired.
   *
   * @return whether link state changed
   */
  private boolean flushLinkState()
  {
    boolean changed = false;
    final long currentTime = JistAPI.getTime();
    Iterator it = linkState.values().iterator();
    while(it.hasNext())
    {
      LinkStateEntry ls = (LinkStateEntry)it.next();
      if(ls.time+LINK_LIFETIME < currentTime)
      {
        it.remove();
        changed = true;
      }
    }
    return changed;
  }

  /**
   * Return the intra-zone routes, computing them if necessary.
   *
   * @return hashtable of minimum intra-zone routes: NetAddress -- RouteEntry
   */
  private Map getRoutes()
  {
    Map routes = null;
    if(computedRoutes!=null) routes = (Map)computedRoutes.get();
    if(routes==null)
    {
      routes = computeRoutes();
      computedRoutes = new SoftReference(routes);
    }
    return routes;
  }

  /**
   * Flush the computed routes.
   */
  private void clearRoutes()
  {
    if(computedRoutes!=null) computedRoutes.clear();
  }

  /**
   * (Re-)Compute a minimum hop route for each node in zone
   * using current link state information.
   *
   * @return hashtable of minimum intra-zone routes: NetAddress -- RouteEntry
   */
  private Map computeRoutes()
  {
    // new routing table
    Map routes = new HashMap();
    final int radius = zrp.getRadius();
    // seed with route to root/source node
    routes.put(zrp.getLocalAddr(), new RouteEntry(NetAddress.EMPTY_ARRAY));
    Vector horizon = new Vector();
    horizon.add(zrp.getLocalAddr());
    // iterate; breadth-first search
    while(horizon.size()>0)
    {
      // pick node on horizon
      NetAddress expand = (NetAddress)horizon.remove(0);
      // find its links
      LinkStateEntry expandLinks = (LinkStateEntry)linkState.get(expand);
      if(expandLinks==null) continue;
      // find route to this node and check length
      NetAddress[] expandRoute = ((RouteEntry)routes.get(expand)).route;
      if(expandRoute.length==radius) continue;
      // expand routing table and horizon
      for(int j=0; j<expandLinks.dst.length; j++)
      {
        NetAddress dst = expandLinks.dst[j];
        RouteEntry dstRoute = (RouteEntry)routes.get(dst);
        if(dstRoute!=null) continue; // route exists (and it must equal or shorter, by construction)
        // add to routing table
        routes.put(dst, new RouteEntry((NetAddress[])Util.append(expandRoute, dst)));
        // add to (new) horizon
        horizon.add(dst);
      }
    }
    return routes;
  }

  /**
   * Display link state.
   */
  public void showLinks()
  {
    System.out.println("Links for "+zrp.getLocalAddr()+" n="+linkState.size()+" t="+JistAPI.getTimeString());
    Iterator it = linkState.entrySet().iterator();
    while(it.hasNext())
    {
      Map.Entry e = (Map.Entry)it.next();
      NetAddress src = (NetAddress)e.getKey();
      LinkStateEntry lse = (LinkStateEntry)e.getValue();
      System.out.println("  "+src+"("+lse.seq+")->"+Util.stringJoin(lse.dst, ","));
    }
  }

  /**
   * Display in-zone routing table.
   */
  public void showRoutes()
  {
    System.out.println("Routes for "+zrp.getLocalAddr()+" n="+getRoutes().size()+" t="+JistAPI.getTimeString());
    int i=0;
    boolean shown=true;
    while(shown)
    {
      shown=false;
      Iterator it = getRoutes().keySet().iterator();
      while(it.hasNext())
      {
        NetAddress dst = (NetAddress)it.next();
        RouteZrpIarp.RouteEntry re = (RouteZrpIarp.RouteEntry)getRoutes().get(dst);
        if(re.route.length!=i) continue;
        System.out.println("  "+dst+":"+Util.stringJoin(re.route, "->"));
        shown=true;
      }
      i++;
    }
  }

  //////////////////////////////////////////////////
  // RouteInterface.Zrp.Iarp implementation
  //

  /** {@inheritDoc} */
  public void start()
  {
    zrp.getProxy().timeout(this);
  }

  /** {@inheritDoc} */
  public void receive(RouteInterface.Zrp.MessageIarp msg, NetAddress from)
  {
    MessageIarp msgImpl = (MessageIarp)msg;
    if(logIARP.isDebugEnabled())
    {
      logIARP.debug("receive t="+JistAPI.getTime()+" at="+zrp.getLocalAddr()+" msg="+msgImpl);
    }
    boolean changed = updateLinkState(msgImpl.getSrc(), msgImpl.getSeq(), msgImpl.getDst());
    if(changed)
    {
      clearRoutes();
      zrp.getIerp().zoneChanged();
      // retransmit link state packet within radius
      msgImpl = msgImpl.decTTL();
      if(msgImpl!=null)
      {
        JistAPI.sleep(Util.randomTime(2*JITTER));
        if(logIARP.isInfoEnabled())
        {
          logIARP.info("send t="+JistAPI.getTime()+" at="+zrp.getLocalAddr()+" msg="+msgImpl);
        }
        zrp.broadcast(msgImpl);
      }
    }
  }

  /** {@inheritDoc} */
  public void linkinfo(Link link, boolean drop)
  {
    neighboursChanged();
  }

  /** {@inheritDoc} */
  public int getNumLinks()
  {
    int count = 0;
    Iterator it = linkState.values().iterator();
    while(it.hasNext())
    {
      LinkStateEntry lse = (LinkStateEntry)it.next();
      count += lse.dst.length;
    }
    return count;
  }

  /** {@inheritDoc} */
  public Enumeration getLinks(NetAddress src)
  {
    final LinkStateEntry lse = (LinkStateEntry)linkState.get(src); 
    if(lse==null) return Util.EMPTY_ENUMERATION;
    return new Enumeration()
    {
      private int i=0;
      public boolean hasMoreElements()
      {
        return i<lse.dst.length;
      }
      public Object nextElement()
      {
        return lse.dst[i++];
      }
    };
  }

  /** {@inheritDoc} */
  public boolean hasRoute(NetAddress dst)
  {
    return getRoutes().containsKey(dst);
  }

  /** {@inheritDoc} */
  public NetAddress[] getRoute(NetAddress dst)
  {
    return ((RouteEntry)getRoutes().get(dst)).route;
  }

  /** {@inheritDoc} */
  public int getNumRoutes()
  {
    return getRoutes().size();
  }

  /** {@inheritDoc} */
  public Collection getPeripheral()
  {
    Vector periphery = new Vector();
    Iterator it = getRoutes().entrySet().iterator();
    while(it.hasNext())
    {
      Map.Entry e = (Map.Entry)it.next();
      RouteEntry re = (RouteEntry)e.getValue();
      if(re.route.length==zrp.getRadius())
      {
        periphery.add((NetAddress)e.getKey());
      }
    }
    return periphery;
  }

  /** {@inheritDoc} */
  public Set computeCoverage(NetAddress src, int depth)
  {
    // new cover set
    HashSet cover = new HashSet();
    // seed with source node
    cover.add(src);
    Vector horizon = new Vector();
    horizon.add(src);
    // loop for depth iterations
    while(depth>0)
    {
      depth--;
      Vector newhorizon = new Vector();
      // process horizon
      while(horizon.size()>0)
      {
        // pick node on horizon
        NetAddress expand = (NetAddress)horizon.remove(0);
        // find its links
        LinkStateEntry expandLinks = (LinkStateEntry)linkState.get(expand);
        if(expandLinks==null) continue;
        // expand cover
        for(int i=0; i<expandLinks.dst.length; i++)
        {
          if(cover.contains(expandLinks.dst[i])) continue;
          cover.add(expandLinks.dst[i]);
          newhorizon.add(expandLinks.dst[i]);
        }
      }
      horizon = newhorizon;
    }
    return cover;
  }


  //////////////////////////////////////////////////
  // timers
  //

  /**
   * IARP refresh timer.
   */
  /** {@inheritDoc} */
  public void timeout()
  {
    boolean changed = flushLinkState();
    neighboursChanged();
    // schedule another one
    JistAPI.sleep(Util.randomTime(2*LINK_LIFETIME));
    zrp.getProxy().timeout(this);
  }

} // class: RouteZrpIarp

