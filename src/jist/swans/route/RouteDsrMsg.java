//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RouteDsrMsg.java Tue 2004/04/06 11:35:14 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.route;

import jist.swans.net.NetAddress;
import jist.swans.misc.Message;
import jist.swans.Constants;

import java.util.*;


/**
 * A message with a DSR Options header.
 *
 * @author Ben Viglietta
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: RouteDsrMsg.java,v 1.10 2004-04-06 16:07:50 barr Exp $
 * @since SWANS1.0
 */
public class RouteDsrMsg implements Message, Cloneable
{
  //////////////////////////////////////////////////
  // constants
  //

  // Option types

  /** Type of a Route Request option. */
  public static final byte OPT_ROUTE_REQUEST =         2;
  /** Type of a Route Reply option. */
  public static final byte OPT_ROUTE_REPLY   =         1;
  /** Type of a Route Error option. */
  public static final byte OPT_ROUTE_ERROR   =         3;
  /** Type of an Acknowledgement Request option. */
  public static final byte OPT_ACK_REQUEST   = (byte)160;
  /** Type of an Acknowledgement option. */
  public static final byte OPT_ACK           =        32;
  /** Type of a Source Route option. */
  public static final byte OPT_SOURCE_ROUTE  =        96;
  /** Type of a Pad1 option. */
  public static final byte OPT_PAD1          = (byte)224;
  /** Type of a PadN option. */
  public static final byte OPT_PADN          =         0;

  // Error types

  /** Error code for "unreachable node". */
  public static final byte ERROR_NODE_UNREACHABLE         = 1;
  /** Error code for "flow state not supported". */
  public static final byte ERROR_FLOW_STATE_NOT_SUPPORTED = 2;
  /** Error code for "option not supported". */
  public static final byte ERROR_OPTION_NOT_SUPPORTED     = 3;

  // Ways of dealing with unrecognized options

  /** Code for ignoring unrecognized options. */
  public static final int UNRECOGNIZED_OPT_IGNORE = 0;
  /** Code for removing unrecognized options. */
  public static final int UNRECOGNIZED_OPT_REMOVE = 1;
  /** Code for marking unrecognized options. */
  public static final int UNRECOGNIZED_OPT_MARK   = 2;
  /** Code for dropping unrecognized options. */
  public static final int UNRECOGNIZED_OPT_DROP   = 3;

  //////////////////////////////////////////////////
  // locals
  //

  /** The payload of the DSR message. */
  private Message content;

  /** The list of options contained in the DSR header. */
  private ArrayList options;

  /** The protocol number of the message contained in this DSR header. */
  private short nextHeaderType = Constants.NET_PROTOCOL_NO_NEXT_HEADER;

  //////////////////////////////////////////////////
  // initialization
  //

  /**
   * Creates a new <code>RouteDsrMsg</code> with the given payload.
   *
   * @param content the payload of the DSR message.  This can be
   *   <code>null</code>.
   */
  public RouteDsrMsg(Message content)
  {
    this.content = content;
    options = new ArrayList();
  }

  //////////////////////////////////////////////////
  // helpers
  //

  /**
   * Returns the options contained in the DSR header.
   *
   * @return <code>List</code> of options.
   */
  public List getOptions()
  {
    return options;
  }

  /**
   * Returns the the content of the DSR message.
   *
   * @return the content of the DSR message, or <code>null</code> if none.
   */
  public Message getContent()
  {
    return content;
  }

  /**
   * Returns the network protocol number of the content of the DSR message.
   *
   * @return the protocol number of the content of the DSR message.  This is
   *   equal to IPv6's No Next Header protocol number if the DSR header has
   *   no content.
   */
  public short getNextHeaderType()
  {
    return nextHeaderType;
  }

  /**
   * Sets the protocol number of the content of the DSR message.
   *
   * @param nextHeaderType the protocol number of the content of the DSR
   *   message.
   */
  public void setNextHeaderType(short nextHeaderType)
  {
    this.nextHeaderType = nextHeaderType;
  }

