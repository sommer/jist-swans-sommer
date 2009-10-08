//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Mac802_11.java Thu 2005/02/10 11:45:06 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.mac;

import jist.swans.radio.RadioInfo;
import jist.swans.radio.RadioInterface;
import jist.swans.net.NetInterface;
import jist.swans.misc.Message;
import jist.swans.misc.Util;

import jist.swans.Constants;
import jist.swans.Main;

import jist.runtime.JistAPI;

/**
 * Implementation of IEEE 802_11b. Please refer to the standards document. For
 * consistency, many of the variable names, constants and equations are taken
 * directly from the specification.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Mac802_11.java,v 1.62 2005-02-10 16:52:28 barr Exp $
 * @since SWANS1.0
 */

public class Mac802_11 implements MacInterface.Mac802_11
{

  ////////////////////////////////////////////////////
  // short 802.11 lexicon:
  //   slot - minimum time to sense medium
  //   nav  - network allocation vector (virtual carrier sense)
  //   sifs - short inter frame space
  //   pifs - point coordination inter frame space
  //   difs - distributed inter frame space
  //   eifs - extended inter frame space
  //   cw   - collision (avoidance) window
  //   DSSS - Direct Sequence Spread spectrum
  //   FHSS - Frequency Hopping Spread Spectrum
  //

  //////////////////////////////////////////////////
  // constants
  //

  /** Physical specification constant: 802_11b-1999 Supplement 2_4GHz Direct Sequence. */
  public static final boolean DSSS = true;

  /** Physical specification constant: 802_11b-1999 2_4GHz Frequency Hopping. */
  public static final boolean FHSS = false;
  static
  {
    if(!DSSS ^ FHSS)
    {
      throw new RuntimeException("select either DSSS or FHSS");
    }
  }

  /** Length of PHY preamble. */
  public static final long PREAMBLE = 144*Constants.MICRO_SECOND;

  /** Length of PLCP Header at 1Mb/s. */
  public static final long PLCP_HEADER = 48*Constants.MICRO_SECOND;

  /** PHY synchronization time. */
  public static final long SYNCHRONIZATION = PREAMBLE + PLCP_HEADER;

  /** Receive-Transmit turnaround time. */
  public static final long RX_TX_TURNAROUND = 5*Constants.MICRO_SECOND;

  /** Air propagation delay. */
  public static final long PROPAGATION = Constants.MICRO_SECOND;

  /** Minimum time to sense medium. */
  public static final long SLOT_TIME;
  static
  {
    if(DSSS) SLOT_TIME = 20*Constants.MICRO_SECOND;
    if(FHSS) SLOT_TIME = 50*Constants.MICRO_SECOND;
  }

  /**
   * Short interframe space. Minimum wait time between two frames in the same
   * communication session.
   */
  public static final long SIFS;
  static
  {
    if(DSSS) SIFS = 10*Constants.MICRO_SECOND;
    if(FHSS) SIFS = 28*Constants.MICRO_SECOND;
  }

  /**
   * Point coordination inter frame space. Wait used by the access point (point
   * coordinator) to gain access to the medium before any of the stations.
   */
  public static final long PIFS = SIFS + SLOT_TIME;

  /**
   * Distributed inter frame space. Wait used by stations to gain access to the medium.
   */
  public static final long DIFS = SIFS + 2*SLOT_TIME;

  /** Transmit start SIFS. */
  public static final long TX_SIFS = SIFS - RX_TX_TURNAROUND;

  /** Transmit start DIFS. */
  public static final long TX_DIFS = DIFS - RX_TX_TURNAROUND;

  /**
   * Extended inter frame space. Wait used by stations to gain access to the
   * medium after an error.
   */
  public static final long EIFS = SIFS + DIFS + SYNCHRONIZATION +
    8*MacMessage.Ack.SIZE*Constants.MICRO_SECOND;

  /**
   * Threshold packet size to activate RTS. Default=3000. Broadcast packets
   * never use RTS. Set to zero to always use RTS.
   */ 
  public static final int THRESHOLD_RTS = 3000;

