//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RouteZrpBrp.java Tue 2004/04/06 11:35:34 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.route;

import jist.swans.net.NetAddress;
import jist.swans.misc.Util;
import jist.swans.misc.Timer;
import jist.swans.misc.SingletonInt;
import jist.swans.Constants;
import jist.swans.Main;

import jist.runtime.JistAPI;

import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.Iterator;
import java.util.Enumeration;

/** 
 * Zone Routing Protocol: Bordercast Resolution (sub)Protocol: Default implementation.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: RouteZrpBrp.java,v 1.17 2004-04-06 16:07:50 barr Exp $
 * @since SWANS1.0
 */
public class RouteZrpBrp implements RouteInterface.Zrp.Brp, Timer
{

  /** logger for BRP events. */
  public static final Logger logBRP = Logger.getLogger(RouteZrpBrp.class.getName());

  //////////////////////////////////////////////////
  // constants
  //

  /** query coverage expiration. */
  public static final long COVERAGE_LIFETIME = 90 * Constants.SECOND;
  /** query coverage refresh timer (check for expiration). */
  public static final long COVERAGE_REFRESH  = 10 * Constants.SECOND;
  /** brp delivery jitter. */
  public static final long JITTER = 2*Constants.SECOND;

  //////////////////////////////////////////////////
  // data structures
  //

  /**
   * BRP query key.
   */
  public static class QueryKey
  {
    /** query source address. */
    public final NetAddress src;
    /** query source identifier. */
    public final short id;

    /**
     * Create new query key.
     *
     * @param src query source address
     * @param id query source identifier
     */
    public QueryKey(NetAddress src, short id)
    {
      this.src = src;
      this.id = id;
    }

    /** {@inheritDoc} */
    public int hashCode()
    {
      return src==null?0:src.hashCode()+id;
    }

    /** {@inheritDoc} */
    public boolean equals(Object o)
    {
      if(this==o) return true;
      if(o==null) return false;
      if(!(o instanceof QueryKey)) return false;
      QueryKey qk = (QueryKey)o;
      if(this.src==null && qk.src!=null) return false;
      if(!this.src.equals(qk.src)) return false;
      if(this.id!=qk.id) return false;
      return true;
    }
  } // class: QueryKey

  /**
   * BRP query entry.
   */
  public static class QueryCoverageEntry
  {
    /** list of sender broadcasters. */
    private Vector from;
    /** entry insertion time. */
    public final long time;
    /** whether node is target for this query. */
    private boolean target;
    /** whether node has already processed query. */
    private boolean processed;

    /**
     * Create new, empty query coverage entry.
     *
     * @param target whether node was target of query broadcast
     */
    public QueryCoverageEntry(boolean target)
    {
      this.time = JistAPI.getTime();
      from = new Vector();
      this.target = target;
      processed = false;
    }

    /**
     * Add source to query coverage.
     *
     * @param from source of bordercast
     */
    public void addFrom(NetAddress from)
    {
      if(!isForwarded()) this.from.add(from);
    }

    /**
     * Return query sources.
     *
     * @return query sources
     */
    public Vector getFrom()
    {
      return from;
    }

    /**
     * Whether query was already processed locally.
     *
     * @return query was already processed locally
     */
    public boolean isForwarded()
    {
      return from==null;
    }

    /**
     * Set the local query processing bit.
     */
    public void forwarded()
    {
      this.from=null;
    }

    /**
     * Return whether node has already processed query.
     *
     * @return whether node has already processed query
     */
    public boolean isProcessed()
    {
      return processed;
    }

    /**
     * Set processed flag of query coverage entry.
     */
    public void processed()
    {
      this.processed = true;
    }

    /**
     * Return whether node was targetted by query.
     *
     * @return whether node was targetted by query
     */
    public boolean isTarget()
    {
      return target;
    }

    /**
     * Set target flag of query coverage entry.
     */
    public void setTarget()
    {
      this.target = true;
    }

  } // class QueryCoverageEntry

  //////////////////////////////////////////////////
  // packet structures
  //

