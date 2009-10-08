//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RouteInterface.java Tue 2004/04/06 11:35:24 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.route;

import jist.swans.mac.MacAddress;
import jist.swans.net.NetAddress;
import jist.swans.net.NetMessage;
import jist.swans.net.NetInterface;
import jist.swans.misc.Message;
import jist.swans.misc.Timer;
import jist.swans.misc.Protocol;

import jist.runtime.JistAPI;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;

/**
 * Defines the interface of all Routing implementations and the Route entity.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: RouteInterface.java,v 1.48 2004-04-06 16:07:50 barr Exp $
 * @since SWANS1.0
 */
public interface RouteInterface extends NetInterface.NetHandler, JistAPI.Proxiable
{

  /**
   * Called by the network layer for every incoming packet. A routing
   * implementation may wish to look at these packets for informational
   * purposes, but should not change their contents.
   *
   * @param msg incoming packet
   * @param lastHop last link-level hop of incoming packet
   */
  void peek(NetMessage msg, MacAddress lastHop);

  /**
   * Called by the network layer to request transmission of a packet that 
   * requires routing. It is the responsibility of the routing layer to provide
   * a best-effort transmission of this packet to an appropriate next hop by
   * calling the network layer sending routines once this routing information
   * becomes available.
   *
   * @param msg outgoing packet
   */
  void send(NetMessage msg);


  //////////////////////////////////////////////////
  // ZRP
  //

  /**
   * ZRP routing entity interface.
   */
  public static interface Zrp extends RouteInterface, Protocol
  {
    /** 
     * Process timer expiration. 
     *
     * @param t timer that expired
     */
    void timeout(Timer t);

    /** 
     * General ZRP (Zone Routing Protocol) packet interface.
     */
    public static interface MessageZrp extends Message
    {
    }



    //////////////////////////////////////////////////
    // NDP
    //

    /** 
     * NDP (Node Discovery Protocol) packet interface.
     */
    public static interface MessageNdp extends MessageZrp
    {
    }

    /** 
     * Node Discovery (sub)Protocol interface.
     */
    public static interface Ndp extends Protocol
    {
      /**
       * Process incoming NDP packet.
       *
       * @param msg ndp packet
       * @param src network address of incoming packet
       * @param macAddr link source address of incoming packet
       * @param macId interface of incoming packet
       */
      void receive(MessageNdp msg, NetAddress src, MacAddress macAddr, byte macId);

      /**
       * Return mac address of neighbour.
       *
       * @param addr ip address of neighbour
       * @return mac address of neighbour, or null if unknown
       */
      MacAddress getMacAddress(NetAddress addr);

      /**
       * Return mac entity for neighbour.
       *
       * @param addr ip address of neighbour
       * @return mac interface id for neighbour, or null if unknown
       */
      byte getMacId(NetAddress addr);

      /**
       * Return neighbours.
       *
       * @return array of neighbours
       */
      NetAddress[] getNeighbours();

      /**
       * Return number of neighbours.
       *
       * @return number of neighbours
       */
      int getNumNeighbours();

      /**
       * Whether given address is a neighbour.
       *
       * @param addr address to check
       * @return whether given address is a neighbour
       */
      boolean isNeighbour(NetAddress addr);

    } // interface: Zrp.Ndp


    //////////////////////////////////////////////////
    // IARP
    //

    /** 
     * IARP (IntrAzone Routing Protocol) packet interface.
     */
    public static interface MessageIarp extends MessageZrp
    {
    }

    /** 
     * IntrAzone Routing (sub)Protocol interface.
     *
     * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
     * @version $Id: RouteInterface.java,v 1.48 2004-04-06 16:07:50 barr Exp $
     * @since SWANS1.0
     */
    public static interface Iarp extends Protocol
    {
      /**
       * Process incoming IARP packet.
       *
       * @param msg iarp packet
       * @param from source of iarp packet
       */
      void receive(MessageIarp msg, NetAddress from);

      /**
       * Notify IARP of link state change.
       * 
       * @param link link that has changed
       * @param drop whether link has failed (or been created)
       */
      void linkinfo(Link link, boolean drop);

      /**
       * Return number of intra-zone links.
       *
       * @return number of intra-zone links
       */
      int getNumLinks();

      /**
       * Return iterator of links from given source address.
       *
       * @param src link source address
       * @return iterator of known link destination addresses
       */
      Enumeration getLinks(NetAddress src);

      /**
       * Return whether node is within zone (if IARP has route to it).
       *
       * @param dst destination node
       * @return whether node is within zone
       */
      boolean hasRoute(NetAddress dst);

      /**
       * Return intra-zone route.
       *
       * @param dst destination node
       * @return route to node, or NullPointerException if dst not in zone
       */
      NetAddress[] getRoute(NetAddress dst);

      /**
       * Return number of intra-zone routes.
       *
       * @return number of intra-zone routes
       */
      int getNumRoutes();

      /**
       * Return all peripheral nodes.
       *
       * @return set of peripheral nodes
       */
      Collection getPeripheral();

      /**
       * Find the set of nodes within a given distance of source.
       *
       * @param src source node
       * @param depth radius
       * @return set of node addresses within distance of source
       */
      Set computeCoverage(NetAddress src, int depth);

      /**
       * Display all intra-zone links known at node.
       */
      void showLinks();

      /**
       * Display all intra-zone routes known at node.
       */
      void showRoutes();

    } // interface: Zrp.Iarp


    //////////////////////////////////////////////////
    // BRP
    //

    /** 
     * BRP (Bordercast Resolution Protocol) packet interface.
     */
    public static interface MessageBrp extends MessageZrp
    {
    }

