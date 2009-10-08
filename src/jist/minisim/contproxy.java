//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <contproxy.java Tue 2004/04/06 11:27:20 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.minisim;

import jist.runtime.JistAPI;

import jargs.gnu.*; // Download from: http://jargs.sourceforge.net

/** 
 * Measures throughput of proxy entity continuation calls in JiST.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: contproxy.java,v 1.7 2004-04-06 16:07:42 barr Exp $
 * @since JIST1.0
 */

public final class contproxy
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
    System.out.println("JiST continuation micro-benchmark v"+VERSION+", Java in Simulation Time Project.");
    System.out.println("Rimon Barr <barr+jist@cs.cornell.edu>, Cornell University.");
    System.out.println();
  }

  /**
   * Print benchmark command-line syntax.
   */
  private static void showUsage() 
  {
    System.out.println("Usage: cont -m <mode> -n <num> [-w <warm>]");
    System.out.println("       cont -v | -h");
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

  //////////////////////////////////////////////////
  // main 
  //

  /**
   * Benchmark entry point: measure regular event performance.
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
      entityInterface ei = (entityInterface)JistAPI.proxy(e, entityInterface.class);
      ei.go(ei);
    }
    catch(CmdLineParser.OptionException e)
    {
      //System.out.println("Error parsing command line: "+e.getMessage());
    }
  }


  /**
   * Interface for benchmarking proxy entity.
   */
  public interface entityInterface extends JistAPI.Proxiable
  {
    /**
     * Perform benchmark.
     *
     * @param ei entity reference
     */
    void go(entityInterface ei);

    /**
     * Proxied blocking call with no parameters.
     *
     * @throws JistAPI.Continuation never; blocking event
     */
    void operation_null() throws JistAPI.Continuation;

    /**
     * Proxied blocking call with primitive integer parameter.
     *
     * @param i dummy int
     * @throws JistAPI.Continuation never; blocking event
     */
    void operation_int(int i) throws JistAPI.Continuation;

    /**
     * Proxied blocking call with primitive double parameter.
     *
     * @param d dummy double
     * @throws JistAPI.Continuation never; blocking event
     */
    void operation_double(double d) throws JistAPI.Continuation;

    /**
     * Proxied blocking call with String parameter.
     *
     * @param s dummy string
     * @throws JistAPI.Continuation never; blocking event
     */
    void operation_string(String s) throws JistAPI.Continuation;
  }

  /**
   * Benchmark proxy entity.
   */
  public static class entity implements entityInterface
  {
    /** benchmark type. */
    private int mode;
    /** number of blocking proxied benchmark events. */
    private long nevents;
    /** number of blocking proxied warm-up events. */
    private long nwarm;

    /**
     * Create new benchmark entity.
     *
     * @param mode benchmark type
     * @param nevents number of proxied blocking benchmark events
     * @param nwarm number of proxied blocking warm-up events
     */
    public entity(int mode, long nevents, long nwarm)
    {
      this.mode = mode;
      this.nevents = nevents;
      this.nwarm = nwarm;
      String type = null;
      switch(mode)
      {
        case contproxy.MODE_NULL:
          type = contproxy.MODE_NULL_STRING;
          break;
        case contproxy.MODE_INT:
          type = contproxy.MODE_INT_STRING;
          break;
        case contproxy.MODE_DOUBLE:
          type = contproxy.MODE_DOUBLE_STRING;
          break;
        case contproxy.MODE_STRING:
          type = contproxy.MODE_STRING_STRING;
          break;
        default:
          throw new RuntimeException("unrecognized event type: "+mode);
      }
      System.out.println("   type: "+type);
      System.out.println(" events: "+nevents);
      System.out.println(" warmup: "+nwarm);
    }

    /**
     * Perform single blocking proxied call.
     *
     * @param e target entity
     */
    private void call(entityInterface e)
    {
      switch(mode)
      {
        case contproxy.MODE_NULL:
          e.operation_null(); 
          break;
        case contproxy.MODE_INT:
          e.operation_int(1);
          break;
        case contproxy.MODE_DOUBLE:
          e.operation_double(1.0);
          break;
        case contproxy.MODE_STRING:
          e.operation_string("foo");
          break;
        default:
          throw new RuntimeException("unrecognized event type: "+mode);
      }
    }

    /** {@inheritDoc} */
    public void go(entityInterface e)
    {
      for(int i=0; i<nwarm; i++)
      {
        JistAPI.sleep(1);
        call(e);
      }
      System.out.println("benchmark BEGIN");
      System.gc();
      long startTime = System.currentTimeMillis();
      for(int i=0; i<nevents; i++)
      {
        JistAPI.sleep(1);
        call(e);
      }
      System.out.println("benchmark END");
      long endTime = System.currentTimeMillis();
      System.out.println("seconds: "+((endTime-startTime)/1000.0));
    }

    /** {@inheritDoc} */
    public void operation_null()
    {
      JistAPI.sleep(1);
    }

    /** {@inheritDoc} */
    public void operation_int(int i)
    {
      //System.out.println("operation_int at t="+JistAPI.getTime());
      JistAPI.sleep(1);
    }

    /** {@inheritDoc} */
    public void operation_double(double d)
    {
      JistAPI.sleep(1);
    }

    /** {@inheritDoc} */
    public void operation_string(String s)
    {
      JistAPI.sleep(1);
    }

  } // class entity

} // class contproxy

