//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <AppHeartbeat.java Tue 2004/04/06 11:59:55 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app;

import jist.swans.mac.MacAddress;
import jist.swans.net.NetInterface;
import jist.swans.net.NetAddress;
import jist.swans.misc.Message;
import jist.swans.misc.Util;
import jist.swans.Constants;

import jist.runtime.JistAPI;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Heartbeat application.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: AppHeartbeat.java,v 1.13 2004-04-06 16:07:46 barr Exp $
 * @since SWANS1.0
 */
public class AppHeartbeat implements AppInterface, NetInterface.NetHandler
{
  //////////////////////////////////////////////////
  // neighbour table entry
  //

  /**
   * Neighbour entry information.
   */
  private static class NeighbourEntry
  {
    /** mac address of neighbour. */
    public MacAddress mac;
    /** heartbeats until expiration. */
    public int beats;
  }

  //////////////////////////////////////////////////
  // constants
  //

  /** minimum heartbeat  period. */
  public static final long  HEARTBEAT_MIN  = 2 * Constants.SECOND;
  /** maximum heartbeat  period. */
  public static final long  HEARTBEAT_MAX  = 5 * Constants.SECOND;
  /** throw out information older than FRESHNESS beats. */
  public static final short FRESHNESS  = 5;

  //////////////////////////////////////////////////
  // messages
  //

  /**
   * Heartbeat packet.
   */
  private static class MessageHeartbeat implements Message
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
  } // class: MessageHeartbeat

  //////////////////////////////////////////////////
  // locals
  //

  /** network entity. */
  private NetInterface netEntity;
  /** self-referencing proxy entity. */
  private Object self;
  /** list of neighbours. */
  private HashMap neighbours;
  /** node identifier. */
  private int nodenum;
  /** whether to display application output. */
  private boolean display;

  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Create new heartbeat application instance.
   *
   * @param nodenum node identifier
   * @param display whether to display application output
   */
  public AppHeartbeat(int nodenum, boolean display)
  {
    this.nodenum = nodenum;
    this.self = JistAPI.proxyMany(
        this, new Class[] { AppInterface.class, NetInterface.NetHandler.class });
    this.display = display;
    neighbours = new HashMap();
  }

  //////////////////////////////////////////////////
  // entity
  //

  /**
   * Set network entity.
   *
   * @param netEntity network entity
   */
  public void setNetEntity(NetInterface netEntity)
  {
    this.netEntity = netEntity;
  }

  /**
   * Return self-referencing NETWORK proxy entity.
   *
   * @return self-referencing NETWORK proxy entity
   */
  public NetInterface.NetHandler getNetProxy()
  {
    return (NetInterface.NetHandler)self;
  }

  /**
   * Return self-referencing APPLICATION proxy entity.
   *
   * @return self-referencing APPLICATION proxy entity
   */
  public AppInterface getAppProxy()
  {
    return (AppInterface)self;
  }

  //////////////////////////////////////////////////
  // neighbour events
  //

  /**
   * Neighbour lost.
   *
   * @param mac mac adddress of neighbour lost
   */
  private void neighbourLost(MacAddress mac)
  {
    if(display)
    {
      System.out.println("("+nodenum+") lost neighbour:  "+mac+", t="+Util.timeSeconds());
    }
  }

  /**
   * Neighbour discovered.
   *
   * @param mac mac address of neighbour discovered
   */
  private void neighbourDiscovered(MacAddress mac)
  {
    if(display)
    {
      System.out.println("("+nodenum+") found neighbour: "+mac+", t="+Util.timeSeconds());
    }
  }

  //////////////////////////////////////////////////
  // NetHandler methods
  //

  /** {@inheritDoc} */
  public void receive(Message msg, NetAddress src, MacAddress lastHop, 
      byte macId, NetAddress dst, byte priority, byte ttl)
  {
    // System.out.println("("+nodenum+") received packet from ip="+src+" mac="+lastHop+" at t="+Util.timeSeconds());
    NeighbourEntry n = (NeighbourEntry)neighbours.get(src);
    if(n==null)
    {
      neighbourDiscovered(lastHop);
      n = new NeighbourEntry();
      neighbours.put(src, n);
    }
    n.mac = lastHop;
    n.beats = FRESHNESS;
  }

  //////////////////////////////////////////////////
  // AppInterface methods
  //

  /**
   * Compute random heartbeat delay.
   *
   * @return delay to next heartbeat
   */
  private long calcDelay()
  {
    return HEARTBEAT_MIN + (long)((HEARTBEAT_MAX-HEARTBEAT_MIN)*Constants.random.nextFloat());
  }

  /** {@inheritDoc} */
  public void run(String[] args)
  {
    // staggered beginning
    if(JistAPI.getTime()==0)
    {
      JistAPI.sleep(calcDelay());
    }
    // send heartbeat
    Message msg = new MessageHeartbeat();
    netEntity.send(msg, NetAddress.ANY, Constants.NET_PROTOCOL_HEARTBEAT,
        Constants.NET_PRIORITY_NORMAL, (byte)1);
    // process neighbour set
    Iterator it = neighbours.values().iterator();
    while(it.hasNext())
    {
      NeighbourEntry n = (NeighbourEntry)it.next();
      n.beats--;
      if(n.beats==0)
      {
        neighbourLost(n.mac);
        it.remove();
      }
    }
    // schedule next
    JistAPI.sleep(calcDelay());
    ((AppInterface)self).run();
  }

  /** {@inheritDoc} */
  public void run()
  {
    run(null);
  }

}