  /**
   * Threshold packet size for fragmentation. Default=2346. Broadcast packets
   * are not fragmented.
   */
  public static final int THRESHOLD_FRAGMENT = 2346;

  /** Retransmissions attempted for short packets (those without RTS). */
  public static final byte RETRY_LIMIT_SHORT = 7;

  /** Retransmissions attempted for long packets (those with RTS). */
  public static final byte RETRY_LIMIT_LONG  = 4;

  /** Minimum collision window (for backoff). */
  public static final short CW_MIN;
  static
  {
    if(DSSS) CW_MIN = 31;
    if(FHSS) CW_MIN = 15;
  }
  
  /** Maximum collision window (for backoff). */
  public static final short CW_MAX = 1023;

  /** Invalid sequence number. */
  public static final short SEQ_INVALID = -1;

  /** Sequence number cache size. */
  public static final short SEQ_CACHE_SIZE = 5;

  // mac modes

  /** mac mode: idle. */
  public static final byte MAC_MODE_SIDLE          = 0;
  /** mac mode: waiting for difs or eifs timer. */
  public static final byte MAC_MODE_DIFS           = 1;
  /** mac mode: waiting for backoff. */
  public static final byte MAC_MODE_SBO            = 2;
  /** mac mode: waiting for virtual carrier sense. */
  public static final byte MAC_MODE_SNAV           = 3;
  /** mac mode: waiting for virtual carrier sense to RTS. */
  public static final byte MAC_MODE_SNAV_RTS       = 4;
  /** mac mode: waiting for CTS packet. */
  public static final byte MAC_MODE_SWFCTS         = 5;
  /** mac mode: waiting for DATA packet. */
  public static final byte MAC_MODE_SWFDATA        = 6;
  /** mac mode: waiting for ACK packet. */
  public static final byte MAC_MODE_SWFACK         = 7;
  /** mac mode: transmitting RTS packet. */
  public static final byte MAC_MODE_XRTS           = 8;
  /** mac mode: transmitting CTS packet. */
  public static final byte MAC_MODE_XCTS           = 9;
  /** mac mode: transmitting unicast DATA packet. */
  public static final byte MAC_MODE_XUNICAST       = 10;
  /** mac mode: transmitting broadcast DATA packet. */
  public static final byte MAC_MODE_XBROADCAST     = 11;
  /** mac mode: transmitting ACK packet. */
  public static final byte MAC_MODE_XACK           = 12;

  public static String getModeString(byte mode)
  {
    switch(mode)
    {
      case MAC_MODE_SIDLE: return "IDLE";
      case MAC_MODE_DIFS: return "DIFS";
      case MAC_MODE_SBO: return "BO";
      case MAC_MODE_SNAV: return "NAV";
      case MAC_MODE_SNAV_RTS: return "NAV_RTS";
      case MAC_MODE_SWFCTS: return "WF_CTS";
      case MAC_MODE_SWFDATA: return "WF_DATA";
      case MAC_MODE_SWFACK: return "WF_ACK";
      case MAC_MODE_XRTS: return "X_RTS";
      case MAC_MODE_XCTS: return "X_CTS";
      case MAC_MODE_XUNICAST: return "X_UNICAST";
      case MAC_MODE_XBROADCAST: return "X_BROADCAST";
      case MAC_MODE_XACK: return "X_ACK";
      default: throw new RuntimeException("unknown mode: "+mode);
    }
  }

  //////////////////////////////////////////////////
  // locals
  //

  // entity hookup
  /** Self-referencing mac entity reference. */
  protected final MacInterface.Mac802_11 self;
  /** Radio downcall entity reference. */
  protected RadioInterface radioEntity;
  /** Network upcall entity interface. */
  protected NetInterface netEntity;
  /** network interface number. */
  protected byte netId;

  // properties

  /** link bandwidth (units: bytes/second). */
  protected final int bandwidth;

  /** mac address of this interface. */
  protected MacAddress localAddr;

  /** whether mac is in promiscuous mode. */
  protected boolean promisc;

  // status

  /** current mac mode. */
  protected byte mode;