  /** {@inheritDoc} */
  public void getBytes(byte[] buf, int offset)
  {
    // Copy the fixed part of the DSR Options header
    if (buf.length - offset > 0)
    {
      buf[offset++] = (byte)nextHeaderType;
    }

    if (buf.length - offset > 0)
    {
      buf[offset++] = 0;
    }

    if (buf.length - offset > 0)
    {
      buf[offset++] = (byte)((getOptionsSize() >> 8) & 0xFF);
    }

    if (buf.length - offset > 0)
    {
      buf[offset++] = (byte)(getOptionsSize() & 0xFF);
    }

    // Copy the options
    for (int i = 0; i < options.size(); i++)
    {
      byte[] option = (byte[])options.get(i);
      int bytesToCopy = Math.min(buf.length - offset, option.length);

      System.arraycopy(buf, offset, option, 0, bytesToCopy);
      offset += bytesToCopy;
    }

    // Copy the remainder of the message
    if (content != null)
    {
      content.getBytes(buf, offset);
    }
  }

  /** {@inheritDoc} */
  public int getSize()
  {
    int headerSize = 4 + getOptionsSize();

    return headerSize + (content == null ? 0 : content.getSize());
  }

  /**
   * Adds a new option to the DSR message.
   *
   * @param opt the option to add
   */
  public void addOption(byte[] opt)
  {
    options.add(opt);
  }

  /**
   * Determines whether the DSR header contains an option of the given type.
   *
   * @param optType the type of option to search for
   * @return whether the DSR header contains an option of type
   *   <code>optType</code>.
   */
  public boolean hasOption(byte optType)
  {
    Iterator iter = options.iterator();

    while (iter.hasNext())
    {
      byte[] opt = (byte[])iter.next();
      if (opt[0] == optType) return true;
    }

    return false;
  }

  /**
   * Determines the size in bytes of the options contained in this DSR message.
   *
   * @return the size in bytes of the options contained in this DSR message.
   *   This value does not include the size of the fixed portion of the DSR
   *   header or the payload of the message.
   */
  private int getOptionsSize()
  {
    int optionsSize = 0;

    for (int i = 0; i < options.size(); i++)
    {
      optionsSize += ((byte[])options.get(i)).length;
    }

    return optionsSize;
  }

  /** {@inheritDoc} */
  public Object clone()
  {
    // Note that this clone is fairly shallow -- it doesn't copy the
    // content of the DSR message, and the individual options are
    // not cloned either.
    RouteDsrMsg copy = new RouteDsrMsg(content);
    copy.options = (ArrayList)options.clone();
    copy.nextHeaderType = nextHeaderType;
    return copy;
  }

  //////////////////////////////////////////////////
  // internal classes
  //

  /**
   * The base class for all DSR header options.
   */
  public abstract static class Option
  {
    /** The raw bytes of the option. */
    protected byte[] optBuf;

    /** The offset into <code>optbuf</code> where the option encoding begins. */
    protected int optBufOffset;

    /**
     * Creates a new option from the given byte array, starting at
     * the given offset.
     *
     * @param buf    the buffer containing the option
     * @param offset the offset into <code>buf</code> where the option begins
     */
    public Option(byte[] buf, int offset)
    {
      optBuf = buf;
      optBufOffset = offset;
    }

    /**
     * Returns the DSR type code for this kind of option.
     *
     * @return the DSR type code for this kind of option.
     */
    public abstract byte getType();

    /**
     * Returns the size in bytes of this option.
     *
     * @return the size in bytes of this option.
     */
    public abstract int getSize();

    /**
     * Returns the DSR type code for the given option.
     *
     * @param buf a DSR option in raw byte form
     * @return the DSR type code of <code>buf</code>.
     */
    public static byte getType(byte[] buf)
    {
      return buf[0];
    }

    /**
     * Retrieves the actual bytes of the option.  Copies up to the
     * end of the buffer or the end of the option, whichever comes
     * first.
     *
     * @param buf the buffer to put the option bytes in
     * @param offset the offset into the buffer to start writing at
     * @return the number of bytes written.
     */
    public int getBytes(byte[] buf, int offset)
    {
      int numBytesToCopy = Math.min(getSize(), buf.length - offset);
      System.arraycopy(optBuf, optBufOffset, buf, offset, Math.min(getSize(), numBytesToCopy));
      return numBytesToCopy;
    }

    /** {@inheritDoc} */
    public boolean equals(Object o)
    {
      if (!(o instanceof Option))
      {
        return false;
      }

      Option opt = (Option)o;
      return optBufOffset == opt.optBufOffset && Arrays.equals(optBuf, opt.optBuf);
    }

