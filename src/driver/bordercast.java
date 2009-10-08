//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <bordercast.java Tue 2004/04/06 11:57:42 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package driver;

import jist.swans.field.Field;
import jist.swans.field.Mobility;
import jist.swans.field.Placement;
import jist.swans.field.Fading;
import jist.swans.field.Spatial;
import jist.swans.field.PathLoss;
import jist.swans.radio.RadioNoise;
import jist.swans.radio.RadioNoiseIndep;
import jist.swans.radio.RadioInfo;
import jist.swans.mac.MacAddress;
import jist.swans.mac.MacDumb;
import jist.swans.net.NetAddress;
import jist.swans.net.NetMessage;
import jist.swans.net.NetIp;
import jist.swans.net.PacketLoss;
import jist.swans.trans.TransUdp;
import jist.swans.route.RouteInterface;
import jist.swans.route.RouteZrp;
import jist.swans.route.RouteZrpNdp;
import jist.swans.route.RouteZrpIarp;
import jist.swans.route.RouteZrpBrp;
import jist.swans.route.RouteZrpBrpFlood;
import jist.swans.route.RouteZrpIerp;
import jist.swans.route.RouteZrpZdp;
import jist.swans.misc.Util;
import jist.swans.misc.Mapper;
import jist.swans.misc.Location;
import jist.swans.misc.Message;
import jist.swans.Constants;

import jist.runtime.JistAPI;

import jargs.gnu.*;

import java.lang.reflect.*;
import java.util.*;

/**
 * Bordercast evaluation. Derived from CBR.java
 *
 * @author Rimon Barr
 * @version $Id: bordercast.java,v 1.24 2004-11-08 19:59:38 barr Exp $
 */
public class bordercast
{
  /** Default port number to send and receive packets. */
  private static final int PORT = 3001;

  //////////////////////////////////////////////////
  // command-line options
  //

  /** Simulation parameters with default values. */
  private static class CommandLineOptions
  {
    /** Whether to print a usage statement. */
    private boolean help = false;
    /** Time to end simulation. */
    private int endTime = -1;
    /** Routing protocol to use. */
    private int protocol = Constants.NET_PROTOCOL_ZRP;
    /** Routing protocol options. */
    private String protocolOpts = "3";
    /** ZRP sub-protocol to use. */
    private int ndp = Constants.NET_PROTOCOL_ZRP_NDP_DEFAULT;
    /** ZRP sub-protocol options. */
    private String ndpOpts = null;
    /** ZRP sub-protocol to use. */
    private int iarp = Constants.NET_PROTOCOL_ZRP_IARP_DEFAULT;
    /** ZRP sub-protocol options. */
    private String iarpOpts = "none";
    /** ZRP sub-protocol to use. */
    private int brp = Constants.NET_PROTOCOL_ZRP_BRP_DEFAULT;
    /** ZRP sub-protocol options. */
    private String brpOpts = null;
    /** ZRP sub-protocol to use. */
    private int ierp = Constants.NET_PROTOCOL_ZRP_IERP_DEFAULT;
    /** ZRP sub-protocol options. */
    private String ierpOpts = null;
    /** Number of nodes. */
    private int nodes = 100;
    /** Field dimensions (in meters). */
    private Location.Location2D field = new Location.Location2D(1000, 1000);
    /** Field wrap-around. */
    private boolean wrapField = false;
    /** Node placement model. */
    private int placement = Constants.PLACEMENT_RANDOM;
    /** Node placement options. */
    private String placementOpts = null;
    /** Node mobility model. */
    private int mobility = Constants.MOBILITY_STATIC;
    /** Node mobility options. */
    private String mobilityOpts = null;
    /** Packet loss model. */
    private int loss = Constants.NET_LOSS_NONE;
    /** Packet loss options. */
    private String lossOpts = "";
    /** Number of bordercasts. */
    private int bordercasts;
    /** Delay between bordercasts (seconds). */
    private int bordercastDelay = 10;
    /** Start of bordercasts (seconds). */
    private int bordercastStart = 60;
    /** Random seed. */
    private int seed = 0;
    /** binning mode. */
    public int spatial_mode = Constants.SPATIAL_HIER;
    /** binning degree. */
    public int spatial_div = 5;
  } // class: CommandLineOptions