  /**
   * BRP (Bordercast Resolution Protocol) packet.
   *  <pre>
   *   query source                  size: 4  -- encapsulated
   *   query destination             size: 4  -- encapsulated
   *   query ID                      size: 2  -- encapsulated
   *   query extension               size: 1  -- not impl.
   *   RESERVED                      size: 1
   *   prev bordercast node          size: 4  -- not necessary (from IP)
   *   target length                 size: 1     -- not in spec.
   *   target addresses              size: 4 * n -- not in spec.
   *   encapsulated packet           size: *
   *  </pre>
   */
  private static class MessageBrp implements RouteInterface.Zrp.MessageBrp
  {
    /** fixed ierp packet size. */
    public static final int FIXED_SIZE = 5;

    /** targets of broadcast packet. */
    private NetAddress[] targets;
    /** encapsulated query. */
    private RouteInterface.Zrp.MessageIerp encapsulated;

    /**
     * Create new bordercast packet.
     *
     * @param query encapsulated query packet
     * @param targets bordercast next hop targets
     */
    public MessageBrp(RouteInterface.Zrp.MessageIerp query, NetAddress[] targets)
    {
      this.encapsulated = query;
      if(Main.ASSERT) Util.assertion(targets.length>0);
      this.targets = targets;
    }

    /**
     * Return query source.
     *
     * @return query source
     */
    public NetAddress getSrc()
    {
      return encapsulated.getSrc();
    }

    /**
     * Return query destination.
     *
     * @return query destination
     */
    public NetAddress getDst()
    {
      return encapsulated.getDst();
    }

    /**
     * Return query identifier.
     *
     * @return query identifier
     */
    public short getID()
    {
      return encapsulated.getID();
    }

    /**
     * Return next hop targets.
     *
     * @return next hop targets
     */
    public NetAddress[] getTargets()
    {
      return targets;
    }

    /**
     * Return encapsulated query.
     *
     * @return encapsulated query
     */
    public RouteInterface.Zrp.MessageIerp getPayload()
    {
      return encapsulated;
    }

    /** {@inheritDoc} */
    public int getSize()
    {
      return FIXED_SIZE+4*targets.length+encapsulated.getSize();
    }

    /** {@inheritDoc} */
    public void getBytes(byte[] b, int offset)
    {
      throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return "brp(targets=["+Util.stringJoin(targets, ",")+"], data="+encapsulated;
    }
  } // class: MessageBRP

  //////////////////////////////////////////////////
  // locals
  //

  /** reference to zrp routing framework. */
  private RouteZrp zrp;
  /** bordercast supression table: QueryKey - QueryCoverageEntry. */
  private HashMap queryCoverage;

  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Create new "default" BRP (ZRP sub-protocol) handler.
   *
   * @param zrp object reference to zrp routing framework
   */
  public RouteZrpBrp(RouteZrp zrp)
  {
    if(JistAPI.isEntity(zrp)) throw new IllegalArgumentException("expecting object reference");
    this.zrp = zrp;
    queryCoverage = new HashMap();
  }

  /**
   * Create new "default" BRP (ZRP sub-protocol) handler.
   *
   * @param zrp object reference to zrp routing framework
   * @param config configuration string
   */
  public RouteZrpBrp(RouteZrp zrp, String config)
  {
    this(zrp);
  }

  //////////////////////////////////////////////////
  // helpers
  //

  /**
   * Return all the uncovered peripheral nodes.
   *
   * @param from source of bordercast
   * @return list of uncovered peripheral nodes; 
   *   null indicates all peripheral nodes are covered
   */
  private Collection getUncoveredPeripheral(Vector from)
  {
    // check already locally processed
    if(from==null) return null;
    // find all peripheral
    Collection result = zrp.getIarp().getPeripheral();
    // remove covered
    for(int i=0; i<from.size(); i++)
    {
      Collection covered = zrp.getIarp().computeCoverage((NetAddress)from.elementAt(i), zrp.getRadius());
      result.removeAll(covered);
      if(result.size()==0) break;
    }
    return result;
  }

