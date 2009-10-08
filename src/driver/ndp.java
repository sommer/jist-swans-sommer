//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <ndp.java Tue 2004/04/06 11:58:00 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package driver;

import jist.swans.field.Field;
import jist.swans.field.Spatial;
import jist.swans.field.PathLoss;
import jist.swans.field.Mobility;
import jist.swans.field.Fading;
import jist.swans.field.Placement;
import jist.swans.net.NetIp;
import jist.swans.net.NetAddress;
import jist.swans.net.PacketLoss;
import jist.swans.mac.MacDumb;
import jist.swans.mac.MacAddress;
import jist.swans.radio.RadioNoiseIndep;
import jist.swans.radio.RadioInfo;
import jist.swans.app.AppHeartbeat;
import jist.swans.misc.Location;
import jist.swans.misc.Mapper;
import jist.swans.misc.Util;
import jist.swans.Constants;

import jist.runtime.JistAPI;

import java.io.*;
import jargs.gnu.*; // Download from: http://jargs.sourceforge.net

/**
 * Node Discovery Protocol macro-benchmark.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: ndp.java,v 1.16 2004-04-06 16:07:42 barr Exp $
 */

public class ndp
{
  //////////////////////////////////////////////////
  // Constants
  //

  /** benchmark version. */
  public static final String VERSION = "0.1";

  /** benchmark mode. */
  private static final int MODE_INVALID = -1;
  /** benchmark mode. */
  private static final int MODE_TIME = 0;
  /** benchmark mode. */
  private static final int MODE_MEM = 1;
  /** benchmark mode. */
  private static final int MODE_DENSITY = 2;

  /** benchmark mode string. */
  private static final String MODE_TIME_STRING = "time";
  /** benchmark mode string. */
  private static final String MODE_MEM_STRING = "mem";
  /** benchmark mode string. */
  private static final String MODE_DENSITY_STRING = "density";

  /** benchmark binning types. */
  private static final int MODE_SPATIAL_LINEAR = 0;
  /** benchmark binning types. */
  private static final int MODE_SPATIAL_GRID   = 1;
  /** benchmark binning types. */
  private static final int MODE_SPATIAL_HIER   = 2;

  /** benchmark binning type strings. */
  private static final String MODE_SPATIAL_STRING_LINEAR = "linear";
  /** benchmark binning type strings. */
  private static final String MODE_SPATIAL_STRING_GRID   = "grid";
  /** benchmark binning type strings. */
  private static final String MODE_SPATIAL_STRING_HIER   = "hier";

  /** random waypoint pause time. */
  public static final int PAUSE_TIME = 30;
  /** random waypoint granularity. */
  public static final int GRANULARITY = 10;
  /** random waypoint minimum speed. */
  public static final int MIN_SPEED = 2;
  /** random waypoint maximum speed. */
  public static final int MAX_SPEED = 10;

  /** benchmark start time. */
  private static long startTime; 

  //////////////////////////////////////////////////
  // Static command-line helper methods
  //

  /**
   * Print benchmark version information.
   */
  private static void showVersion()
  {
    System.out.println("JiST Project: Node discovery protocol macro-benchmark v"+VERSION);
    System.out.println("Rimon Barr <barr+jist@cs.cornell.edu>, Cornell University.");
    System.out.println();
  }

  /**
   * Print benchmark command-line syntax.
   */
  private static void showUsage() 
  {
    System.out.println("Usage: ndp -m <mode> -n <num> -d <size> -t <time>");
    System.out.println("       ndp -v | -h");
    System.out.println();
    System.out.println("  -h, --help         display this help information");
    System.out.println("  -v, --version      display version information");
    System.out.println("  -m, --mode         benchmark mode: time, mem, density");
    System.out.println("  -n, --num          number of nodes");
    System.out.println("  -d, --dim          length of square field (meters)");
    System.out.println("  -t, --time         duration of simulation (seconds)");
    System.out.println("  -s, --spatial      [linear], grid:n, hier:n");
    System.out.println();
    System.out.println("eg: swans driver.ndp -m time -n 50 -d 4082 -t 900");
    System.out.println("    swans driver.ndp -m time -n 10000 -d 57735 -t 120 -s hier:9");
    System.out.println();
  }