  /** Prints a usage statement. */
  private static void showUsage() 
  {
    System.out.println("Usage: swans driver.bordercast [options]");
    System.out.println();
    System.out.println("  -h, --help           print this message");
    System.out.println("  -e, --endat          simulation ending time: [infinite]");
    System.out.println("  -p, --protocol       routing protocol: [zrp:radius]");
    System.out.println("  -1, --ndp            ndp config");
    System.out.println("  -2, --iarp           iarp config");
    System.out.println("  -3, --brp            brp config");
    System.out.println("  -4, --ierp           ierp config");
    System.out.println("  -n, --nodes          number of nodes: n [100] ");
    System.out.println("  -f, --field          field dimensions: x,y [100,100]");
    System.out.println("  -a, --arrange        placement model: [random],grid:ixj");
    System.out.println("  -m, --mobility       mobility: [static],waypoint:opts,teleport:p,walk:opts");
    System.out.println("  -l, --loss           packet loss model: [none],uniform:p");
    System.out.println("  -b, --bordercasts    number of transmissions: num,start,delay [0,60,10]");
    System.out.println("  -r, --randomseed     random seed: [0]");
    System.out.println("  -w, --wrap           wrap-around field");
    System.out.println("  -s, --spatial        spatial binning: linear, grid:NxN, hier:N");
    System.out.println();
    System.out.println("e.g.");
    System.out.println("  swans driver.bordercast -p zrp:2 --iarp=iarp:inf -e 300 -n 10 -f 200x200 -a grid:5x5 -b 5,100,20");
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
    if(args.length==0)
    {
      args = new String[] { "-h" };
    }
    CmdLineParser parser = new CmdLineParser();
    CmdLineParser.Option opt_help = parser.addBooleanOption('h', "help");
    CmdLineParser.Option opt_endat = parser.addIntegerOption('e', "endat");
    CmdLineParser.Option opt_protocol = parser.addStringOption('p', "protocol");
    CmdLineParser.Option opt_ndp = parser.addStringOption('1', "ndp");
    CmdLineParser.Option opt_iarp = parser.addStringOption('2', "iarp");
    CmdLineParser.Option opt_brp = parser.addStringOption('3', "brp");
    CmdLineParser.Option opt_ierp = parser.addStringOption('4', "ierp");
    CmdLineParser.Option opt_nodes = parser.addIntegerOption('n', "nodes");
    CmdLineParser.Option opt_field = parser.addStringOption('f', "field");
    CmdLineParser.Option opt_placement = parser.addStringOption('a', "arrange");
    CmdLineParser.Option opt_mobility = parser.addStringOption('m', "mobility");
    CmdLineParser.Option opt_loss = parser.addStringOption('l', "loss");
    CmdLineParser.Option opt_bordercast = parser.addStringOption('b', "bordercasts");
    CmdLineParser.Option opt_randseed = parser.addIntegerOption('r', "randomseed");
    CmdLineParser.Option opt_wrap = parser.addBooleanOption('w', "wrap");
    CmdLineParser.Option opt_spatial = parser.addStringOption('s', "spatial");
    parser.parse(args);

    CommandLineOptions cmdOpts = new CommandLineOptions();
    // help
    if(parser.getOptionValue(opt_help) != null)
    {
      cmdOpts.help = true;
    }
    // endat
    if (parser.getOptionValue(opt_endat) != null)
    {
      cmdOpts.endTime = ((Integer)parser.getOptionValue(opt_endat)).intValue();
    }
    // protocol
    if(parser.getOptionValue(opt_protocol)!=null)
    {
      String routeProtocolString = ((String)parser.getOptionValue(opt_protocol)).split(":")[0];
      if(routeProtocolString!=null)
      {
        if(routeProtocolString.equalsIgnoreCase("zrp"))
        {
          cmdOpts.protocol = Constants.NET_PROTOCOL_ZRP;
        }
        else
        {
          throw new CmdLineParser.IllegalOptionValueException(opt_protocol, "Unrecognized routing protocol");
        }
      }
      cmdOpts.protocolOpts = Util.stringJoin((String[])Util.rest(((String)parser.getOptionValue(opt_protocol)).split(":")), ":");
    }
    // ndp
    if(parser.getOptionValue(opt_ndp)!=null)
    {
      String protString = ((String)parser.getOptionValue(opt_ndp)).split(":")[0];
      if(protString!=null)
      {
        if(protString.equalsIgnoreCase("ndp"))
        {
          cmdOpts.ndp = Constants.NET_PROTOCOL_ZRP_NDP_DEFAULT;
        }
        else
        {
          throw new CmdLineParser.IllegalOptionValueException(opt_ndp, "Unrecognized routing protocol");
        }
      }
      cmdOpts.ndpOpts = Util.stringJoin((String[])Util.rest(((String)parser.getOptionValue(opt_ndp)).split(":")), ":");
    }
    // iarp
    if(parser.getOptionValue(opt_iarp)!=null)
    {
      String protString = ((String)parser.getOptionValue(opt_iarp)).split(":")[0];
      if(protString!=null)
      {
        if(protString.equalsIgnoreCase("iarp"))
        {
          cmdOpts.iarp = Constants.NET_PROTOCOL_ZRP_IARP_DEFAULT;
        }
        else if(protString.equalsIgnoreCase("zdp"))
        {
          cmdOpts.iarp = Constants.NET_PROTOCOL_ZRP_IARP_ZDP;
        }
        else
        {
          throw new CmdLineParser.IllegalOptionValueException(opt_iarp, "Unrecognized routing protocol");
        }
      }
      cmdOpts.iarpOpts = Util.stringJoin((String[])Util.rest(((String)parser.getOptionValue(opt_iarp)).split(":")), ":");
    }
    // brp
    if(parser.getOptionValue(opt_brp)!=null)
    {
      String protString = ((String)parser.getOptionValue(opt_brp)).split(":")[0];
      if(protString!=null)
      {
        if(protString.equalsIgnoreCase("brp"))
        {
          cmdOpts.brp = Constants.NET_PROTOCOL_ZRP_BRP_DEFAULT;
        }
        else if(protString.equalsIgnoreCase("flood"))
        {
          cmdOpts.brp = Constants.NET_PROTOCOL_ZRP_BRP_FLOOD;
        }
        else
        {
          throw new CmdLineParser.IllegalOptionValueException(opt_brp, "Unrecognized routing protocol");
        }
      }
      cmdOpts.brpOpts = Util.stringJoin((String[])Util.rest(((String)parser.getOptionValue(opt_brp)).split(":")), ":");
    }
    // ierp
    if(parser.getOptionValue(opt_ierp)!=null)
    {
      String protString = ((String)parser.getOptionValue(opt_ierp)).split(":")[0];
      if(protString!=null)
      {
        if(protString.equalsIgnoreCase("ierp"))
        {
          cmdOpts.ierp = Constants.NET_PROTOCOL_ZRP_IERP_DEFAULT;
        }
        else
        {
          throw new CmdLineParser.IllegalOptionValueException(opt_ierp, "Unrecognized routing protocol");
        }
      }
      cmdOpts.ierpOpts = Util.stringJoin((String[])Util.rest(((String)parser.getOptionValue(opt_ierp)).split(":")), ":");
    }
    // nodes
    if(parser.getOptionValue(opt_nodes) != null)
    {
      cmdOpts.nodes = ((Integer)parser.getOptionValue(opt_nodes)).intValue();
    }
    // field
    if(parser.getOptionValue(opt_field) != null)
    {
      cmdOpts.field = (Location.Location2D)Location.parse((String)parser.getOptionValue(opt_field));
    }
    // placement
    if(parser.getOptionValue(opt_placement) != null)
    {
      String placementString = ((String)parser.getOptionValue(opt_placement)).split(":")[0];
      if(placementString!=null)
      {
        if(placementString.equalsIgnoreCase("random"))
        {
          cmdOpts.placement = Constants.PLACEMENT_RANDOM;
        }
        else if(placementString.equalsIgnoreCase("grid"))
        {
          cmdOpts.placement = Constants.PLACEMENT_GRID;
        }
        else
        {
          throw new CmdLineParser.IllegalOptionValueException(opt_placement, "unrecognized placement model");
        }
      }
      cmdOpts.placementOpts = Util.stringJoin((String[])Util.rest(((String)parser.getOptionValue(opt_placement)).split(":")), ":");
    }
    // mobility
    if(parser.getOptionValue(opt_mobility)!=null)
    {
      String mobilityString = ((String)parser.getOptionValue(opt_mobility)).split(":")[0];
      if(mobilityString!=null)
      {
        if(mobilityString.equalsIgnoreCase("static"))
        {
          cmdOpts.mobility = Constants.MOBILITY_STATIC;
        }
        else if(mobilityString.equalsIgnoreCase("waypoint"))
        {
          cmdOpts.mobility = Constants.MOBILITY_WAYPOINT;
        }
        else if(mobilityString.equalsIgnoreCase("teleport"))
        {
          cmdOpts.mobility = Constants.MOBILITY_TELEPORT;
        }
        else if(mobilityString.equalsIgnoreCase("walk"))
        {
          cmdOpts.mobility = Constants.MOBILITY_WALK;
        }
        else
        {
          throw new CmdLineParser.IllegalOptionValueException(opt_mobility, "unrecognized mobility model");
        }
      }
      cmdOpts.mobilityOpts = Util.stringJoin((String[])Util.rest(((String)parser.getOptionValue(opt_mobility)).split(":")), ":");
    }
    // loss
    if(parser.getOptionValue(opt_loss)!=null)
    {
      String lossString = ((String)parser.getOptionValue(opt_loss)).split(":")[0];
      if(lossString!=null)
      {
        if(lossString.equalsIgnoreCase("none"))
        {
          cmdOpts.loss = Constants.NET_LOSS_NONE;
        }
        else if(lossString.equalsIgnoreCase("uniform"))
        {
          cmdOpts.loss = Constants.NET_LOSS_UNIFORM;
        }
        else
        {
          throw new CmdLineParser.IllegalOptionValueException(opt_loss, "unrecognized mobility model");
        }
      }
      cmdOpts.lossOpts = Util.stringJoin((String[])Util.rest(((String)parser.getOptionValue(opt_loss)).split(":")), ":");
    }
    // bordercasts
    if(parser.getOptionValue(opt_bordercast) != null)
    {
      String[] data = ((String)parser.getOptionValue(opt_bordercast)).split(",");
      if(data.length!=3) throw new CmdLineParser.IllegalOptionValueException(opt_bordercast, "bad format: num,locs,start,delay");
      cmdOpts.bordercasts = Integer.parseInt(data[0]);
      cmdOpts.bordercastStart = Integer.parseInt(data[1]);
      cmdOpts.bordercastDelay = Integer.parseInt(data[2]);
    }
    // random seed
    if (parser.getOptionValue(opt_randseed) != null)
    {
      cmdOpts.seed = ((Integer)parser.getOptionValue(opt_randseed)).intValue();
    }
    // wrap-around field
    if(parser.getOptionValue(opt_wrap) != null)
    {
      cmdOpts.wrapField = true;
    }
    // spatial
    if(parser.getOptionValue(opt_spatial)!=null)
    {
      String spatialString = ((String)parser.getOptionValue(opt_spatial)).split(":")[0];
      String spatialOpts = Util.stringJoin((String[])Util.rest(((String)parser.getOptionValue(opt_spatial)).split(":")), ":");
      if(spatialString!=null)
      {
        if(spatialString.equalsIgnoreCase("linear"))
        {
          cmdOpts.spatial_mode = Constants.SPATIAL_LINEAR;
        }
        else if(spatialString.equalsIgnoreCase("grid"))
        {
          cmdOpts.spatial_mode = Constants.SPATIAL_GRID;
          cmdOpts.spatial_div = Integer.parseInt(spatialOpts);
        }
        else if(spatialString.equalsIgnoreCase("hier"))
        {
          cmdOpts.spatial_mode = Constants.SPATIAL_HIER;
          cmdOpts.spatial_div = Integer.parseInt(spatialOpts);
        }
        else
        {
          throw new CmdLineParser.IllegalOptionValueException(opt_spatial, "unrecognized spatial binning model");
        }
      }
    }

    return cmdOpts;

  } // parseCommandLineOptions

