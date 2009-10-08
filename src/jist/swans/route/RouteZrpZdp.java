//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RouteZrpZdp.java Sun 2005/03/13 11:08:07 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.route;

import jist.swans.net.NetAddress;
import jist.swans.misc.Timer;
import jist.swans.misc.Util;
import jist.swans.misc.SingletonInt;
import jist.swans.Constants;

import jist.runtime.JistAPI;

import org.apache.log4j.Logger;

import java.util.Hashtable;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Collection;
import java.util.Collections;
import java.lang.ref.SoftReference;

/** 
 * Zone Routing Protocol: Zone Discovery (sub)Protocol - an alternate IARP.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: RouteZrpZdp.java,v 1.32 2005-03-13 16:11:55 barr Exp $
 * @since SWANS1.0
 */
public class RouteZrpZdp implements RouteInterface.Zrp.Iarp
{
  /** logger for ZDP events. */
  public static final Logger logZDP = RouteZrpIarp.logIARP;

  //////////////////////////////////////////////////
  // constants
  //

  /** zdp update jitter. */
  public final long JITTER = 2 * Constants.SECOND;
  /** zdp delay time. */
  public final long DELAY = 7 * Constants.SECOND;
  /** zdp flush time. */
  public final long FLUSH = 10 * Constants.MINUTE;

  /** compression constant: no aggregation. */
  public static final byte COMPRESS_NONE = 0;
  /** compression constant: aggregate across single node. */
  public static final byte COMPRESS_NODE = 1;
  /** compression constant: aggregate across zone. */
  public static final byte COMPRESS_ZONE = 2;
  /** compression constant: aggregate across zone. */
  public static final byte COMPRESS_ZONE_REVERSE = 3;
  /** compression constant: inifinite. */
  public static final byte COMPRESS_INFINITE = 4;

  //////////////////////////////////////////////////
  // data structures
  //

  /**
   * ZDP link entry.
   */
  private static class LinkEntry
  {
    /** Empty link entry array. */
    public static final LinkEntry[] EMPTY_ARRAY = new LinkEntry[0];

    /** drop flag mask. */
    public static final byte FLAG_DROP  = 0x1;
    /** fresh flag mask. */
    public static final byte FLAG_FRESH = 0x2;
    /** flush flag mask. */
    public static final byte FLAG_FLUSH = 0x4;
    
    /** source-destination pair. */
    private final Link link;
    /** link sequence identifier. */
    private short id;
    /** flush, link failure and freshness bits. */
    private byte flags;

    /**
     * Create new link entry.
     *
     * @param link source-destination pair
     * @param id link sequence identifier
     * @param drop link failure indicator
     */
    public LinkEntry(Link link, short id, boolean drop)
    {
      this.link = link;
      set(id, drop);
    }

    /**
     * Set link entry information. Sets freshness on.
     *
     * @param id link sequence identifier
     * @param drop link failure indicator
     */
    public void set(short id, boolean drop)
    {
      this.id = id;
      setDrop(drop);
      setFlush(false);
      setFresh(true);
    }

    /**
     * Whether link entry has been processed.
     *
     * @return whether link entry has been processed
     */
    public boolean isFresh()
    {
      return Util.getFlag(flags, FLAG_FRESH);
    }

    /**
     * Set freshness flag.
     *
     * @param value whether to set or clear freshness flag
     */
    private void setFresh(boolean value)
    {
      flags = Util.setFlag(flags, FLAG_FRESH, value);
    }

    /**
     * Clears information freshness indicator.
     */
    public void processed()
    {
      setFresh(false);
    }

    /**
     * Whether drop flag is set.
     *
     * @return whether drop flag is set
     */
    public boolean isDrop()
    {
      return Util.getFlag(flags, FLAG_DROP);
    }
 
    /**
     * Set drop flag.
     *
     * @param value whether to set or clear drop flag
     */
    private void setDrop(boolean value)
    {
      flags = Util.setFlag(flags, FLAG_DROP, value);
    }

    /**
     * Whether flush flag is set.
     *
     * @return whether flush flag is set
     */
    public boolean isFlush()
    {
      return Util.getFlag(flags, FLAG_FLUSH);
    }

