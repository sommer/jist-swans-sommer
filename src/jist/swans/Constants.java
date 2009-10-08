//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Constants.java Wed 2005/02/09 18:18:27 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2005 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans;

import java.util.Random;

/**
 * SWANS constants.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: Constants.java,v 1.62 2005-02-09 23:32:20 barr Exp $
 * @since SWANS1.0
 */
public final class Constants
{

  //////////////////////////////////////////////////
  // Randomness
  //

  /** Global random number generator. */
  public static Random random = new Random(0);

  //////////////////////////////////////////////////
  // Time
  //

  /** zero delay. */
  public static final long PROCESS_IMMEDIATELY = 0;
  /** smallest possible delay. */
  public static final int EPSILON_DELAY = 1;
  /** one nano-second in simulation time units. */
  public static final long NANO_SECOND  = 1;
  /** one micro-second in simulation time units. */
  public static final long MICRO_SECOND = 1000 * NANO_SECOND;
  /** one milli-second in simulation time units. */
  public static final long MILLI_SECOND = 1000 * MICRO_SECOND;
  /** one second in simulation time units. */
  public static final long SECOND       = 1000 * MILLI_SECOND;
  /** one minute in simulation time units. */
  public static final long MINUTE       = 60 * SECOND;
  /** one hour in simulation time units. */
  public static final long HOUR         = 60 * MINUTE;
  /** one day in simulation time units. */
  public static final long DAY          = 24 * HOUR;

  //////////////////////////////////////////////////
  // Nature
  //

  /** Boltzmann's constant (units: Joules/Kelvin). */
  public static final double BOLTZMANN = 1.3807e-23;
  /** Speed of light in a vacuum (units: meter/second). */
  public static final double SPEED_OF_LIGHT = 2.9979e8;
  /** Pre-computed natural logarithm of 10. */
  public static final double log10 = Math.log(10);

  //////////////////////////////////////////////////
  // Field-related constants
  //

  // constants

  /** Default field boundary (units: sim distance, usually meters). */
  public static final float FIELD_BOUND_X = (float)200.0, FIELD_BOUND_Y = (float)200.0;

  /** node placement choice constant. */
  public static final int PLACEMENT_INVALID = -1;
  /** node placement choice constant. */
  public static final int PLACEMENT_RANDOM  = 1;
  /** node placement choice constant. */
  public static final int PLACEMENT_GRID    = 2;
  /** node placement choice constant. */
  public static final int PLACEMENT_MAX     = 2;
  /** node placement choice constant. */
  public static final int PLACEMENT_DEFAULT = PLACEMENT_RANDOM;

  /** node mobility choice constant. */
  public static final int MOBILITY_INVALID  = -1;
  /** node mobility choice constant. */
  public static final int MOBILITY_STATIC   = 1;
  /** node mobility choice constant. */
  public static final int MOBILITY_WAYPOINT = 2;
  /** node mobility choice constant. */
  public static final int MOBILITY_TELEPORT = 3;
  /** node mobility choice constant. */
  public static final int MOBILITY_WALK     = 4;
  /** node mobility choice constant. */
  public static final int MOBILITY_DEFAULT  = MOBILITY_STATIC;

  /** spatial data structure choice constant. */
  public static final int SPATIAL_INVALID = -1;
  /** spatial data structure choice constant. */
  public static final int SPATIAL_LINEAR  = 0;
  /** spatial data structure choice constant. */
  public static final int SPATIAL_GRID    = 1;
  /** spatial data structure choice constant. */
  public static final int SPATIAL_HIER    = 2;
  /** spatial data structure choice constant. */
  public static final int SPATIAL_WRAP    = 16;

  //////////////////////////////////////////////////
  // packet constants
  //

  /** packet with zero wire size. */
  public static final int ZERO_WIRE_SIZE = Integer.MIN_VALUE;

  //////////////////////////////////////////////////
  // Radio-related constants
  //

  // radio modes

  /** Radio mode: sleeping. */
  public static final byte RADIO_MODE_SLEEP        = -1;
  /** Radio mode: idle, no signals. */
  public static final byte RADIO_MODE_IDLE         = 0;
  /** Radio mode: some signals above sensitivity. */
  public static final byte RADIO_MODE_SENSING      = 1;
  /** Radio mode: signal locked and receiving packet. */
  public static final byte RADIO_MODE_RECEIVING    = 2;
  /** Radio mode: transmitting packet. */
  public static final byte RADIO_MODE_TRANSMITTING = 3;

  // timing constants

