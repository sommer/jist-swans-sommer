//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RouteDsr.java Tue 2004/04/06 11:35:09 barr pompom.cs.cornell.edu>
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
import jist.swans.misc.Pickle;
import jist.swans.Constants;

import jist.runtime.JistAPI;

import org.apache.log4j.Logger;

import java.util.*;

/**
 * An implementation of the Dynamic Source Routing protocol.
 *
 * @see <a href="http://www.ietf.org/internet-drafts/draft-ietf-manet-dsr-09.txt">
 *   DSR Specification</a>
 * @author Ben Viglietta
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: RouteDsr.java,v 1.30 2004-11-25 17:52:17 barr Exp $
 * @since SWANS1.0
 */
public class RouteDsr implements RouteInterface.Dsr
{
  //////////////////////////////////////////////////
  // constants
  //

  /** The maximum amount of jitter before sending a packet. */ 
  public static final long BROADCAST_JITTER       = 10 * Constants.MILLI_SECOND;
  /** The maximum amount of time a packet can remain in the Send Buffer. */
  public static final long SEND_BUFFER_TIMEOUT    = 30 * Constants.SECOND;
  /** The initial timeout before retransmitting a Route Request. */
  public static final long REQUEST_PERIOD         = 500 * Constants.MILLI_SECOND;
  /** The maximum timeout before retransmitting a Route Request. */
  public static final long MAX_REQUEST_PERIOD     = 10 * Constants.SECOND;
  /**
   * The maximum number of times a packet will be retransmitted using
   * network-level acknowledgements.
   */
  public static final int  MAX_MAINT_REXMT        = 2;
  /**
   * The timeout before retransmitting a packet using network-level
   * acknowledgements.
   */
  public static final long MAINT_PERIOD           = 500 * Constants.MILLI_SECOND;
  /** The minimum time between sending gratuitous Route Replies. */
  public static final long GRAT_REPLY_HOLDOFF     = 1 * Constants.SECOND;
  /**
   * The timeout before retransmitting a packet using passive
   * acknowledgements.
   */
  public static final long PASSIVE_ACK_TIMEOUT    = 100 * Constants.MILLI_SECOND;
  /**
   * The number of times to try retransmission using passive
   * ackknowledgments.
   */
  public static final int  TRY_PASSIVE_ACKS       = 1;
  /**
   * The maximum number of ID values to store in a single Route Request
   * Table entry.
   */
  public static final int  MAX_REQUEST_TABLE_IDS  = 16;

  /** The maximum Time-To-Live for a DSR packet. */
  public static final byte MAX_TTL             = (byte)255;
  /** The maximum number of times a packet can be salvaged. */
  public static final int  MAX_SALVAGE_COUNT   = 15;

  /*
  Unused at the present
  public static final long MAX_PACKET_BUFFER_TIME = 11110 * Constants.SECOND;
  */


  //////////////////////////////////////////////////
  // locals
  //

  /** DSR logger. */
  private static Logger log = Logger.getLogger(RouteDsr.class.getName());

  /**
   * An entry in the Route Request Table.
   */
  private class RouteRequestTableEntry
  {
    /** The IP TTL on the last Route Request for this destination. */
    public byte lastRequestTTL;

    /** The time of the last Route Request for this destination. */
    public long lastRequestTime;

    /**
     * The number of Route Requests for this destination since we last
     * received a valid Route Reply.
     */
    public int numRequestsSinceLastReply;

    /**
     * The amount of time necessary to wait (starting at lastRequestTime)
     * before sending out another Route Request.
     */
    public long timeout;

    /** Identification values of recently seen requests coming from this node. */
    public LinkedList ids;

    /** Creates a new RouteRequestTableEntry. */
    public RouteRequestTableEntry()
    {
      lastRequestTTL = MAX_TTL;
      lastRequestTime = JistAPI.getTime();
      numRequestsSinceLastReply = 0;
      timeout = REQUEST_PERIOD;
      ids = new LinkedList();
    }
  }

  /** An entry in the Gratuitous Route Reply Table. */
  private class RouteReplyTableEntry
  {
    /** The originator of the shortened Source Route. */
    public NetAddress originator;

    /**
     * The last hop address of the shortened Source Route before reaching
     * this node.
     */
    public NetAddress lastHop;

    /**
     * Creates a new <code>RouteReplyTableEntry</code>.
     *
     * @param o the originator of the shortened Source Route
     * @param l the last hop address of the shortened Source Route
     */
    public RouteReplyTableEntry(NetAddress o, NetAddress l)
    {
      originator = o;
      lastHop = l;
    }

    /** {@inheritDoc} */
    public int hashCode()
    {
      return originator.hashCode() + lastHop.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object o)
    {
      if (o == null || !(o instanceof RouteReplyTableEntry)) return false;

      RouteReplyTableEntry other = (RouteReplyTableEntry)o;
      return other.originator.equals(originator) && other.lastHop.equals(lastHop);
    }
  }

  /**
   * The Gratuitous Route Reply Table is a set of
   * <code>RouteReplyTableEntry</code>s indicating which nodes have recently
   * triggered gratuitous Route Replies because of automatic route shortening.
   */
  private HashSet routeReplyTable;

  /**
   * Entries in the Maintenance Buffer correspond to messages that have
   * been sent and are currently waiting passive acknowledgement.  An
   * overheard message is taken as a passive acknowledgement of a previously
   * sent message if they have equal source addresses, destination addresses,
   * protocol numbers, id numbers, fragmentation offsets, and if the
   * Segments Left field of the Source Route option of the overheard
   * message has a lower value than the corresponding field in the sent
   * message.
   */
  private class MaintenanceBufferEntry
  {
    /** Source address. */
    public NetAddress src;

    /** Destination address. */
    public NetAddress dest;

    /** Network protocol. */
    public short protocol;

    /** IP Identification number. */
    public short id;

    /** IP Fragmentation Offset. */
    public short fragOffset;

    /**
     * Creates a new <code>MaintenanceBufferEntry</code>.
     *
     * @param src        source address
     * @param dest       destination address
     * @param protocol   network protocol
     * @param id         IP Identification number
     * @param fragOffset IP Fragmentation Offset
     */
    public MaintenanceBufferEntry(NetAddress src, NetAddress dest,
                                  short protocol, short id,
                                  short fragOffset)
    {
      this.src = src;
      this.dest = dest;
      this.protocol = protocol;
      this.id = id;
      this.fragOffset = fragOffset;
    }

    /** {@inheritDoc} */
    public int hashCode()
    {
      return src.hashCode() + dest.hashCode() + protocol + id + fragOffset;
    }

    /** {@inheritDoc} */
    public boolean equals(Object o)
    {
      if (o == null || !(o instanceof MaintenanceBufferEntry)) return false;

      MaintenanceBufferEntry other = (MaintenanceBufferEntry)o;

      return other.src.equals(src) && other.dest.equals(dest) &&
        other.protocol == protocol && other.id == id &&
        other.fragOffset == fragOffset;
    }
  }

  /**
   * Maintenance Buffer.  Only messages awaiting passive acknowledgement
   * (not messages awaiting network-level acknowledgement) are stored in here.
   * It maps <code>MaintenanceBufferEntry</code>s to <code>Integer</code>s
   * representing the Segments Left field of the Source Route option of the
   * corresponding message.
   */
  private Hashtable maintenanceBuffer;

  /** The interface to the network layer. */
  private NetInterface netEntity;

  /** The IP address of this node. */
  private NetAddress localAddr;

  /**
   * The route cache maps <code>NetAddress</code>es (destinations) to
   * <code>LinkedList</code>s of arrays of <code>NetAddress</code>es (routes).
   * Right now the route cache has no sophisticated timeout or replacement
   * policies.
   */
  private Hashtable routeCache;

