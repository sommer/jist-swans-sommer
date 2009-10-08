//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <TransTcp.java Tue 2004/04/06 11:38:10 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.trans;

import jist.swans.mac.MacAddress;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.misc.Message;
import jist.swans.misc.MessageBytes;
import jist.swans.misc.Pickle;
import jist.swans.Constants;

import java.util.HashMap;

import jist.runtime.JistAPI;

/** 
 * Implementation of TCP Transport Layer.
 *
 * @author Kelwin Tamtoro &lt;kt222@cs.cornell.edu&gt;
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: TransTcp.java,v 1.49 2004-04-06 16:07:51 barr Exp $
 * @since SWANS1.0
 */
public class TransTcp implements TransInterface.TransTcpInterface
{

  //////////////////////////////////////////////////
  // message structures
  //

  /**
   * Implementation for TCP Options.
   * (currently not used)
   */
  public static class TcpOptions extends TransInterface.TransMessage
  {
    /** Constructor. */
    public TcpOptions ()
    {
    }
  
    /** 
     * Returns the size of the option in a message.
     *
     * @return size of option
     */
    public int getSize()
    {
      return 0;
    }
    
    /**
     * Retrieve the option and store it in the given byte array.
     *
     * @param msg byte array to copy the option to
     * @param offset starting index in the destination array
     */
    public void getBytes(byte[] msg, int offset)
    {
      throw new RuntimeException("not implemented");
    }
  }

  /** 
   * Data structure for TCP Message.
   * TCP Packet
   *  header:
   *      srcPort           : 2
   *      dstPort           : 2
   *      seqNum            : 4
   *      ackNum            : 4
   *      offset and flags  : 2
   *      window size       : 2
   *      checksum          : 2
   *      urgent pointer    : 2
   *  TOTAL HEADER SIZE     : 20
   *  options               : variable
   *  data                  : variable
   */
  public static class TcpMessage extends TransInterface.TransMessage
  {

    /**
     * Minimum size of TCP message.
     */
    public static final int HEADER_SIZE = 20;

    /**
     * 16-bit source port number.
     */
    private short srcPort;

    /**
     * 16 bit destination port number.
     */
    private short dstPort;

    /**
     * 32 bit sequence number of first data octet in this segment.
     */
    private int seqNum;

    /**
     * 32 bit acknowledgement number.
     */
    private int ackNum;

    /**
     * 16 bit offset and flags.
     * this contains:
     *  4 bit data offset (number of 32 bit words in the TCP header)
     *  6 bit reserved
     *  1 bit URG  - urgent pointer field significant
     *  1 bit ACK  - acknowledgement field significant
     *  1 bit PSH  - push function
     *  1 bit RST  - reset the connection
     *  1 bit SYN  - synchronize sequence numbers
     *  1 bit FIN  - no more data from sender
     */
    private short offsetAndFlags;

    /**
     * 16 bit window size (number of data octets to be accepted).
     */
    private short windowSize;

    /**
     * checksum.
     */
    private short errorChecksum;

    /**
     * current value of the urgent pointer as a positive offset from the sequence number.
     */
    private short urgentPointer;

    /**
     * options field (contains padding to make this field 32 bit boundary).
     */
    private TcpOptions options;

    /**
     * data.
     */
    private Message payload;


    /**
     * constructor for TcpMessage.
     *
     * @param srcPort source port number
     * @param dstPort destination port number
     * @param seqNum sequence number
     * @param ackNum acknowledgement number
     * @param offset data offset (start of data in the header; used when the packet has options) 
     * @param URG urgent flag
     * @param ACK acknowledgement flag
     * @param PSH push flag
     * @param RST reset flag
     * @param SYN SYN flag
     * @param FIN FIN flag
     * @param windowSize size of receiving window
     * @param data data 
     */
    public TcpMessage (short srcPort, short dstPort, int seqNum, int ackNum, short offset,
                          boolean URG, boolean ACK, boolean PSH, boolean RST, boolean SYN,
                          boolean FIN, short windowSize, Message data)
    {
      this.srcPort = srcPort;
      this.dstPort = dstPort;
      this.seqNum = seqNum;
      this.ackNum = ackNum;
      // filling offsetAndFlags
      this.offsetAndFlags = (short)((short)(offset << 12) + (short)((URG ? 1 : 0) << 5) 
        + (short)((ACK ? 1 : 0) << 4) + (short)((PSH ? 1 : 0) << 3) + 
        (short)((RST ? 1 : 0) << 2) + (short)((SYN ? 1 : 0) << 1) + (short)(FIN ? 1 : 0));
      this.windowSize = windowSize;
      this.payload = data;
      this.options = new TcpOptions ();
    }

