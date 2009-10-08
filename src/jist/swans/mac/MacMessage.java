//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <MacMessage.java Sun 2005/03/13 11:06:45 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.mac;

import jist.swans.misc.Message;
import jist.swans.Constants;

/**
 * Defines the various message used by the Mac entity.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: MacMessage.java,v 1.17 2005-03-13 16:11:55 barr Exp $
 * @since SWANS1.0
 */

public abstract class MacMessage implements Message
{

  //////////////////////////////////////////////////
  // frame control
  //

  /** RTS packet constant: type = 01, subtype = 1011. */
  public static final byte TYPE_RTS  = 27;

  /** CTS packet constant: type = 01, subtype = 1100. */
  public static final byte TYPE_CTS  = 28;

  /** ACK packet constant: type = 01, subtype = 1101. */
  public static final byte TYPE_ACK  = 29;

  /** DATA packet constant: type = 10, subtype = 0000. */
  public static final byte TYPE_DATA = 32;

  /**
   * packet type.
   */
  private byte type;

  /**
   * packet retry bit.
   */
  private boolean retry;

  //////////////////////////////////////////////////
  // initialization
  //

  /**
   * Create a mac packet.
   * 
   * @param type packet type
   * @param retry packet retry bit
   */
  protected MacMessage(byte type, boolean retry)
  {
    this.type = type;
    this.retry = retry;
  }

  //////////////////////////////////////////////////
  // accessors
  //

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
   * Return retry bit.
   *
   * @return retry bit
   */
  public boolean getRetry()
  {
    return retry;
  }

  /**
   * Return packet destination address.
   *
   * @return packet destination address
   */
  public abstract MacAddress getDst();

  /**
   * Return packet transmission duration.
   *
   * @return packet transmission duration
   */
  public abstract int getDuration();


  //////////////////////////////////////////////////
  // RTS frame: (size = 20)
  //   frame control          size: 2
  //   duration               size: 2
  //   address: destination   size: 6
  //   address: source        size: 6
  //   CRC                    size: 4
  //

  /**
   * An 802_11 Request-To-Send packet.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */

  public static class Rts extends MacMessage
  {
    /**
     * RTS packet size.
     */
    public static final int SIZE = 20;

    /**
     * packet destination address.
     */
    private MacAddress dst;

    /**
     * packet source address.
     */
    private MacAddress src;

    /**
     * packet transmission duration.
     */
    private int duration;

    //////////////////////////////////////////////////
    // initialization
    //

    /**
     * Create an 802_11 RTS packet.
     *
     * @param dst packet destination address
     * @param src packet source address
     * @param duration packet transmission duration
     */
    public Rts(MacAddress dst, MacAddress src, int duration)
    {
      super(TYPE_RTS, false);
      this.dst = dst;
      this.src = src;
      this.duration = duration;
    }

    //////////////////////////////////////////////////
    // accessors
    //

    /**
     * Return packet destination address.
     *
     * @return packet destination address
     */
    public MacAddress getDst()
    {
      return dst;
    }

    /**
     * Return packet source address.
     *
     * @return packet source address
     */
    public MacAddress getSrc()
    {
      return src;
    }

    /**
     * Return packet transmission duration.
     *
     * @return packet transmission duration
     */
    public int getDuration()
    {
      return duration;
    }

    //////////////////////////////////////////////////
    // message interface 
    //

    // Message interface
    /** {@inheritDoc} */
    public int getSize()
    {
      return SIZE;
    }

    // Message interface
    /** {@inheritDoc} */
    public void getBytes(byte[] msg, int offset)
    {
      throw new RuntimeException("todo: not implemented");
    }

  } // class: RTS


  //////////////////////////////////////////////////
  // CTS frame: (size = 14)
  //   frame control          size: 2
  //   duration               size: 2
  //   address: destination   size: 6
  //   CRC                    size: 4

  /**
   * An 802_11 Clear-To-Send packet.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */

  public static class Cts extends MacMessage
  {
    /**
     * CTS packet size.
     */
    public static final int SIZE = 14;

    /**
     * packet destination address.
     */
    private MacAddress dst;

    /**
     * packet transmission duration.
     */
    private int duration;

    //////////////////////////////////////////////////
    // initialization
    //

    /**
     * Create an 802_11 CTS packet.
     *
     * @param dst packet destination address
     * @param duration packet transmission duration
     */
    public Cts(MacAddress dst, int duration)
    {
      super(TYPE_CTS, false);
      this.dst = dst;
      this.duration = duration;
    }

    //////////////////////////////////////////////////
    // accessors
    //

    /**
     * Return packet destination address.
     *
     * @return packet destination address
     */
    public MacAddress getDst()
    {
      return dst;
    }

    /**
     * Return packet transmission duration.
     *
     * @return packet transmission duration
     */
    public int getDuration()
    {
      return duration;
    }

    //////////////////////////////////////////////////
    // message interface 
    //

    // Message interface
    /** {@inheritDoc} */
    public int getSize()
    {
      return SIZE;
    }

    // Message interface
    /** {@inheritDoc} */
    public void getBytes(byte[] msg, int offset)
    {
      throw new RuntimeException("todo: not implemented");
    }

  } // class: CTS


