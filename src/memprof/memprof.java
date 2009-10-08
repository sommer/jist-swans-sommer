//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <memprof.java Thu 2003/12/25 17:39:32 barr pompom.cs.cornell.edu>
//

package memprof;

import jargs.gnu.*; // Download from: http://jargs.sourceforge.net
import java.lang.reflect.*;

/** 
 * JiST memory profile library. To use this library you run Java as:
 *   <code>java -Xrunmemprof[:&lt;filename&gt;]</code>
 * where the filename is optional. If omitted, the memory profile will be
 * dumped on standard error. By default, a memory dump will be taken during
 * JVM shutdown, unless a dump has been requested earlier using the
 * <code>dumpHeap</code> function. A memory dump at shutdown may requested
 * nonetheless using the <code>dumpOnShutdown</code> function.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: memprof.java,v 1.4 2003-12-26 05:05:27 barr Exp $
 * @since JIST1.0
 */
public class memprof
{
  //////////////////////////////////////////////////
  // Constants
  //

  /** memory profiler version. */
  public static final String VERSION = "0.1";

  //////////////////////////////////////////////////
  // Native
  //

  static { 
    try
    {
      System.loadLibrary("memprof"); 
    }
    catch(UnsatisfiedLinkError e)
    {
      System.err.println("unable to load memprof library; start java with -Xrunmemprof switch");
    }
  }

  /** 
   * Print heap information (native code). 
   * 
   * @param ident some string to print which identifies program location
   */
  private static native void _dumpHeap(String ident);
  /** Request that heap information be displayed on shutdown (native code). */
  private static native void _dumpOnShutdown();

  /**
   * Request dump of the entire Java heap at this instant.
   *
   * @param ident some string identifier for this execution point
   */
  public static void dumpHeap(String ident)
  {
    try
    {
      _dumpHeap(ident);
    }
    catch(UnsatisfiedLinkError e)
    {
      System.err.println("unable to dump heap; start java with -Xrunmemprof switch");
    }
  }

  /**
   * Request that heap information be displayed on shutdown.
   */
  public static void dumpOnShutdown()
  {
    try
    {
      _dumpOnShutdown();
    }
    catch(UnsatisfiedLinkError e)
    {
      System.err.println("unable to schedule shutdown heap dump; start java with -Xrunmemprof switch");
    }
  }

  //////////////////////////////////////////////////
  // Static command-line helper methods
  //

  /**
   * Print version information.
   */
  public static void showVersion()
  {
    System.out.println("JiST memory profiler v"+VERSION+", Java in Simulation Time Project.");
    System.out.println("Rimon Barr <barr+jist@cs.cornell.edu>, Cornell University.");
    System.out.println();
  }

  /**
   * Print syntax information.
   */
  public static final void showUsage() 
  {
    System.out.println("Usage: memprof java_prog");
    System.out.println("       jist -v | -h");
    System.out.println();
    System.out.println("  -h, --help      display this help information");
    System.out.println("  -v, --version   display version information");
    System.out.println("where: ");
    System.out.println("  java_prog   is:   java program with command-line arguments");
    System.out.println();
  }

  /**
   * Parsed command-line options structure.
   */
  private static class CommandLineOptions
  {
    // defaults
    /** print help. */
    public boolean help = false;
    /** print version. */
    public boolean version = false;
    /** name of program to profile. */
    public String prog = null;
    /** command-line parameters for program to profile. */
    public String[] args = new String[0];
  }

  /**
   * Parse command-line options.
   *
   * @param args command-line parameters
   * @return parsed command-line options
   * @throws CmdLineParser.OptionException invalid option encountered
   */
  private static CommandLineOptions parseCommandLineOptions(String[] args)
    throws CmdLineParser.OptionException
  {
    // setup
    CmdLineParser parser = new CmdLineParser();
    CmdLineParser.Option opt_help = parser.addBooleanOption('h', "help");
    CmdLineParser.Option opt_version = parser.addBooleanOption('v', "version");

    // parse
    parser.parse(args);
    CommandLineOptions options = new CommandLineOptions();
    if(parser.getOptionValue(opt_help)!=null) 
    {
      options.help = true;
    }
    if(parser.getOptionValue(opt_version)!=null) 
    {
      options.version = true;
    }
    String[] rest = parser.getRemainingArgs();
    if(rest.length>0) 
    {
      options.prog = rest[0];
      options.args = new String[rest.length-1];
      System.arraycopy(rest, 1, options.args, 0, options.args.length);
    }
    return options;
  }


  //////////////////////////////////////////////////
  // MAIN
  //

  /**
   * Entry point.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    try 
    {
      // command-line
      final CommandLineOptions options = parseCommandLineOptions(args);
      // show usage
      if(options.help || options.prog==null)
      {
        showVersion();
        showUsage();
        return;
      }
      // show version
      if(options.version)
      {
        showVersion();
        return;
      }
      System.out.println("profile begin");
      Class c = Class.forName(options.prog);
      Class[] sig = { String[].class };
      Method m = c.getMethod("main", sig);
      m.invoke(null, new Object[] { options.args });
      System.gc();
      System.out.println("profile end");
      dumpHeap(null);
    }
    catch(CmdLineParser.OptionException e)
    {
      System.out.println("Error parsing command line: "+e.getMessage());
    }
    catch(Exception e)
    {
      System.out.println(e);
    }
  }
}