    /**
     * constructor for TcpMessage (reconstruct TcpMessage from byte array).
     *
     * @param data array containing TCP message
     * @param offset start index to read the array
     */
    public TcpMessage (byte[] data, int offset)
    {
      byte[] temp;
      // source port (unsigned short)
      temp = new byte [2];
      System.arraycopy(data, offset, temp, 0, 2);
      srcPort = (short) Pickle.arrayToUShort(temp, 0);
      // destination port (unsigned short)
      temp = new byte [2];
      System.arraycopy(data, offset+2, temp, 0, 2);
      dstPort = (short) Pickle.arrayToUShort(temp, 0);
      // sequence number (unsigned integer)
      temp = new byte [4];
      System.arraycopy(data, offset+4, temp, 0, 4);
      seqNum = (int) Pickle.arrayToUInt(temp, 0);
      // acknowledgement number (unsigned integer)
      System.arraycopy(data, offset+8, temp, 0, 4);
      ackNum = (int)Pickle.arrayToUInt(temp, 0);
      // offset and flags (unsigned short)
      System.arraycopy(data, offset+12, temp, 0, 2);
      offsetAndFlags = (short) Pickle.arrayToUShort(temp, 0);
      // window size (unsigned short)
      System.arraycopy(data, offset+14, temp, 0, 2);
      windowSize = (short) Pickle.arrayToUShort(temp, 0);
      // checksum (short)
      System.arraycopy(data, offset+16, temp, 0, 2);
      errorChecksum = (short) Pickle.arrayToUShort(temp, 0);
      // urgent pointer (short)
      System.arraycopy(data, offset+18, temp, 0, 2);
      urgentPointer = (short) Pickle.arrayToUShort(temp, 0);
      // options
      int tempOffset = getOffset ();
      int diff = tempOffset - 5;
      if (diff == 0)
      {
        this.options = new TcpOptions ();
      }
      else
      {
        this.options = new TcpOptions ();
      }
      // payload
      temp = new byte [data.length-20-options.getSize()];
      System.arraycopy(data, offset+20+options.getSize(), temp, 0, 
        data.length-20-options.getSize());
      payload = new MessageBytes (temp);
    }

    /**
     * Method called to create a SYN packet.
     *
     * @param sourcePort source port number
     * @param destPort destination port number
     * @param seqNumber sequence number
     * @param windowSize size of receiving window
     * @return SYN packet
     */
    public static TcpMessage createSYNPacket (int sourcePort, int destPort, int seqNumber, 
      short windowSize)
    {
      int seqNum = seqNumber;
      int ackNum = 0;
      short offset = 5;
      boolean URG = false;
      boolean ACK = false;
      boolean PSH = false;
      boolean RST = false;
      boolean SYN = true;
      boolean FIN = false;
      return new TcpMessage ((short)sourcePort, (short)destPort, seqNum, ackNum, offset, 
        URG, ACK, PSH, RST, SYN, FIN, windowSize, new MessageBytes (""));
    }

    /**
     * Method called to create a SYNACK packet.
     *
     * @param sourcePort source port number
     * @param destPort destination port number
     * @param seqNumber sequence number
     * @param ackNumber acknowledgement number
     * @param windowSize size of receiving window
     * @return SYNACK packet
     */
    public static TcpMessage createSYNACKPacket (int sourcePort, int destPort, 
      int seqNumber, int ackNumber, short windowSize)
    {
      int seqNum = seqNumber;
      int ackNum = ackNumber;
      short offset = 5;
      boolean URG = false;
      boolean ACK = true;
      boolean PSH = false;
      boolean RST = false;
      boolean SYN = true;
      boolean FIN = false;
      return new TcpMessage ((short)sourcePort, (short)destPort, seqNum, ackNum, offset, 
        URG, ACK, PSH, RST, SYN, FIN, windowSize, new MessageBytes (""));
    }

