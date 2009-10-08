//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <aodvsim.java Tue 2004/04/06 11:57:32 barr pompom.cs.cornell.edu>
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
import jist.swans.route.RouteAodv;
import jist.swans.misc.Util;
import jist.swans.misc.Mapper;
import jist.swans.misc.Location;
import jist.swans.misc.Message;
import jist.swans.Constants;

import jist.runtime.JistAPI;

import jargs.gnu.*;

import java.util.Date;
import java.util.Random;
import java.util.Vector;


/**
 * AODV simulation.  Derived from bordercast
 * 
 * @author Clifton
 *
 */
public class aodvsim
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
    ///** Time to end simulation. */
    //private int endTime = -1;
    /** Routing protocol to use. */
    private int protocol = Constants.NET_PROTOCOL_AODV;
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
    /** Number of messages sent per minute per node. */
    private double sendRate = 1.0;
    /** Start of sending (seconds). */
    private int startTime = 60;
    /** Number of seconds to send messages. */
    private int duration = 3600;
    /** Number of seconds after messages stop sending to end simulation. */
    private int resolutionTime = 30; 
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
    System.out.println("Usage: java driver.aodvsim [options]");
    System.out.println();
    System.out.println("  -h, --help           print this message");
    System.out.println("  -n, --nodes          number of nodes: n [100] ");
    System.out.println("  -f, --field          field dimensions: x,y [100,100]");
    System.out.println("  -a, --arrange        placement model: [random],grid:ixj");
    System.out.println("  -m, --mobility       mobility: [static],waypoint:opts,teleport:p,walk:opts");
    System.out.println("  -l, --loss           packet loss model: [none],uniform:p");
    System.out.println("  -s, --send rate      send rate per-minute: [1.0]");
    System.out.println("  -t, --timing         node activity timing: start,duration,resolution [60,3600,30]");
    System.out.println("  -r, --randomseed     random seed: [0]");
    System.out.println();
    System.out.println("e.g.");
    System.out.println("  swans driver.aodvsim -n 25 -f 2000x2000 -a grid:5x5 -t 10,600,60");
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
    CmdLineParser.Option opt_nodes = parser.addIntegerOption('n', "nodes");
    CmdLineParser.Option opt_field = parser.addStringOption('f', "field");
    CmdLineParser.Option opt_placement = parser.addStringOption('a', "arrange");
    CmdLineParser.Option opt_mobility = parser.addStringOption('m', "mobility");
    CmdLineParser.Option opt_loss = parser.addStringOption('l', "loss");
    CmdLineParser.Option opt_rate = parser.addDoubleOption('s', "send rate");
    CmdLineParser.Option opt_timing = parser.addStringOption('t', "timing");
    CmdLineParser.Option opt_randseed = parser.addIntegerOption('r', "randomseed");
    parser.parse(args);

    CommandLineOptions cmdOpts = new CommandLineOptions();
    // help
    if(parser.getOptionValue(opt_help) != null)
    {
      cmdOpts.help = true;
    }
    //// endat
    //if (parser.getOptionValue(opt_endat) != null)
    //{
    //  cmdOpts.endTime = ((Integer)parser.getOptionValue(opt_endat)).intValue();
    //}
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
    
    //// bordercast
    //if(parser.getOptionValue(opt_bordercast) != null)
    //{
    //  String[] data = ((String)parser.getOptionValue(opt_bordercast)).split(",");
    //  if(data.length!=3) throw new CmdLineParser.IllegalOptionValueException(opt_bordercast, "bad format: num,start,delay");
    //  cmdOpts.bordercasts = Integer.parseInt(data[0]);
    //  cmdOpts.bordercastStart = Integer.parseInt(data[1]);
    //  cmdOpts.bordercastDelay = Integer.parseInt(data[2]);
    //}
    
    //send rate
    if (parser.getOptionValue(opt_rate) != null)
    {
      cmdOpts.sendRate = ((Double)parser.getOptionValue(opt_rate)).doubleValue();      
    }
    
    //timing parameters
    if (parser.getOptionValue(opt_timing) != null)
    {
      String[] data = ((String)parser.getOptionValue(opt_timing)).split(",");
      if(data.length!=3) throw new CmdLineParser.IllegalOptionValueException(opt_timing, "bad format: start,duration,resolution");
      cmdOpts.startTime = Integer.parseInt(data[0]);      
      cmdOpts.duration = Integer.parseInt(data[1]);
      cmdOpts.resolutionTime = Integer.parseInt(data[2]);
    }
    
    // random seed
    if (parser.getOptionValue(opt_randseed) != null)
    {
      cmdOpts.seed = ((Integer)parser.getOptionValue(opt_randseed)).intValue();
    }

    return cmdOpts;

  } // parseCommandLineOptions


  //////////////////////////////////////////////////
  // simulation setup
  //

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
  public static void addNode(CommandLineOptions opts, int i, Vector routers, RouteAodv.AodvStats stats,
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
      case Constants.NET_PROTOCOL_AODV:
        RouteAodv aodv = new RouteAodv(address);
        aodv.setNetEntity(net.getProxy());
        aodv.getProxy().start();      
        route = aodv.getProxy();
        routers.add(aodv);
        // statistics
        aodv.setStats(stats);
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
  }  //method: addNode




  /**
   * Constructs field and nodes with given command-line options, establishes
   * client/server pairs and starts them.
   *
   * @param opts command-line parameters
   * @param routers vectors to place zrp objects into
   * @param stats zrp statistics collection object
   */
  private static void buildField(CommandLineOptions opts, final Vector routers, final RouteAodv.AodvStats stats)
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
      addNode(opts, i, routers, stats, field, place, radioInfo, protMap, inLoss, outLoss);
    }

    // set up message sending events
    JistAPI.sleep(opts.startTime*Constants.SECOND);
    //System.out.println("clear stats at t="+JistAPI.getTimeString());
    stats.clear();
    int numTotalMessages = (int)Math.floor(((double)opts.sendRate/60) * opts.nodes * opts.duration);
    long delayInterval = (long)Math.ceil((double)opts.duration * (double)Constants.SECOND / (double)numTotalMessages);
    for(int i=0; i<numTotalMessages; i++)
    {
      //pick random send node
      int srcIdx = Constants.random.nextInt(routers.size());
      int destIdx;
      do
      {
        //pick random dest node
        destIdx = Constants.random.nextInt(routers.size());
      } while (destIdx == srcIdx);
      RouteAodv srcAodv = (RouteAodv)routers.elementAt(srcIdx);
      RouteAodv destAodv = (RouteAodv)routers.elementAt(destIdx);
      TransUdp.UdpMessage udpMsg = new TransUdp.UdpMessage(PORT, PORT, Message.NULL);
      NetMessage msg = new NetMessage.Ip(udpMsg, srcAodv.getLocalAddr(), destAodv.getLocalAddr(), 
          Constants.NET_PROTOCOL_UDP, Constants.NET_PRIORITY_NORMAL, Constants.TTL_DEFAULT);
      srcAodv.getProxy().send(msg);
      //stats
      if (stats != null)
      {
        stats.netMsgs++;
      }
      JistAPI.sleep(delayInterval);
    }

  } // buildField


  /**
   * Display statistics at end of simulation.
   *
   * @param routers vectors to place zrp objects into
   * @param stats zrp statistics collection object
   */
  public static void showStats(Vector routers, RouteAodv.AodvStats stats, CommandLineOptions opt, Date startTime)
  {
    Date endTime = new Date();
    long elapsedTime = endTime.getTime() - startTime.getTime();
    
    System.err.println("-------------");   
    System.err.println("Packet stats:");
    System.err.println("-------------");
    
    System.err.println("Rreq packets sent = "+stats.send.rreqPackets);
    System.err.println("Rreq packets recv = "+stats.recv.rreqPackets);
    
    System.err.println("Rrep packets sent = "+stats.send.rrepPackets);
    System.err.println("Rrep packets recv = "+stats.recv.rrepPackets);
   
    System.err.println("Rerr packets sent = "+stats.send.rerrPackets);
    System.err.println("Rerr packets recv = "+stats.recv.rerrPackets);
    
    System.err.println("Hello packets sent = "+stats.send.helloPackets);
    System.err.println("Hello packets recv = "+stats.recv.helloPackets);
    
    System.err.println("Total aodv packets sent = "+stats.send.aodvPackets);
    System.err.println("Total aodv packets recv = "+stats.recv.aodvPackets);

    System.err.println("Non-hello packets sent = "+(stats.send.aodvPackets - stats.send.helloPackets));
    System.err.println("Non-hello packets recv = "+(stats.recv.aodvPackets - stats.recv.helloPackets));
    
    System.err.println("--------------");
    System.err.println("Overall stats:");
    System.err.println("--------------");
    System.err.println("Messages to deliver = "+stats.netMsgs);
    System.err.println("Route requests      = "+stats.rreqOrig);
    System.err.println("Route replies       = "+stats.rrepOrig);
    System.err.println("Routes added        = "+stats.rreqSucc);
    
    System.err.println();
    System.gc();
    System.err.println("freemem:  "+Runtime.getRuntime().freeMemory());
    System.err.println("maxmem:   "+Runtime.getRuntime().maxMemory());
    System.err.println("totalmem: "+Runtime.getRuntime().totalMemory());
    long usedMem = Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
    System.err.println("used:     "+usedMem);

    System.err.println("start time  : "+startTime);
    System.err.println("end time    : "+endTime);
    System.err.println("elapsed time: "+elapsedTime);
    System.err.flush();

    System.out.println(opt.nodes+"\t"
      +stats.send.rreqPackets+"\t"
      +stats.recv.rreqPackets+"\t"
      +stats.send.rrepPackets+"\t"
      +stats.recv.rrepPackets+"\t"
      +stats.send.rerrPackets+"\t"
      +stats.recv.rerrPackets+"\t"
      +stats.send.helloPackets+"\t"
      +stats.recv.helloPackets+"\t"
      +stats.send.aodvPackets+"\t"
      +stats.recv.aodvPackets+"\t"
      +(stats.send.aodvPackets - stats.send.helloPackets)+"\t"
      +(stats.recv.aodvPackets - stats.recv.helloPackets)+"\t"
      +usedMem+"\t"
      +elapsedTime);
        
    //clear memory
    routers = null;
    stats = null;
  }

  /**
   * Main entry point.
   *
   * @param args command-line arguments 
   */  
  public static void main(String[] args)
  {
    try
    {
      final CommandLineOptions options = parseCommandLineOptions(args);
      if(options.help) 
      {
        showUsage();
        return;
      }
      long endTime = options.startTime+options.duration+options.resolutionTime;
      if(endTime>0)
      {
        JistAPI.endAt(endTime*Constants.SECOND);
      }
      //Constants.random = new Random(options.seed);
      Constants.random = new Random();
      final Vector routers = new Vector();
      final RouteAodv.AodvStats stats = new RouteAodv.AodvStats();
      final Date startTime = new Date();
      buildField(options, routers, stats);
      JistAPI.runAt(new Runnable()
          {
            public void run()
            {
              showStats(routers, stats, options, startTime);
            }
          }, JistAPI.END);

      //added
      /*
      JistAPI.runAt(new Runnable()
          {
            public void run()
            {
              Iterator itr = routers.iterator();
              while (itr.hasNext())
              {
                RouteAodv aodv = (RouteAodv)itr.next();
                aodv.printPrecursors();
                aodv.printOutgoing();
              }
            }
          }, JistAPI.END);
       */
    }
    catch(CmdLineParser.OptionException e)
    {
      System.out.println(e.getMessage());
    }

  }

}