  /** radio mode used for carrier sense. */
  protected byte radioMode;

  /** whether last reception had an error. */
  protected boolean needEifs;

  // timer

  /** timer identifier. */
  protected byte timerId;

  // backoff

  /** backoff time remaining. */
  protected long bo;

  /** backoff start time. */
  protected long boStart;

  /** current contention window size. */
  protected short cw;

  // nav

  /** virtual carrier sense; next time when network available. */
  protected long nav;

  // sequence numbers

  /** sequence number counter. */
  protected short seq;

  /** received sequence number cache list. */
  protected SeqEntry seqCache;

  /** size of received sequence number cache list. */
  protected byte seqCacheSize;

  // retry counts

  /** short retry counter. */
  protected byte shortRetry;

  /** long retry counter. */
  protected byte longRetry;

  // current packet

  /** packet currently being transmitted. */
  protected Message packet;

  /** next hop of packet current being transmitted. */
  protected MacAddress packetNextHop;


  //////////////////////////////////////////////////
  // initialization 
  //

  /**
   * Instantiate new 802_11b entity.
   *
   * @param addr local mac address
   * @param radioInfo radio properties
   */
  public Mac802_11(MacAddress addr, RadioInfo radioInfo)
  {
    // properties
    bandwidth = radioInfo.getShared().getBandwidth() / 8;
    localAddr = addr;
    promisc = Constants.MAC_PROMISCUOUS_DEFAULT;
    // status
    mode = MAC_MODE_SIDLE;
    radioMode = Constants.RADIO_MODE_IDLE;
    needEifs = false;
    // timer identifier
    timerId = 0;
    // backoff
    bo = 0;
    boStart = 0;
    cw = CW_MIN;
    // virtual carrier sense
    nav = -1;
    // sequence numbers
    seq = 0;
    seqCache = null;
    seqCacheSize = 0;
    // retry counts
    shortRetry = 0;
    longRetry = 0;
    // current packet
    packet = null;
    packetNextHop = null;
    // proxy
    self = (MacInterface.Mac802_11)JistAPI.proxy(this, MacInterface.Mac802_11.class);
  }

  //////////////////////////////////////////////////
  // entity hookup
  //

  /**
   * Return proxy entity of this mac.
   *
   * @return self-referencing proxy entity.
   */
  public MacInterface.Mac802_11 getProxy()
  {
    return this.self;
  }

  /**
   * Hook up with the radio entity.
   *
   * @param radio radio entity
   */
  public void setRadioEntity(RadioInterface radio)
  {
    if(!JistAPI.isEntity(radio)) throw new IllegalArgumentException("expected entity");
    this.radioEntity = radio;
  }

  /**
   * Hook up with the network entity.
   *
   * @param net network entity
   * @param netid network interface number
   */
  public void setNetEntity(NetInterface net, byte netid)
  {
    if(!JistAPI.isEntity(net)) throw new IllegalArgumentException("expected entity");
    this.netEntity = net;
    this.netId = netid;
  }

  //////////////////////////////////////////////////
  // accessors
  //

  /**
   * Set promiscuous mode (whether to pass all packets through).
   *
   * @param promisc promiscuous flag
   */
  public void setPromiscuous(boolean promisc)
  {
    this.promisc = promisc;
  }

  //////////////////////////////////////////////////
  // mac states
  //

  /**
   * Set the current mac mode.
   *
   * @param mode new mac mode
   */
  private void setMode(byte mode)
  {
    this.mode = mode;
  }

  /**
   * Return whether the mac is currently waiting for a response.
   *
   * @return whether mac waiting for response
   */
  public boolean isAwaitingResponse()
  {
    switch(mode)
    {
      case MAC_MODE_SWFCTS:
      case MAC_MODE_SWFDATA:
      case MAC_MODE_SWFACK:
        return true;
      default:
        return false;
    }
  }

  /**
   * Return whether the mac is currently transmitting.
   *
   * @return whether mac is currently transmitting
   */
  public boolean isTransmitting()
  {
    switch(mode)
    {
      case MAC_MODE_XRTS:
      case MAC_MODE_XCTS:
      case MAC_MODE_XUNICAST:
      case MAC_MODE_XBROADCAST:
      case MAC_MODE_XACK:
        return true;
      default:
        return false;
    }
  }