    /** {@inheritDoc} */
    public int hashCode()
    {
      // I don't think this method ever gets used
      return optBuf.hashCode();
    }

    /**
     * Creates a new <code>Option</code> from the given byte array, starting at
     * the given offset.
     *
     * @param buf    the DSR option in raw byte form
     * @param offset the offset into <code>buf</code> where the option begins
     * @return the DSR option encoded in <code>buf</code>, or <code>null</code>
     *   if <code>buf</code> is not a recognized option.
     */
    public static Option create(byte[] buf, int offset)
    {
      switch (buf[0])
      {
        case OPT_ROUTE_REQUEST:
          return new OptionRouteRequest(buf, offset);

        case OPT_ROUTE_REPLY:
          return new OptionRouteReply(buf, offset);

        case OPT_ROUTE_ERROR:
          return new OptionRouteError(buf, offset);

        case OPT_ACK_REQUEST:
          return new OptionAckRequest(buf, offset);

        case OPT_ACK:
          return new OptionAck(buf, offset);

        case OPT_SOURCE_ROUTE:
          return new OptionSourceRoute(buf, offset);

        case OPT_PAD1:
          return new OptionPad1(buf, offset);

        case OPT_PADN:
          return new OptionPadN(buf, offset);

        default:
          return null;
      }
    }
  }

  /** A Route Request option. */
  public static class OptionRouteRequest extends Option
  {
    /** {@inheritDoc} */
    public OptionRouteRequest(byte[] buf, int offset)
    {
      super(buf, offset);
    }

    /** {@inheritDoc} */
    public byte getType()
    {
      return OPT_ROUTE_REQUEST;
    }

    /** {@inheritDoc} */
    public int getSize()
    {
      // Defeat sign-extension and add 2 for the Option Type and Opt Data Len fields
      return (optBuf[optBufOffset + 1] & 0x000000FF) + 2;
    }

    /**
     * Returns the identification number of this route request.
     *
     * @return the identification number of this route request.
     */
    public short getId()
    {
      // Why doesn't Java have unsigned integer types?
      short highOrderByte = (short)(optBuf[optBufOffset + 2] & 0x000000FF);
      short lowOrderByte = (short)(optBuf[optBufOffset + 3] & 0x000000FF);
      return (short)((highOrderByte << 8) | lowOrderByte);
    }

    /**
     * Returns the target address of this route request.
     *
     * @return the target address of this route request.
     */
    public NetAddress getTargetAddress()
    {
      return new NetAddress(new byte[] { optBuf[optBufOffset + 4], optBuf[optBufOffset + 5],
                                         optBuf[optBufOffset + 6], optBuf[optBufOffset + 7] });
    }

    /**
     * Returns the number of addresses listed in the route request option
     * (not includingd the target address).
     *
     * @return the number of addresses listed in the route request option
     *   (not including the target address).
     */
    public int getNumAddresses()
    {
      return (getSize() - 8) / 4;
    }

    /**
     * Returns the nth address listed in the route request option, counting
     * from zero.
     *
     * @param n the index into the Route Request
     * @return the address indexed by <code>n</code>.
     */
    public NetAddress getAddress(int n)
    {
      if (n >= getNumAddresses())
        throw new IndexOutOfBoundsException();

      int addressOffset = optBufOffset + 8 + 4*n;
      return new NetAddress(new byte[] { optBuf[addressOffset], optBuf[addressOffset + 1],
                                         optBuf[addressOffset + 2], optBuf[addressOffset + 3] });
    }

    /**
     * Creates a new Route Request option.
     *
     * @param id the identification number of the route request
     * @param target the address of the node being searched for
     * @param addrs the addresses of the nodes that have forwarded this request
     * @return the byte array corresponding to the desired option.
     */
    public static byte[] create(short id, NetAddress target, NetAddress[] addrs)
    {
      byte[] opt = new byte[8 + 4*addrs.length];

      // Set the Option Type and Option Data Length fields
      opt[0] = OPT_ROUTE_REQUEST;
      opt[1] = (byte)(opt.length - 2);

      // Set the identification field
      opt[2] = (byte)(id >>> 8);
      opt[3] = (byte)(id & 0xFF);

      // Set the target and intermediate addresses
      System.arraycopy(target.getIP().getAddress(), 0, opt, 4, 4);

      for (int i = 0; i < addrs.length; i++)
      {
        System.arraycopy(addrs[i].getIP().getAddress(), 0, opt, 4*(i+2), 4);
      }

      return opt;
    }
  }