    /**
     * Set flush flag.
     *
     * @param value whether to set or clear flush flag
     */
    private void setFlush(boolean value)
    {
      flags = Util.setFlag(flags, FLAG_FLUSH, value);
    }

  } // class: LinkEntry


  //////////////////////////////////////////////////
  // packet structures
  //

  /**
   * ZDP (Zone Discovery Protocol) packet.
   *  <pre>
   *    todo: TBD (see draft ZDP specification)
   *  </pre>
   */
  public static class MessageZdp implements RouteInterface.Zrp.MessageIarp
  {
    /** number of links in packet. */
    private int num;
    /** link source-destination pairs. */
    private Link[] links;
    /** link identifiers. */
    private short[] ids;
    /** link failure indicators. */
    private boolean[] drops;
    /** whether packet is immutable. */
    private boolean frozen;
    /** type of packet compression/structure. */
    private byte compress;
    /** precomputed packet size. */
    private int size;

    /**
     * Create new packet with given initial capacity.
     *
     * @param capacity initial packet link capacity
     * @param compress packet compression mode/format
     */
    public MessageZdp(int capacity, byte compress)
    {
      this.num = 0;
      this.frozen = false;
      links = new Link[capacity];
      ids = new short[capacity];
      drops = new boolean[capacity];
      this.compress = compress;
      this.size = -1;
    }

    /**
     * Expand internal arrays to ensure room for one more link.
     */
    private void ensureCapacity()
    {
      if(num==links.length)
      {
        Link[] links2 = new Link[links.length*2];
        System.arraycopy(links, 0, links2, 0, links.length);
        links = links2;
        short[] ids2 = new short[ids.length*2];
        System.arraycopy(ids, 0, ids2, 0, ids.length);
        ids = ids2;
        boolean[] drops2 = new boolean[drops.length*2];
        System.arraycopy(drops, 0, drops2, 0, drops.length);
        drops = drops2;
      }
    }

    /**
     * Add link to the packet structure.
     *
     * @param link source-destination pair
     * @param id destination sequenced link identifier
     * @param drop whether link is up or down
     */
    public void addLink(Link link, short id, boolean drop)
    {
      if(frozen) throw new IllegalStateException("packet immutable");
      ensureCapacity();
      links[num] = link;
      ids[num] = id;
      drops[num] = drop;
      num++;
      size = -1;
    }

    /**
     * Freeze packet; make it immutable.
     */
    public void freeze()
    {
      frozen = true;
    }

    /**
     * Return whether packet is immutable.
     *
     * @return whether packet is immutable
     */
    public boolean isFrozen()
    {
      return frozen;
    }

    /**
     * Return number of links in packet.
     *
     * @return number of links in packet
     */
    public int getNumLinks()
    {
      return num;
    }

    /**
     * Return link source-destination pair.
     *
     * @param i link number
     * @return link source-destination pair
     */
    public Link getLink(int i)
    {
      if(i>=num) throw new ArrayIndexOutOfBoundsException();
      return links[i];
    }

    /**
     * Return link identifier.
     *
     * @param i link number
     * @return link identifier
     */
    public short getLinkId(int i)
    {
      if(i>=num) throw new ArrayIndexOutOfBoundsException();
      return ids[i];
    }

    /**
     * Return whether link has drop bit set.
     *
     * @param i link number
     * @return whether link has drop bit set
     */
    public boolean getLinkDrop(int i)
    {
      if(i>=num) throw new ArrayIndexOutOfBoundsException();
      return drops[i];
    }

    /** {@inheritDoc} */
    public int getSize()
    {
      if(size!=-1) return size;
      switch(compress)
      {
        case COMPRESS_NONE:
          size = getSizeCompressNone();
          break;
        case COMPRESS_NODE:
          size = getSizeCompressNode();
          break;
        case COMPRESS_ZONE:
          size = getSizeCompressZone();
          break;
        case COMPRESS_ZONE_REVERSE:
          size = getSizeCompressZoneReverse();
          break;
        case COMPRESS_INFINITE:
          size = Constants.ZERO_WIRE_SIZE;
          break;
        default:
          throw new RuntimeException("unknown packet structure");
      }
      return size;
    }