  /** RX-TX switching delay. */
  public static final long RADIO_TURNAROUND_TIME = 5 * MICRO_SECOND;
  /** physical layer delay. */
  public static final long RADIO_PHY_DELAY = RADIO_TURNAROUND_TIME;
  /** Constant used to specify the default "delay to the wire". */
  public static final int RADIO_NOUSER_DELAY = -1;

  // defaults

  /** Default radio frequency (units: Hz). */
  public static final double FREQUENCY_DEFAULT = 2.4e9; // 2.4 GHz
  /** Default radio bandwidth (units: bits/second). */
  public static final int BANDWIDTH_DEFAULT = (int)1e6; // 1Mb/s
  /** Default transmission strength (units: dBm). */
  public static final double TRANSMIT_DEFAULT = 15.0;
  /** Default antenna gain (units: dB). */
  public static final double GAIN_DEFAULT = 0.0;
  /** Default radio reception sensitivity (units: dBm). */
  public static final double SENSITIVITY_DEFAULT = -91.0;
  /** Default radio reception threshold (units: dBm). */
  public static final double THRESHOLD_DEFAULT = -81.0;
  /** Default temperature (units: degrees Kelvin). */
  public static final double TEMPERATURE_DEFAULT = 290.0;
  /** Default temperature noise factor. */
  public static final double TEMPERATURE_FACTOR_DEFAULT = 10.0;
  /** Default ambient noise (units: mW). */
  public static final double AMBIENT_NOISE_DEFAULT = 0.0;
  /** Default minimum propagated signal (units: dBm). */
  //public static final double PROPAGATION_LIMIT_DEFAULT = -111.0;
  public static final double PROPAGATION_LIMIT_DEFAULT = SENSITIVITY_DEFAULT; 
  /** Default radio height (units: sim distance units, usually meters). */
  public static final double HEIGHT_DEFAULT = 1.5;
  /** Default threshold signal-to-noise ratio. */
  public static final double SNR_THRESHOLD_DEFAULT = 10.0;


  //////////////////////////////////////////////////
  // Mac-related constants
  //

  // defaults

  /** Default mac promiscuous mode. */
  public static final boolean MAC_PROMISCUOUS_DEFAULT = false;
  /** link layer delay. */
  public static final long LINK_DELAY = MICRO_SECOND;

  //////////////////////////////////////////////////
  // Network-related constants
  //

  /** network layer loss model choice constant. */
  public static final int NET_LOSS_INVALID       = -1;
  /** network layer loss model choice constant. */
  public static final int NET_LOSS_NONE          = 0;
  /** network layer loss model choice constant. */
  public static final int NET_LOSS_UNIFORM       = 1;
  /** network layer loss model choice constant. */
  public static final int NET_LOSS_DEFAULT       = NET_LOSS_NONE;

  /** network packet priority level. */
  public static final byte NET_PRIORITY_CONTROL  = 0;
  /** network packet priority level. */
  public static final byte NET_PRIORITY_REALTIME = 1;
  /** network packet priority level. */
  public static final byte NET_PRIORITY_NORMAL   = 2;
  /** network packet priority level. */
  public static final byte NET_PRIORITY_NUM      = 3;
  /** network packet priority level. */
  public static final byte NET_PRIORITY_INVALID  = -1;


  /** network interface constant. */
  public static final int NET_INTERFACE_INVALID = -1;
  /** network interface constant. */
  public static final int NET_INTERFACE_LOOPBACK = 0;
  /** network interface constant. */
  public static final int NET_INTERFACE_DEFAULT = 1;

  /** network layer delay. */
  public static final long NET_DELAY = MICRO_SECOND;
  /** default time-to-live. */
  public static final byte TTL_DEFAULT = 64;

  /** network level (IP) protocol number. */
  public static final short NET_PROTOCOL_INVALID        = -1;
  /** network level (IP) protocol number. */
  public static final short NET_PROTOCOL_TCP            = 6;
  /** network level (IP) protocol number. */
  public static final short NET_PROTOCOL_UDP            = 17;
  /** network level (IP) protocol number. */
  public static final short NET_PROTOCOL_OSPF           = 87;
  /** network level (IP) protocol number. */
  public static final short NET_PROTOCOL_BELLMANFORD    = 520;
  /** network level (IP) protocol number. */
  public static final short NET_PROTOCOL_FISHEYE        = 530;
  /** network level (IP) protocol number. */
  public static final short NET_PROTOCOL_AODV           = 123;
  /** network level (IP) protocol number. */
  public static final short NET_PROTOCOL_DSR            = 135;
  /** network level (IP) protocol number. */
  public static final short NET_PROTOCOL_ODMRP          = 145;
  /** network level (IP) protocol number. */
  public static final short NET_PROTOCOL_LAR1           = 110;
  /** network level (IP) protocol number. */
  public static final short NET_PROTOCOL_ZRP            = 133;
  /** zrp-subprotocol number. */
  public static final short NET_PROTOCOL_ZRP_NDP_DEFAULT  = 1;
  /** zrp-subprotocol number. */
  public static final short NET_PROTOCOL_ZRP_IARP_DEFAULT = 2;
  /** zrp-subprotocol number. */
  public static final short NET_PROTOCOL_ZRP_BRP_DEFAULT  = 3;
  /** zrp-subprotocol number. */
  public static final short NET_PROTOCOL_ZRP_IERP_DEFAULT = 4;
  /** zrp-subprotocol number. */
  public static final short NET_PROTOCOL_ZRP_IARP_ZDP     = 5;
  /** zrp-subprotocol number. */
  public static final short NET_PROTOCOL_ZRP_BRP_FLOOD    = 6;