  //////////////////////////////////////////////////
  // packet queries
  //

  /**
   * Return whether mac currently has a packet to send.
   *
   * @return whether mac has packet to send.
   */
  public boolean hasPacket()
  {
    return packet!=null;
  }

  /**
   * Return whether current packet is to be broadcast.
   *
   * @return whether current packet is to be broadcast.
   */
  private boolean isBroadcast()
  {
    return MacAddress.ANY.equals(packetNextHop);
  }

  /**
   * Return whether current packet large enough to require RTS.
   *
   * @return does current packet require RTS.
   */
  private boolean shouldRTS()
  {
    return packet.getSize()>THRESHOLD_RTS && !isBroadcast();
  }

  /**
   * Return whether current packet requires fragmentation.
   *
   * @return does current packet require fragmentation.
   */
  private boolean shouldFragment()
  {
    return packet.getSize()>THRESHOLD_FRAGMENT && !isBroadcast();
  }

  /**
   * Compute packet transmission time at current bandwidth.
   *
   * @param msg packet to transmit
   * @return time to transmit given packet at current bandwidth
   */
  private long transmitTime(Message msg)
  {
    int size = msg.getSize();
    if(size==Constants.ZERO_WIRE_SIZE)
    {
      return Constants.EPSILON_DELAY;
    }
    return SYNCHRONIZATION + size * Constants.SECOND/bandwidth;
  }

  //////////////////////////////////////////////////
  // backoff
  //

  /**
   * Return whether there is a backoff.
   *
   * @return whether backoff non-zero.
   */
  private boolean hasBackoff()
  {
    return bo > 0;
  }

  /**
   * Reset backoff counter to zero.
   */
  private void clearBackoff()
  {
    bo = 0;
  }

  /**
   * Set new random backoff, if current backoff timer has elapsed.
   */
  private void setBackoff()
  {
    if(!hasBackoff())
    {
      bo = Constants.random.nextInt(cw) * SLOT_TIME;
    }
  }

  /**
   * Pause the current backoff (invoked when the channel becomes busy).
   */
  private void pauseBackoff()
  {
    bo -= JistAPI.getTime() - boStart;
    if(Main.ASSERT) Util.assertion(bo>=0);
  }

  /**
   * Perform backoff.
   */
  private void backoff()
  {
    if(Main.ASSERT) Util.assertion(bo>=0);
    boStart = JistAPI.getTime();
    startTimer(bo, MAC_MODE_SBO);
  }

  /**
   * Increase Collision Window.
   */
  private void incCW()
  {
    cw = (short)Math.min(2*cw+1, CW_MAX);
  }

  /**
   * Decrease Collision Windows.
   */
  private void decCW()
  {
    cw = CW_MIN;
  }

  //////////////////////////////////////////////////
  // nav
  //

  /**
   * Return whether the virtual carrier sense (network allocation vector)
   * indicates that the channel is reserved.
   *
   * @return virtual carrier sense
   */
  private boolean waitingNav()
  {
    return nav > JistAPI.getTime();
  }

  /**
   * Clear the virtual carrier sense (network allocation vector).
   */
  private void resetNav()
  {
    nav = -1;
  }

  /**
   * Determine whether channel is idle according to both physical and virtual
   * carrier sense.
   *
   * @return physical and virtual carrier sense
   */
  private boolean isCarrierIdle()
  {
    return !waitingNav() && isRadioIdle();
  }

  //////////////////////////////////////////////////
  // sequence numbers
  //

  /**
   * Local class to used manage sequence number records.
   */
  private static class SeqEntry
  {
    /** src node address. */
    public MacAddress from = MacAddress.NULL;
    /** sequence number. */
    public short seq = 0;
    /** next record. */
    public SeqEntry next = null;
  }

  /**
   * Increment local sequence counter.
   *
   * @return new sequence number.
   */
  private short incSeq()
  {
    seq = (short)((seq+1)%MacMessage.Data.MAX_SEQ);
    return seq;
  }