  /**
   * Compute first hop in bordercast tree (without minimization).
   *
   * @param from sources that have already processed this query
   * @return array of addresses of first hop neighbours on bordercast tree
   */
  private NetAddress[] bordercastNeighbours(Vector from)
  {
    Collection uncovered = getUncoveredPeripheral(from);
    if(uncovered==null) return NetAddress.EMPTY_ARRAY;
    HashSet neighbours = new HashSet();
    Iterator it = uncovered.iterator();
    while(it.hasNext())
    {
      neighbours.add(zrp.getIarp().getRoute((NetAddress)it.next())[0]);
    }
    return (NetAddress[])(new Vector(neighbours)).toArray(NetAddress.EMPTY_ARRAY);
  }

  /**
   * Compute first hop in bordercast tree using greedy neighbour minimisation heuristic.
   *
   * @param from sources that have already processed this query
   * @return array of addresses of first hop neighbours on bordercast tree
   */
  private NetAddress[] bordercastNeighbours2(Vector from)
  {
    // check whether allready processed
    if(from==null) return null;
    // start with all uncovered peripheral nodes
    Collection remainingUncovered = getUncoveredPeripheral(from);
    // check if there are any peripherals left
    if(remainingUncovered.size()==0) return NetAddress.EMPTY_ARRAY;
    // compute neighbours to target
    Collection neighbours = new Vector();
    Map closestNeighbours = computeClosestNeighbours();

    // iteratively pick "best" neighbour node in greedy manner
    while(remainingUncovered.size()>0)
    {
      // compute counts (and max) of closest neighbours of remaining peripherals
      Map neighbourCount = new HashMap();
      Iterator it = remainingUncovered.iterator();
      int max = 0;
      while(it.hasNext())
      {
        NetAddress peripheral = (NetAddress)it.next();
        Iterator it2 = ((Set)closestNeighbours.get(peripheral)).iterator();
        while(it2.hasNext())
        {
          NetAddress neighbour = (NetAddress)it2.next();
          SingletonInt count = null;
          if(neighbourCount.containsKey(neighbour))
          {
            count = (SingletonInt)neighbourCount.get(neighbour);
          }
          else
          {
            count = new SingletonInt();
            neighbourCount.put(neighbour, count);
          }
          count.i++;
          if(count.i>max) max=count.i;
        }
      }
      // pick some neighbour closest to max peripheral nodes
      it = neighbourCount.entrySet().iterator();
      NetAddress neighbour = null;
      while(it.hasNext())
      {
        Map.Entry e = (Map.Entry)it.next();
        SingletonInt count = (SingletonInt)e.getValue();
        if(count.i==max) 
        {
          neighbour = (NetAddress)e.getKey();
          break;
        }
      }
      // add it to targetted neighbours
      neighbours.add(neighbour);
      // remove all peripheral nodes closest to that neighbour node
      it = remainingUncovered.iterator();
      while(it.hasNext())
      {
        NetAddress peripheral = (NetAddress)it.next();
        if(((Set)closestNeighbours.get(peripheral)).contains(neighbour)) it.remove();
      }
    }
    return (NetAddress[])neighbours.toArray(NetAddress.EMPTY_ARRAY);
  }

  /**
   * For each node in the zone, compute the set of closest neighbours.
   *
   * @return map of NetAddress to Set of neighbour addresses
   */
  private Map computeClosestNeighbours()
  {
    NetAddress src = zrp.getLocalAddr();
    RouteInterface.Zrp.Iarp iarp = zrp.getIarp();
    Map closestNeighbours = new HashMap();
    Set inside = new HashSet();
    // insert source node inside, so as not to backtrack
    inside.add(src);
    // now, seed with first-hop (neighbours)
    Enumeration e = iarp.getLinks(src);
    Vector horizon = new Vector();
    while(e.hasMoreElements())
    {
      Set s = new HashSet();
      NetAddress n = (NetAddress)e.nextElement();
      s.add(n);
      closestNeighbours.put(n, s);
      horizon.add(n);
    }
    // expand to border
    int radius = zrp.getRadius();
    while(radius>0)
    {
      radius--;
      for(int i=0; i<horizon.size(); i++)
      {
        inside.add((NetAddress)horizon.elementAt(i));
      }
      Vector horizon2 = new Vector();
      while(horizon.size()>0)
      {
        src = (NetAddress)horizon.remove(horizon.size()-1);
        e = iarp.getLinks(src);
        while(e.hasMoreElements())
        {
          NetAddress dst = (NetAddress)e.nextElement();
          if(inside.contains(dst)) continue;
          Set s = null;
          if(closestNeighbours.containsKey(dst))
          {
            s = (Set)closestNeighbours.get(dst);
          }
          else
          {
            s = new HashSet();
            closestNeighbours.put(dst, s);
            horizon2.add(dst);
          }
          s.addAll((Set)closestNeighbours.get(src));
        }
      }
      horizon = horizon2;
    }
    return closestNeighbours;
  }

