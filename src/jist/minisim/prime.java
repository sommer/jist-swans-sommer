//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <prime.java Tue 2004/04/06 11:28:43 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.minisim;

import java.io.*;

import jargs.gnu.*; // Download from: http://jargs.sourceforge.net

/**
 * Pulls in all the classes that it find below a given classpath in order to
 * get rewriter statistics.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: prime.java,v 1.4 2004-04-06 16:07:42 barr Exp $
 * @since JIST1.0
 */

public class prime
{
  //////////////////////////////////////////////////
  // Constants
  //

  /** benchmark version. */
  public static final String VERSION = "0.1";


  //////////////////////////////////////////////////
  // Static command-line helper methods
  //

  /**
   * Print benchmark version information.
   */
  private static void showVersion()
  {
    System.out.println("JiST primer v"+VERSION+", Java in Simulation Time Project.");
    System.out.println("Rimon Barr <barr+jist@cs.cornell.edu>, Cornell University.");
    System.out.println();
  }

  /**
   * Print benchmark command-line syntax.
   */
  private static void showUsage() 
  {
    System.out.println("Usage: prime [-d <dir>] [-f <filter>]");
    System.out.println("       prime -v | -h");
    System.out.println();
    System.out.println("  -h, --help         display this help information");
    System.out.println("  -v, --version      display version information");
    System.out.println("  -d, --dir          base classfile directory ['.']");
    System.out.println("  -f, --filter       prefix of classes to load");
    System.out.println();
  }

  /** Parsed command-line options. */
  private static class CmdlineOpts
  {
    // defaults
    /** print help. */
    public boolean help = false;
    /** print version. */
    public boolean version = false;
    /** directory to recursively scan. */
    public String dir = ".";
    /** package prefix selection filter. */
    public String filter = "";
  }

  /** parsed command-line options. */
  private static CmdlineOpts options;
  /** number of classes loaded. */
  private static int count = 0;

  /**
   * Parse command-line options.
   *
   * @param args command-line parameters
   * @return parsed command-line options
   * @throws CmdLineParser.OptionException invalid option encountered
   */
  private static CmdlineOpts parseCommandLineOptions(String[] args)
    throws CmdLineParser.OptionException
  {
    // setup
    CmdlineOpts options = new CmdlineOpts();
    CmdLineParser parser = new CmdLineParser();
    CmdLineParser.Option opt_help = parser.addBooleanOption('h', "help");
    CmdLineParser.Option opt_version = parser.addBooleanOption('v', "version");
    CmdLineParser.Option opt_dir = parser.addStringOption('d', "dir");
    CmdLineParser.Option opt_filter = parser.addStringOption('f', "filter");

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
    if(parser.getOptionValue(opt_dir)!=null)
    {
      options.dir = ((String)parser.getOptionValue(opt_dir));
    }
    if(parser.getOptionValue(opt_filter)!=null)
    {
      options.filter = ((String)parser.getOptionValue(opt_filter));
    }
    String[] rest = parser.getRemainingArgs();
    return options;
  }


  //////////////////////////////////////////////////
  // main
  //

  /**
   * A filename filter to return only directories.
   */
  public static class ClassFileFilter implements FilenameFilter
  {
    /** {@inheritDoc} */
    public boolean accept(File dir, String name)
    {
      return name.endsWith(".class") &&
        (new File(dir, name)).isFile();
    }
  }

  /**
   * A filename filter to return only class files.
   */
  public static class DirFileFilter implements FilenameFilter
  {
    /** {@inheritDoc} */
    public boolean accept(File dir, String name)
    {
      return (new File(dir, name)).isDirectory();
    }
  }

  /** class file list filter. */
  public static final ClassFileFilter classFileFilter = new ClassFileFilter();
  /** directory file list filter. */
  public static final DirFileFilter dirFileFilter = new DirFileFilter();

  /** 
   * Process a directory of class files.
   *
   * @param dir directory path
   * @param base relative path from package root directory
   */
  public static void loadDir(File dir, String base)
  {
    // recurse
    File[] dirs = dir.listFiles(dirFileFilter);
    for(int i=0; i<dirs.length; i++)
    {
      if(dir.getAbsolutePath().startsWith(dirs[i].getAbsolutePath()))
      {
        continue;
      }
      loadDir(dirs[i], base+File.separatorChar+dirs[i].getName());
    }
    // process classes
    File[] classFiles = dir.listFiles(classFileFilter);
    for(int i=0; i<classFiles.length; i++)
    {
        String classname = base+File.separatorChar+classFiles[i].getName();
        classname = classname.substring(2, classname.length()-6);
        classname = classname.replace(File.separatorChar, '.');
        if(!classname.startsWith(options.filter)) continue;
        try
        {
          Class c = Class.forName(classname);
          System.out.println("Load success: "+classname);
          count++;
        }
        catch(ClassNotFoundException e)
        {
          System.out.println("Load failure: "+classname);
          System.exit(1);
        }
        catch(Exception e)
        {
          System.out.println("REWRITER FAILURE: "+classname);
          System.exit(1);
        }
    }
  }

  /**
   * Benchmark entry point: recursively load and rewrite classes.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    try 
    {
      // command line
      options = parseCommandLineOptions(args);
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
      // run benchmark
      loadDir(new File(options.dir), ".");
      System.out.println("Total classes rewritten: "+count);
    }
    catch(CmdLineParser.OptionException e)
    {
      System.out.println("Error parsing command line: "+e.getMessage());
    }
  }

}