  /**
   * Parsed command-line options.
   */
  private static class cmdlineOpts
  {
    // defaults
    /** print help. */
    public boolean help = false;
    /** print version. */
    public boolean version = false;
    /** benchmark mode. */
    public int mode = MODE_INVALID;
    /** number of nodes. */
    public long num = -1;
    /** length of field. */
    public long dim = 1;
    /** total simulation time. */
    public long time = 0;
    /** string of binning mode. */
    public String spatial = "linear";
    /** binning mode. */
    public int spatial_mode = MODE_SPATIAL_LINEAR;
    /** binning degree. */
    public int spatial_div = -1;
  }

  /**
   * Parse command-line options.
   *
   * @param args command-line parameters
   * @return parsed command-line options
   * @throws CmdLineParser.OptionException invalid option encountered
   */
  private static cmdlineOpts parseCommandLineOptions(String[] args)
    throws CmdLineParser.OptionException
  {
    // setup
    cmdlineOpts options = new cmdlineOpts();
    CmdLineParser parser = new CmdLineParser();
    CmdLineParser.Option opt_help = parser.addBooleanOption('h', "help");
    CmdLineParser.Option opt_version = parser.addBooleanOption('v', "version");
    CmdLineParser.Option opt_mode = parser.addStringOption('m', "mode");
    CmdLineParser.Option opt_num = parser.addStringOption('n', "num");
    CmdLineParser.Option opt_dim = parser.addStringOption('d', "dim");
    CmdLineParser.Option opt_time = parser.addStringOption('t', "time");
    CmdLineParser.Option opt_spatial = parser.addStringOption('s', "spatial");

    // parse
    parser.parse(args);
    if(parser.getOptionValue(opt_help)!=null) 
    {
      options.help = true;
    }
    if(parser.getOptionValue(opt_version)!=null) 
    {
      options.version = true;
    }
    if(parser.getOptionValue(opt_mode)!=null)
    {
      String mode = ((String)parser.getOptionValue(opt_mode)).toLowerCase();
      if(mode.equals(MODE_TIME_STRING.toLowerCase()))
      {
        options.mode = MODE_TIME;
      }
      else if(mode.equals(MODE_MEM_STRING))
      {
        options.mode = MODE_MEM;
      }
      else if(mode.equals(MODE_DENSITY_STRING))
      {
        options.mode = MODE_DENSITY;
      }
      else
      {
        throw new RuntimeException("unrecognized mode:"+mode);
      }
    }
    if(parser.getOptionValue(opt_num)!=null)
    {
      options.num = Long.parseLong((String)parser.getOptionValue(opt_num));
    }
    if(parser.getOptionValue(opt_dim)!=null)
    {
      options.dim = Long.parseLong((String)parser.getOptionValue(opt_dim));
    }
    if(parser.getOptionValue(opt_time)!=null)
    {
      options.time = Long.parseLong((String)parser.getOptionValue(opt_time));
    }
    if(parser.getOptionValue(opt_spatial)!=null)
    {
      options.spatial = ((String)parser.getOptionValue(opt_spatial)).toLowerCase();
    }
    if(options.spatial.startsWith(MODE_SPATIAL_STRING_LINEAR))
    {
      options.spatial_mode = MODE_SPATIAL_LINEAR;
    }
    else if(options.spatial.startsWith(MODE_SPATIAL_STRING_GRID))
    {
      options.spatial_mode = MODE_SPATIAL_GRID;
      options.spatial_div = Integer.parseInt(options.spatial.split(":")[1]);
    }
    else if(options.spatial.startsWith(MODE_SPATIAL_STRING_HIER))
    {
      options.spatial_mode = MODE_SPATIAL_HIER;
      options.spatial_div = Integer.parseInt(options.spatial.split(":")[1]);
    }
    else
    {
      throw new RuntimeException("invalid spatial structure: "+options.spatial);
    }
    String[] rest = parser.getRemainingArgs();
    return options;
  }