    /**
     * Compute size of uncompressed link state delta.
     * <pre>
     * - added links count     1 byte
     * - dropped links count   1 byte
     * - array of links:
     *   - src address         4 bytes
     *   - dst address         4 bytes
     *   - seq-id              1 byte
     * </pre>
     *
     * @return size of link state without any comrpession
     */
    private int getSizeCompressNone()
    {
      return 2 + 9*num;
    }

    /**
     * Compute size of link state compressed at the node level.
     * <pre>
     * - number of unique dsts    1 byte
     * - list of dst entries:
     *   - dst address            4 bytes
     *   - seq-id                 1 byte
     *   - number of added        1 byte
     *   - number of dropped      1 byte
     *   - list of src addresses:
     *     - src address          4 bytes
     * </pre>
     *
     * @return size of link state compressed at the node level
     */
    private int getSizeCompressNode()
    {
      return 1 + 7*getUniqueDstAddresses() + 4*num;
    }

    /**
     * Return number of unique link destinations in packet.
     *
     * @return number of unique link destinations in packet
     */
    private int getUniqueDstAddresses()
    {
      Set s = new HashSet();
      for(int i=0; i<num; i++)
      {
        s.add(links[i].dst);
      }
      return s.size();
    }

    /**
     * Return number of unique addresses in packet.
     *
     * @return number of unique addresses in packet
     */
    private int getUniqueAddresses()
    {
      Set s = new HashSet();
      for(int i=0; i<num; i++)
      {
        s.add(links[i].src);
        s.add(links[i].dst);
      }
      return s.size();
    }

    /**
     * Compute size of link state compressed at the zone level.
     * <pre>
     * - number of unique addr     k byte(s)
     * - list of addr entries:
     *   - addr                    4 bytes
     *   - seq-id                  1 byte
     * - list of links:
     *   - dst index               k byte(s)
     *   - list list srcs:   
     *     - src index             k byte(s)
     *     - flags                 1 byte
     *       - drop 
     *       - another-src
     *       - another-dst
     * </pre>
     *
     * @return size of link state compressed at the zone level
     */
    private int getSizeCompressZone()
    {
      final int k = 1;
      int uniqAddr = getUniqueAddresses();
      int uniqDstAddr = getUniqueDstAddresses();
      return k + 5*uniqAddr + k*uniqDstAddr + (k+1)*num;
    }