  /** A Route Reply option. */
  public static class OptionRouteReply extends Option
  {
    /** {@inheritDoc} */
    public OptionRouteReply(byte[] buf, int offset)
    {
      super(buf, offset);
    }

    /** {@inheritDoc} */
    public byte getType()
    {
      return OPT_ROUTE_REPLY;
    }

    /** {@inheritDoc} */
    public int getSize()
    {
      // Defeat sign-extension and add 2 for the Option Type and Opt Data Len fields
      return (optBuf[optBufOffset + 1] & 0x000000FF) + 2;
    }

    /**
     * Determines whether the last hop of the path is external.
     *
     * @return whether the last hop of this route is external.
     */
    public boolean isLastHopExternal()
    {
      return (optBuf[optBufOffset + 2] & 0x80) != 0;
    }

    /**
     * Returns the number of addresses listed in the Route Reply option.
     *
     * @return the number of addresses listed in the Route Reply option.
     */
    public int getNumAddresses()
    {
      return (getSize() - 3) / 4;
    }

    /**
     * Returns the nth address listed in the route request option, counting
     * from zero.
     *
     * @param n the index into the route
     * @return the address indexed by <code>n</code>.
     */
    public NetAddress getAddress(int n)
    {
      if (n >= getNumAddresses())
        throw new IndexOutOfBoundsException();

      int addressOffset = optBufOffset + 3 + 4*n;
      return new NetAddress(new byte[] { optBuf[addressOffset], optBuf[addressOffset + 1],
                                         optBuf[addressOffset + 2], optBuf[addressOffset + 3] });
    }

    /**
     * Creates a new Route Reply option.
     *
     * @param lastHopExternal whether the last hop in the route is external
     * @param addrs the addresses of the nodes in the route
     * @return the byte array corresponding to the desired option.
     */
    public static byte[] create(boolean lastHopExternal, NetAddress[] addrs)
    {
      byte[] opt = new byte[3 + 4*addrs.length];

      // Set the Option Type and Option Data Length fields
      opt[0] = OPT_ROUTE_REPLY;
      opt[1] = (byte)(opt.length - 2);

      // Set the Last Hop External and Reserved fields
      opt[3] = lastHopExternal ? (byte)0x80 : 0x00;

      // Set the route addresses
      for (int i = 0; i < addrs.length; i++)
      {
        System.arraycopy(addrs[i].getIP().getAddress(), 0, opt, 3 + 4*i, 4);
      }

      return opt;
    }

    /**
     * Creates a new Route Reply option with the Last Hop External field
     * set to false.
     *
     * @param addrs the addresses of the nodes in the route
     * @return the byte array corresponding to the desired option.
     */
    public static byte[] create(NetAddress[] addrs)
    {
      return create(false, addrs);
    }
  }

  /** A Route Error option. */
  public static class OptionRouteError extends Option
  {
    /** {@inheritDoc} */
    public OptionRouteError(byte[] buf, int offset)
    {
      super(buf, offset);
    }

    /** {@inheritDoc} */
    public byte getType()
    {
      return OPT_ROUTE_ERROR;
    }

    /** {@inheritDoc} */
    public int getSize()
    {
      // Defeat sign-extension and add 2 for the Option Type and Opt Data Len fields
      return (optBuf[optBufOffset + 1] & 0x000000FF) + 2;
    }

    /**
     * Returns the error code contained in this Route Error.
     *
     * @return the error code contained in this Route Error.
     */
    public int getErrorType()
    {
      return optBuf[optBufOffset + 2] & 0xFF;
    }

    /**
     * Returns the salvage count of this Route Error.
     *
     * @return the salvage count of this Route Error.
     */
    public int getSalvageCount()
    {
      return optBuf[optBufOffset + 3] & 0xF;
    }

    /**
     * Returns the source of this Route Error.
     * 
     * @return the source of this Route Error.
     */
    public NetAddress getSourceAddress()
    {
      return new NetAddress(new byte[] { optBuf[optBufOffset + 4], optBuf[optBufOffset + 5],
                                         optBuf[optBufOffset + 6], optBuf[optBufOffset + 7] });
    }

