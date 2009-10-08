//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <ip.java Sun 2005/03/13 11:02:31 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package driver;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.Field;
import jist.swans.field.Spatial;
import jist.swans.field.PathLoss;
import jist.swans.field.Fading;
import jist.swans.field.Placement;
import jist.swans.mac.MacInterface;
import jist.swans.mac.MacDumb;
import jist.swans.mac.MacAddress;
import jist.swans.radio.RadioNoise;
import jist.swans.radio.RadioNoiseIndep;
import jist.swans.radio.RadioNoiseAdditive;
import jist.swans.radio.RadioInfo;
import jist.swans.net.NetInterface;
import jist.swans.net.NetIp;
import jist.swans.net.NetMessage;
import jist.swans.net.NetAddress;
import jist.swans.net.PacketLoss;
import jist.swans.misc.Util;
import jist.swans.misc.Mapper;
import jist.swans.misc.Message;
import jist.swans.misc.Location;
import jist.swans.misc.MessageBytes;

import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;

import jist.runtime.JistAPI;

import jargs.gnu.*;

/**
 * Small scenario that tests stuff below the IP layer on the network stack.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: ip.java,v 1.4 2005-03-13 16:11:53 barr Exp $
 * @since JIST1.0
 */
public class ip
{

  public static final int RADIO_TYPE_INDEPENDENT = 0;
  public static final int RADIO_TYPE_ADDITIVE    = 1;
  public static final String RADIO_STRING_INDEPENDENT = "indep";
  public static final String RADIO_STRING_ADDITIVE    = "additive";

  public static final short MY_PROTOCOL = Constants.NET_PROTOCOL_HEARTBEAT;

  /** Simulation parameters with default values. */
  private static class CommandLineOptions
  {
    /** Whether to print a usage statement. */
    private boolean help = false;
    /** Distance between two nodes. */
    private int distance = 50;
    /** Number of packets to send. */
    private int packets = 1;
    /** Time to sleep between transmissions (ms). */
    private long sleep = 100 * Constants.MILLI_SECOND;
    /** Radio interference model to use. */
    private int radio = RADIO_TYPE_INDEPENDENT;
  } // class: CommandLineOptions

  /** Prints a usage statement. */
  private static void showUsage() 
  {
    System.out.println("Usage: swans driver.ip [options]");
    System.out.println();
    System.out.println("  -h, --help           print this message");
    System.out.println("  -d, --distance       distance between nodes [50m]");
    System.out.println("  -p, --packets        number of packets to transmit [1]");
    System.out.println("  -s, --sleep          delay between packets [100ms]");
    System.out.println("  -r, --radio          interference model: [indep]|additive");
    System.out.println();
    System.out.println("e.g.");
    System.out.println("  swans driver.ip ");
    System.out.println();
  }

  /**
   * Parses command-line arguments.
   *
   * @param args command-line arguments
   * @return parsed command-line options
   * @throws CmdLineParser.OptionException if the command-line arguments are not well-formed.
   */
  private static CommandLineOptions parseCommandLineOptions(String[] args)
    throws CmdLineParser.OptionException
  {
    CmdLineParser parser = new CmdLineParser();
    CmdLineParser.Option opt_help = parser.addBooleanOption('h', "help");
    CmdLineParser.Option opt_distance = parser.addIntegerOption('d', "distance");
    CmdLineParser.Option opt_packets = parser.addIntegerOption('p', "packets");
    CmdLineParser.Option opt_sleep = parser.addIntegerOption('s', "sleep");
    CmdLineParser.Option opt_radio = parser.addStringOption('r', "radio");
    parser.parse(args);

    CommandLineOptions cmdOpts = new CommandLineOptions();
    // help
    if(parser.getOptionValue(opt_help) != null)
    {
      cmdOpts.help = true;
    }
    if(parser.getOptionValue(opt_distance) != null)
    {
      cmdOpts.distance = ((Integer)parser.getOptionValue(opt_distance)).intValue();
    }
    if(parser.getOptionValue(opt_packets) != null)
    {
      cmdOpts.packets = ((Integer)parser.getOptionValue(opt_packets)).intValue();
    }
    if(parser.getOptionValue(opt_sleep) != null)
    {
      cmdOpts.sleep = ((Integer)parser.getOptionValue(opt_sleep)).intValue() * Constants.MILLI_SECOND;
    }
    if(parser.getOptionValue(opt_radio) != null)
    {
      String radioTypeString = (String)parser.getOptionValue(opt_radio);
      if(radioTypeString.equalsIgnoreCase(RADIO_STRING_INDEPENDENT ))
      {
        cmdOpts.radio = RADIO_TYPE_INDEPENDENT;
      }
      else if(radioTypeString.equalsIgnoreCase(RADIO_STRING_ADDITIVE))
      {
        cmdOpts.radio = RADIO_TYPE_ADDITIVE;
      }
      else
      {
        throw new CmdLineParser.IllegalOptionValueException(opt_radio, "Unrecognized radio inteference model");
      }
    }
    return cmdOpts;
  } // parseCommandLineOptions