  /**
   * Return latest seen sequence number from given address.
   *
   * @param from source address
   * @return latest sequence number from given address
   */
  private short getSeqEntry(MacAddress from)
  {
    SeqEntry curr = seqCache, prev = null;
    short seq = SEQ_INVALID;
    while(curr!=null)
    {
      if(from.equals(curr.from))
      {
        seq = curr.seq;
        if(prev!=null)
        {
          prev.next = curr.next;
          curr.next = seqCache;
          seqCache = curr;
        }
        break;
      }
      prev = curr;
      curr = curr.next;
    }
    return seq;
  }

  /**
   * Update latest sequence number entry for given address.
   *
   * @param from source address
   * @param seq latest sequence number
   */
  private void updateSeqEntry(MacAddress from, short seq)
  {
    SeqEntry curr = seqCache, prev = null;
    while(curr!=null)
    {
      if(seqCacheSize==SEQ_CACHE_SIZE && curr.next==null)
      {
        curr.from = from;
        curr.seq = seq;
        break;
      }
      if(from.equals(curr.from))
      {
        curr.seq = seq;
        if(prev!=null)
        {
          prev.next = curr.next;
          curr.next = seqCache;
          seqCache = curr;
        }
        break;
      }
      prev = curr;
      curr = curr.next;
    }
    if(curr==null)
    {
      if(Main.ASSERT) Util.assertion(seqCacheSize < SEQ_CACHE_SIZE);
      curr = new SeqEntry();
      curr.from = from;
      curr.seq = seq;
      curr.next = seqCache;
      seqCache = curr;
      seqCacheSize++;
    }
  }

  //////////////////////////////////////////////////
  // send-related functions
  //

  // MacInterface interface
  public void send(Message msg, MacAddress nextHop)
  {
    if(Main.ASSERT) Util.assertion(!hasPacket());
    if(Main.ASSERT) Util.assertion(nextHop!=null);
    packet = msg;
    packetNextHop = nextHop;
    if(mode==MAC_MODE_SIDLE)
    {
      if(!isCarrierIdle())
      {
        setBackoff();
      }
      doDifs();
    }
  }

  private void doDifs()
  {
    if(isRadioIdle())
    {
      if(waitingNav())
      {
        startTimer(nav-JistAPI.getTime(), MAC_MODE_SNAV);
      }
      else
      {
        startTimer(needEifs ? EIFS : DIFS, MAC_MODE_DIFS);
      }
    }
    else
    {
      idle();
    }
  }

  // MacInterface.Mac802_11 interface
  public void cfDone(boolean backoff, boolean delPacket)
  {
    if(backoff)
    {
      setBackoff();
    }
    doDifs();
    if(delPacket)
    {
      packet = null;
      packetNextHop = null;
      netEntity.pump(netId);
    }
  }

  private void sendPacket()
  {
    if(shouldRTS())
    {
      sendRts();
    }
    else
    {
      sendData(false);
    }
  }

  private void sendRts()
  {
    // create rts packet
    MacMessage.Rts rts = new MacMessage.Rts(packetNextHop, localAddr, (int)(
          ((MacMessage.Ack.SIZE
            +MacMessage.Cts.SIZE
            +MacMessage.Data.HEADER_SIZE) 
           * Constants.SECOND/bandwidth)
          + 4*PROPAGATION
          + 3*SIFS
          + 2*SYNCHRONIZATION
          + transmitTime(packet)));
    // set mode and transmit
    setMode(MAC_MODE_XRTS);
    long delay = RX_TX_TURNAROUND, duration = transmitTime(rts);
    radioEntity.transmit(rts, delay, duration);
    // wait for EOT, schedule CTS wait timer
    JistAPI.sleep(delay+duration);
    self.startTimer(MacMessage.Cts.SIZE*Constants.SECOND/bandwidth 
        + (SYNCHRONIZATION + PROPAGATION + SIFS)
        + SLOT_TIME
        + PROPAGATION
        + Constants.EPSILON_DELAY,
        MAC_MODE_SWFCTS);
  }

