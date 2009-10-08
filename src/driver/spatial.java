//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <spatial.java Mon 2005/07/11 13:21:23 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package driver;

import jist.swans.field.Field;
import jist.swans.field.Spatial;
import jist.swans.field.PathLoss;
import jist.swans.field.Fading;
import jist.swans.field.Placement;
import jist.swans.radio.RadioNoiseIndep;
import jist.swans.radio.RadioInfo;
import jist.swans.misc.Location;
import jist.swans.misc.Mapper;
import jist.swans.misc.Util;
import jist.swans.Constants;

import java.io.*;
import jargs.gnu.*; // Download from: http://jargs.sourceforge.net

/**
 * Spatial data structure benchmark.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: spatial.java,v 1.6 2005-07-11 16:23:27 barr Exp $
 */

public class spatial
{
  //////////////////////////////////////////////////
  // Constants
  //

  /** benchmark version. */
  public static final String VERSION = "0.1";

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

  //////////////////////////////////////////////////
  // Static command-line helper methods
  //

  /**
   * Print benchmark version information.
   */
  private static void showVersion()
  {
    System.out.println("JiST Project: Spatial data structure performance v"+VERSION);
    System.out.println("Rimon Barr <barr+jist@cs.cornell.edu>, Cornell University.");
    System.out.println();
  }

  /**
   * Print benchmark command-line syntax.
   */
  private static void showUsage() 
  {
    System.out.println("Usage: spatial -n <num> -d <size> -s <spatial>");
    System.out.println("       spatial -v | -h");
    System.out.println();
    System.out.println("  -h, --help         display this help information");
    System.out.println("  -v, --version      display version information");
    System.out.println("  -n, --num          number of nodes");
    System.out.println("  -d, --dim          length of square field (meters)");
    System.out.println("  -s, --spatial      [linear], grid:n, hier:n");
    System.out.println();
  }

  /** Parsed command-line options. */
  private static class cmdlineOpts
  {
    // defaults
    /** print help. */
    public boolean help = false;
    /** print version. */
    public boolean version = false;
    /** number of nodes. */
    public long num = -1;
    /** length of field. */
    public long dim = 1;
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
    CmdLineParser.Option opt_num = parser.addStringOption('n', "num");
    CmdLineParser.Option opt_dim = parser.addStringOption('d', "dim");
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
    if(parser.getOptionValue(opt_num)!=null)
    {
      options.num = Long.parseLong((String)parser.getOptionValue(opt_num));
    }
    if(parser.getOptionValue(opt_dim)!=null)
    {
      options.dim = Long.parseLong((String)parser.getOptionValue(opt_dim));
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
    Fading fading = new Fading.None();
    PathLoss pathloss = new PathLoss.FreeSpace();
    Field field = new Field(spatial, fading, pathloss, null, Constants.PROPAGATION_LIMIT_DEFAULT);
    // shared radio information
    RadioInfo.RadioInfoShared radioInfoShared = RadioInfo.createShared(
        Constants.FREQUENCY_DEFAULT, Constants.BANDWIDTH_DEFAULT,
        Constants.TRANSMIT_DEFAULT, Constants.GAIN_DEFAULT,
        Util.fromDB(Constants.SENSITIVITY_DEFAULT), Util.fromDB(Constants.THRESHOLD_DEFAULT),
        Constants.TEMPERATURE_DEFAULT, Constants.TEMPERATURE_FACTOR_DEFAULT, Constants.AMBIENT_NOISE_DEFAULT);
    // protocol mapper
    Mapper protMap = new Mapper(Constants.NET_PROTOCOL_MAX);
    protMap.mapToNext(Constants.NET_PROTOCOL_HEARTBEAT);
    // create nodes
    for(int i=0; i<nodes; i++)
    {
      RadioNoiseIndep radio = new RadioNoiseIndep(i, radioInfoShared);
      field.addRadio(radio.getRadioInfo(), radio.getProxy(), placement.getNextLocation());
    }
    return field;
  }

  /**
   * Benchmark entry point: spatial data structure performance.
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
      if(options.num==-1)
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
          throw new RuntimeException("invalid spatial binning mode");
      }
      // run benchmark
      System.out.println("nodes   = "+options.num);
      System.out.println("size    = "+options.dim+" x "+options.dim);
      System.out.println("spatial = "+options.spatial);
      System.out.print("Creating simulation nodes... ");
      Field f = createSim(bounds, spatial, options.num);
      System.out.println("done.");
      // compute node density metrics
      System.out.println("Average density  = "+f.computeDensity()*1000*1000+"/km^2");
      System.out.println("Average sensing  = "+f.computeAvgConnectivity(true));
      System.out.println("Average receive  = "+f.computeAvgConnectivity(false));
      long bins=0;
      switch(options.spatial_mode)
      {
        case MODE_SPATIAL_LINEAR:
          bins = 1;
          break;
        case MODE_SPATIAL_GRID:
          bins = options.spatial_div*options.spatial_div;
          break;
        case MODE_SPATIAL_HIER:
          bins = (long)Math.pow(4, options.spatial_div);
          break;
        default:
          throw new RuntimeException("invalid spatial binning mode");
      }
      System.out.println("Bins             = "+bins);
      double nodebin = options.num/(double)bins;
      System.out.println("Average node/bin = "+nodebin);
      // run spatial data structure benchmark
      System.out.print("Running benchmark... ");
      long startTime = System.currentTimeMillis();
      for(int i=0; i<10; i++)
      {
        f.computeAvgConnectivity(false);
      }
      long endTime = System.currentTimeMillis();
      System.out.println("done.");
      /*
      // count bins
      Spatial.LinearList.NUM = 0;
      f.computeAvgConnectivity(false);
      double numbins = Spatial.LinearList.NUM/(double)options.num;
      // results
      System.out.println("seconds: "+((endTime-startTime)/1000.0)
          +" bins="+numbins
          +" work="+(numbins*Math.max(nodebin,1)));
      */
    }
    catch(CmdLineParser.OptionException e)
    {
      System.out.println("Error parsing command line: "+e.getMessage());
    }
  }

} // class: spatial