    /**
     * Returns the destination of this Route Error.
     *
     * @return the destination of this Route Error.
     */
    public NetAddress getDestAddress()
    {
      return new NetAddress(new byte[] { optBuf[optBufOffset + 8], optBuf[optBufOffset + 9],
                                         optBuf[optBufOffset + 10], optBuf[optBufOffset + 11] });
    }

    /**
     * Returns the size in bytes of any type-specific information contained in
     * this Route Error.
     *
     *  @return the size in bytes of the type-specific information.
     */
    public int getTypeSpecificInfoSize()
    {
      return getSize() - 12;
    }

    /**
     * Returns the type-specific information contained in this Route Error.
     *
     * @param buf    the byte array to copy the type-specific information into
     * @param offset the offset into <code>buf</code> where copying should begin
     * @return the number of bytes copied into <code>buf</code>.
     */
    public int getTypeSpecificInfoBytes(byte[] buf, int offset)
    {
      int numBytesToCopy = Math.min(getTypeSpecificInfoSize(), buf.length - offset);
      System.arraycopy(optBuf, optBufOffset + 12, buf, offset, Math.min(getSize(), numBytesToCopy));
      return numBytesToCopy;
    }

    /**
     * Creates a new Route Error option.
     *
     * @param type the code corresponding to the type of error being reported
     * @param salvage the salvage count
     * @param src the source address of the error
     * @param dest the destination address of the error
     * @param tsi any type-specific information for this error
     * @return the byte array corresponding to the desired option.
     */
    public static byte[] create(byte type, int salvage, NetAddress src, NetAddress dest, byte[] tsi)
    {
      byte[] opt = new byte[12 + tsi.length];

      // Set the Option Type, Option Data Length, and Error Type fields
      opt[0] = OPT_ROUTE_ERROR;
      opt[1] = (byte)(opt.length - 2);
      opt[2] = type;

      // Set the Salvage and Reserved fields.
      if (salvage < 0 || salvage > 0xF)
        throw new IllegalArgumentException("Salvage count too high");

      opt[3] = (byte)salvage;

      // Set the source and destination fields
      System.arraycopy(src.getIP().getAddress(), 0, opt, 4, 4);
      System.arraycopy(dest.getIP().getAddress(), 0, opt, 8, 4);

      // Set the Type-Specific Information field
      System.arraycopy(tsi, 0, opt, 12, tsi.length);

      return opt;
    }
  }

  /** An Acknowledgement Request option. */
  public static class OptionAckRequest extends Option
  {
    /** {@inheritDoc} */
    public OptionAckRequest(byte[] buf, int offset)
    {
      super(buf, offset);
    }

    /** {@inheritDoc} */
    public byte getType()
    {
      return OPT_ACK_REQUEST;
    }

    /** {@inheritDoc} */
    public int getSize()
    {
      // Defeat sign-extension and add 2 for the Option Type and Opt Data Len fields
      return (optBuf[optBufOffset + 1] & 0x000000FF) + 2;
    }

    /**
     * Returns the id number of the Acknowledgement Request.
     *
     * @return the id number of the Acknowledgement Request.
     */
    public short getId()
    {
      short highOrderByte = (short)(optBuf[optBufOffset + 2] & 0x000000FF);
      short lowOrderByte = (short)(optBuf[optBufOffset + 3] & 0x000000FF);
      return (short)((highOrderByte << 8) | lowOrderByte);
    }

    /**
     * Creates a new Acknowledgement Request option.
     *
     * @param id the identification number of the acknowledgement request
     * @return the byte array corresponding to the desired option.
     */
    public static byte[] create(short id)
    {
      byte[] opt = new byte[4];

      // Set the Option Type and Option Data Length fields
      opt[0] = OPT_ACK_REQUEST;
      opt[1] = 4;

      // Set the identification field
      opt[2] = (byte)(id >>> 8);
      opt[3] = (byte)(id & 0xFF);

      return opt;
    }
  }

  /** An Acknowledgement option. */
  public static class OptionAck extends Option
  {
    /** {@inheritDoc} */
    public OptionAck(byte[] buf, int offset)
    {
      super(buf, offset);
    }

    /** {@inheritDoc} */
    public byte getType()
    {
      return OPT_ACK;
    }