  private void sendCts(MacMessage.Rts rts)
  {
    // create cts packet
    MacMessage.Cts cts = new MacMessage.Cts(rts.getSrc(), (int)(
          rts.getDuration() 
          - MacMessage.Cts.SIZE*Constants.SECOND/bandwidth
          - SYNCHRONIZATION - PROPAGATION - SIFS));
    // set mode and transmit
    setMode(MAC_MODE_XCTS);
    long delay = SIFS, duration = transmitTime(cts);
    radioEntity.transmit(cts, delay, duration);
    // wait for EOT, schedule DATA wait timer
    JistAPI.sleep(delay+duration);
    self.startTimer(cts.getDuration()
        - MacMessage.Ack.SIZE*Constants.SECOND/bandwidth
        - (SYNCHRONIZATION + PROPAGATION + SIFS)
        + SLOT_TIME
        + Constants.EPSILON_DELAY,
        MAC_MODE_SWFDATA);
  }

  private void sendData(boolean afterCts)
  {
    if(isBroadcast())
    {
      sendDataBroadcast();
    }
    else
    {
      sendDataUnicast(afterCts);
    }
  }

  private void sendDataBroadcast()
  {
    // create data packet
    MacMessage.Data data = new MacMessage.Data(
        packetNextHop, localAddr, 
        0, packet);
    // set mode and transmit
    setMode(MAC_MODE_XBROADCAST);
    long delay = RX_TX_TURNAROUND, duration = transmitTime(data);
    radioEntity.transmit(data, delay, duration);
    // wait for EOT, check for outgoing packet
    JistAPI.sleep(delay+duration);
    self.cfDone(true, true);
  }

  private void sendDataUnicast(boolean afterCts)
  {
    // create data packet
    MacMessage.Data data = new MacMessage.Data(
        packetNextHop, localAddr, (int)(
          MacMessage.Ack.SIZE*Constants.SECOND/bandwidth
          + (SYNCHRONIZATION + PROPAGATION + SIFS)
          + PROPAGATION),
        incSeq(), (byte)0, false, 
        (shouldRTS() ? longRetry : shortRetry) > 0,
        packet);
    // set mode and transmit
    setMode(MAC_MODE_XUNICAST);
    long delay = afterCts ? SIFS : RX_TX_TURNAROUND, duration = transmitTime(data);
    radioEntity.transmit(data, delay, duration);
    // wait for EOT, and schedule ACK timer
    JistAPI.sleep(delay+duration);
    self.startTimer(MacMessage.Ack.SIZE*Constants.SECOND/bandwidth
        + (SYNCHRONIZATION + PROPAGATION + SIFS)
        + SLOT_TIME
        + PROPAGATION,
        MAC_MODE_SWFACK);
  }

  private void sendAck(MacMessage.Data data)
  {
    // create ack
    MacMessage.Ack ack = new MacMessage.Ack(data.getSrc(), 0);
    // set mode and transmit
    setMode(MAC_MODE_XACK);
    long delay = SIFS, duration = transmitTime(ack);
    radioEntity.transmit(ack, delay, duration);
    // wait for EOT, check for outgoing packet
    JistAPI.sleep(delay+duration);
    self.cfDone(false, false);
  }

  //////////////////////////////////////////////////
  // retry
  //

  private void retry()
  {
    // use long retry count for frames larger than RTS_THRESHOLD
    if(shouldRTS() && mode!=MAC_MODE_SWFCTS)
    {
      if(longRetry < RETRY_LIMIT_LONG)
      {
        longRetry++;
        retryYes();
      }
      else
      {
        longRetry = 0;
        retryNo();
      }
    }
    else
    {
      if(shortRetry < RETRY_LIMIT_SHORT)
      {
        shortRetry++;
        retryYes();
      }
      else
      {
        shortRetry = 0;
        retryNo();
      }
    }
  }

  private void retryYes()
  {
    incCW();
    setBackoff();
    doDifs();
  }

  private void retryNo()
  {
    // todo:
    // NetworkIpNotifyOfPacketDrop(packet, packetNextHop);
    decCW();
    cfDone(true, true);
  }