  public static class MyIpHandler implements NetInterface.NetHandler
  {
    public void receive(Message msg, NetAddress src, MacAddress lastHop,
        byte macId, NetAddress dst, byte priority, byte ttl)
    {
      System.out.println("t="+JistAPI.getTime()+": received packet "+msg+" from "+src+"("+lastHop+") on nic "+macId);
    }
  }

  public static NetInterface createNode(Field field, 
      int i, Location location, RadioNoise radio,
      NetAddress addr, NetInterface.NetHandler nethandler)
  {
    // create entities
    MacDumb mac = new MacDumb(new MacAddress(i), radio.getRadioInfo());
    Mapper protMap = new Mapper(Constants.NET_PROTOCOL_MAX);
    protMap.mapToNext(MY_PROTOCOL);
    PacketLoss pl = new PacketLoss.Zero();
    NetIp net = new NetIp(addr, protMap, pl, pl);
    // hookup
    field.addRadio(radio.getRadioInfo(), radio.getProxy(), location);
    radio.setFieldEntity(field.getProxy());
    radio.setMacEntity(mac.getProxy());
    mac.setRadioEntity(radio.getProxy());
    byte nicid = net.addInterface(mac.getProxy());
    mac.setNetEntity(net.getProxy(), nicid);
    net.setProtocolHandler(MY_PROTOCOL, nethandler);
    return net.getProxy();
  }

  public static RadioNoise createRadio(int type, int i, RadioInfo.RadioInfoShared radioInfoShared)
  {
    switch(type)
    {
      case RADIO_TYPE_INDEPENDENT:
        return new RadioNoiseIndep(i, radioInfoShared);
      case RADIO_TYPE_ADDITIVE:
        return new RadioNoiseAdditive(i, radioInfoShared);
      default:
        throw new RuntimeException("unknown radio interference model");
    }
  }

  /**
   * Program entry point: small IP-layer-and-below two-node test.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    try
    {
      CommandLineOptions cmdopts = parseCommandLineOptions(args);
      if(cmdopts.help)
      {
        showUsage();
        return;
      }
      // create field
      Location.Location2D bounds = new Location.Location2D(cmdopts.distance+1, cmdopts.distance+1);
      Spatial spatial = new Spatial.LinearList(bounds);
      Fading fading = new Fading.None();
      PathLoss pathloss = new PathLoss.FreeSpace();
      Field field = new Field(spatial, fading, pathloss, null, Constants.PROPAGATION_LIMIT_DEFAULT);
      // shared radio information
      RadioInfo.RadioInfoShared radioInfoShared = RadioInfo.createShared(
          Constants.FREQUENCY_DEFAULT, Constants.BANDWIDTH_DEFAULT,
          Constants.TRANSMIT_DEFAULT, Constants.GAIN_DEFAULT,
          Util.fromDB(Constants.SENSITIVITY_DEFAULT), Util.fromDB(Constants.THRESHOLD_DEFAULT),
          Constants.TEMPERATURE_DEFAULT, Constants.TEMPERATURE_FACTOR_DEFAULT, Constants.AMBIENT_NOISE_DEFAULT);

      // create nodes
      NetInterface n1 = createNode(field, 1, 
          new Location.Location2D(0,0), 
          createRadio(cmdopts.radio, 1, radioInfoShared),
          new NetAddress(1), new MyIpHandler());
      NetInterface n2 = createNode(field, 2, 
          new Location.Location2D(cmdopts.distance,0), 
          createRadio(cmdopts.radio, 2, radioInfoShared),
          new NetAddress(2), new MyIpHandler());
      // generate traffic
      Message msg = new MessageBytes("hi");
      for(int i=0; i<cmdopts.packets; i++)
      {
        n1.send(msg, NetAddress.ANY, MY_PROTOCOL, 
            Constants.NET_PRIORITY_NORMAL, (byte)2);
        JistAPI.sleep(cmdopts.sleep);
      }
    }
    catch(Exception e) 
    { 
      e.printStackTrace(); 
    }
  }

} // class: ip

// todo: command-line parameter distance between two nodes
// todo: command-line parameter radio noise model: indep / additive
