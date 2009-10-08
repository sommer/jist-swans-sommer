//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <NetMessage.java Sun 2005/03/13 11:08:45 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.net;

import jist.swans.misc.Message;
import jist.swans.misc.Util;

import jist.swans.Constants;

/**
 * Network packet.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: NetMessage.java,v 1.17 2005-03-13 16:11:55 barr Exp $
 * @since SWANS1.0
 */
public abstract class NetMessage implements Message, Cloneable
{

  //////////////////////////////////////////////////
  // IPv4 packet: (size = 20 + 4 * options + body)
  //   version                size: 4 bits
  //   header length          size: 4 bits
  //   type of service (tos)  size: 1
  //     - priority             size: 3 bits
  //     - delay bit            size: 1 bit
  //     - throughput bit       size: 1 bit
  //     - reliability bit      size: 1 bit
  //     - reserved             size: 2 bits
  //   total length           size: 2 (measured in 64 bit chunks)
  //   id                     size: 2
  //   control flags          size: 3 bits
  //     - reserved (=0)        size: 1 bit
  //     - unfragmentable       size: 1 bit
  //     - more fragments       size: 1 bit
  //   fragment offset        size: 13 bits
  //   ttl                    size: 1
  //   protocol               size: 1
  //   header chksum          size: 2
  //   src                    size: 4
  //   dst                    size: 4
  //   options:               size: 4 * number
  //   packet payload:        size: variable
  //

  /**
   * IPv4 network packet.
   */
  public static class Ip extends NetMessage
  {

    /** Fixed IP packet size. */
    public static final int BASE_SIZE = 20;

    //////////////////////////////////////////////////
    // message contents
    //

    /** immutable bit. */
    private boolean frozen;
    /** ip packet source address. */
    private NetAddress src;
    /** ip packet destination address. */
    private NetAddress dst;
    /** ip packet payload. */
    private Message payload;
    /** ip packet priority level. */
    private byte priority;
    /** ip packet protocol, such as TCP, UDP, etc. */
    private short protocol;
    /** ip packet time-to-live. */
    private byte ttl;
    /** ip packet identification. */
    private short id;
    /** ip packet fragment offset. */
    private short fragOffset;

    // options
    /** source route. */
    private IpOptionSourceRoute srcRoute;

    /** Next identification number to use. */
    private static short nextId = 0;

    /**
     * Create new IPv4 packet.
     *
     * @param payload packet payload
     * @param src packet source address
     * @param dst packet destination address
     * @param protocol packet protocol
     * @param priority packet priority
     * @param ttl packet time-to-live
     * @param id packet identification
     * @param fragOffset packet fragmentation offset
     */
    public Ip(Message payload, NetAddress src, NetAddress dst, 
        short protocol, byte priority, byte ttl, short id, short fragOffset)
    {
      if(payload==null) throw new NullPointerException();
      this.frozen = false;
      this.payload = payload;
      this.src = src;
      this.dst = dst;
      this.protocol = protocol;
      this.priority = priority;
      this.ttl = ttl;
      this.id = id;
      this.fragOffset = fragOffset;
    }

    /**
     * Create new IPv4 packet with default id.
     *
     * @param payload packet payload
     * @param src packet source address
     * @param dst packet destination address
     * @param protocol packet protocol
     * @param priority packet priority
     * @param ttl packet time-to-live
     */
    public Ip(Message payload, NetAddress src, NetAddress dst,
        short protocol, byte priority, byte ttl)
    {
      this(payload, src, dst, protocol, priority, ttl, nextId++, (short)0);
    }


    /**
     * Render packet immutable.
     *
     * @return immutable packet, possibly intern()ed
     */
    public Ip freeze()
    {
      // todo: could perform an intern/hashCons here
      this.frozen = true;
      return this;
    }

    /**
     * Whether packet is immutable.
     *
     * @return whether packet is immutable
     */
    public boolean isFrozen()
    {
      return frozen;
    }

    /**
     * Make copy of packet, usually in order to modify it.
     *
     * @return mutable copy of packet.
     */
    public Ip copy()
    {
      NetMessage.Ip ip2 = new Ip(payload, src, dst, protocol, priority, ttl);
      ip2.srcRoute = this.srcRoute;
      return ip2;
    }

    //////////////////////////////////////////////////
    // accessors 
    //

    /**
     * Return packet source.
     *
     * @return packet source
     */
    public NetAddress getSrc()
    {
      return src;
    }

    /**
     * Return packet destination.
     *
     * @return packet destination
     */
    public NetAddress getDst()
    {
      return dst;
    }