    /**
     * Method called to create an ACK packet.
     *
     * @param sourcePort source port number
     * @param destPort destination port number
     * @param seqNumber sequence number
     * @param ackNumber acknowledgement number
     * @param windowSize size of receiving window
     * @return first ACK packet (ACK for SYNACK packet)
     */
    public static TcpMessage createACKPacket (int sourcePort, int destPort, 
      int seqNumber, int ackNumber, short windowSize)
    {
      int seqNum = seqNumber;
      int ackNum = ackNumber;
      short offset = 5;
      boolean URG = false;
      boolean ACK = true;
      boolean PSH = false;
      boolean RST = false;
      boolean SYN = false;
      boolean FIN = false;
      return new TcpMessage ((short)sourcePort, (short)destPort, seqNum, ackNum, offset, 
        URG, ACK, PSH, RST, SYN, FIN, windowSize, new MessageBytes (""));

    }

    /**
     * Method called to create a FIN packet.
     *
     * @param sourcePort source port number
     * @param destPort destination port number
     * @param seqNumber sequence number
     * @param ackNumber acknowledgement number
     * @param windowSize size of receiving window
     * @return FIN packet
     */
    public static TcpMessage createFINPacket (int sourcePort, int destPort, 
      int seqNumber, int ackNumber, short windowSize)
    {
      int seqNum = seqNumber;
      int ackNum = ackNumber;
      short offset = 5;
      boolean URG = false;
      boolean ACK = true;
      boolean PSH = false;
      boolean RST = false;
      boolean SYN = false;
      boolean FIN = true;
      return new TcpMessage ((short)sourcePort, (short)destPort, seqNum, ackNum, offset, 
        URG, ACK, PSH, RST, SYN, FIN, windowSize, new MessageBytes (""));
    }

    /**
     * Method called to create a RST packet.
     *
     * @param sourcePort source port number
     * @param destPort destination port number
     * @param seqNumber sequence number
     * @param ackNumber acknowledgement number
     * @param windowSize size of receiving window
     * @return RST packet
     */
    public static TcpMessage createRSTPacket (int sourcePort, int destPort, 
      int seqNumber, int ackNumber, short windowSize)
    {
      int seqNum = seqNumber;
      int ackNum = ackNumber;
      short offset = 5;
      boolean URG = false;
      boolean ACK = false;
      boolean PSH = false;
      boolean RST = true;
      boolean SYN = false;
      boolean FIN = true;
      return new TcpMessage ((short)sourcePort, (short)destPort, seqNum, ackNum, offset, 
        URG, ACK, PSH, RST, SYN, FIN, windowSize, new MessageBytes (""));
    }


    // Accessor functions
    
    /**
     * Accessor for source port.
     *
     * @return source port
     */
    public short getSrcPort ()
    {
      return this.srcPort;
    }

    /**
     * Accessor for destination port.
     *
     * @return destination port
     */
    public short getDstPort ()
    {
      return this.dstPort;
    }

    /**
     * Accessor for sequence number.
     *
     * @return sequence number
     */
    public int getSeqNum ()
    {
      return this.seqNum;
    }

    /**
     * Accessor for acknowledgement number.
     *
     * @return acknowledgement number
     */
    public int getAckNum ()
    {
      return this.ackNum;
    }

    /**
     * Accessor for offset in the message.
     *
     * @return offset from the beginning to header to data
     */
    public short getOffset ()
    {
      return (short)(this.offsetAndFlags >> 12);
    }

    /**
     * Accessor for URGENT flag.
     *
     * @return state of the URG flag (true if flag is set)
     */
    public boolean getURG ()
    {
      return (((this.offsetAndFlags >> 5) % 2) > 0 ? true : false);
    }

    /**
     * Accessor for ACK flag.
     *
     * @return state of the ACK flag (true if flag is set)
     */
    public boolean getACK ()
    {
      return (((this.offsetAndFlags >> 4) % 2) > 0 ? true : false);
    }

    /**
     * Accessor for PSH flag.
     *
     * @return state of the PSH flag (true if flag is set)
     */
    public boolean getPSH ()
    {
      return (((this.offsetAndFlags >> 3) % 2) > 0 ? true : false);
    }

    /**
     * Accessor for RST flag.
     *
     * @return state of the RST flag (true if flag is set)
     */
    public boolean getRST ()
    {
      return (((this.offsetAndFlags >> 2) % 2) > 0 ? true : false);
    }

    /**
     * Accessor for SYN flag.
     *
     * @return state of the SYN flag (true if flag is set)
     */
    public boolean getSYN ()
    {
      return (((this.offsetAndFlags >> 1) % 2) > 0 ? true : false);
    }