  //////////////////////////////////////////////////
  // simulation setup
  //

  /** addNode method stub. */
  private static Method method_addNode;
  static
  {
    try
    {
      method_addNode = findUniqueMethod(bordercast.class, "addNode");
    }
    catch(NoSuchMethodException e)
    {
      throw new RuntimeException("should not occur");
    }
  }

  /**
   * Lookup method in a class by name.
   *
   * @param c class to scan
   * @param name method name
   * @return method, if found and unique
   * @throws NoSuchMethodException if method not found or not unique
   */
  public static Method findUniqueMethod(Class c, String name) throws NoSuchMethodException
  {
    Method[] methods = c.getDeclaredMethods();
    Method m = null;
    for(int i=0; i<methods.length; i++)
    {
      if(name.equals(methods[i].getName()))
      {
        if(m!=null)
        {
          throw new NoSuchMethodException("method name not unique: "+name);
        }
        m=methods[i];
      }
    }
    if(m==null)
    {
      throw new NoSuchMethodException("method not found: "+name);
    }
    return m;
  }


  /**
   * Add node to the field and start it.
   *
   * @param opts command-line options
   * @param i node number, which also serves as its address
   * @param routers list of zrp entities to be appended to
   * @param stats statistics collector
   * @param field simulation field
   * @param place node placement model
   * @param radioInfo shared radio information
   * @param protMap registered protocol map
   * @param inLoss packet incoming loss model
   * @param outLoss packet outgoing loss model
   */
  public static void addNode(CommandLineOptions opts, int i, Vector routers, RouteZrp.ZrpStats stats,
      Field field, Placement place, RadioInfo.RadioInfoShared radioInfo, Mapper protMap,
      PacketLoss inLoss, PacketLoss outLoss)
  {
    // radio
    RadioNoise radio = new RadioNoiseIndep(i, radioInfo);

    // mac
    MacDumb mac = new MacDumb(new MacAddress(i), radio.getRadioInfo());

    // network
    final NetAddress address = new NetAddress(i);
    NetIp net = new NetIp(address, protMap, inLoss, outLoss);

    // routing
    RouteInterface route = null;
    switch(opts.protocol)
    {
      case Constants.NET_PROTOCOL_ZRP:
        RouteZrp zrp = new RouteZrp(address, opts.protocolOpts);
        zrp.setNetEntity(net.getProxy());
        zrp.getProxy().start();
        route = zrp.getProxy();
        routers.add(zrp);
        // ndp
        RouteInterface.Zrp.Ndp ndp = null;
        switch(opts.ndp)
        {
          case Constants.NET_PROTOCOL_ZRP_NDP_DEFAULT:
            ndp = new RouteZrpNdp(zrp, opts.ndpOpts);
            break;
          default:
            throw new RuntimeException("invalid ndp protocol");
        }
        // iarp
        RouteInterface.Zrp.Iarp iarp = null;
        switch(opts.iarp)
        {
          case Constants.NET_PROTOCOL_ZRP_IARP_DEFAULT:
            iarp = new RouteZrpIarp(zrp, opts.iarpOpts);
            break;
          case Constants.NET_PROTOCOL_ZRP_IARP_ZDP:
            iarp = new RouteZrpZdp(zrp, opts.iarpOpts);
            break;
          default:
            throw new RuntimeException("invalid iarp protocol");
        }
        // brp
        RouteInterface.Zrp.Brp brp = null;
        switch(opts.brp)
        {
          case Constants.NET_PROTOCOL_ZRP_BRP_DEFAULT:
            brp = new RouteZrpBrp(zrp, opts.brpOpts);
            break;
          case Constants.NET_PROTOCOL_ZRP_BRP_FLOOD:
            brp = new RouteZrpBrpFlood(zrp, opts.brpOpts);
            break;
          default:
            throw new RuntimeException("invalid brp protocol");
        }
        // ierp
        RouteInterface.Zrp.Ierp ierp = null;
        switch(opts.ierp)
        {
          case Constants.NET_PROTOCOL_ZRP_IERP_DEFAULT:
            ierp = new RouteZrpIerp(zrp, opts.ierpOpts);
            break;
          default:
            throw new RuntimeException("invalid ierp protocol");
        }
        // set sub-protocols
        zrp.setSubProtocols(ndp, iarp, brp, ierp);
        // statistics
        zrp.setStats(stats);
        break;
      default:
        throw new RuntimeException("invalid routing protocol");
    }

    // transport
    TransUdp udp = new TransUdp();

    // placement
    Location location = place.getNextLocation();
    field.addRadio(radio.getRadioInfo(), radio.getProxy(), location);
    field.startMobility(radio.getRadioInfo().getUnique().getID());

    // node entity hookup
    radio.setFieldEntity(field.getProxy());
    radio.setMacEntity(mac.getProxy());
    byte intId = net.addInterface(mac.getProxy());
    net.setRouting(route);
    mac.setRadioEntity(radio.getProxy());
    mac.setNetEntity(net.getProxy(), intId);
    udp.setNetEntity(net.getProxy());
    net.setProtocolHandler(Constants.NET_PROTOCOL_UDP, udp.getProxy());
    net.setProtocolHandler(opts.protocol, route);
  }


