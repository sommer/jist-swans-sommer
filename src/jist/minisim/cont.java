//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <cont.java Tue 2004/04/06 11:27:17 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.minisim;

import jist.runtime.JistAPI;

import jargs.gnu.*; // Download from: http://jargs.sourceforge.net

/** 
 * Measures throughput of continuation calls in JiST.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: cont.java,v 1.20 2004-04-06 16:07:42 barr Exp $
 * @since JIST1.0
 */

public final class cont
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
  /** benchmark event types. */
  private static final int MODE_ARRAY = 4;
  /** benchmark event types. */
  private static final int MODE_SHOW = 5;

  /** benchmark event type strings. */
  private static final String MODE_NULL_STRING = "null";
  /** benchmark event type strings. */
  private static final String MODE_INT_STRING = "int";
  /** benchmark event type strings. */
  private static final String MODE_DOUBLE_STRING = "double";
  /** benchmark event type strings. */
  private static final String MODE_STRING_STRING = "string";
  /** benchmark event type strings. */
  private static final String MODE_ARRAY_STRING = "array";
  /** benchmark event type strings. */
  private static final String MODE_SHOW_STRING = "show";

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
    System.out.println("  -m, --mode         [show], null, int, double, string, array");
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
    public int mode = MODE_SHOW;
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
      else if(mode.equals(MODE_ARRAY_STRING))
      {
        options.mode = MODE_ARRAY;
      }
      else if(mode.equals(MODE_SHOW_STRING))
      {
        options.mode = MODE_SHOW;
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
      e.go();
    }
    catch(CmdLineParser.OptionException e)
    {
      System.out.println("Error parsing command line: "+e.getMessage());
    }
  }

  /**
   * Entity to test continuation entity calls.
   */
  public static class entity implements JistAPI.Entity
  {
    /** benchmark type. */
    private int mode;
    /** number of continuation events. */
    private long nevents;
    /** number of warm-up events. */
    private long nwarm;

    /**
     * Create new continuation event benchmarking entity.
     *
     * @param mode benchmark type
     * @param nevents number of continuation events
     * @param nwarm number of warm-up events
     */
    public entity(int mode, long nevents, long nwarm)
    {
      this.mode = mode;
      this.nevents = nevents;
      this.nwarm = nwarm;
      String type = null;
      switch(mode)
      {
        case cont.MODE_NULL:
          type = cont.MODE_NULL_STRING;
          break;
        case cont.MODE_INT:
          type = cont.MODE_INT_STRING;
          break;
        case cont.MODE_DOUBLE:
          type = cont.MODE_DOUBLE_STRING;
          break;
        case cont.MODE_STRING:
          type = cont.MODE_STRING_STRING;
          break;
        case cont.MODE_ARRAY:
          type = cont.MODE_ARRAY_STRING;
          break;
        case cont.MODE_SHOW:
          type = cont.MODE_SHOW_STRING;
          break;
        default:
          throw new RuntimeException("unrecognized event type: "+mode);
      }
      System.out.println("   type: "+type);
      System.out.println(" events: "+nevents);
      System.out.println(" warmup: "+nwarm);
    }

    /**
     * Perform single continuation call.
     */
    private void call()
    {
      switch(mode)
      {
        case cont.MODE_NULL:
          operation_null(); 
          break;
        case cont.MODE_INT:
          operation_int(1);
          break;
        case cont.MODE_DOUBLE:
          operation_double(1.0);
          break;
        case cont.MODE_STRING:
          operation_string("foo");
          break;
        case cont.MODE_ARRAY:
          operation_array(new byte[] { });
          break;
        case cont.MODE_SHOW:
          try
          {
            operation_show();
            int i=1;
          }
          catch(RuntimeException e)
          {
            System.out.println("caught exception: "+e);
          }
          break;
        default:
          throw new RuntimeException("unrecognized event type: "+mode);
      }
    }

    /**
     * Perform benchmark.
     */
    public void go()
    {
      for(int i=0; i<nwarm; i++)
      {
        JistAPI.sleep(1);
        call();
      }
      System.out.println("benchmark BEGIN");
      System.gc();
      long startTime = System.currentTimeMillis();
      for(int i=0; i<nevents; i++)
      {
        JistAPI.sleep(1);
        call();
      }
      System.out.println("benchmark END");
      long endTime = System.currentTimeMillis();
      System.out.println("seconds: "+((endTime-startTime)/1000.0));
    }

    /**
     * Blocking operation that displays time.
     *
     * @throws JistAPI.Continuation never; blocking event
     */
    public void operation_show() throws JistAPI.Continuation
    {
      System.out.println("operation_show at t="+JistAPI.getTime());
      JistAPI.sleep(1);
      //throw new RuntimeException("hi");
    }

    /**
     * Blocking operation with no parameters.
     *
     * @throws JistAPI.Continuation never; blocking event
     */
    public void operation_null() throws JistAPI.Continuation
    {
      JistAPI.sleep(1);
    }

    /**
     * Blocking operation with primitive integer parameter.
     *
     * @param i dummy int parameter
     * @throws JistAPI.Continuation never; blocking event
     */
    public void operation_int(int i) throws JistAPI.Continuation
    {
      JistAPI.sleep(1);
    }

    /**
     * Blocking operation with primitive double parameter.
     *
     * @param d dummy double parameter
     * @throws JistAPI.Continuation never; blocking event
     */
    public void operation_double(double d) throws JistAPI.Continuation
    {
      JistAPI.sleep(1);
    }

    /**
     * Blocking operation with String parameter.
     *
     * @param s dummy string parameter
     * @throws JistAPI.Continuation never; blocking event
     */
    public void operation_string(String s) throws JistAPI.Continuation
    {
      JistAPI.sleep(1);
    }

    /**
     * Blocking operation with array parameter.
     *
     * @param b dummy array parameter
     * @return dummy return
     * @throws JistAPI.Continuation never; blocking event
     */
    public byte[] operation_array(byte[] b) throws JistAPI.Continuation
    {
      JistAPI.sleep(1);
      return b;
    }

  } // class entity

} // class cont