    /**
     * Accessor for FIN flag.
     *
     * @return state of the FIN flag (true if flag is set)
     */
    public boolean getFIN ()
    {
      return ((this.offsetAndFlags % 2) > 0 ? true : false);
    }

    /**
     * Accessor for window size.
     *
     * @return window size
     */
    public short getWindowSize ()
    {
      return this.windowSize;
    }
    
    /**
     * Accessor for options.
     *
     * @return TcpOptions object
     */
    public TcpOptions getOptions ()
    {
      return this.options;
    }
    
    /**
     * Accessor for payload.
     *
     * @return payload
     */
    public Message getPayload ()
    {
      return this.payload;
    }
    
    /**
     * Returns the size of the TCP message.
     *
     * @return size of message
     */
    public int getSize()
    {
      return HEADER_SIZE + options.getSize() + payload.getSize();
    }

    /**
     * Retrieves the message in byte array.
     *
     * @param msg byte array to store the message
     * @param offset start index of the destination array
     */
    public void getBytes(byte[] msg, int offset)
    {
      // source port (unsigned short)
      Pickle.ushortToArray(srcPort, msg, offset);
      // destination port (unsigned short)
      Pickle.ushortToArray(dstPort, msg, offset+2);
      // sequence number (unsigned integer)
      Pickle.uintToArray(seqNum, msg, offset+4);
      // acknowledgement number (unsigned integer)
      Pickle.uintToArray(ackNum, msg, offset+8);
      // offset and flags (unsigned short)
      Pickle.ushortToArray(offsetAndFlags, msg, offset+12);
      // window size (unsigned short)
      Pickle.ushortToArray(windowSize, msg, offset+14);
      // checksum (short)
      Pickle.ushortToArray(errorChecksum, msg, offset+16);
      // urgent pointer (short)
      Pickle.ushortToArray(urgentPointer, msg, offset+18);
      // options
      options.getBytes (msg, offset+20);
      // payload
      payload.getBytes (msg, offset+20+options.getSize());
    }

    /**
     * Returns string representation of the TCP message.
     *
     * @return string representation of the message
     */
    public String toString()
    {
      StringBuffer sb = new StringBuffer();
      sb.append("src="+getSrcPort());
      sb.append(" dst="+getDstPort());
      sb.append(" seq="+getSeqNum());
      sb.append(" ack="+getAckNum());
      sb.append(" off="+getOffset());
      sb.append(" flags:");
      if(getURG()) sb.append(" URG");
      if(getACK()) sb.append(" ACK");
      if(getPSH()) sb.append(" PSH");
      if(getRST()) sb.append(" RST");
      if(getSYN()) sb.append(" SYN");
      if(getFIN()) sb.append(" FIN");
      sb.append(" win="+getWindowSize());
      String payload = new String (((MessageBytes)getPayload()).getBytes());
      sb.append(" payload=("+payload.length()+") ");
      if (payload.length()>10) payload=payload.substring(0, 10)+"...";
      sb.append(payload);
      return sb.toString();
    }

    /** 
     * Prints out the message header and payload. 
     *
     * @param numTabs number of tabs
     * @param isPrintPayload set to true to print out payload in message
     */
    public void printMessage (int numTabs, boolean isPrintPayload)
    {
      
      String tabs = "";
      for (int i = 0; i < numTabs; i++)
      {
        tabs = tabs + "\t";
      }
      
      System.out.println (tabs + "\tsrc port: " + getSrcPort() + "\tdst port: " + getDstPort());
      System.out.println (tabs + "\tseq num: " + getSeqNum());
      System.out.println (tabs + "\tack num: " + getAckNum());
      System.out.println (tabs + "\toffset: " + getOffset());
      System.out.print (tabs + "\tflags: ");
      if (getURG()) 
      {
        System.out.print ("URG ");
      }
      if (getACK()) 
      {
        System.out.print ("ACK ");
      }
      if (getPSH()) 
      {
        System.out.print ("PSH ");
      }
      if (getRST()) 
      {
        System.out.print ("RST ");
      }
      if (getSYN()) 
      {
        System.out.print ("SYN ");
      }
      if (getFIN()) 
      {
        System.out.print ("FIN ");
      }
      System.out.println ();
      System.out.println (tabs + "\twindow size: " + getWindowSize());
      if (isPrintPayload)
      {
        String temp = new String (((MessageBytes)getPayload()).getBytes());
        int length = temp.length();
        if (length > 10)
        {
          temp = temp.substring (0, 10);
          temp = temp + " ...";
        }      
        System.out.println (tabs + "\tpayload: " + temp + " (" + length + ")");
      }
    }
    