  /**
   * Constructs field and nodes with given command-line options, establishes
   * client/server pairs and starts them.
   *
   * @param opts command-line parameters
   * @param routers vectors to place zrp objects into
   * @param stats zrp statistics collection object
   */
  private static void buildField(CommandLineOptions opts, final Vector routers, final RouteZrp.ZrpStats stats)
  {
    // initialize node mobility model
    Mobility mobility = null;
    switch(opts.mobility)
    {
      case Constants.MOBILITY_STATIC:
        mobility = new Mobility.Static();
        break;
      case Constants.MOBILITY_WAYPOINT:
        mobility = new Mobility.RandomWaypoint(opts.field, opts.mobilityOpts);
        break;
      case Constants.MOBILITY_TELEPORT:
        mobility = new Mobility.Teleport(opts.field, Long.parseLong(opts.mobilityOpts));
        break;
      case Constants.MOBILITY_WALK:
        mobility = new Mobility.RandomWalk(opts.field, opts.mobilityOpts);
        break;
      default:
        throw new RuntimeException("unknown node mobility model");
    }
    // initialize spatial binning
    Spatial spatial = null;
    switch(opts.spatial_mode)
    {
      case Constants.SPATIAL_LINEAR:
        spatial = new Spatial.LinearList(opts.field);
        break;
      case Constants.SPATIAL_GRID:
        spatial = new Spatial.Grid(opts.field, opts.spatial_div);
        break;
      case Constants.SPATIAL_HIER:
        spatial = new Spatial.HierGrid(opts.field, opts.spatial_div);
        break;
      default:
        throw new RuntimeException("unknown spatial binning model");
    }
    if(opts.wrapField) spatial = new Spatial.TiledWraparound(spatial);
    // initialize field
    Field field = new Field(spatial, new Fading.None(), new PathLoss.FreeSpace(), 
        mobility, Constants.PROPAGATION_LIMIT_DEFAULT);
    // initialize shared radio information
    RadioInfo.RadioInfoShared radioInfo = RadioInfo.createShared(
      Constants.FREQUENCY_DEFAULT,
      Constants.BANDWIDTH_DEFAULT,
      Constants.TRANSMIT_DEFAULT,
      Constants.GAIN_DEFAULT,
      Util.fromDB(Constants.SENSITIVITY_DEFAULT),
      Util.fromDB(Constants.THRESHOLD_DEFAULT),
      Constants.TEMPERATURE_DEFAULT,
      Constants.TEMPERATURE_FACTOR_DEFAULT,
      Constants.AMBIENT_NOISE_DEFAULT);
    // initialize shared protocol mapper
    Mapper protMap = new Mapper(new int[] { Constants.NET_PROTOCOL_UDP, opts.protocol, });
    // initialize packet loss models
    PacketLoss outLoss = new PacketLoss.Zero();
    PacketLoss inLoss = null;
    switch(opts.loss)
    {
      case Constants.NET_LOSS_NONE:
        inLoss = new PacketLoss.Zero();
        break;
      case Constants.NET_LOSS_UNIFORM:
        inLoss = new PacketLoss.Uniform(Double.parseDouble(opts.lossOpts));
      default:
        throw new RuntimeException("unknown packet loss model");
    }
    // initialize node placement model
    Placement place = null;
    switch(opts.placement)
    {
      case Constants.PLACEMENT_RANDOM:
        place = new Placement.Random(opts.field);
        break;
      case Constants.PLACEMENT_GRID:
        place = new Placement.Grid(opts.field, opts.placementOpts);
        break;
      default:
        throw new RuntimeException("unknown node placement model");
    }
    // create each node
    for (int i=1; i<=opts.nodes; i++)
    {
      JistAPI.callStaticAt(method_addNode,
          new Object[]
          {
            opts, new Integer(i), routers, stats, field, place, radioInfo, protMap, inLoss, outLoss
          }, i);
    }
    // set up bordercast events
    if(opts.bordercasts!=0)
    {
      JistAPI.runAt(new Runnable()
          {
            public void run()
            {
              System.out.println("clear stats at t="+JistAPI.getTimeString());
              stats.clear();
            }
          }, opts.bordercastStart*Constants.SECOND);
      for(int i=0; i<opts.bordercasts; i++)
      {
        final int j=i;
        JistAPI.runAt(new Runnable()
            {
              public void run()
              {
                RouteZrp zrp = (RouteZrp)routers.elementAt(j%routers.size());
                NetMessage msg = new NetMessage.Ip(Message.NULL, zrp.getLocalAddr(), NetAddress.NULL, 
                  Constants.NET_PROTOCOL_INVALID, Constants.NET_PRIORITY_NORMAL, Constants.TTL_DEFAULT);
                zrp.getProxy().send(msg);
              }
            }, (opts.bordercastStart+opts.bordercastDelay*j)*Constants.SECOND);
      }
    }

  } // buildField