  //////////////////////////////////////////////////
  // receive-related functions
  //

  // MacInterface
  public void peek(Message msg)
  {
    needEifs = true;
    if (mode == MAC_MODE_SNAV_RTS) 
    {
      idle();
    }
  }

  // MacInterface
  public void receive(Message msg)
  {
    receivePacket((MacMessage)msg);
  }

  private void receivePacket(MacMessage msg)
  {
    needEifs = false;
    MacAddress dst = msg.getDst();
    if(localAddr.equals(dst))
    {
      switch(msg.getType())
      {
        case MacMessage.TYPE_RTS:
          receiveRts((MacMessage.Rts)msg);
          break;
        case MacMessage.TYPE_CTS:
          receiveCts((MacMessage.Cts)msg);
          break;
        case MacMessage.TYPE_ACK:
          receiveAck((MacMessage.Ack)msg);
          break;
        case MacMessage.TYPE_DATA:
          receiveData((MacMessage.Data)msg);
          break;
        default:
          throw new RuntimeException("illegal frame type");
      }
    }
    else if(MacAddress.ANY.equals(dst))
    {
      switch(msg.getType())
      {
        case MacMessage.TYPE_DATA:
          receiveData((MacMessage.Data)msg);
          break;
        default:
          throw new RuntimeException("illegal frame type");
      }
    }
    else
    {
      // does not belong to node
      receiveForeign(msg);
    }
  }

  private void receiveRts(MacMessage.Rts rts)
  {
    if(!isAwaitingResponse() && !waitingNav())
    {
      cancelTimer();
      sendCts(rts);
    } 
  }

  private void receiveCts(MacMessage.Cts cts)
  {
    if(mode==MAC_MODE_SWFCTS)
    {
      cancelTimer();
      // not in standard, but ns2 does
      // decCW();
      shortRetry = 0;
      sendData(true);
    }
  }

  private void receiveAck(MacMessage.Ack ack)
  {
    if(mode==MAC_MODE_SWFACK)
    {
      cancelTimer();
      shortRetry = 0;
      longRetry = 0;
      decCW();
      cfDone(true, true);
    }
  }

  private void receiveData(MacMessage.Data msg)
  {
    // not in standard, but ns-2 does.
    // decCW();
    // shortRetry = 0;
    if(mode==MAC_MODE_SWFDATA || !isAwaitingResponse())
    {
      if(MacAddress.ANY.equals(msg.getDst()))
      {
        netEntity.receive(msg.getBody(), msg.getSrc(), netId, false);
        cfDone(false, false);
      }
      else
      {
        cancelTimer();
        sendAck(msg);
        // duplicate suppression
        if(!(msg.getRetry() && getSeqEntry(msg.getSrc())==msg.getSeq()))
        {
          updateSeqEntry(msg.getSrc(), msg.getSeq());
          netEntity.receive(msg.getBody(), msg.getSrc(), netId, false);
        }
      }
    }
  }

  private void receiveForeign(MacMessage msg)
  {
    long currentTime = JistAPI.getTime();
    long nav2 = currentTime + msg.getDuration() + Constants.EPSILON_DELAY;
    if(nav2 > this.nav)
    {
      this.nav = nav2;
      if (isRadioIdle() && hasPacket())
      {
        // This is what we should do.
        //
        //if (msg.getType()==MacMessage.TYPE_RTS) 
        //{
        // If RTS-ing node failed to get a CTS and start sending then
        // reset the NAV (MAC layer virtual carrier sense) for this
        // bystander node.
        //   startTimer(PROPAGATION + SIFS + 
        //     SYNCHRONIZATION + MacMessage.Cts.SIZE*Constants.SECOND/bandwidth + 
        //     PROPAGATION + 2*SLOT_TIME, 
        //     MAC_MODE_SNAV_RTS);
        //} 
        //else 
        //{
        //   startTimer(NAV - currentTime, MAC_MODE_SNAV);
        //}

        // This is for ns-2 comparison.
        startTimer(nav - currentTime, MAC_MODE_SNAV);
      }
    }
    if (promisc && msg.getType()==MacMessage.TYPE_DATA)
    {
      MacMessage.Data macDataMsg = (MacMessage.Data)msg;
      netEntity.receive(macDataMsg.getBody(), macDataMsg.getSrc(), netId, true);
    }
  }