  /**
   * Return query coverage entry for given source and identifier. Create a new
   * query coverage entry if one does not exist.
   *
   * @param src query source address
   * @param seq query source identifier
   * @param target whether node was targetted in query broadcast
   * @return query coverage
   */
  private QueryCoverageEntry getQueryCoverageEntry(NetAddress src, short seq, boolean target)
  {
    QueryKey qk = new QueryKey(src, seq);
    QueryCoverageEntry qce = (QueryCoverageEntry)queryCoverage.get(qk);
    if(qce==null)
    {
      qce = new QueryCoverageEntry(target);
      queryCoverage.put(qk, qce);
    }
    return qce;
  }

  //////////////////////////////////////////////////
  // RouteZrpBrp implementation
  //

  /** {@inheritDoc} */
  public void start()
  {
    zrp.getProxy().timeout(this);
  }

  /** {@inheritDoc} */
  public void receive(RouteInterface.Zrp.MessageBrp msg, NetAddress from)
  {
    if(msg==null) throw new NullPointerException("null BRP message received");
    if(from==null) throw new NullPointerException("null BRP from address");
    MessageBrp msgImpl = (MessageBrp)msg;
    if(logBRP.isDebugEnabled())
    {
      logBRP.debug("receive t="+JistAPI.getTime()+" at="+zrp.getLocalAddr()+" from="+from+" msg="+msgImpl);
    }
    QueryCoverageEntry qce = getQueryCoverageEntry(msgImpl.getSrc(), msgImpl.getID(), false);
    qce.addFrom(from);
    if(Util.contains(msgImpl.getTargets(), zrp.getLocalAddr()))
    {
      qce.setTarget();
    }
    else
    {
      qce.forwarded();
    }
    if(!qce.isProcessed())
    {
      // pass up to ierp
      JistAPI.sleep(Util.randomTime(2*JITTER));
      zrp.getProxy().receive(msgImpl.getPayload(), from, null, (byte)Constants.NET_INTERFACE_INVALID, null, Constants.NET_PRIORITY_NORMAL, (byte)1);
      qce.processed();
    }
  }

  /** {@inheritDoc} */
  public void send(RouteInterface.Zrp.MessageIerp msg)
  {
    if(logBRP.isDebugEnabled())
    {
      logBRP.debug("request t="+JistAPI.getTimeString()+" from="+zrp.getLocalAddr()+" msg="+msg);
    }
    QueryCoverageEntry qce = getQueryCoverageEntry(msg.getSrc(), msg.getID(), true);
    if(!qce.isForwarded() && qce.isTarget())
    {
      // compute next hop in bordercast tree, if any
      NetAddress[] neighbours = bordercastNeighbours2(qce.getFrom());
      if(neighbours.length>0)
      {
        MessageBrp brp = new MessageBrp(msg, neighbours);
        if(logBRP.isInfoEnabled())
        {
          logBRP.info("send t="+JistAPI.getTimeString()+" from="+zrp.getLocalAddr()+" msg="+brp);
        }
        zrp.broadcast(brp);
      }
      // never process this query again
      qce.forwarded();
    }
  }

  //////////////////////////////////////////////////
  // timers
  //

  /** {@inheritDoc} */
  public void timeout()
  {
    // flush query coverage table
    final long currentTime = JistAPI.getTime();
    Iterator it = queryCoverage.values().iterator();
    while(it.hasNext())
    {
      QueryCoverageEntry qce = (QueryCoverageEntry)it.next();
      if(qce.time+COVERAGE_LIFETIME < currentTime) it.remove();
    }
    // schedule another one
    JistAPI.sleep(Util.randomTime(2*COVERAGE_REFRESH));
    zrp.getProxy().timeout(this);
  }

} // class: RouteZrpBrp