  //////////////////////////////////////////////////
  // simulation setup
  //

  /**
   * Initialize simulation node.
   *
   * @param i node number
   * @param field simulation field
   * @param placement node placement model
   * @param radioInfoShared shared radio information
   * @param protMap shared protocol map
   * @param plIn incoming packet loss model
   * @param plOut outgoing packet loss model
   */
  public static void createNode(int i,
      Field field, Placement placement,
      RadioInfo.RadioInfoShared radioInfoShared, Mapper protMap,
      PacketLoss plIn, PacketLoss plOut)
  {
    // create entities
    RadioNoiseIndep radio = new RadioNoiseIndep(i, radioInfoShared);
    //Mac802_11 mac = new Mac802_11(new MacAddress(i), radio.getRadioInfo());
    MacDumb mac = new MacDumb(new MacAddress(i), radio.getRadioInfo());
    NetIp net = new NetIp(new NetAddress(i), protMap, plIn, plOut);
    AppHeartbeat app = new AppHeartbeat(i, false);
    // hookup entities
    field.addRadio(radio.getRadioInfo(), radio.getProxy(), placement.getNextLocation());
    field.startMobility(radio.getRadioInfo().getUnique().getID());
    radio.setFieldEntity(field.getProxy());
    radio.setMacEntity(mac.getProxy());
    mac.setRadioEntity(radio.getProxy());
    byte intId = net.addInterface(mac.getProxy());
    mac.setNetEntity(net.getProxy(), intId);
    net.setProtocolHandler(Constants.NET_PROTOCOL_HEARTBEAT, app.getNetProxy());
    app.setNetEntity(net.getProxy());
    app.getAppProxy().run();
  }

  /**
   * Initialize simulation field.
   *
   * @param bounds size of field
   * @param spatial binning radio container
   * @param nodes number of nodes
   * @return simulation field
   */
  public static Field createSim(Location.Location2D bounds, Spatial spatial, long nodes)
  {
    // create field
    Placement placement = new Placement.Random(bounds);
    Mobility mobility = new Mobility.RandomWaypoint(bounds, PAUSE_TIME, GRANULARITY, MAX_SPEED, MIN_SPEED);
    Fading fading = new Fading.None();
    PathLoss pathloss = new PathLoss.FreeSpace();
    Field field = new Field(spatial, fading, pathloss, mobility, Constants.PROPAGATION_LIMIT_DEFAULT);
    // shared radio information
    RadioInfo.RadioInfoShared radioInfoShared = RadioInfo.createShared(
        Constants.FREQUENCY_DEFAULT, Constants.BANDWIDTH_DEFAULT,
        Constants.TRANSMIT_DEFAULT, Constants.GAIN_DEFAULT,
        Util.fromDB(Constants.SENSITIVITY_DEFAULT), Util.fromDB(Constants.THRESHOLD_DEFAULT),
        Constants.TEMPERATURE_DEFAULT, Constants.TEMPERATURE_FACTOR_DEFAULT, Constants.AMBIENT_NOISE_DEFAULT);
    // protocol mapper
    Mapper protMap = new Mapper(Constants.NET_PROTOCOL_MAX);
    protMap.mapToNext(Constants.NET_PROTOCOL_HEARTBEAT);
    // packet loss
    PacketLoss pl = new PacketLoss.Zero();
    // create nodes
    for(int i=0; i<nodes; i++)
    {
      final int PACE = 10000;
      createNode(i, field, placement, radioInfoShared, protMap, pl, pl);
      if((i+1)%PACE==0) System.out.print(" "+(i/PACE+1));
    }
    return field;
  }