  //////////////////////////////////////////////////
  // ACK frame: (size = 14)
  //   frame control          size: 2
  //   duration               size: 2
  //   address: destination   size: 6
  //   CRC                    size: 4

  /**
   * An 802_11 Acknowlege packet.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */

  public static class Ack extends MacMessage
  {

    /**
     * ACK packet size.
     */
    public static final int SIZE = 14;

    /**
     * packet destination address.
     */
    private MacAddress dst;

    /**
     * packet transmission duration.
     */
    private int duration;

    //////////////////////////////////////////////////
    // initialization
    //

    /**
     * Create 802_11 ACK packet.
     *
     * @param dst packet destination address
     * @param duration packet transmission duration
     */
    public Ack(MacAddress dst, int duration)
    {
      super(TYPE_ACK, false);
      this.dst = dst;
      this.duration = duration;
    }

    //////////////////////////////////////////////////
    // accessors
    //

    /**
     * Return packet destination address.
     *
     * @return packet destination address
     */
    public MacAddress getDst()
    {
      return dst;
    }

    /**
     * Return packet transmission duration.
     *
     * @return packet transmission duration
     */
    public int getDuration()
    {
      return duration;
    }

    //////////////////////////////////////////////////
    // message interface 
    //

    // Message interface
    /** {@inheritDoc} */
    public int getSize()
    {
      return SIZE;
    }

    // Message interface
    /** {@inheritDoc} */
    public void getBytes(byte[] msg, int offset)
    {
      throw new RuntimeException("todo: not implemented");
    }

  } // class: ACK



  //////////////////////////////////////////////////
  // DATA frame: (size = 34 + body)
  //   frame control          size: 2
  //   duration / ID          size: 2
  //   address: destination   size: 6
  //   address: source        size: 6
  //   address: #3            size: 6
  //   sequence control       size: 2
  //   address: #4            size: 6
  //   frame body             size: 0 - 2312
  //   CRC                    size: 4

  /**
   * An 802_11 Data packet.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */

  public static class Data extends MacMessage
  {
    /**
     * Packet header size.
     */
    public static final short HEADER_SIZE = 34;

    /**
     * Packet sequence number limit.
     */
    public static final short MAX_SEQ = 4096;

    /**
     * Packet destination address.
     */
    private MacAddress dst;

    /**
     * Packet source address.
     */
    private MacAddress src;

    /**
     * Packet transmission duration.
     */
    private int duration;

    /**
     * Packet sequence number.
     */
    private short seq;

    /**
     * Packet fragment number.
     */
    private short frag;

    /**
     * Packet moreFlag bit.
     */
    private boolean moreFrag;

    /**
     * Packet data payload.
     */
    private Message body;

    //////////////////////////////////////////////////
    // initialization
    //

    /**
     * Create 802_11 data packet.
     *
     * @param dst packet destination address
     * @param src packet source address
     * @param duration packet transmission duration
     * @param seq packet sequence number
     * @param frag packet fragment number
     * @param moreFrag packet moreFrag flag
     * @param retry packet retry bit
     * @param body packet data payload
     */
    public Data(MacAddress dst, MacAddress src, int duration, short seq, short frag, 
        boolean moreFrag, boolean retry, Message body)
    {
      super(TYPE_DATA, retry);
      this.dst = dst;
      this.src = src;
      this.duration = duration;
      this.seq = seq;
      this.frag = frag;
      this.body = body;
    }

    /**
     * Create 802_11 data packet.
     *
     * @param dst packet destination address
     * @param src packet source address
     * @param duration packet transmission duration
     * @param body packet data payload
     */
    public Data(MacAddress dst, MacAddress src, int duration, Message body)
    {
      this(dst, src, duration, (short)-1, (short)-1, false, false, body);
    }

    //////////////////////////////////////////////////
    // accessors
    //

    /**
     * Return packet destination address.
     *
     * @return packet destination address
     */
    public MacAddress getDst()
    {
      return dst;
    }

    /**
     * Return packet source address.
     *
     * @return packet source address
     */
    public MacAddress getSrc()
    {
      return src;
    }

    /**
     * Return packet transmission time.
     *
     * @return packet transmission time
     */
    public int getDuration()
    {
      return duration;
    }

    /**
     * Return packet sequence number.
     *
     * @return packet sequence number
     */
    public short getSeq()
    {
      return seq;
    }

    /**
     * Return packet fragment number.
     *
     * @return packet fragment number
     */
    public short getFrag()
    {
      return frag;
    }

    /**
     * Return packet data payload.
     *
     * @return packet data payload
     */
    public Message getBody()
    {
      return body;
    }

    //////////////////////////////////////////////////
    // message interface 
    //

    // Message interface
    /** {@inheritDoc} */
    public int getSize()
    {
      int size = body.getSize();
      if(size==Constants.ZERO_WIRE_SIZE)
      {
        return Constants.ZERO_WIRE_SIZE;
      }
      return HEADER_SIZE+size;
    }

    // Message interface
    /** {@inheritDoc} */
    public void getBytes(byte[] msg, int offset)
    {
      throw new RuntimeException("todo: not implemented");
    }

  } // class: Data

} // class: MacMessage