  /**
   * Display statistics at end of simulation.
   *
   * @param routers vectors to place zrp objects into
   * @param stats zrp statistics collection object
   */
  public static void showStats(Vector routers, RouteZrp.ZrpStats stats)
  {
    // print stats: ndp
    System.out.println("NDP-packets-sent = "+stats.send.ndpPackets);
    System.out.println("NDP-packets-recv = "+stats.recv.ndpPackets);
    System.out.println("NDP-bytes-sent = "+stats.send.ndpBytes);
    System.out.println("NDP-bytes-recv = "+stats.recv.ndpBytes);
    // print stats: iarp
    System.out.println("IARP-packets-sent = "+stats.send.iarpPackets);
    System.out.println("IARP-packets-recv = "+stats.recv.iarpPackets);
    System.out.println("IARP-bytes-sent = "+stats.send.iarpBytes);
    System.out.println("IARP-bytes-recv = "+stats.recv.iarpBytes);
    // print stats: brp
    System.out.println("BRP-packets-sent = "+stats.send.brpPackets);
    System.out.println("BRP-packets-recv = "+stats.recv.brpPackets);
    System.out.println("BRP-bytes-sent = "+stats.send.brpBytes);
    System.out.println("BRP-bytes-recv = "+stats.recv.brpBytes);
    // print stats: ierp
    System.out.println("IERP-packets-sent = "+stats.send.ierpPackets);
    System.out.println("IERP-packets-recv = "+stats.recv.ierpPackets);
    System.out.println("IERP-bytes-sent = "+stats.send.ierpBytes);
    System.out.println("IERP-bytes-recv = "+stats.recv.ierpBytes);
    // print stats: ndp neighbours
    long neighbours = 0;
    Iterator it = routers.iterator();
    while(it.hasNext())
    {
      RouteZrp zrp = (RouteZrp)it.next();
      RouteInterface.Zrp.Ndp ndp = zrp.getNdp();
      neighbours += ndp.getNumNeighbours();
    }
    System.out.println("NDP-neighbours = "+neighbours);
    // print stats: iarp routes and links
    long links = 0;
    long routes = 0;
    it = routers.iterator();
    while(it.hasNext())
    {
      RouteZrp zrp = (RouteZrp)it.next();
      RouteInterface.Zrp.Iarp iarp = zrp.getIarp();
      links += iarp.getNumLinks();
      routes += iarp.getNumRoutes();
    }
    System.out.println("IARP-links = "+links);
    System.out.println("IARP-routes = "+routes);
    // clear mem
    routers = null;
    stats = null;
  }

  /**
   * Starts the bordercast simulation.
   *
   * @param args command-line arguments that may determine the parameters
   *   of the simulation
   */
  public static void main(String[] args)
  {
    try
    {
      CommandLineOptions options = parseCommandLineOptions(args);
      if(options.help) 
      {
        showUsage();
        return;
      }
      if(options.endTime>0)
      {
        JistAPI.endAt(options.endTime*Constants.SECOND);
      }
      Constants.random = new Random(options.seed);
      final Vector routers = new Vector();
      final RouteZrp.ZrpStats stats = new RouteZrp.ZrpStats();
      buildField(options, routers, stats);
      JistAPI.runAt(new Runnable()
          {
            public void run()
            {
              showStats(routers, stats);
            }
          }, JistAPI.END);
    }
    catch(CmdLineParser.OptionException e)
    {
      System.out.println(e.getMessage());
    }
  }

} // class: bordercast