    /** 
     * Prints out the message header with zero tabs. 
     */
    public void printMessage ()
    {
      printMessage (0);
    }
    
    /** 
     * Prints out the message header. 
     *
     * @param numTabs number of tabs
     */
    public void printMessage (int numTabs)
    {
      printMessage (numTabs, false);
    }
  
  
  } // class: TcpMessage


  //////////////////////////////////////////////////
  // tcp entity implementation
  //

  /**
   * probability (in percent) that a message will not be sent.
   * (0 --> no packets are dropped; 100 --> no packets are transmitted)
   */
  private static final int DROP_PROBABILITY = 5;

  /** Entity reference to itself. */
  private TransInterface.TransTcpInterface self;
  
  /** Entity reference to network layer. */
  private NetInterface netEntity;
  
  /** Hashmap to hold references to socket callbacks. */
  private HashMap handlers;

  /**
   * Constructor.
   */
  public TransTcp()
  {    
    self = (TransInterface.TransTcpInterface)JistAPI.proxy(
        this, TransInterface.TransTcpInterface.class);        
    handlers = new HashMap (); 
  }

  /**
   * Returns an entity reference to this object.
   *
   * @return entity reference to TransTcp itself
   */
  public TransInterface.TransTcpInterface getProxy()
  {
    return self;
  }
 
  /**
   * Sets the reference to the network layer.
   *
   * @param netEntity entity reference to network layer
   */
  public void setNetEntity(NetInterface netEntity)
  {
    if(!JistAPI.isEntity(netEntity)) throw new IllegalArgumentException("expected entity");
    this.netEntity = netEntity;
  }

  /** {@inheritDoc} */
  public void addSocketHandler(int port, SocketHandler socketCallback)
  {
    handlers.put(new Integer (port), socketCallback);
    if (TcpSocket.PRINTOUT >= TcpSocket.INFO)
    {
      System.out.println ("TransTcp::addSockethandler: port = " + port);
    }
  }
  
  /** {@inheritDoc} */
  public void delSocketHandler(int port)
  {
    handlers.remove (new Integer (port));
    if (TcpSocket.PRINTOUT >= TcpSocket.INFO)
    {
      System.out.println ("TransTcp::delSockethandler: port = " + port);
    }
  }

  /** {@inheritDoc} */
  public boolean checkSocketHandler(int port)
  {
    boolean ret = handlers.containsKey (new Integer (port));
    return ret;    
  }
  
  /** {@inheritDoc} */
  public void receive(Message msg, NetAddress src, MacAddress lastHop, 
      byte macId, NetAddress dst, byte priority, byte ttl)
  {
    int dstPort = ((TcpMessage)msg).getDstPort();
    SocketHandler handler = 
      (SocketHandler)handlers.get(new Integer (dstPort));
    if(handler==null)
    {
      if (TcpSocket.PRINTOUT >= TcpSocket.FULL_DEBUG)
      {
        System.out.println ("%%%%%%% TransTcp::receive (t=" + JistAPI.getTime()+") -> handler for port " + dstPort + " = null!!!"); 
      }
      return;
    }
    JistAPI.sleep(Constants.TRANS_DELAY);
    handler.receive(msg, src, ((TcpMessage)msg).getDstPort());
  }

  /** {@inheritDoc} */
  public void send(Message msg, NetAddress dst, int dstPort, 
      int srcPort, byte priority)
  {
    // get a random number between 0 and 100
    int prob = Math.abs(Constants.random.nextInt()) % 101;
    if (prob >= DROP_PROBABILITY)
    {
      JistAPI.sleep(Constants.TRANS_DELAY);
      netEntity.send(msg, dst, Constants.NET_PROTOCOL_TCP, 
          priority, Constants.TTL_DEFAULT);
    }
    else
    {
      if (TcpSocket.PRINTOUT >= TcpSocket.TCP_DEBUG)
      {
        System.out.println ("%%%%%% TransTcp::send: PACKET DROPPED: " + msg);
      }
    }
  }
  
  
}// class: TransTcp

