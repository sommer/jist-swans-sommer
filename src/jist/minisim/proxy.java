//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <proxy.java Tue 2004/04/06 11:28:46 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.minisim;

import jist.runtime.JistAPI;

import jargs.gnu.*; // Download from: http://jargs.sourceforge.net

/**
 * Measures event throughput for proxy entities in JiST.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: proxy.java,v 1.13 2004-04-06 16:07:42 barr Exp $
 * @since JIST1.0
 */

public class proxy
{
  //////////////////////////////////////////////////
  // Constants
  //

  /** benchmark version. */
  public static final String VERSION = "0.1";

  /** benchmark event types. */
  private static final int MODE_INVALID = -1;
  /** benchmark event types. */
  private static final int MODE_NULL = 0;
  /** benchmark event types. */
  private static final int MODE_INT = 1;
  /** benchmark event types. */
  private static final int MODE_DOUBLE = 2;
  /** benchmark event types. */
  private static final int MODE_STRING = 3;

  /** benchmark event type strings. */
  private static final String MODE_NULL_STRING = "null";
  /** benchmark event type strings. */
  private static final String MODE_INT_STRING = "int";
  /** benchmark event type strings. */
  private static final String MODE_DOUBLE_STRING = "double";
  /** benchmark event type strings. */
  private static final String MODE_STRING_STRING = "string";

  //////////////////////////////////////////////////
  // Static command-line helper methods
  //

  /**
   * Print benchmark version information.
   */
  private static void showVersion()
  {
    System.out.println("JiST proxy event micro-benchmark v"+VERSION+", Java in Simulation Time Project.");
    System.out.println("Rimon Barr <barr+jist@cs.cornell.edu>, Cornell University.");
    System.out.println();
  }

  /**
   * Print benchmark command-line syntax.
   */
  private static void showUsage() 
  {
    System.out.println("Usage: events -m <mode> -n <num> [-w <warm>]");
    System.out.println("       events -v | -h");
    System.out.println();
    System.out.println("  -h, --help         display this help information");
    System.out.println("  -v, --version      display version information");
    System.out.println("  -m, --mode         [null], int, double, string");
    System.out.println("  -n, --num          number of events");
    System.out.println("  -w, --warm         number of warm-up events");
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
    /** benchmark event type. */
    public int mode = MODE_NULL;
    /** number of events to time. */
    public long num = 0;
    /** number of warmup events. */
    public long warm = 0;
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
    CmdLineParser.Option opt_warm = parser.addStringOption('w', "warm");

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
      if(mode.equals(MODE_NULL_STRING.toLowerCase()))
      {
        options.mode = MODE_NULL;
      }
      else if(mode.equals(MODE_INT_STRING))
      {
        options.mode = MODE_INT;
      }
      else if(mode.equals(MODE_DOUBLE_STRING))
      {
        options.mode = MODE_DOUBLE;
      }
      else if(mode.equals(MODE_STRING_STRING))
      {
        options.mode = MODE_STRING;
      }
      else
      {
        throw new RuntimeException("unrecognized event type: "+mode);
      }
    }
    if(parser.getOptionValue(opt_num)!=null)
    {
      options.num = Long.parseLong((String)parser.getOptionValue(opt_num));
    }
    if(parser.getOptionValue(opt_warm)!=null)
    {
      options.warm = Long.parseLong((String)parser.getOptionValue(opt_warm));
    }
    String[] rest = parser.getRemainingArgs();
    return options;
  }


  /**
   * Benchmark entity proxy interface.
   */
  public interface entityInterface extends JistAPI.Proxiable
  {
    /** 
     * Begin benchmark.
     */
    void start();
    /** 
     * End benchmark.
     */
    void finish();
    /** 
     * Proxied event with no parameters. 
     */
    void operation_null();
    /** 
     * Proxied event with primitive integer parameter. 
     * @param i dummy int
     */
    void operation_int(int i);
    /** 
     * Proxied event with primitive double parameter.
     *
     * @param d dummy double
     */
    void operation_double(double d);
    /** 
     * Proxied event with String parameter. 
     *
     * @param s dummy string
     */
    void operation_string(String s);
  }

  /** 
   * Benchmark proxy entity.
   */
  public static final class entity implements entityInterface
  {
    /** benchmark type. */
    private final int mode;
    /** total number of benchmark proxied events. */
    private final long total;
    /** number of warm-up proxied events. */
    private final long nwarm;
    /** benchmark start time. */
    private long startTime;
    /** benchmark end time. */
    private long endTime;
    /** entity proxy. */
    private entityInterface e;

    /**
     * Create new proxy benchmark entity.
     *
     * @param mode benchmark type
     * @param nevents number of benchmark proxied events
     * @param nwarm number of warm-up proxied events
     */
    public entity(int mode, long nevents, long nwarm)
    {
      this.mode = mode;
      this.nwarm = nwarm;
      this.total = nevents+nwarm;
    }

    /**
     * Perform benchmark.
     *
     * @param e entity
     */
    public void go(entityInterface e)
    {
      this.e = e;
      String type = null;
      switch(mode)
      {
        case proxy.MODE_NULL:
          type = proxy.MODE_NULL_STRING;
          e.operation_null();
          break;
        case proxy.MODE_INT:
          type = proxy.MODE_INT_STRING;
          e.operation_int(1);
          break;
        case proxy.MODE_DOUBLE:
          type = proxy.MODE_DOUBLE_STRING;
          e.operation_double(1);
          break;
        case proxy.MODE_STRING:
          type = proxy.MODE_STRING_STRING;
          e.operation_string("jist");
          break;
        default:
          throw new RuntimeException("illegal event type");
      }
      long nevents = total-nwarm;
      System.out.println("   type: "+type);
      System.out.println(" events: "+nevents);
      System.out.println(" warmup: "+nwarm);
      JistAPI.sleep(nwarm);
      e.start();
      JistAPI.sleep(nevents+1);
      e.finish();
    }

    /**
     * Measure start time.
     */
    public void start()
    {
      System.out.println("benchmark BEGIN");
      System.gc();
      startTime = System.currentTimeMillis();
    }

    /**
     * Measure finish time.
     */
    public void finish()
    {
      System.out.println("benchmark END");
      endTime = System.currentTimeMillis();
      System.out.println("seconds: "+((endTime-startTime)/1000.0));
      JistAPI.end();
    }

    /** {@inheritDoc} */
    public void operation_null()
    {
      JistAPI.sleep(1);
      e.operation_null();
    }

    /** {@inheritDoc} */
    public void operation_int(int i)
    {
      JistAPI.sleep(1);
      e.operation_int(i);
    }

    /** {@inheritDoc} */
    public void operation_double(double d)
    {
      JistAPI.sleep(1);
      e.operation_double(d);
    }

    /** {@inheritDoc} */
    public void operation_string(String s)
    {
      JistAPI.sleep(1);
      e.operation_string(s);
    }

  } // class entity

  //////////////////////////////////////////////////
  // main
  //

  /**
   * Benchmark entry point: measure proxy event performance.
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
        showVersion();
        return;
      }
      // check parameters
      if(options.mode==MODE_INVALID || options.num==0)
      {
        showUsage();
        return;
      }
      // run benchmark
      entity e = new entity(options.mode, options.num, options.warm);
      e.go((entityInterface)JistAPI.proxy(e, entityInterface.class));
    }
    catch(CmdLineParser.OptionException e)
    {
      System.out.println("Error parsing command line: "+e.getMessage());
    }
  }

} // class proxy