  /** network level (IP) protocol number. */
  public static final short NET_PROTOCOL_NO_NEXT_HEADER = 59;
  /** network level (IP) protocol number. */
  public static final short NET_PROTOCOL_HEARTBEAT      = 500;
  /** network level (IP) protocol number. */
  public static final short NET_PROTOCOL_MAX            = 999;

  //////////////////////////////////////////////////
  // Routing-related constants
  //

  //////////////////////////////////////////////////
  // Transport-related constants
  //

  /** transport layer delay. */
  public static final long TRANS_DELAY = MICRO_SECOND;
  /** socket delay. */
  public static final long TRANS_PROCESSING_DELAY = MICRO_SECOND;

  /** transport level (tcp/udp) protocol number. */
  public static final short TRANS_PROTOCOL_INVALID = -1;
  /** transport level (tcp/udp) protocol number. */
  public static final short TCP_PROTOCOL_ECHO      =  7;
  /** transport level (tcp/udp) protocol number. */
  public static final short TCP_PROTOCOL_FTP       = 21;
  /** transport level (tcp/udp) protocol number. */
  public static final short TCP_PROTOCOL_TELNET    = 23;
  /** transport level (tcp/udp) protocol number. */
  public static final short TCP_PROTOCOL_SMTP      = 25;
  /** transport level (tcp/udp) protocol number. */
  public static final short TCP_PROTOCOL_TIME      = 37;
  /** transport level (tcp/udp) protocol number. */
  public static final short TCP_PROTOCOL_HTTP      = 80;

  /** TCP States. */
  public final class TCPSTATES
  {
    /**
     * TCP state: LISTEN - represents waiting for a connection request from
     * any remote TCP and port.
     */
    public static final int LISTEN = 800;
    /**
     * TCP state: SYN-SENT - represents waiting for a matching connection
     * request after having sent a connection request.
     */
    public static final int SYN_SENT = 801;
    /**
     * TCP state: SYN-RECEIVED - represents waiting for a confirming
     * connection request acknowledgment after having both received and sent a
     * connection request.
     */
    public static final int SYN_RECEIVED = 802;
    /**
     * TCP state: ESTABLISHED - represents an open connection, data received
     * can be delivered to the user.  The normal state for the data transfer
     * phase of the connection.
     */
    public static final int ESTABLISHED = 803;
    /**
     * TCP state: FIN-WAIT-1 - represents waiting for a connection
     * termination request from the remote TCP, or an acknowledgment of the
     * connection termination request previously sent.
     */
    public static final int FIN_WAIT_1 = 804;
    /**
     * TCP state: FIN-WAIT-2 - represents waiting for a connection
     * termination request from the remote TCP.
     */
    public static final int FIN_WAIT_2 = 805;
    /**
     * TCP state: CLOSE-WAIT - represents waiting for a connection termination
     * request from the local user.
     */
    public static final int CLOSE_WAIT = 806;
    /**
     * TCP state: CLOSING - represents waiting for a connection termination
     * request acknowledgment from the remote TCP.
     */
    public static final int CLOSING = 807;
    /**
     * TCP state: LAST-ACK - represents waiting for an acknowledgment of the
     * connection termination request previously sent to the remote TCP (which
     * includes an acknowledgment of its connection termination request).
     */
    public static final int LAST_ACK = 808;
    /**
     * TCP state: TIME-WAIT - represents waiting for enough time to pass to be
     * sure the remote TCP received the acknowledgment of its connection
     * termination request.
     */
    public static final int TIME_WAIT = 809;
    /**
     * TCP state: CLOSED - represents no connection state at all.
     */
    public static final int CLOSED = 810;

  } // class: TCPSTATES

} // class: Constants