    /** {@inheritDoc} */
    public int getSize()
    {
      // Defeat sign-extension and add 2 for the Option Type and Opt Data Len fields
      return (optBuf[optBufOffset + 1] & 0x000000FF) + 2;
    }

    /**
     * Returns the id number of the Acknowledgement.
     *
     * @return the id number of the Acknowledgement.
     */
    public short getId()
    {
      short highOrderByte = (short)(optBuf[optBufOffset + 2] & 0x000000FF);
      short lowOrderByte = (short)(optBuf[optBufOffset + 3] & 0x000000FF);
      return (short)((highOrderByte << 8) | lowOrderByte);
    }

    /**
     * Returns the source of the Acknowledgement.
     *
     * @return the source of the Acknowledgement.
     */
    public NetAddress getSourceAddress()
    {
      return new NetAddress(new byte[] { optBuf[optBufOffset + 4], optBuf[optBufOffset + 5],
                                         optBuf[optBufOffset + 6], optBuf[optBufOffset + 7] });
    }

    /**
     * Returns the destination of the Acknowledgement.
     *
     * @return the destination of the Acknowledgement.
     */
    public NetAddress getDestAddress()
    {
      return new NetAddress(new byte[] { optBuf[optBufOffset + 8], optBuf[optBufOffset + 9],
                                         optBuf[optBufOffset + 10], optBuf[optBufOffset + 11] });
    }

    /**
     * Creates a new Acknowledgement option.
     *
     * @param id the identification number of the acknowledgement
     * @param src the source address of the acknowledgement
     * @param dest the destination address of the acknowledgement
     * @return the byte array corresponding to the desired option.
     */
    public static byte[] create(short id, NetAddress src, NetAddress dest)
    {
      byte[] opt = new byte[12];

      // Set the Option Type and Option Data Length fields
      opt[0] = OPT_ACK;
      opt[1] = 4;

      // Set the identification field
      opt[2] = (byte)(id >>> 8);
      opt[3] = (byte)(id & 0xFF);

      // Set the source and destination fields
      System.arraycopy(src.getIP().getAddress(), 0, opt, 4, 4);
      System.arraycopy(dest.getIP().getAddress(), 0, opt, 8, 4);

      return opt;
    }
  }

  /** A Source Route option. */
  public static class OptionSourceRoute extends Option
  {
    /** {@inheritDoc} */
    public OptionSourceRoute(byte[] buf, int offset)
    {
      super(buf, offset);
    }

    /** {@inheritDoc} */
    public byte getType()
    {
      return OPT_SOURCE_ROUTE;
    }

    /** {@inheritDoc} */
    public int getSize()
    {
      return (optBuf[optBufOffset + 1] & 0x000000FF) + 2;
    }

    /**
     * Indicates whether the first hop on the route is external to the DSR
     * network.
     *
     * @return whether the first hop is external.
     */
    public boolean isFirstHopExternal()
    {
      return (optBuf[optBufOffset + 2] & 0x80) != 0;
    }

    /**
     * Indicates whether the last hop on the route is external to the DSR
     * network.
     *
     * @return whether the last hop is external.
     */
    public boolean isLastHopExternal()
    {
      return (optBuf[optBufOffset + 2] & 0x40) != 0;
    }

    /**
     * Returns the salvage count of this option.
     *
     * @return the salvage count of this option.
     */
    public int getSalvageCount()
    {
      // The salvage count overlaps the third and fourth byte
      int b3 = optBuf[optBufOffset + 2] & 0xFF;
      int b4 = optBuf[optBufOffset + 3] & 0xFF;
      return ((b3 & 0x3) << 2) | ((b4 & 0xC0) >> 6);
    }

    /**
     * Returns the number of segments on the source route that have yet to be
     * traversed.
     *
     * @return the number of segments on the route left to go.
     */
    public int getNumSegmentsLeft()
    {
      return optBuf[optBufOffset + 3] & 0x3F;
    }

    /**
     * Returns the number of addresses in the route, not including the source
     * and destination addresses.
     *
     * @return the number of addresses in the route.
     */
    public int getNumAddresses()
    {
      return (getSize() - 4) / 4;
    }