    /** 
     * Protocol: Bordercast Resolution (sub)Protocol interface.
     *
     */
    public static interface Brp extends Protocol
    {
      /**
       * Process incoming BRP packet.
       *
       * @param msg brp packet
       * @param from source of incoming brp packet
       */
      void receive(MessageBrp msg, NetAddress from);

      /**
       * Bordercast a query.
       *
       * @param msg query to bordercast
       */
      void send(MessageIerp msg);

    } // interface: Zrp.Brp


    //////////////////////////////////////////////////
    // IERP
    //

    /** 
     * IERP (IntErzone Routing Protocol) packet interface.
     */
    public static interface MessageIerp extends MessageZrp
    {
      /**
       * Return packet identifier, if available.
       *
       * @return packet identifier
       * @throws UnsupportedOperationException information not available
       */
      short getID() throws UnsupportedOperationException;

      /** 
       * Return packet source address, if available.
       *
       * @return packet source address
       * @throws UnsupportedOperationException information not available
       */
      NetAddress getSrc() throws UnsupportedOperationException;

      /**
       * Return packet destination address, if available.
       *
       * @return packet destination address
       * @throws UnsupportedOperationException information not available
       */
      NetAddress getDst() throws UnsupportedOperationException;

    } // interface: MessageIERP


    /** 
     * IntErzone Routing (sub)Protocol interface.
     */
    public static interface Ierp extends Protocol
    {
      /**
       * Process incoming IERP packet.
       *
       * @param msg ierp packet
       */
      void receive(MessageIerp msg);

      /**
       * Route and send given network message.
       *
       * @param msg network message to route and send
       */
      void send(NetMessage.Ip msg);

      /**
       * Process IARP signal to IERP that zone information has changed.
       */
      void zoneChanged();

    } // interface: Zrp.Ierp

  } // interface: Zrp

  //////////////////////////////////////////////////
  // DSR
  //

  /**
   * DSR routing entity interface.
   */
  public static interface Dsr extends RouteInterface
  {
    /**
     * Contains a packet and the time it was inserted into the buffer.
     */
    public class BufferedPacket
    {
      /** The buffered packet. */
      public NetMessage.Ip msg;

      /** The time it was inserted into the buffer. */
      public long bufferTime;
      
      /**
       * Creates a new BufferedPacket.
       *
       * @param msg the packet to buffer.
       */
      public BufferedPacket(NetMessage.Ip msg)
      {
        this.msg = msg;
        this.bufferTime = JistAPI.getTime();
      }
    }

    /**
     * Initiates route discovery for the given destination.  Route requests
     * are retransmitted if no reply is received after a timeout elapses.
     *
     * @param dest      the destination to which a route is being sought
     * @param requestId the ID number of this route discovery
     */ 
    void DiscoverRoute(NetAddress dest, short requestId);

    /**
     * Transmits the given packet with a request for a network-level
     * acknowledgement.  The packet is retransmitted if no acknowledgement
     * is received before the timeout elapses.
     *
     * @param msg            the message to send
     * @param ackId          the ID number of the acknowledgement request
     * @param timeout        the timeout before retransmitting the packet
     * @param numRetransmits the number of times <code>msg</code> has
     *   already been retransmitted
     */
    void TransmitWithNetworkAck(NetMessage.Ip msg, Short ackId,
                                long timeout, int numRetransmits);

    /**
     * Transmits the given packet and waits for a passive acknowledgement.  If
     * no passive acknowledgement is received before a timeout occurs, the
     * packet is retransmitted.  If no acknowledgement is received after
     * <code>TRY_PASSIVE_ACKS</code> attempts, the packet is retransmitted
     * with a request for a network-level acknowledgement.
     *
     * @param msg            the message to be sent
     * @param numRetransmits the number of times <code>msg</code> has already
     *   been retransmitted
     */ 
    void TransmitWithPassiveAck(NetMessage.Ip msg, int numRetransmits);

    /**
     * Adds an entry into the gratuitous route reply table.
     *
     * @param originator the originator of the packet being replied to
     * @param lastHop    the last-hop address of the packet being replied to
     */
    void AddRouteReplyEntry(NetAddress originator, NetAddress lastHop);

    /**
     * Removes an entry from the gratuitous route reply table.
     *
     * @param originator the originator of the packet being removed
     * @param lastHop    the last-hop address of the packet being removed
     */
    void DeleteRouteReplyEntry(NetAddress originator, NetAddress lastHop);

    /**
     * Inserts an packet into the send buffer.
     *
     * @param msg the packet to insert into the send buffer
     */
    void InsertBuffer(NetMessage.Ip msg);

    /**
     * Removes an entry from the send buffer.
     *
     * @param packet the <code>BufferedPacket</code> to be removed from the
     *   send buffer
     */
    void DeleteBuffer(BufferedPacket packet);
  } // class: DSR


  //////////////////////////////////////////////////
  // AODV
  //

  /**
   * AODV routing entity interface.
   */
  public static interface Aodv extends RouteInterface, Protocol
  {
    /**
     * AODV Timeout event, which gets called periodically.
     * 
     * Clears expired RREQ buffer entries.
     * Sends hello messages.
     * Updates wait counters, and checks for idle outgoing-nodes
     */
    void timeout();
    
    /**
     * This event is called periodically after a route request is originated, until
     * a route has been found.
     * 
     * Each time it is called, it rebroadcasts the route request message with a new
     * rreq id and incremented TTL. 
     * 
     * @param rreqObj RouteRequest object
     */    
    void RREQtimeout(Object rreqObj);
    
    /**
     * Sends IP message after transmission delay, and renews precursor list entry.
     * 
     * @param ipMsg IP message to send
     * @param destMacAddr next hop mac address
     */
    void sendIpMsg(NetMessage.Ip ipMsg, MacAddress destMacAddr);
    
  } // class: AODV

} // class: RouteInterface