  /** List of <code>BufferedPacket</code>s waiting to be sent. */
  private LinkedList sendBuffer;

  /**
   * The route request table maps <code>NetAddress</code>es(destinations) to
   * <code>RouteRequestTableEntry</code>s, which are structures containing various
   * information used when performing Route Discovery.
   */
  private Hashtable routeRequestTable;

  /** The next ID number to use when sending a route request. */ 
  private short nextRequestId;

  /** The next ID number to use when sending an acknowledgement request. */
  private short nextAckId;

  /**
   * Set of <code>NetAddress</code>es of destinations of currently active
   * Route Requests.
   */
  private HashSet activeRequests;

  /** Set of <code>Short</code>s indicating outstanding acknowledgement requests. */
  private HashSet activeAcks;

  /** The proxy interface for this object. */
  private RouteInterface.Dsr self;


  //////////////////////////////////////////////////
  // initialization
  //

  /** 
   * Creates a new RouteDsr object.
   *
   * @param localAddr local node address
   */
  public RouteDsr(NetAddress localAddr)
  {
    this.localAddr = localAddr;
    InitRouteCache();
    InitBuffer();
    InitRequestTable();
    InitRouteReplyTable();
    InitMaintenanceBuffer();

    nextRequestId = 0;
    nextAckId = 0;
    activeRequests = new HashSet();
    activeAcks = new HashSet();

    self = (RouteInterface.Dsr)JistAPI.proxy(this, RouteInterface.Dsr.class);
  }

  //////////////////////////////////////////////////
  // entity hookup
  //

  /**
   * Sets the interface to the network layer.
   *
   * @param netEntity the interface to the network layer
   */
  public void setNetEntity(NetInterface netEntity)
  {
    this.netEntity = netEntity;
  }

  /**
   * Gets the proxy interface for this object.
   *
   * @return the proxy <code>RouteInterface.Dsr</code> interface for this object
   */
  public RouteInterface.Dsr getProxy()
  {
    return self;
  }

  //////////////////////////////////////////////////
  // Helper methods
  //

  /**
   * Sends a Route Reply to a node that recently sent us a Route Request.
   *
   * @param opt the Route Request option
   * @param src the originator of the Route Request
   */
  private void SendRouteReply(RouteDsrMsg.OptionRouteRequest opt, NetAddress src)
  {
    NetAddress[] routeToHere = new NetAddress[opt.getNumAddresses() + 2];
    routeToHere[0] = src;
    for (int i = 1; i < routeToHere.length-1; i++)
    {
      routeToHere[i] = opt.getAddress(i-1);
    }
    
    routeToHere[routeToHere.length - 1] = localAddr;
    
    NetAddress[] routeFromHere = new NetAddress[routeToHere.length - 2];
    for (int i = 0; i < routeFromHere.length; i++)
    {
      routeFromHere[i] = routeToHere[routeToHere.length - i - 2];
    }
    
    // Add a Route Reply option indicating how to get here from the
    // source and a Source Route option indicating how to get to the
    // source from here.
    RouteDsrMsg reply = new RouteDsrMsg(null);
    reply.addOption(RouteDsrMsg.OptionRouteReply.create(routeToHere));
    
    if (routeFromHere.length > 0)
    {
      reply.addOption(RouteDsrMsg.OptionSourceRoute.create(0,
        routeFromHere.length, routeFromHere));
    }

    NetMessage.Ip replyMsg = new NetMessage.Ip(reply, localAddr,
      src, Constants.NET_PROTOCOL_DSR, Constants.NET_PRIORITY_NORMAL,
      Constants.TTL_DEFAULT);

    JistAPI.sleep((long)(Math.random() * BROADCAST_JITTER));
    Transmit(replyMsg);
  }