    /**
     * Returns the nth address on the route, counting the first hop after the
     * source as node zero.
     *
     * @param n the index into the route
     * @return the address in the route indexed by <code>n</code>.
     */
    public NetAddress getAddress(int n)
    {
      if (n >= getNumAddresses())
        throw new IndexOutOfBoundsException();

      int addressOffset = optBufOffset + 4 + 4*n;
      return new NetAddress(new byte[] { optBuf[addressOffset], optBuf[addressOffset + 1],
                                         optBuf[addressOffset + 2], optBuf[addressOffset + 3] });
    }

    /**
     * Creates a new Source Route option.
     *
     * @param firstHopExternal whether the first hop on the route is external
     * @param lastHopExternal whether the last hop on the route is external
     * @param salvage the salvage count
     * @param segsLeft the number of explicitly listed intermediate nodes still to be
     *   visited before reaching the final destination
     * @param addrs the addresses of the nodes along the route
     * @return The byte array corresponding to the desired option.
     */
    public static byte[] create(boolean firstHopExternal, boolean lastHopExternal,
                                int salvage, int segsLeft, NetAddress[] addrs)
    {
      byte[] opt = new byte[4 + 4*addrs.length];

      // Set the Option Type and Option Data Length fields
      opt[0] = OPT_SOURCE_ROUTE;
      opt[1] = (byte)(opt.length - 2);

      // Set the First Hop External, Last Hop External, and Reserved fields, along
      // with the upper two bits of the Salvage field
      if (salvage < 0 || salvage > 0xF)
        throw new IllegalArgumentException("Salvage count too high");

      opt[2] = 0;
      if (firstHopExternal) opt[2] |= 0x80;
      if (lastHopExternal) opt[2] |= 0x40;
      opt[2] |= (byte)(salvage >> 2);

      // Set the rest of the Salvage field and the Segments Left field
      if (segsLeft < 0 || segsLeft > 0x3F)
        throw new IllegalArgumentException("Segments Left count too high");

      opt[3] = 0;
      opt[3] |= (byte)((salvage << 6) & 0xC0);
      opt[3] |= segsLeft;

      // Set the addresses
      for (int i = 0; i < addrs.length; i++)
      {
        System.arraycopy(addrs[i].getIP().getAddress(), 0, opt, 4 + 4*i, 4);
      }

      return opt;
    }

    /**
     * Creates a new Source Route option with the First Hop External and the
     * Last Hop External fields set to false.
     *
     * @param salvage the salvage count
     * @param segsLeft the number of explicitly listed intermediate nodes still to be
     *   visited before reaching the final destination
     * @param addrs the addresses of the nodes along the route
     * @return the byte array corresponding to the desired option.
     */
    public static byte[] create(int salvage, int segsLeft, NetAddress[] addrs)
    {
      return create(false, false, salvage, segsLeft, addrs);
    }
  }

  /** A Pad1 option.  This represents one byte of padding in the DSR header. */
  public static class OptionPad1 extends Option
  {
    /** {@inheritDoc} */
    public OptionPad1(byte[] buf, int offset)
    {
      super(buf, offset);
    }

    /** {@inheritDoc} */
    public byte getType()
    {
      return OPT_PAD1;
    }

    /** {@inheritDoc} */
    public int getSize()
    {
      // Pad1 option is always one byte long
      return 1;
    }

    /**
     * Creates a new Pad1 option.
     *
     * @return a byte array corresponding to the desired option.
     */
    public static byte[] create()
    {
      return new byte[] { OPT_PAD1 };
    }
  }

  /** A PadN option.  This represents N bytes of padding in the DSR header. */
  public static class OptionPadN extends Option
  {
    /** {@inheritDoc} */
    public OptionPadN(byte[] buf, int offset)
    {
      super(buf, offset);
    }

    /** {@inheritDoc} */
    public byte getType()
    {
      return OPT_PADN;
    }

    /** {@inheritDoc} */
    public int getSize()
    {
      return (optBuf[optBufOffset + 1] & 0x000000FF) + 2;
    }

    /**
     * Creates a new PadN option.
     *
     * @param len the length of the option in bytes, not including the
     *   Option Type or Option Data Length fields
     * @return a byte array corresponding to the desired option.
     */
    public static byte[] create(byte len)
    {
      byte[] opt = new byte[len + 2];

      // Set the Option Type and Option Data Length fields
      opt[0] = OPT_PADN;
      opt[1] = len;

      // The rest of the option is already set to zero, thanks to Java
      return opt;
    }
  }
}