    /**
     * Compute size of link state compressed at the zone level, with reversal bit.
     * <pre>
     * - number of unique addr     k byte(s)
     * - list of addr entries:
     *   - addr                    4 bytes
     *   - seq-id                  1 byte
     * - list of links:
     *   - dst index               k byte(s)
     *   - list list srcs:   
     *     - src index             k byte(s)
     *     - flags                 1 byte
     *       - drop 
     *       - and-reverse ******  ADDITONAL FLAG
     *       - another-src
     *       - another-dst
     * </pre>
     *
     * @return size of link state compressed at the zone level with link reversal
     */
    private int getSizeCompressZoneReverse()
    {
      throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public void getBytes(byte[] b, int offset)
    {
      throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public String toString()
    {
      StringBuffer sb = new StringBuffer("zdp(num="+num+" links=");
      for(int i=0; i<num; i++)
      {
        if(i>0) sb.append(",");
        sb.append((drops[i]?"!":"")+links[i]);
      }
      sb.append(")");
      return sb.toString();
    }

  } // class: MessageZdp








  //////////////////////////////////////////////////
  // locals
  //

  /** reference to zrp routing framework. */
  private RouteZrp zrp;
  /** zdp link destination-sequence identifier. */
  private short seq;
  /** zone link state: NetAddress - LinkEntry. */
  private Hashtable links;
  /** zone link state: src - Vector(dst). */
  private Hashtable linksSrcDst;
  /** intra-zone routes: Hashtable dst - RouteEntry. */
  private SoftReference computedRoutes;
  /** whether the send timer is active. */
  private boolean sendScheduled;
  /** send timer callback. */
  private TimerSend sendTimer;
  /** refresh/flush timer callback. */
  private TimerRefresh refreshTimer;
  /** compression mode. */
  private byte compress;


  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Create a new "default" IARP (ZRP sub-protocol) handler.
   *
   * @param zrp object reference to zrp routing framework
   */
  public RouteZrpZdp(RouteZrp zrp)
  {
    if(JistAPI.isEntity(zrp)) throw new IllegalArgumentException("expecting object reference");
    this.zrp = zrp;
    seq = 0;
    links = new Hashtable();
    linksSrcDst = new Hashtable();
    sendScheduled = false;
    sendTimer = new TimerSend();
    refreshTimer = new TimerRefresh();
    compress = COMPRESS_NONE;
  }

  /**
   * Create a new "default" IARP (ZRP sub-protocol) handler.
   *
   * @param zrp object reference to zrp routing framework
   * @param config configuration string
   */
  public RouteZrpZdp(RouteZrp zrp, String config)
  {
    this(zrp);
    String[] data = config.split("x|,");
    if(data[0].equalsIgnoreCase("none"))
    {
      setCompress(COMPRESS_NONE);
    }
    else if(data[0].equalsIgnoreCase("node"))
    {
      setCompress(COMPRESS_NODE);
    }
    else if(data[0].equalsIgnoreCase("zone"))
    {
      setCompress(COMPRESS_ZONE);
    }
    else if(data[0].equalsIgnoreCase("zonerev"))
    {
      setCompress(COMPRESS_ZONE_REVERSE);
    }
    else if(data[0].equalsIgnoreCase("inf"))
    {
      setCompress(COMPRESS_INFINITE);
    }
    else
    {
      throw new RuntimeException("invalid configuration string: "+config);
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
  // helpers: linkSrcDst
  //

  /**
   * Add link from source-destination hashes.
   *
   * @param src link source address
   * @param dst link destination address
   */
  private void addLinkSrcDst(NetAddress src, NetAddress dst)
  {
    Vector dsts = (Vector)linksSrcDst.get(src);
    if(dsts==null)
    {
      dsts = new Vector();
      linksSrcDst.put(src, dsts);
    }
    if(!dsts.contains(dst))
    {
      dsts.add(dst);
      // todo: incremental route table maintenance possible
      clearRoutes();
    }
  }

  /**
   * Remove link from source-destination hashes.
   *
   * @param src link source address
   * @param dst link destination address
   */
  private void removeLinkSrcDst(NetAddress src, NetAddress dst)
  {
    Vector dsts = (Vector)linksSrcDst.get(src);
    if(dsts!=null)
    {
      dsts.remove(dst);
      if(dsts.size()==0) linksSrcDst.remove(src);
    }
    if(routesComputed() && getRoutes().get(dst)!=null)
    {
      clearRoutes();
    }
  }

  /**
   * Add or remove link from source-destination hashes.
   *
   * @param src link source address
   * @param dst link destination address
   * @param drop whether to add or remove link
   */
  private void updateLinkSrcDst(NetAddress src, NetAddress dst, boolean drop)
  {
    if(drop) removeLinkSrcDst(src, dst);
    else addLinkSrcDst(src, dst);
  }

  /**
   * Get known link sources.
   *
   * @return collection of known link source addresses
   */
  private Collection getLinkSrcs()
  {
    return linksSrcDst.keySet();
  }

  /**
   * Get known link destinations for given source address.
   *
   * @param src link source address
   * @return collection of link destinations for given source address
   */
  private Collection getLinkDsts(NetAddress src)
  {
    return linksSrcDst.containsKey(src) 
      ? (Collection)linksSrcDst.get(src) 
      : Collections.EMPTY_LIST;
  }

  //////////////////////////////////////////////////
  // helpers: links
  //

  /**
   * Return link entry.
   *
   * @param link link source-destination pair
   * @return link entry
   */
  private LinkEntry getLink(Link link)
  {
    return (LinkEntry)links.get(link);
  }

  /**
   * Whether link entry exists.
   *
   * @param link link source-destination pair
   * @return whether link entry exists
   */
  private boolean isLinkUp(Link link)
  {
    if(!links.containsKey(link)) return false;
    LinkEntry le = getLink(link);
    return !le.isDrop();
  }

  /**
   * Update link information in data structures.
   *
   * @param link source-destination pair
   * @param id destination sequenced identifier
   * @param drop whether link failed
   * @return whether routing tables need update
   */
  private boolean updateLink(Link link, short id, boolean drop)
  {
    LinkEntry le = getLink(link);
    if(le==null)
    {
      links.put(link, new LinkEntry(link, id, drop));
      updateLinkSrcDst(link.src, link.dst, drop);
      return true;
    }
    else if(RouteZrp.seqAfter(id, le.id))
    {
      le.set(id, drop);
      updateLinkSrcDst(link.src, link.dst, drop);
      return true;
    }
    else return false;
  }

  /**
   * Remove link from data structures.
   *
   * @param link link source-destination pair
   * @param it active iterator of link entries, if any
   */
  private void removeLink(Link link, Iterator it)
  {
    if(it==null)
    {
      links.remove(link);
    }
    else
    {
      it.remove();
    }
    updateLinkSrcDst(link.src, link.dst, true);
  }

  /**
   * Return link entries collection.
   *
   * @return link entries collection
   */
  private Collection getLinks()
  {
    return links.values();
  }

  /**
   * Remove any links that have flush bit set, and set flush bit on others
   * (clock algorithm).
   */
  private void flushLinks()
  {
    Iterator it = getLinks().iterator();
    while(it.hasNext())
    {
      LinkEntry le = (LinkEntry)it.next();
      if(le.isFlush()) 
      {
        removeLink(le.link, it);
      }
      else
      {
        le.setFlush(true);
      }
    }
  }

  /**
   * Display link state.
   */
  public void showLinks()
  {
    System.out.println("Links for "+zrp.getLocalAddr()+" n="+getLinks().size()+" t="+JistAPI.getTimeString());
    Iterator srcs = getLinkSrcs().iterator();
    while(srcs.hasNext())
    {
      NetAddress src = (NetAddress)srcs.next();
      Collection dstsColl = getLinkDsts(src);
      Iterator dsts = dstsColl.iterator();
      System.out.print("  "+src+" "+dstsColl.size()+":");
      int i=0;
      while(dsts.hasNext())
      {
        if(i>0) System.out.print(",");
        NetAddress dst = (NetAddress)dsts.next();
        LinkEntry le = getLink(new Link(src, dst));
        System.out.print(dst+"["+(le.isFresh()?"n":" ")+(le.isDrop()?"d":" ")+"]");
        i++;
      }
      System.out.println();
    }
  }

  //////////////////////////////////////////////////
  // helpers: routes
  //

  /**
   * Return whether routes have already been computed.
   *
   * @return whether routes have already been computed
   */
  private boolean routesComputed()
  {
    Hashtable routes = null;
    if(computedRoutes!=null) routes = (Hashtable)computedRoutes.get();
    return routes!=null;
  }

  /**
   * Return the intra-zone routes, computing them if necessary.
   *
   * @return hashtable of minimum intra-zone routes: NetAddress -- RouteEntry
   */
  private Hashtable getRoutes()
  {
    Hashtable routes = null;
    if(computedRoutes!=null) routes = (Hashtable)computedRoutes.get();
    if(routes==null)
    {
      routes = computeRoutes(zrp.getLocalAddr());
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
   * @param src local address, center of zone
   * @return hashtable of minimum intra-zone routes: NetAddress -- RouteEntry
   */
  private Hashtable computeRoutes(NetAddress src)
  {
    // new routing table
    Hashtable routes = new Hashtable();
    // seed with route to root/source node
    routes.put(src, new RouteZrpIarp.RouteEntry(NetAddress.EMPTY_ARRAY));
    Vector horizon = new Vector();
    horizon.add(src);
    // iterate; breadth-first search
    int radius = zrp.getRadius();
    while(radius>0)
    {
      radius--;
      Vector horizon2 = new Vector();
      while(horizon.size()>0)
      {
        // pick node on horizon
        src = (NetAddress)horizon.remove(horizon.size()-1);
        // find its links and route to this node
        Iterator expandLinks = ((Collection)getLinkDsts(src)).iterator();
        NetAddress[] expandRoute = ((RouteZrpIarp.RouteEntry)routes.get(src)).route;
        while(expandLinks.hasNext())
        {
          NetAddress expandDst = (NetAddress)expandLinks.next();
          RouteZrpIarp.RouteEntry dstRoute = 
            (RouteZrpIarp.RouteEntry)routes.get(expandDst);
          if(dstRoute!=null) continue; // route exists (and it must equal or shorter, by construction)
          // add to routing table
          NetAddress[] route = (NetAddress[])Util.append(expandRoute, expandDst);
          routes.put(expandDst, new RouteZrpIarp.RouteEntry(route));
          // add to (new) horizon
          horizon2.add(expandDst);
        }
      }
      horizon = horizon2;
    }
    return routes;
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
  // helpers: distances to source
  //

  /**
   * Compute distances FROM every node TO source.
   *
   * @return map containing shortest distances (Integer) from every node
   *    (NetAddress) to the source
   */
  private Map computeDistancesToSource()
  {
    Map dstToSrc = new HashMap();
    Vector horizon = new Vector();
    NetAddress src = zrp.getLocalAddr();
    // seed with source
    dstToSrc.put(src, SingletonInt.getSmallInteger(0));
    horizon.add(src);
    // iterate
    while(horizon.size()>0)
    {
      Vector horizon2 = new Vector();
      while(horizon.size()>0)
      {
        src = (NetAddress)horizon.remove(horizon.size()-1);
        Integer dist = SingletonInt.getSmallInteger(((Integer)dstToSrc.get(src)).intValue()+1);
        Iterator links = getLinks().iterator();
        while(links.hasNext())
        {
          LinkEntry le = (LinkEntry)links.next();
          if(le.isDrop()) continue;
          if(!le.link.dst.equals(src)) continue;
          if(dstToSrc.containsKey(le.link.src)) continue;
          dstToSrc.put(le.link.src, dist);
          horizon2.add(le.link.src);
        }
      }
      horizon = horizon2;
    }
    return dstToSrc;
  }

  //////////////////////////////////////////////////
  // helpers: other
  //

  /**
   * Increment local link sequence identifier.
   *
   * @return new link sequence identifier
   */
  private short incLinkSeq()
  {
    seq++;
    if(seq==Short.MAX_VALUE) seq = 0;
    return seq;
  }

  /**
   * Process incoming link information.
   *
   * @param link link source-destination pair
   * @param id destination sequenced identifier
   * @param drop whether link failed
   */
  private void linkinfo(Link link, short id, boolean drop)
  {
    boolean changed = updateLink(link, id, drop);
    if(changed)
    {
      if(logZDP.isDebugEnabled())
      {
        logZDP.debug("linkinfo t="+JistAPI.getTimeString()+" at="+zrp.getLocalAddr()+" link="+link+" id="+id+" drop="+drop);
      }
      if(!sendScheduled)
      {
        sendScheduled = true;
        JistAPI.sleep(DELAY+Util.randomTime(JITTER));
        zrp.getProxy().timeout(sendTimer);
      }
    }
  }

  /**
   * Transmit a zone-wide link state delta.
   */
  private void send()
  {
    // count links to send
    int numLinksToSend = 0;
    Iterator it = getLinks().iterator();
    while(it.hasNext())
    {
      LinkEntry le = (LinkEntry)it.next();
      if(!le.isFresh()) continue;
      if(!hasRoute(le.link.dst)) continue;
      numLinksToSend++;
    }
    if(numLinksToSend>0)
    {
      // generate packet
      MessageZdp msg = new MessageZdp(numLinksToSend, compress);
      it = getLinks().iterator();
      while(it.hasNext())
      {
        LinkEntry le = (LinkEntry)it.next();
        if(!le.isFresh()) continue;
        if(!hasRoute(le.link.dst)) continue;
        msg.addLink(le.link, le.id, le.isDrop());
        le.processed();
        if(le.isDrop()) removeLink(le.link, it);
      }
      msg.freeze();
      // send packet
      if(logZDP.isInfoEnabled())
      {
        logZDP.info("send from="+zrp.getLocalAddr()+" t="+JistAPI.getTimeString()+" msg="+msg);
      }
      zrp.broadcast(msg);
    }
    // clear send flag
    sendScheduled = false;
  }

  //////////////////////////////////////////////////
  // RouteInterface.Zrp.Iarp implementation
  //

  /** {@inheritDoc} */
  public void receive(RouteInterface.Zrp.MessageIarp msg, NetAddress from)
  {
    MessageZdp msgImpl = (MessageZdp)msg;
    if(logZDP.isDebugEnabled())
    {
      logZDP.debug("receive t="+JistAPI.getTimeString()+" at="+zrp.getLocalAddr()+" msg="+msgImpl);
    }
    // process message
    for(int i=0; i<msgImpl.getNumLinks(); i++)
    {
      linkinfo(msgImpl.getLink(i), msgImpl.getLinkId(i), msgImpl.getLinkDrop(i));
    }
    Link msgLink = new Link(from, zrp.getLocalAddr());
    if(!isLinkUp(msgLink)) linkinfo(msgLink, false);
    // prune links out of zone
    Map dstToSrc = computeDistancesToSource();
    Iterator it = getLinks().iterator();
    while(it.hasNext())
    {
      LinkEntry le = (LinkEntry)it.next();
      if(!le.isFresh()) continue;
      Integer dist = (Integer)dstToSrc.get(le.link.src);
      if(dist==null || dist.intValue()>=zrp.getRadius()) 
      {
        removeLink(le.link, it);
      }
    }
    if(logZDP.isDebugEnabled())
    {
      showLinks();
      showRoutes();
    }
  }

  /** {@inheritDoc} */
  public void linkinfo(Link link, boolean drop)
  {
    linkinfo(link, seq, drop);
  }

  /** {@inheritDoc} */
  public int getNumLinks()
  {
    int count = 0;
    Iterator it = getLinks().iterator();
    while(it.hasNext())
    {
      LinkEntry le = (LinkEntry)it.next();
      if(le.isDrop()) continue;
      count++;
    }
    return count;
  }

  /** {@inheritDoc} */
  public Enumeration getLinks(NetAddress src)
  {
    return Collections.enumeration(getLinkDsts(src));
  }

  /** {@inheritDoc} */
  public boolean hasRoute(NetAddress dst)
  {
    return getRoutes().get(dst)!=null;
  }

  /** {@inheritDoc} */
  public NetAddress[] getRoute(NetAddress dst)
  {
    // look up route
    RouteZrpIarp.RouteEntry re = (RouteZrpIarp.RouteEntry)getRoutes().get(dst);
    if(re==null) return null;
    return re.route;
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
    Iterator it = getRoutes().values().iterator();
    while(it.hasNext())
    {
      RouteZrpIarp.RouteEntry re = 
        (RouteZrpIarp.RouteEntry)it.next();
      if(re.route.length==zrp.getRadius())
      {
        periphery.add(re.route[re.route.length-1]);
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
        Iterator expandLinks = ((Collection)getLinkDsts(expand)).iterator();
        // expand cover
        while(expandLinks.hasNext())
        {
          NetAddress expandDst = (NetAddress)expandLinks.next();
          if(cover.contains(expandDst)) continue;
          cover.add(expandDst);
          newhorizon.add(expandDst);
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
   * Link refresh timer.
   */
  private class TimerRefresh implements Timer
  {
    /** {@inheritDoc} */
    public void timeout()
    {
      // flush links
      flushLinks();
      // refresh sequence number on all neighbour links
      incLinkSeq();
      Iterator it = getLinks().iterator();
      NetAddress addr = zrp.getLocalAddr();
      while(it.hasNext())
      {
        LinkEntry le = (LinkEntry)it.next();
        if(!addr.equals(le.link.dst)) continue;
        linkinfo(le.link, seq, le.isDrop());
      }
      // reset refresh timer
      JistAPI.sleep(FLUSH+Util.randomTime(JITTER));
      zrp.getProxy().timeout(this);
    }
  }

  /**
   * Delayed outgoing send timer.
   */
  private class TimerSend implements Timer
  {
    /** {@inheritDoc} */
    public void timeout()
    {
      send();
    }
  }

  //////////////////////////////////////////////////
  // protocol implementation
  //

  /** {@inheritDoc} */
  public void start()
  {
    zrp.getProxy().timeout(refreshTimer);
  }

} // class: RouteZrpZdp