  /**
   * Benchmark entry point: heartbeat benchmark.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    try
    {
      // command line
      final cmdlineOpts options = parseCommandLineOptions(args);
      // show usage
      showVersion();
      if(options.help)
      {
        showUsage();
        return;
      }
      // show version
      if(options.version)
      {
        return;
      }
      if(options.mode==MODE_INVALID || options.num==-1)
      {
        showUsage();
        return;
      }
      // create spatial structure
      Location.Location2D bounds = new Location.Location2D(options.dim, options.dim);
      Spatial spatial = null;
      switch(options.spatial_mode)
      {
        case MODE_SPATIAL_LINEAR:
          spatial = new Spatial.LinearList(bounds);
          break;
        case MODE_SPATIAL_GRID:
          spatial = new Spatial.Grid(bounds, options.spatial_div);
          break;
        case MODE_SPATIAL_HIER:
          spatial = new Spatial.HierGrid(bounds, options.spatial_div);
          break;
        default:
          throw new RuntimeException("invalid binning type");
      }
      // run benchmark
      System.out.println("nodes   = "+options.num);
      System.out.println("size    = "+options.dim+" x "+options.dim);
      System.out.println("time    = "+options.time+" seconds");
      System.out.println("spatial = "+options.spatial);
      startTime = System.currentTimeMillis();
      System.out.print("Creating simulation nodes...");
      Field f = createSim(bounds, spatial, options.num);
      System.out.println(" done.");
      switch(options.mode)
      {
        case MODE_MEM:
          reportMem();
          JistAPI.end();
          break;
        case MODE_TIME:
          final long initTime = System.currentTimeMillis();
          JistAPI.runAt(new Runnable()
              {
                public void run()
                {
                  long endTime = System.currentTimeMillis();
                  System.out.println("seconds:"+
                    " init="+ ((initTime-startTime)/1000.0)+
                    " run="+  ((endTime-initTime)/1000.0)+
                    " total="+((endTime-startTime)/1000.0));
                }
              }, options.time*Constants.SECOND);
          JistAPI.endAt(options.time*Constants.SECOND);
          break;
        case MODE_DENSITY:
          System.out.println("Average density  = "+f.computeDensity()*1000*1000+"/km^2");
          System.out.println("Average sensing  = "+f.computeAvgConnectivity(true));
          System.out.println("Average receive  = "+f.computeAvgConnectivity(false));
          switch(options.spatial_mode)
          {
            case MODE_SPATIAL_LINEAR:
              System.out.println("Average node/bin = "+options.num);
              break;
            case MODE_SPATIAL_GRID:
              System.out.println("Average node/bin = "+options.num/(double)(options.spatial_div*options.spatial_div));
              break;
            case MODE_SPATIAL_HIER:
              System.out.println("Average node/bin = "+options.num/Math.pow(4, options.spatial_div));
              break;
            default:
              throw new RuntimeException("invalid binning type");
          }
          JistAPI.end();
          break;
        default:
          throw new RuntimeException("unknown benchmark mode");
      }
    }
    catch(CmdLineParser.OptionException e)
    {
      System.out.println("Error parsing command line: "+e.getMessage());
    }
  }

  /**
   * Print memory information.
   */
  public static void reportMem()
  {
    // report internal memory use
    System.gc();
    System.out.println("freemem:  "+Runtime.getRuntime().freeMemory());
    System.out.println("maxmem:   "+Runtime.getRuntime().maxMemory());
    System.out.println("totalmem: "+Runtime.getRuntime().totalMemory());
    System.out.println("used:     "+
        (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));

    // report system memory numbers
    try
    {
      byte[] b = new byte[5000];
      FileInputStream fin = new FileInputStream("/proc/self/status");
      int readbytes = fin.read(b);
      System.out.write(b, 0, readbytes);
    }
    catch(IOException ex) 
    { 
    }
  }

} // class: ndp