    /**
     * Return packet payload.
     *
     * @return packet payload
     */
    public Message getPayload()
    {
      return payload;
    }

    /**
     * Return packet priority.
     *
     * @return packet priority
     */
    public byte getPriority()
    {
      return priority;
    }

    /**
     * Return packet protocol.
     *
     * @return packet protocol
     */
    public short getProtocol()
    {
      return protocol;
    }

    /**
     * Return packet identification.
     *
     * @return packet identification
     */
    public short getId()
    {
      return id;
    }

    /**
     * Return packet fragmentation offset.
     *
     * @return packet fragmentation offset
     */
    public short getFragOffset()
    {
      return fragOffset;
    }

    //////////////////////////////////////////////////
    // TTL
    //

    /**
     * Return packet time-to-live.
     *
     * @return time-to-live
     */
    public byte getTTL()
    {
      return ttl;
    }

    /**
     * Create indentical packet with decremented TTL.
     */
    public void decTTL()
    {
      if(frozen) throw new IllegalStateException();
      ttl--;
    }

    //////////////////////////////////////////////////
    // source route
    //

    /**
     * Returns whether packet contains source route.
     *
     * @return whether packet contains source route
     */
    public boolean hasSourceRoute()
    {
      return srcRoute!=null;
    }

    /**
     * Return source route. (do not modify)
     *
     * @return source route (do not modify)
     */
    public NetAddress[] getSourceRoute()
    {
      return srcRoute.getRoute();
    }

    /**
     * Return source route pointer.
     *
     * @return source route pointer
     */
    public int getSourceRoutePointer()
    {
      return srcRoute.getPtr();
    }

    /**
     * Set source route.
     *
     * @param srcRoute source route
     */
    public void setSourceRoute(IpOptionSourceRoute srcRoute)
    {
      if(frozen) throw new IllegalStateException();
      this.srcRoute = srcRoute;
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return "ip(src="+src+" dst="+dst+" size="+getSize()+" prot="+protocol+" ttl="+ttl+" route="+srcRoute+" data="+payload+")";
    }

    //////////////////////////////////////////////////
    // message interface
    //

    /** {@inheritDoc} */
    public int getSize()
    {
      int size = payload.getSize();
      if(size==Constants.ZERO_WIRE_SIZE)
      {
        return Constants.ZERO_WIRE_SIZE;
      }
      // todo: options
      return BASE_SIZE + size;
    }

    /** {@inheritDoc} */
    public void getBytes(byte[] b, int offset)
    {
      throw new RuntimeException("not implemented");
    }

  } // class: Ip


  /**
   * A generic IP packet option.
   */
  public abstract static class IpOption implements Message
  {
    /**
     * Return option type field.
     *
     * @return option type field
     */
    public abstract byte getType();

    /**
     * Return option length (in bytes/octets).
     *
     * @return option length (in bytes/octets)
     */
    public abstract int getSize();

  } // class: IpOption


  /**
   * An IP packet source route option.
   */
  public static class IpOptionSourceRoute extends IpOption
  {
    /** option type constant: source route. */
    public static final byte TYPE = (byte)137;

    /** source route. */
    private final NetAddress[] route;
    /** source route pointer: index into route. */
    private final int ptr;

    /**
     * Create new source route option.
     *
     * @param route source route
     */
    public IpOptionSourceRoute(NetAddress[] route)
    {
      this(route, (byte)0);
    }

    /**
     * Create new source route option.
     *
     * @param route source route
     * @param ptr source route pointer
     */
    public IpOptionSourceRoute(NetAddress[] route, int ptr)
    {
      this.route = route;
      this.ptr = ptr;
    }

    /**
     * Return source route.
     *
     * @return source route (do not modify)
     */
    public NetAddress[] getRoute()
    {
      return route;
    }

    /**
     * Return source route pointer: index into route.
     *
     * @return source route pointer: index into route
     */
    public int getPtr()
    {
      return ptr;
    }

    /** {@inheritDoc} */
    public byte getType()
    {
      return TYPE;
    }

    /** {@inheritDoc} */
    public int getSize()
    {
      return (byte)(route.length*4 + 3);
    }

    /** {@inheritDoc} */
    public void getBytes(byte[] msg, int offset)
    {
      throw new RuntimeException("not implemented");
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return ptr+":["+Util.stringJoin(route, ",")+"]";
    }

  } // class: IpOptionSourceRoute


} // class: NetMessage

/*
todo:
#define IP_MAXPACKET    65535       // maximum packet size
#define MAXTTL      255     // maximum time to live (seconds) 
#define IPDEFTTL    64      // default ttl, from RFC 1340 
#define IP_MSS      576     // default maximum segment size 
*/