  /**
   * Propagates a Route Request to all nodes within range.
   *
   * @param msg        the message containing the Route Request
   * @param opt        the Route Request option
   * @param optBuf     the bytes of the Route Request option
   * @param src        the originator of the Route Request
   * @param dst        the destination of the Route Request (usually broadcast)
   * @param protocol   the IP protocol of the message
   * @param priority   the IP priority of the message
   * @param ttl        the IP time to live of the message
   * @param id         the IP identification of the message
   * @param fragOffset the IP fragmentation offset of the message
   */
  private void ForwardRequest(RouteDsrMsg msg, RouteDsrMsg.OptionRouteRequest opt,
                              byte[] optBuf, NetAddress src, NetAddress dst,
                              short protocol, byte priority, byte ttl,
                              short id, short fragOffset)
  {
    // If I've already forwarded this request, ignore it
    for (int i = 0; i < opt.getNumAddresses(); i++)
    {
      if (localAddr.equals(opt.getAddress(i))) return;
    }

    // To do in future: Check the Route Cache to see if we know a route to the
    // destination
    
    // Clone the message, add this node's address to the Source Route option,
    // and retransmit it.
    RouteDsrMsg newRequest = (RouteDsrMsg)msg.clone();
    List newOptions = newRequest.getOptions();
    newOptions.remove(optBuf);
    NetAddress[] newAddresses = new NetAddress[opt.getNumAddresses() + 1];
    for (int i = 0; i < newAddresses.length-1; i++)
    {
      newAddresses[i] = opt.getAddress(i);
    }
    
    newAddresses[newAddresses.length - 1] = localAddr;
    newRequest.addOption(RouteDsrMsg.OptionRouteRequest.create(opt.getId(),
      opt.getTargetAddress(), newAddresses));

    NetMessage.Ip newRequestIp = new NetMessage.Ip(newRequest, src, dst,
      protocol, priority, (byte)(ttl - 1), id, fragOffset);

    JistAPI.sleep((long)(Math.random() * BROADCAST_JITTER));
    netEntity.send(newRequestIp, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
  }

  /**
   * Processes an incoming Route Request option.  If this request has been seen
   * recently, it is ignored.  Otherwise, if this node knows a route to the
   * desired destination, a Route Reply is sent to the originator of the request.
   * Otherwise, the request is propagated to all nodes within range.
   *
   * @param msg        the <code>RouteDsrMsg</code> containing the request
   * @param opt        the Route Request option
   * @param optBuf     the bytes of the Route Request option
   * @param src        the address of the originator of the Route Request
   * @param dst        the destination address of this request (usually broadcast)
   * @param protocol   the IP protocol of this Route Request (usually DSR)
   * @param priority   the IP priority of this Route Request
   * @param ttl        the IP time to live of this Route Request
   * @param id         the IP identification of this Route Request
   * @param fragOffset the IP fragmentation offset of this Route Request
   */
  private void HandleRequest(RouteDsrMsg msg, RouteDsrMsg.OptionRouteRequest opt,
                             byte[] optBuf, NetAddress src, NetAddress dst,
                             short protocol, byte priority, byte ttl,
                             short id, short fragOffset)
  {
    // If this request came from this node, ignore it
    if (localAddr.equals(src)) return;

    // If we've seen this request lately, ignore it
    if (SeenRequestLately(src, opt.getId())) return;

    if (localAddr.equals(opt.getTargetAddress()))
    {
      // They're looking for this node, so send a reply
      SendRouteReply(opt, src);
    }
    else
    {
      // Otherwise propagate the request
      ForwardRequest(msg, opt, optBuf, src, dst, protocol,
        priority, ttl, id, fragOffset);
    }

    // Make a note of this request in the route request table
    AddRequestId(src, opt.getId());
  }

  /**
   * Processes an incoming Route Reply.  The new route is added to the Route
   * Cache if it is useful and not already in the Route Cache.
   *
   * @param msg   the <code>RouteDsrMsg</code> containing the Route Reply
   * @param reply the Route Reply option
   */
  private void HandleReply(RouteDsrMsg msg, RouteDsrMsg.OptionRouteReply reply)
  {
    NetAddress dest;
    RouteRequestTableEntry entry;

    // Update the Route Request Table
    dest = reply.getAddress(reply.getNumAddresses() - 1);
    entry = (RouteRequestTableEntry)routeRequestTable.get(dest);
    if (entry != null)  entry.numRequestsSinceLastReply = 0;

    activeRequests.remove(dest);

    // Add the route to our Route Cache
    for (int i = 0; i < reply.getNumAddresses()-1; i++)
    {
      if (localAddr.equals(reply.getAddress(i)))
      {
        NetAddress[] route = new NetAddress[reply.getNumAddresses() - 2 - i];
        for (int j = i; j < i + route.length; j++)
        {
          route[j - i] = reply.getAddress(j+1);
        }

        InsertRouteCache(dest, route);
        break;
      }
    }
  }

  /**
   * Determines the intended next recipient of a packet with the given source
   * route option and IP destination.
   *
   * @param sourceRoute the Source Route option (or <code>null</code> if none)
   * @param dst         the destination IP address
   * @return the address of the next recipient of the message.  If the Source
   *   Route option is invalid in some way, <code>null</code> can be returned.
   */
  private NetAddress NextRecipient(RouteDsrMsg.OptionSourceRoute sourceRoute,
                                   NetAddress dst)
  {
    if (sourceRoute == null)
      return dst;

    int curSegment = sourceRoute.getNumAddresses() - sourceRoute.getNumSegmentsLeft();

    if (curSegment < sourceRoute.getNumAddresses())
    {
      return sourceRoute.getAddress(curSegment);
    }
    else if (curSegment == sourceRoute.getNumAddresses())
    {
      return dst;
    }
    else
    {
      return null;
    }
  }

  /**
   * Determines the previous recipient of a packet with the given source
   * route option and IP source.
   *
   * @param sourceRoute the Source Route option (or <code>null</code> if none)
   * @param src         the source IP address
   * @return the address of the previous recipient of this message.  If the
   *   Source Route option is invalid in some way, <code>null</code> can be
   *   returned.
   */
  private NetAddress PrevRecipient(RouteDsrMsg.OptionSourceRoute sourceRoute,
                                   NetAddress src)
  {
    if (sourceRoute == null)
      return src;

    int prevSegment = sourceRoute.getNumAddresses() -
      sourceRoute.getNumSegmentsLeft() - 1;

    if (0 <= prevSegment && prevSegment < sourceRoute.getNumAddresses())
    {
      return sourceRoute.getAddress(prevSegment);
    }
    else if (prevSegment == -1)
    {
      return src;
    }
    else
    {
      return null;
    }
  }

  /**
   * Forwards a DSR packet containing a Source Route option to the next intended
   * recipient.  An Acknowledgement Request option is added to the headers, and
   * the packet is retransmitted if no acknowledgement is received before the
   * allotted timeout elapses.
   *
   * @param msg        the <code>RouteDsrMsg</code> to be forwarded
   * @param opt        the Source Route option
   * @param optBuf     the bytes of the Source Route option
   * @param src        the address of the originator of this packet
   * @param dest       the address of the ultimate destination of this packet
   * @param protocol   the IP protocol of this packet (usually DSR)
   * @param priority   the IP priority of this packet
   * @param ttl        the IP time to live of this packet
   * @param id         the IP identification of this packet
   * @param fragOffset the IP fragmentation offset of this packet
   */
  private void ForwardPacket(RouteDsrMsg msg, RouteDsrMsg.OptionSourceRoute opt,
                             byte[] optBuf, NetAddress src, NetAddress dest,
                             short protocol, byte priority, byte ttl,
                             short id, short fragOffset)
  {
    // If the packet is for me it doesn't need to be forwarded
    if (localAddr.equals(dest)) return;

    // Check if I am the intended next recipient of this packet
    if (localAddr.equals(NextRecipient(opt, dest)))
    {
      NetAddress[] route = new NetAddress[opt.getNumAddresses()];
      for (int i = 0; i < route.length; i++)
      {
        route[i] = opt.getAddress(i);
      }

      RouteDsrMsg newMsg = (RouteDsrMsg)msg.clone();
      List newOptions = newMsg.getOptions();
      newOptions.remove(optBuf);
      newMsg.addOption(RouteDsrMsg.OptionSourceRoute.create(opt.isFirstHopExternal(),
        opt.isLastHopExternal(), opt.getSalvageCount(), opt.getNumSegmentsLeft() - 1,
        route));

      // Remove any acknowlegement requests from the DSR header.
      // The DSR spec doesn't say to do this, but it seems essential,
      // given that there can be at most one acknowledgement request
      // per packet.
      Iterator iter = newOptions.iterator();
      while (iter.hasNext())
      {
        byte[] option = (byte[])iter.next();
        if (RouteDsrMsg.Option.getType(option) == RouteDsrMsg.OPT_ACK_REQUEST)
        {
          iter.remove();
        }
      }

      NetMessage.Ip ipMsg = new NetMessage.Ip(newMsg, src, dest, protocol,
        priority, (byte)(ttl - 1), id, fragOffset);

      Transmit(ipMsg);
    }
  }

  /**
   * Checks to see if we know a shorter route from <code>src</code> to
   * <code>dest</code> than the one given in <code>sourceRoute</code>.
   * If this is the case, we send a gratuitous Route Reply to <code>src</code>
   * letting him know of the shorter route.
   *
   * @param sourceRoute the route that can potentially be shortened
   * @param src         the originator of the Source Route
   * @param dest        the destination of the Source Route
   */
  private void PerformRouteShortening(RouteDsrMsg.OptionSourceRoute sourceRoute,
                                      NetAddress src, NetAddress dest)
  {
    int routeLength =
      sourceRoute.getNumAddresses() - sourceRoute.getNumSegmentsLeft();

    // Check to see if this node occurs later on the source route

    if (localAddr.equals(dest))
    {
      SendGratuitousRouteReply(src, sourceRoute, dest, routeLength);
      return;
    }

    for (int i = routeLength + 1; i < sourceRoute.getNumAddresses(); i++)
    {
      if (localAddr.equals(sourceRoute.getAddress(i)))
      {
        // Yes, this route can be shortened
        SendGratuitousRouteReply(src, sourceRoute, dest, routeLength);
        return;
      }
    }
  }

  /**
   * Sends a gratuitous Route Reply to <code>src</code>.  That is,
   * <code>src</code> hasn't sent out a Route Request, but we happen to know
   * a shorter route than the one he's using, so we're letting him know.
   *
   * @param src         the originator of the shortened Source Route
   * @param sourceRoute the Source Route we know how to shorten
   * @param dest        the destination of the shortened Source Route
   * @param routeLength the length of the new (shortened) Source Route
   */
  private void SendGratuitousRouteReply(NetAddress src,
                                        RouteDsrMsg.OptionSourceRoute sourceRoute,
                                        NetAddress dest, int routeLength)
  {
    NetAddress prevRecipient = PrevRecipient(sourceRoute, src);

    // Don't reply if we have recently sent a gratuitous route reply
    if (RouteReplyEntryExists(src, prevRecipient)) return;

    // Figure out the shortened route from here to the source
    NetAddress[] routeToSrc = new NetAddress[routeLength];
    NetAddress[] routeFromSrc = new NetAddress[routeLength + 2];
    for (int j = 0; j < routeLength; j++)
    {
      routeToSrc[j] = sourceRoute.getAddress(routeLength - j - 1);
      routeFromSrc[routeLength - j] = routeToSrc[j];
    }

    routeFromSrc[0] = src;
    routeFromSrc[routeLength + 1] = dest;

    RouteDsrMsg dsrMsg = new RouteDsrMsg(null);

    try
    {
    dsrMsg.addOption(RouteDsrMsg.OptionRouteReply.create(routeFromSrc));
    }
    catch (NullPointerException e)
    {
      System.out.print("");
    }

    if (routeLength > 0)
    {
      dsrMsg.addOption(RouteDsrMsg.OptionSourceRoute.create(0, routeLength,
        routeToSrc));
    }

    NetMessage.Ip ipMsg = new NetMessage.Ip(dsrMsg, localAddr, src,
      Constants.NET_PROTOCOL_DSR, Constants.NET_PRIORITY_NORMAL,
      Constants.TTL_DEFAULT);

    // Working out the timing here is a little tricky, since both Transmit
    // and AddRouteReplyEntry call JistAPI.sleep, but since Transmit only
    // calls sleep indirectly, via an entity invocation, I think we'll be
    // OK (meaning the two calls to sleep won't accumulate).
    Transmit(ipMsg);
    self.AddRouteReplyEntry(src, prevRecipient);
    return;
  }

  /**
   * Processes an incoming Acknowledgement Request option.  If the packet
   * containing the Acknowledgement Request is destined for this node, then a
   * packet is returned containing an Acknowledgement option.  No retransmission
   * of the acknowledgement is performed.
   *
   * @param msg         the <code>RouteDsrMsg</code> containing the
   *   Acknowledgement Request
   * @param opt         the Acknowledgement Request option
   * @param src         the originator of the Acknowledgement Request
   * @param dst         the destination of the Acknowledgement Request
   * @param sourceRoute the Source Route option that came in the same message as
   *   the Acknowledgement Request.  This parameter can be <code>null</code> if
   *   there was no Source Route option.
   */
  private void HandleAckRequest(RouteDsrMsg msg, RouteDsrMsg.OptionAckRequest opt,
                                NetAddress src, NetAddress dst,
                                RouteDsrMsg.OptionSourceRoute sourceRoute)
  {
    if (localAddr.equals(NextRecipient(sourceRoute, dst)))
    {
      // The ack request is meant for me, so respond to it
      NetAddress ackDest = PrevRecipient(sourceRoute, src);

      RouteDsrMsg dsrMsg = new RouteDsrMsg(null);
      dsrMsg.addOption(RouteDsrMsg.OptionAck.create(opt.getId(), localAddr, ackDest));

      NetMessage.Ip ipMsg = new NetMessage.Ip(dsrMsg, localAddr,
        ackDest, Constants.NET_PROTOCOL_DSR, Constants.NET_PRIORITY_NORMAL,
        Constants.TTL_DEFAULT);

      netEntity.send(ipMsg, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
    }
  }

  /**
   * Processes an incoming Acknowledgement option.  If this Acknowledgement was
   * intended for this node, then the packet containing the corresponding
   * Acknowledgement Request will not be retransmitted.
   *
   * @param opt  the Acknowledgement option
   * @param dest the destination of the Acknowledgement
   */
  private void HandleAck(RouteDsrMsg.OptionAck opt, NetAddress dest)
  {
    if (localAddr.equals(dest))
    {
      activeAcks.remove(new Short(opt.getId()));
    }
  }

  /**
   * Processes an incoming Route Error option.  If this error was intended for
   * this node and indicates that a particular node is unreachable, then the
   * Route Cache will be updated to no longer use the broken links, and new
   * Route Discoveries may be initiated as a result.  NODE_UNREACHABLE is the
   * only kind of error that is currently handled.
   *
   * @param opt the Route Error option
   */
  private void HandleError(RouteDsrMsg.OptionRouteError opt)
  {
    switch (opt.getErrorType())
    {
      // This is the only error type I care about
      case RouteDsrMsg.ERROR_NODE_UNREACHABLE:
        byte[] unreachableAddrBytes = new byte[4];
        opt.getTypeSpecificInfoBytes(unreachableAddrBytes, 4);
        NetAddress unreachableAddr = new NetAddress(unreachableAddrBytes);

        // Remove every path from the route cache that makes use of this
        // link.  (How expensive is this?)
        RemoveCachedLink(opt.getSourceAddress(), unreachableAddr);
        break;

      default:
        break;
    }
  }

  /**
   * Handles each of the options in a given DSR header.
   *
   * @param msg        the message containing the DSR header
   * @param src        the IP source address of the message
   * @param dst        the IP destination address of the message
   * @param protocol   the IP protocol of the message
   * @param priority   the IP priority of the message
   * @param ttl        the IP time to live of the message
   * @param id         the IP identification of the message
   * @param fragOffset the IP fragmentation offset of the message
   */
  private void ProcessOptions(RouteDsrMsg msg, NetAddress src, NetAddress dst,
                              short protocol, byte priority, byte ttl,
                              short id, short fragOffset)
  {
    Iterator iter = msg.getOptions().iterator();
    RouteDsrMsg.OptionAckRequest ackRequest = null;
    RouteDsrMsg.OptionSourceRoute sourceRoute = null;

    while (iter.hasNext())
    {
      byte[] optBuf = (byte[])iter.next();
      RouteDsrMsg.Option opt = RouteDsrMsg.Option.create(optBuf, 0);

      if (opt == null)
      {
        // This should never happen in the simulation
        throw new RuntimeException("Unrecognized DSR Option");
      }

      switch (opt.getType())
      {
        case RouteDsrMsg.OPT_ROUTE_REQUEST:
          HandleRequest(msg, (RouteDsrMsg.OptionRouteRequest)opt, optBuf, src,
            dst, protocol, priority, ttl, id, fragOffset);

          break;

        case RouteDsrMsg.OPT_ROUTE_REPLY:
          HandleReply(msg, (RouteDsrMsg.OptionRouteReply)opt);
          break;

        case RouteDsrMsg.OPT_SOURCE_ROUTE:
          sourceRoute = (RouteDsrMsg.OptionSourceRoute)opt;

          if (localAddr.equals(NextRecipient(sourceRoute, dst)))
          {
            ForwardPacket(msg, sourceRoute, optBuf, src, dst, protocol,
              priority, ttl, id, fragOffset);
          }
          else
          {
            PerformRouteShortening(sourceRoute, src, dst);
          }

          break;

        case RouteDsrMsg.OPT_ACK_REQUEST:
          ackRequest = (RouteDsrMsg.OptionAckRequest)opt;
          break;

        case RouteDsrMsg.OPT_ACK:
          HandleAck((RouteDsrMsg.OptionAck)opt, dst);
          break;

        case RouteDsrMsg.OPT_ROUTE_ERROR:
          HandleError((RouteDsrMsg.OptionRouteError)opt);
          break;

        case RouteDsrMsg.OPT_PAD1:
        case RouteDsrMsg.OPT_PADN:
          break;

        default:
          // Possible problem: The processing of unrecognized options should
          // probably occur *before* the processing of any other options.
          // This will never arise in the simulation, though.
          switch ((opt.getType() & 0x60) >> 5)
          {
            case RouteDsrMsg.UNRECOGNIZED_OPT_IGNORE:
              // Ignore this option
              break;

            case RouteDsrMsg.UNRECOGNIZED_OPT_REMOVE:
              {
                // Remove this option from the packet
                RouteDsrMsg newMsg = (RouteDsrMsg)msg.clone();
                List options = newMsg.getOptions();
                options.remove(optBuf);
                msg = newMsg;
                break;
              }

            case RouteDsrMsg.UNRECOGNIZED_OPT_MARK:
              {
                // Set a particular bit inside the option
                RouteDsrMsg newMsg = (RouteDsrMsg)msg.clone();
                byte[] newOptBuf = new byte[optBuf.length];
                System.arraycopy(optBuf, 0, newOptBuf, 0, optBuf.length);
                newOptBuf[2] |= 0x80;
                
                List options = newMsg.getOptions();
                options.remove(optBuf);
                options.add(newOptBuf);
                msg = newMsg;
                break;
              }

            case RouteDsrMsg.UNRECOGNIZED_OPT_DROP:
              // Drop the packet
              return;

            default:
              throw new RuntimeException("Should never reach this point");
          }
 
          break;
      }
    }

    if (ackRequest != null)
    {
      // The DSR spec has some contradictory instructions regarding how to
      // handle packets containing both acknowledgements and acknowledgement
      // requests (in section 8.3.3).  It doesn't make any sense, so I'm
      // ignoring those instructions.
      HandleAckRequest(msg, ackRequest, src, dst, sourceRoute);
    }
  }

  /**
   * Initiates a Route Discovery for the given address.  Messages containing
   * Route Request headers are broadcast to all nodes within range.
   *
   * @param dest      the address to which we desire a route
   * @param requestId the ID number of the request to be performed.
   *   <code>DiscoverRoute</code> should always be invoked with a unique
   *   value in this parameter.
   */
  public void DiscoverRoute(NetAddress dest, short requestId)
  {
    RouteRequestTableEntry entry = (RouteRequestTableEntry)routeRequestTable.get(dest);

    if (entry == null)
    {
      entry = new RouteRequestTableEntry();
      routeRequestTable.put(dest, entry);
    }

    // Check to see if we're allowed to make a route request at this time
    if ((entry.numRequestsSinceLastReply == 0) ||
        (entry.lastRequestTime + entry.timeout <= JistAPI.getTime()))
    {
      if (!activeRequests.contains(dest)) return;

      RouteDsrMsg routeRequest = new RouteDsrMsg(null);
      routeRequest.addOption(RouteDsrMsg.OptionRouteRequest.create(requestId,
        dest, new NetAddress[0]));

      NetMessage.Ip routeRequestMsg = new NetMessage.Ip(routeRequest, localAddr,
        NetAddress.ANY, Constants.NET_PROTOCOL_DSR, Constants.NET_PRIORITY_NORMAL,
        Constants.TTL_DEFAULT);

      netEntity.send(routeRequestMsg, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
      entry.lastRequestTime = JistAPI.getTime();

      // Double timeout and retransmit route request if no response
      // after timeout elapses
      if (entry.numRequestsSinceLastReply > 0) entry.timeout *= 2;

      if (entry.timeout < MAX_REQUEST_PERIOD)
      {
        JistAPI.sleep(entry.timeout);
        self.DiscoverRoute(dest, requestId);
      }

      entry.numRequestsSinceLastReply++;
    }
  }

  /** Initializes the Route Cache. */
  private void InitRouteCache()
  {
    routeCache = new Hashtable();
  }

  /**
   * Inserts a new route into the Route Cache.  All routes stored in the Route
   * Cache are assumed to be routes from this node to another node.
   *
   * @param dest  the destination of the route to be added
   * @param route the sequence of nodes from here to <code>dest</code>.  Neither
   *   the IP address of this node nor of the <code>dest</code> node should be
   *   included in the <code>route</code> array.
   */
  public void InsertRouteCache(NetAddress dest, NetAddress[] route)
  {
    if (routeCache.containsKey(dest))
    {
      LinkedList routes = (LinkedList)routeCache.get(dest);

      // Insert the new route in the appropriate place
      for (ListIterator iter = routes.listIterator(); iter.hasNext(); )
      {
        NetAddress[] curRoute = (NetAddress[])iter.next();

        if (curRoute.length < route.length) continue;

        if (curRoute.length == route.length)
        {
          // Make sure the cache doesn't already have this route
          if (Arrays.equals(curRoute, route)) return;
        }

        if (curRoute.length > route.length)
        {
          iter.previous();
          iter.add(route);
          CheckBuffer(dest);
          return;
        }
      }

      // If the we haven't added the route to the cache yet, append it
      // now to the end of the list
      routes.addLast(route);
      CheckBuffer(dest);
    }
    else
    {
      // Create a new entry in the route cache for this destination
      LinkedList routes = new LinkedList();
      routes.add(route);
      routeCache.put(dest, routes);
      CheckBuffer(dest);
    }
  }


  /**
   * Removes every path in the cache that uses a direct link between
   * <code>addr1</code> and <code>addr2</code>.
   *
   * @param addr1 the first address in the link
   * @param addr2 the second address in the link
   */
  private void RemoveCachedLink(NetAddress addr1, NetAddress addr2)
  {
    Enumeration e = routeCache.keys();
    while (e.hasMoreElements())
    {
      NetAddress dest = (NetAddress)e.nextElement();
      LinkedList routes = (LinkedList)routeCache.get(dest);
      Iterator routeIter = routes.iterator();

      while (routeIter.hasNext())
      {
        NetAddress[] route = (NetAddress[])routeIter.next();

        // Check to see if this route contains the link in question
        if (addr1.equals(localAddr) && route.length > 0 && addr2.equals(route[0]))
        {
          routeIter.remove();
        }
        else if (addr1.equals(localAddr) && route.length == 0 && addr2.equals(dest))
        {
          routeIter.remove();
        }
        else if (addr2.equals(dest) && route.length > 0 &&
                 addr1.equals(route[route.length - 1]))
        {
          routeIter.remove();
        }
        else
        {
          for (int i = 1; i < route.length; i++)
          {
            if (addr1.equals(route[i-1]) && addr2.equals(route[i]))
            {
              routeIter.remove();
              break;
            }
          }
        }
      }
    }
  }

  /**
   * Retrieves a route from the Route Cache from here to the given node.
   *
   * @param dest the address of the node to find a route to
   * @return An array of addresses denoting a route from here to <code>dest</code>.
   *   Neither the address of this node nor of <code>dest</code> is included in
   *   this array.  If the Route Cache contains no route to <code>dest</code>,
   *   <code>null</code> is returned.  If the Route Cache contains more than one
   *   route, the shortest is returned, with ties broken arbitrarily.
   */
  private NetAddress[] GetCachedRoute(NetAddress dest)
  {
    LinkedList routes = (LinkedList)routeCache.get(dest);

    if (routes == null || routes.isEmpty())
    {
      // No cached route to this destination
      return null;
    }

    // Otherwise return the shortest (i.e., first) cached route
    return (NetAddress[])routes.getFirst();
  }

  /** Initializes the Send Buffer. */
  private void InitBuffer()
  {
    sendBuffer = new LinkedList();
  }

  /**
   * Inserts a new packet into the Send Buffer, annotating it with the
   * current system time.
   *
   * @param msg the message to insert into the buffer
   */
  public void InsertBuffer(NetMessage.Ip msg)
  {
    BufferedPacket packet = new BufferedPacket(msg);
    sendBuffer.add(packet);
    JistAPI.sleep(SEND_BUFFER_TIMEOUT);
    self.DeleteBuffer(packet);
  }

  /**
   * Removes the given <code>BufferedPacket</code> from the Send Buffer.
   *
   * @param msg the packet to remove from the Send Buffer
   */
  public void DeleteBuffer(BufferedPacket msg)
  {
    sendBuffer.remove(msg);
  }

  /**
   * Searches the Send Buffer for any packets intended for the given
   * destination and sends any that are found.  This is typically called
   * immediately after finding a route to <code>dest</code>.
   *
   * @param dest the destination we now have a route to
   */
  private void CheckBuffer(NetAddress dest)
  {
    ListIterator iter = sendBuffer.listIterator();

    while (iter.hasNext())
    {
      BufferedPacket packet = (BufferedPacket)iter.next();

      if (packet.msg.getDst().equals(dest))
      {
        SendWithRoute(packet.msg, GetCachedRoute(dest));
        iter.remove();
      }
    }
  }

  /** Initializes the Gratuitous Route Reply Table. */
  private void InitRouteReplyTable()
  {
    routeReplyTable = new HashSet();
  }

  /**
   * Determines whether there is an entry in the Gratuitous Route Reply Table
   * corresponding to the given addresses.
   *
   * @param originator the originator of the shortened Source Route
   * @param lastHop the most recent hop address of the shortened Source Route
   * @return whether the entry exists in the table.
   */
  private boolean RouteReplyEntryExists(NetAddress originator, NetAddress lastHop)
  {
    return routeReplyTable.contains(new RouteReplyTableEntry(originator, lastHop));
  }

  /**
   * Adds a new entry to the Gratuitous Route Reply Table.
   *
   * @param originator the originator of the shortened Source Route
   * @param lastHop the most recent hop address of the shortened Source Route
   */
  public void AddRouteReplyEntry(NetAddress originator, NetAddress lastHop)
  {
    routeReplyTable.add(new RouteReplyTableEntry(originator, lastHop));

    // Remove this entry from the table after the appropriate timeout
    JistAPI.sleep(GRAT_REPLY_HOLDOFF);
    self.DeleteRouteReplyEntry(originator, lastHop);
  }

  /**
   * Deletes an entry from the Gratuitous Route Reply Table.
   *
   * @param originator the originator of the shortened Source Route
   * @param lastHop the most recent hop address of the shortened Source Route
   */
  public void DeleteRouteReplyEntry(NetAddress originator, NetAddress lastHop)
  {
    routeReplyTable.remove(new RouteReplyTableEntry(originator, lastHop));
  }

  /** Initializes the Maintenance Buffer. */
  private void InitMaintenanceBuffer()
  {
    maintenanceBuffer = new Hashtable();
  }
  
  /** Initializes the Route Request Table. */
  private void InitRequestTable()
  {
    routeRequestTable = new Hashtable();
  }

  /**
   * Enters a new Route Request ID number into the Route Request Table.
   *
   * @param src the address of the originator of the Route Request
   * @param id  the ID number of the Route Request
   */
  private void AddRequestId(NetAddress src, short id)
  {
    // Do nothing if it's already in the table
    if (SeenRequestLately(src, id)) return;

    // Otherwise add this id to the table
    RouteRequestTableEntry entry = (RouteRequestTableEntry)routeRequestTable.get(src);

    if (entry == null)
    {
      entry = new RouteRequestTableEntry();
      routeRequestTable.put(src, entry);
    }

    entry.ids.addFirst(new Short(id));
    if (entry.ids.size() > MAX_REQUEST_TABLE_IDS)
    {
      // Make sure the list doesn't grow too large by removing the least
      // recently seen id number
      entry.ids.removeLast();
    }
  }

  /**
   * Checks if we have recently seen the Route Request with the given id
   * coming from the given source.  "Recently" here means within the last
   * <code>MAX_REQUEST_TABLE_IDS</code> Route Requests coming
   * from <code>src</code>.
   *
   * @param src the source address of the Route Request
   * @param id  the ID number of the Route Request
   * @return whether the given request has been seen recently.
   */
  private boolean SeenRequestLately(NetAddress src, short id)
  {
    RouteRequestTableEntry entry = (RouteRequestTableEntry)routeRequestTable.get(src);

    if (entry == null)
    {
      return false;
    }

    ListIterator iter = entry.ids.listIterator();
    while (iter.hasNext())
    {
      short curId = ((Short)iter.next()).shortValue();

      if (curId == id)
      {
        // Move this id to the front of the list
        iter.remove();
        entry.ids.addFirst(new Short(curId));
        return true;
      }
    }

    return false;
  }

  /**
   * Sends a DSR message with a Route Error option to <code>src</code> indicating
   * that the next hop in the intended route cannot be reached from this node.
   *
   * @param msg  the <code>RouteDsrMsg</code> containing the Source Route option
   *   that contains the broken link
   * @param src  the originator of <code>msg</code>
   * @param dest the destination of <code>msg</code>
   */
  private void SendRouteError(RouteDsrMsg msg, NetAddress src, NetAddress dest)
  {
    // Find the Source Route option so we know what the intended next hop was
    Iterator iter = msg.getOptions().iterator();

    while (iter.hasNext())
    {
      byte[] optBuf = (byte[])iter.next();
      RouteDsrMsg.Option opt = RouteDsrMsg.Option.create(optBuf, 0);

      if (opt.getType() == RouteDsrMsg.OPT_SOURCE_ROUTE)
      {
        RouteDsrMsg.OptionSourceRoute sourceRoute =
          (RouteDsrMsg.OptionSourceRoute)opt;

        // Find out the address of the node we couldn't reach
        NetAddress nextAddr = NextRecipient(sourceRoute, dest);
        byte[] nextAddrBuf = new byte[4];
        Pickle.InetAddressToArray(nextAddr.getIP(), nextAddrBuf, 0);

        // Create a packet containing a Route Error option
        RouteDsrMsg errorMsg = new RouteDsrMsg(null);

        errorMsg.addOption(RouteDsrMsg.OptionRouteError.create(
          RouteDsrMsg.ERROR_NODE_UNREACHABLE, sourceRoute.getSalvageCount(),
          localAddr, src, nextAddrBuf));

        int curSegment =
          sourceRoute.getNumAddresses() - sourceRoute.getNumSegmentsLeft();

        if (curSegment > 1)
        {
          // Need to add a Source Route option to the packet
          NetAddress[] route = new NetAddress[curSegment - 1];

          while (--curSegment > 0)
          {
            route[curSegment - 1] = sourceRoute.getAddress(route.length - curSegment);
          }

          errorMsg.addOption(RouteDsrMsg.OptionSourceRoute.create(0,
            route.length, route));

          // Might as well add this route to the cache if it isn't already there
          InsertRouteCache(src, route);
        }

        // Slap an IP header on it and send it off
        NetMessage.Ip ipMsg = new NetMessage.Ip(errorMsg, localAddr, src,
          Constants.NET_PROTOCOL_DSR, Constants.NET_PRIORITY_NORMAL,
          Constants.TTL_DEFAULT);

        Transmit(ipMsg);

        if (log.isInfoEnabled())
        {
          log.info("Originated route error from " + localAddr + " to " + src +
             ": Cannot contact " + nextAddr);
        }

        break;
      }
    }
  }

  /**
   * Retrieves the Source Route option from the given DSR message, or
   * <code>null</code> if none exists.
   *
   * @param msg the DSR message
   * @return the Source Route option from <code>msg</code>, or <code>null</code>
   *   if none exists.
   */
  private RouteDsrMsg.OptionSourceRoute GetSourceRoute(RouteDsrMsg msg)
  {
    Iterator iter = msg.getOptions().iterator();

    while (iter.hasNext())
    {
      byte[] opt = (byte[])iter.next();
      if (RouteDsrMsg.Option.getType(opt) == RouteDsrMsg.OPT_SOURCE_ROUTE)
      {
        return (RouteDsrMsg.OptionSourceRoute)RouteDsrMsg.Option.create(opt, 0);
      }
    }

    return null;
  }

  /**
   * Sends the given message and looks for acknowledgement.  If no
   * acknowledgement is forthcoming the message is retransmitted a limited
   * number of times.
   *
   * @param msg the message to send.  The payload of this IP packet should be
   *   a <code>RouteDsrMsg</code>, and it should not already contain an
   *   Acknowledgement Request.
   */
  private void Transmit(NetMessage.Ip msg)
  {
    RouteDsrMsg dsrMsg = (RouteDsrMsg)msg.getPayload();
    RouteDsrMsg.OptionSourceRoute sourceRoute = GetSourceRoute(dsrMsg);

    if (sourceRoute == null || sourceRoute.getNumSegmentsLeft() == 0)
    {
      // Messages on their last hop must use network-level acknowledgements.
      // Add an Acknowledgement Request to the packet.
      RouteDsrMsg newMsg = (RouteDsrMsg)dsrMsg.clone();

      Short ackId = new Short(nextAckId++);
      newMsg.addOption(RouteDsrMsg.OptionAckRequest.create(ackId.shortValue()));
      activeAcks.add(ackId);

      NetMessage.Ip ipMsg = new NetMessage.Ip(newMsg, msg.getSrc(),
        msg.getDst(), msg.getProtocol(), msg.getPriority(), msg.getTTL(),
        msg.getId(), msg.getFragOffset());

      self.TransmitWithNetworkAck(ipMsg, ackId, MAINT_PERIOD, 0);
      return;
    }

    // Otherwise add an entry to the Maintenance Buffer and try passive
    // acknolwedgements
    MaintenanceBufferEntry entry = new MaintenanceBufferEntry(msg.getSrc(),
      msg.getDst(), msg.getProtocol(), msg.getId(), msg.getFragOffset());

    maintenanceBuffer.put(entry, new Integer(sourceRoute.getNumSegmentsLeft()));
    self.TransmitWithPassiveAck(msg, 0);
  }

  /**
   * Sends the given message and waits for a passive acknowledgement.  If no
   * acknowledgement is heard, the message will be retransmitted up to
   * <code>TRY_PASSIVE_ACKS</code> times.
   *
   * @param msg            the message to be sent
   * @param numRetransmits the number of times this message has already been
   *   retransmitted.  Callers should usually pass in zero for this.
   */
  public void TransmitWithPassiveAck(NetMessage.Ip msg, int numRetransmits)
  {
    RouteDsrMsg dsrMsg = (RouteDsrMsg)msg.getPayload();
    RouteDsrMsg.OptionSourceRoute sourceRoute = GetSourceRoute(dsrMsg);
    MaintenanceBufferEntry entry = null;

    if (sourceRoute != null && sourceRoute.getNumSegmentsLeft() > 0)
    {
      entry = new MaintenanceBufferEntry(msg.getSrc(), msg.getDst(),
        msg.getProtocol(), msg.getId(), msg.getFragOffset());

      // If the maintenance buffer does not contain this entry, then it
      // has already been sent and acknowledged
      if (!maintenanceBuffer.containsKey(entry))
      {
        return;
      }

      // If we have exceeded the maximum number of tries on passive
      // acknowledgement, remove this entry from the maintenance buffer
      if (numRetransmits >= TRY_PASSIVE_ACKS)
      {
        maintenanceBuffer.remove(entry);
      }
    }

    if (sourceRoute == null || sourceRoute.getNumSegmentsLeft() == 0 ||
        numRetransmits >= TRY_PASSIVE_ACKS)
    {
      // Messages on their final hop and messages that have already tried and
      // failed to receive a passive acknowledgement must use network-level
      // acknowledgments
      RouteDsrMsg newMsg = (RouteDsrMsg)dsrMsg.clone();

      Short ackId = new Short(nextAckId++);
      newMsg.addOption(RouteDsrMsg.OptionAckRequest.create(ackId.shortValue()));
      activeAcks.add(ackId);

      NetMessage.Ip ipMsg = new NetMessage.Ip(newMsg, msg.getSrc(),
        msg.getDst(), msg.getProtocol(), msg.getPriority(), msg.getTTL(),
        msg.getId(), msg.getFragOffset());

      TransmitWithNetworkAck(ipMsg, ackId, MAINT_PERIOD, 0);
      return;
    }

    netEntity.send(msg, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
    JistAPI.sleep(PASSIVE_ACK_TIMEOUT + (long)(Math.random()*BROADCAST_JITTER));

    self.TransmitWithPassiveAck(msg, numRetransmits + 1);
  }

  /**
   * Sends the given message.  The message should be a DSR packet containing an
   * acknowledgement request with the given id.  If no acknowledgement is
   * received within the given timeout, the packet will be retransmitted up to
   * <code>MAX_MAINT_REXMT</code> times.
   *
   * @param msg            the message to be sent
   * @param ackId          the ID number of the Acknowledgement Request
   * @param timeout        the number of clock ticks to wait before
   *   retransmitting the message
   * @param numRetransmits the number of times this packet has already been
   *   transmitted.  Callers should normally pass in zero for this.
   */
  public void TransmitWithNetworkAck(NetMessage.Ip msg, Short ackId,
                                     long timeout, int numRetransmits)
  {
    if (!activeAcks.contains(ackId)) return;

    if (numRetransmits > MAX_MAINT_REXMT)
    {
      // Max retransmissions exceeded -- must send Route Error to message source
      activeAcks.remove(ackId);

      if (msg.getProtocol() == Constants.NET_PROTOCOL_DSR)
      {
        RouteDsrMsg dsrMsg = (RouteDsrMsg)msg.getPayload();
        
        // If this nodle is the source of the message, must simply remove the
        // broken link from the route cache
        if (msg.getSrc().equals(localAddr))
        {
          NetAddress unreachableAddress = null;

          // Find out what the unreachable address was
          if (!dsrMsg.hasOption(RouteDsrMsg.OPT_SOURCE_ROUTE))
          {
            unreachableAddress = msg.getDst();
          }
          else
          {
            Iterator iter = dsrMsg.getOptions().iterator();
            while (iter.hasNext())
            {
              byte[] optBuf = (byte[])iter.next();
              if (RouteDsrMsg.Option.getType(optBuf) == RouteDsrMsg.OPT_SOURCE_ROUTE)
              {
                RouteDsrMsg.OptionSourceRoute sourceRoute =
                  (RouteDsrMsg.OptionSourceRoute)RouteDsrMsg.Option.create(optBuf, 0);

                unreachableAddress = NextRecipient(sourceRoute, msg.getDst());
                break;
              }
            }
          }

          // Remove the broken link from the cache, then try to retransmit the
          // message if we know any other routes to the destination
          RemoveCachedLink(localAddr, unreachableAddress);
          if (GetCachedRoute(msg.getDst()) != null) CheckBuffer(msg.getDst());
        }
        else
        {
          // Otherwise, must send Route Error message to the originator
          // of this message
          SendRouteError(dsrMsg, msg.getSrc(), msg.getDst());
        }
      }

      return;
    }

    if (log.isDebugEnabled())
    {
      if (numRetransmits > 0)
      {
        log.debug(localAddr + " retransmitting from " + msg.getSrc() + " to " +
          msg.getDst() + "!");
      }
    }

    netEntity.send(msg, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
    JistAPI.sleep(timeout + (long)(Math.random() * BROADCAST_JITTER));
    self.TransmitWithNetworkAck(msg, ackId, 2*timeout, numRetransmits + 1);
  }

  /**
   * Checks the given message to see if it matches any of the messages in the
   * Maintenance Buffer for which we are currently awaiting passive passive
   * acknowledgements.  If this message matches a message in the Maintenance
   * Buffer, that message will be removed from the buffer and will not be
   * retransmitted.
   *
   * @param msg        the message that has been overheard
   * @param src        the IP source of the message
   * @param dest       the IP destination of the message
   * @param protocol   the IP protocol of the message
   * @param id         the IP id number of the message
   * @param fragOffset the IP fragmentation offset of the message
   */
  private void CheckForPassiveAck(RouteDsrMsg msg, NetAddress src,
                                  NetAddress dest, short protocol, short id,
                                  short fragOffset)
  {
    RouteDsrMsg.OptionSourceRoute sourceRoute = GetSourceRoute(msg);

    if (sourceRoute == null) return;

    MaintenanceBufferEntry entry =
      new MaintenanceBufferEntry(src, dest, protocol, id, fragOffset);

    Integer segsLeft = (Integer)maintenanceBuffer.get(entry);
    if (segsLeft == null) return;

    if (segsLeft.intValue() > sourceRoute.getNumSegmentsLeft())
    {
      maintenanceBuffer.remove(entry);
    }
  }

  /**
   * Sends the given message along the given route.  An Acknowledgement Request
   * option is added to the message, and it is retransmitted if no acknowledgement
   * is received before a timeout occurs.
   *
   * @param msg   the <code>RouteDsrMsg</code> to be sent
   * @param route the sequence of nodes to route the message along
   */
  private void SendWithRoute(NetMessage.Ip msg, NetAddress[] route)
  {
    // Slap on a DSR Options header with the proper Source Route option
    RouteDsrMsg dsrMsg = new RouteDsrMsg(msg.getPayload());
    dsrMsg.setNextHeaderType(msg.getProtocol());

    if (route.length > 0)
    {
      dsrMsg.addOption(RouteDsrMsg.OptionSourceRoute.create(0, route.length, route));
    }

    if (log.isInfoEnabled())
    {
      log.info("Route length: " + route.length);
    }

    NetMessage.Ip ipMsg = new NetMessage.Ip(dsrMsg, msg.getSrc(), msg.getDst(),
      Constants.NET_PROTOCOL_DSR, msg.getPriority(), msg.getTTL());

    Transmit(ipMsg);
  }

  //////////////////////////////////////////////////
  // route interface
  //

  /**
   * If the given message uses the DSR protocol, the DSR header is examined
   * to see if any actions need to be performed on this packet (such as
   * forwarding it).
   *
   * @param msg     the message to examine
   * @param lastHop the MAC address of the node that sent this message
   */
  public void peek(NetMessage msg, MacAddress lastHop)
  {
    if (msg instanceof NetMessage.Ip)
    {
      NetMessage.Ip ipMsg = (NetMessage.Ip)msg;
      
      if (ipMsg.getProtocol() == Constants.NET_PROTOCOL_DSR)
      {
        RouteDsrMsg dsrMsg = (RouteDsrMsg)ipMsg.getPayload();

        if (log.isDebugEnabled())
        {
          log.debug(localAddr + " saw message from " + ipMsg.getSrc() +
            " to " + ipMsg.getDst());

          Iterator iter = dsrMsg.getOptions().iterator();
          while (iter.hasNext())
          {
            byte[] optBuf = (byte[])iter.next();

            switch (RouteDsrMsg.Option.getType(optBuf))
            {
              case RouteDsrMsg.OPT_SOURCE_ROUTE:
                log.debug("    Source Route");
                break;

              case RouteDsrMsg.OPT_ACK_REQUEST:
                RouteDsrMsg.OptionAckRequest ackRequest = 
                  (RouteDsrMsg.OptionAckRequest)RouteDsrMsg.Option.create(optBuf, 0);
                log.debug("    Acknowledgement Request " + ackRequest.getId());
                break;

              case RouteDsrMsg.OPT_ACK:
                RouteDsrMsg.OptionAck ack =
                  (RouteDsrMsg.OptionAck)RouteDsrMsg.Option.create(optBuf, 0);
                log.debug("    Acknowledgement " + ack.getId());
                break;

              case RouteDsrMsg.OPT_ROUTE_REPLY:
                log.debug("    Route Reply");
                break;

              case RouteDsrMsg.OPT_ROUTE_REQUEST:
                RouteDsrMsg.OptionRouteRequest routeRequest =
                  (RouteDsrMsg.OptionRouteRequest)RouteDsrMsg.Option.create(optBuf, 0);
                log.debug("    Route Request " + routeRequest.getId());
                break;

              case RouteDsrMsg.OPT_ROUTE_ERROR:
                log.debug("    Route Error");
                break;

              default:
                log.debug("    Other");
                break;
            }
          }
        }

        CheckForPassiveAck(dsrMsg, ipMsg.getSrc(), ipMsg.getDst(),
          ipMsg.getProtocol(), ipMsg.getId(), ipMsg.getFragOffset());

        ProcessOptions(dsrMsg, ipMsg.getSrc(), ipMsg.getDst(),
          ipMsg.getProtocol(), ipMsg.getPriority(), ipMsg.getTTL(),
          ipMsg.getId(), ipMsg.getFragOffset());
      }
    }
  }

  /**
   * Sends the given message.  This method can be called because this node is
   * originating a packet (in which case a DSR header is added to the packet and
   * it is sent) or because this node is forwarding a packet (in which case this
   * method actually does nothing, with all DSR header option processing being
   * performed by <code>peek</code>.
   *
   * @param msg the message to be sent
   */
  public void send(NetMessage msg)
  {
    if (!(msg instanceof NetMessage.Ip))
    {
      throw new RuntimeException("Non-IP packets not supported");
    }

    NetMessage.Ip ipMsg = (NetMessage.Ip)msg;

    if (ipMsg.getProtocol() == Constants.NET_PROTOCOL_DSR)
    {
      // We're not really supposed to "send" this packet -- we might have to
      // to forward it, but that happens along with all the other option
      // processing in peek().
    }
    else
    {
      // Need to encapsulate and send this packet
      NetAddress[] route = GetCachedRoute(ipMsg.getDst());

      if (route == null)
      {
        self.InsertBuffer(ipMsg);
        activeRequests.add(ipMsg.getDst());
        DiscoverRoute(ipMsg.getDst(), nextRequestId++);
      }
      else
      {
        SendWithRoute(ipMsg, route);
      }
    }
  }

  /**
   * Receives a message from the network.  This method merely strips off the
   * DSR header and hands the message off to the transport layer.
   *
   * @param msg      the message being received
   * @param src      the address of the originator of the message
   * @param lastHop  the MAC address of the most recent node to forward the message
   * @param macId    the ID of the MAC interface
   * @param dst      the address of the destination of the message (which should
   *   be the IP address of this node)
   * @param priority the IP priority of the message
   * @param ttl      the IP time to live of the message
   */
  public void receive(Message msg, NetAddress src, MacAddress lastHop, byte macId, NetAddress dst, byte priority, byte ttl)
  {
    if (!(msg instanceof RouteDsrMsg))
    {
      throw new RuntimeException("Non-DSR message received by DSR");
    }

    if (log.isInfoEnabled() && localAddr.equals(dst))
    {
      // Don't count received broadcast packets?
      log.info("Received packet from " + src + " at " + dst);
    }

    // Don't process any options here -- that's all done by peek.  Just forward
    // any content on to the transport layer (or whatever).

    RouteDsrMsg dsrMsg = (RouteDsrMsg)msg;

    RouteDsrMsg.OptionSourceRoute sourceRoute = GetSourceRoute(dsrMsg);
    if (sourceRoute != null)
    {
      // Strange as it may seem, we will discard this packet, which is
      // in fact intended for us, if it arrives here before traversing
      // the other links in the intended route.  (Route shortening should
      // prevent this from happening too often.)
      if (!localAddr.equals(NextRecipient(sourceRoute, dst))) return;
    }

    if (dsrMsg.getContent() != null)
    {
      // Now go through some strange contortions to get this message received by
      // the proper protocol handler
      NetMessage.Ip newIp = new NetMessage.Ip(dsrMsg.getContent(), src, dst,
        dsrMsg.getNextHeaderType(), priority, ttl);        

      netEntity.receive(newIp, lastHop, macId, false);

      if (log.isInfoEnabled())
      {
        log.info("Received data packet from " + src + " at " + dst);
      }
    }
  }
}