  //////////////////////////////////////////////////
  // radio mode
  //

  // MacInterface
  public void setRadioMode(byte mode)
  {
    this.radioMode = mode;
    switch(mode)
    {
      case Constants.RADIO_MODE_IDLE:
        radioIdle();
        break;
      case Constants.RADIO_MODE_SENSING:
        radioBusy();
        break;
      case Constants.RADIO_MODE_RECEIVING:
        radioBusy();
        /*
          todo:
          ExaminePotentialIncomingMessage(newMode, receiveDuration, potentialIncomingPacket);
        */
        break;
      case Constants.RADIO_MODE_SLEEP:
        radioBusy();
        break;
    }
  }

  private boolean isRadioIdle()
  {
    return radioMode==Constants.RADIO_MODE_IDLE;
  }

  private void radioBusy()
  {
    switch(mode)
    {
      case MAC_MODE_SBO:
        pauseBackoff();
        idle();
        break;
      case MAC_MODE_DIFS:
      case MAC_MODE_SNAV:
        idle();
        break;
      case MAC_MODE_SIDLE:
      case MAC_MODE_SWFCTS:
      case MAC_MODE_SWFDATA:
      case MAC_MODE_SWFACK:    
      case MAC_MODE_XBROADCAST:
      case MAC_MODE_XUNICAST:
      case MAC_MODE_XACK:
        // don't care
        break;
      default:
        // todo: really unexpected? check the states above as well
        throw new RuntimeException("unexpected mode: "+getModeString(mode));
    }
  }

  private void radioIdle()
  {
    switch(mode)
    {
      case MAC_MODE_SIDLE:
      case MAC_MODE_SNAV:
        doDifs();
        break;
      case MAC_MODE_SWFCTS:
      case MAC_MODE_SWFDATA:
      case MAC_MODE_SWFACK:
      case MAC_MODE_DIFS:
      case MAC_MODE_SBO:
      case MAC_MODE_XUNICAST:
      case MAC_MODE_XBROADCAST:
      case MAC_MODE_XACK:
        // don't care
        break;
      default:
        // todo: really unexpected? check the states above as well
        throw new RuntimeException("unexpected mode: "+getModeString(mode));
    }
  }

  //////////////////////////////////////////////////
  // timer routines
  //

  // MacInterface
  public void startTimer(long delay, byte mode)
  {
    cancelTimer();
    setMode(mode);
    JistAPI.sleep(delay);
    self.timeout(timerId);
  }

  /**
   * Cancel timer event, by incrementing the timer identifer.
   */
  private void cancelTimer()
  {
    timerId++;
  }

  private void idle()
  {
    cancelTimer();
    setMode(MAC_MODE_SIDLE);
  }

  // MacInterface
  public void timeout(int timerId)
  {
    if(timerId!=this.timerId) return;
    switch(mode)
    {
    	case MAC_MODE_SIDLE:
    	    idle();
    	    break;
      case MAC_MODE_DIFS:
        if(hasBackoff())
        {
          backoff();
        }
        else if(hasPacket())
        {
          sendPacket();
        }
        else
        {
          idle();
        }
        break;
      case MAC_MODE_SBO:
        if(Main.ASSERT) Util.assertion(boStart + bo == JistAPI.getTime());
        clearBackoff();
        if(hasPacket())
        {
          sendPacket();
        }
        else
        {
          idle();
        }
        break;
      case MAC_MODE_SNAV_RTS:
      case MAC_MODE_SNAV:
        resetNav();
        doDifs();
        break;
      case MAC_MODE_SWFDATA:
        setBackoff();
        doDifs();
        break;
      case MAC_MODE_SWFCTS:
      case MAC_MODE_SWFACK:
        retry();
        break;
      default:
        throw new RuntimeException("unexpected mode: "+mode);
    }
  }

}

