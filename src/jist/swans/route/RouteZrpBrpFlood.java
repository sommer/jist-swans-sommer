//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RouteZrpBrpFlood.java Tue 2004/04/06 11:35:37 barr pompom.cs.cornell.edu>
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

import java.util.Iterator;
import java.util.HashMap;

/** 
 * Zone Routing Protocol: Bordercast Resolution (sub)Protocol: Naive flooding.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: RouteZrpBrpFlood.java,v 1.4 2004-04-06 16:07:51 barr Exp $
 * @since SWANS1.0
 */
public class RouteZrpBrpFlood implements RouteInterface.Zrp.Brp, Timer
{
  /** logger for BRP events. */
  public static final Logger logBRPFlood = RouteZrpBrp.logBRP;

  //////////////////////////////////////////////////
  // constants
  //

  /** query coverage expiration. */
  public static final long COVERAGE_LIFETIME = RouteZrpBrp.COVERAGE_LIFETIME;
  /** query coverage refresh timer (check for expiration). */
  public static final long COVERAGE_REFRESH  = RouteZrpBrp.COVERAGE_REFRESH;
  /** brp delivery jitter. */
  public static final long JITTER = RouteZrpBrp.JITTER;


  /**
   * Query flooding packet.
   *  <pre>
   *   query source                  size: 4  -- encapsulated
   *   query destination             size: 4  -- encapsulated
   *   query ID                      size: 2  -- encapsulated
   *   encapsulated packet           size: *
   *  </pre>
   */
  private static class MessageBrpFlood implements RouteInterface.Zrp.MessageBrp
  {
    /** encapsulated query. */
    private RouteInterface.Zrp.MessageIerp encapsulated;

    /**
     * Create new bordercast packet.
     *
     * @param query encapsulated query packet
     */
    public MessageBrpFlood(RouteInterface.Zrp.MessageIerp query)
    {
      this.encapsulated = query;
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
      return encapsulated.getSize();
    }

    /** {@inheritDoc} */
    public void getBytes(byte[] b, int offset)
    {
      throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return "flood(data="+encapsulated;
    }
  } // class: MessageBRPFlood

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
  public RouteZrpBrpFlood(RouteZrp zrp)
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
  public RouteZrpBrpFlood(RouteZrp zrp, String config)
  {
    this(zrp);
  }

  //////////////////////////////////////////////////
  // helpers
  //

  /**
   * Return query coverage entry for given source and identifier. Create a new
   * query coverage entry if one does not exist.
   *
   * @param src query source address
   * @param seq query source identifier
   * @return query coverage
   */
  private RouteZrpBrp.QueryCoverageEntry getQueryCoverageEntry(NetAddress src, short seq)
  {
    RouteZrpBrp.QueryKey qk = new RouteZrpBrp.QueryKey(src, seq);
    RouteZrpBrp.QueryCoverageEntry qce = (RouteZrpBrp.QueryCoverageEntry)queryCoverage.get(qk);
    if(qce==null)
    {
      qce = new RouteZrpBrp.QueryCoverageEntry(false);
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
    MessageBrpFlood msgImpl = (MessageBrpFlood)msg;
    if(logBRPFlood.isDebugEnabled())
    {
      logBRPFlood.debug("receive t="+JistAPI.getTime()+" at="+zrp.getLocalAddr()+" from="+from+" msg="+msgImpl);
    }
    RouteZrpBrp.QueryCoverageEntry qce = getQueryCoverageEntry(msgImpl.getSrc(), msgImpl.getID());
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
    if(logBRPFlood.isDebugEnabled())
    {
      logBRPFlood.debug("request t="+JistAPI.getTimeString()+" from="+zrp.getLocalAddr()+" msg="+msg);
    }
    RouteZrpBrp.QueryCoverageEntry qce = getQueryCoverageEntry(msg.getSrc(), msg.getID());
    if(!qce.isForwarded())
    {
      // compute next hop in bordercast tree, if any
      MessageBrpFlood brp = new MessageBrpFlood(msg);
      if(logBRPFlood.isInfoEnabled())
      {
        logBRPFlood.info("send t="+JistAPI.getTimeString()+" from="+zrp.getLocalAddr()+" msg="+brp);
      }
      zrp.broadcast(brp);
      // never forward this query again
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
      RouteZrpBrp.QueryCoverageEntry qce = (RouteZrpBrp.QueryCoverageEntry)it.next();
      if(qce.time+COVERAGE_LIFETIME < currentTime) it.remove();
    }
    // schedule another one
    JistAPI.sleep(Util.randomTime(2*COVERAGE_REFRESH));
    zrp.getProxy().timeout(this);
  }

}
