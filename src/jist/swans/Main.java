//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Main.java Tue 2004/04/06 11:30:32 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans;

import jist.runtime.JistAPI;

import jargs.gnu.*; // Download from: http://jargs.sourceforge.net
import org.apache.log4j.*; // Download from: http://jakarta.apache.org/log4j/docs/index.html

import java.util.*;
import java.io.*;

/** 
 * Primary entry-point into the SWANS simulator. Performs cmd-line parsing, and
 * general initialisation of simulation.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: Main.java,v 1.58 2004-04-06 16:07:46 barr Exp $
 * @since SWANS1.0
 */

public final class Main
{

  //////////////////////////////////////////////////
  // Constants
  //

  /**
   * SWANS version.
   */
  public static final String VERSION = "1.0.0";

  /**
   * Whether certain checks are performed. (Java 1.3.x compatibility)
   */
  public static final boolean ASSERT = false;

  //////////////////////////////////////////////////
  // Static command-line helper methods
  //

  /**
   * Print SWANS syntax.
   */
  private static void showVersion()
  {
    System.out.println("SWANS v"+VERSION+", Java In Simulation Time Runtime.");
    System.out.println("Rimon Barr <barr+jist@cs.cornell.edu>, Cornell University.");
    System.out.println();
  }

  /**
   * Print SWANS syntax.
   */
  private static void showUsage() 
  {
    System.out.println("Usage: swans [-c=file] [switches] sim");
    System.out.println("       swans -v | -h");
    System.out.println();
    System.out.println("  -h, --help      display this help information");
    System.out.println("  -v, --version   display version information");
    System.out.println("  -c, --conf      use given properties file [swans.properties]");
    System.out.println("  -l, --logger    use given logging implementation class");
    System.out.println("switches:");
    System.out.println("  --bsh           run input with BeanShell script engine");
    System.out.println("  --jpy           run input with Jython script engine");
    System.out.println("where: ");
    System.out.println("  sim       is:   SWANS driver program with command-line arguments, or");
    System.out.println("                  SWANS driver script with command-line arguments, or");
    System.out.println("                   (missing script or -- implies interactive shell)");
    System.out.println();
  }

  /**
   * Data structure for command-line options.
   */
  private static class CommandLineOptions
  {
    // defaults
    /** show help. */
    public boolean help = false;
    /** show version. */
    public boolean version = false;
    /** properties filename. */
    public String properties = "swans.properties";
    /** beanshell mode. */
    public boolean bsh = false;
    /** jython mode. */
    public boolean jpy = false;
    /** name of simulation program. */
    public String sim = null;
    /** name of custom logger. */
    public String logger = null;
    /** command-line parameters. */
    public String[] args = new String[0];
  }

  /**
   * Parse command-line options.
   *
   * @param args command-line options
   * @return parsed command-line options
   * @throws CmdLineParser.OptionException invalid option
   */
  private static CommandLineOptions parseCommandLineOptions(String[] args)
    throws CmdLineParser.OptionException
  {
    // setup
    CommandLineOptions options = new CommandLineOptions();
    CmdLineParser parser = new CmdLineParser();
    CmdLineParser.Option opt_help = parser.addBooleanOption('h', "help");
    CmdLineParser.Option opt_version = parser.addBooleanOption('v', "version");
    CmdLineParser.Option opt_properties = parser.addStringOption('c', "conf");
    CmdLineParser.Option opt_bsh = parser.addBooleanOption('.', "bsh");
    CmdLineParser.Option opt_jpy = parser.addBooleanOption(',', "jpy");
    CmdLineParser.Option opt_logger = parser.addStringOption('l', "logger");

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
    if(parser.getOptionValue(opt_properties)!=null)
    {
      options.properties = (String)parser.getOptionValue(opt_properties);
    }
    if(parser.getOptionValue(opt_bsh)!=null)
    {
      options.bsh = true;
    }
    if(parser.getOptionValue(opt_jpy)!=null)
    {
      options.jpy = true;
    }
    if(parser.getOptionValue(opt_logger)!=null)
    {
      options.logger = (String)parser.getOptionValue(opt_logger);
    }
    String[] rest = parser.getRemainingArgs();
    if(rest.length>0) 
    {
      options.sim = rest[0];
      options.args = new String[rest.length-1];
      System.arraycopy(rest, 1, options.args, 0, options.args.length);
    }
    return options;
  }

  /**
   * SWANS entry point.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) 
  {
    try 
    {
      // command line
      final CommandLineOptions options = parseCommandLineOptions(args);
      // show usage
      if(options.help ||
          (options.sim==null && !options.bsh && !options.jpy))
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
      // load swans properties
      Properties config = null;
      try
      {
        File f = new File(options.properties);
        FileInputStream fin = new FileInputStream(f);
        config = new Properties();
        config.load(fin);
        fin.close();
      }
      catch(IOException e) 
      { 
      }
      // set up logging
      if(config!=null)
      {
        Logger.getRootLogger().setLevel(Level.OFF);
        PropertyConfigurator.configure(config);
      }
      else
      {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.OFF);
      }
      // install logger
      if(options.logger!=null)
      {
        try
        {
          Class loggerClass = Class.forName(options.logger);
          JistAPI.Logger logger = (JistAPI.Logger)loggerClass.newInstance();
          JistAPI.setLog(logger);
        }
        catch(ClassNotFoundException e)
        {
          System.out.println("Logger class not found: "+e.getMessage());
          return;
        }
        catch(InstantiationException e)
        {
          System.out.println("Could not instantiate logger class: "+e.getMessage());
          return;
        }
        catch(IllegalAccessException e)
        {
          System.out.println("Illegal access exception to logger class: "+e.getMessage());
          return;
        }
      }

      // install swans rewriter
      JistAPI.installRewrite(new Rewriter());
      // set simulation time seconds
      JistAPI.setSimUnits(Constants.SECOND, "s");
      // and start the show
      if(options.bsh)
      {
        String bshInit = 
          "import jist.swans.*;" +
          "import jist.swans.misc.*;"+
          "import jist.swans.field.*;"+
          "import jist.swans.radio.*;"+
          "import jist.swans.mac.*;"+
          "import jist.swans.net.*;"+
          "import jist.swans.route.*;"+
          "import jist.swans.trans.*;"+
          "import jist.swans.app.*;";
        JistAPI.run(JistAPI.RUN_BSH, options.sim, options.args, bshInit);
      }
      else if(options.jpy)
      {
        JistAPI.run(JistAPI.RUN_JPY, options.sim, options.args, null);
      }
      else if(options.sim!=null)
      {
        JistAPI.run(JistAPI.RUN_CLASS, options.sim, options.args, null);
      }
    }
    catch(CmdLineParser.OptionException e)
    {
      System.out.println("Error parsing command line: "+e.getMessage());
    }
  }

} // class: Main

