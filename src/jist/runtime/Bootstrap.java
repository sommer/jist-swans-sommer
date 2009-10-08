//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Bootstrap.java Tue 2004/04/06 11:22:28 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.lang.reflect.*;
import java.io.*;
import java.rmi.*;

/** 
 * Bootstrap entities to start simulations.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Bootstrap.java,v 1.17 2004-04-06 16:07:43 barr Exp $
 * @since JIST1.0
 */

public abstract class Bootstrap extends Entity.Empty
{
  //////////////////////////////////////////////////
  // constants
  //

  /**
   * Method stub for event that starts the simulation.
   */
  public static Method method_startSimulation;

  static
  {
    try
    {
      method_startSimulation = Bootstrap.class.getDeclaredMethod("startSimulation",
          new Class[] { Rewriter.class, String.class, String[].class, Object.class });
    }
    catch(NoSuchMethodException e)
    {
      throw new JistException("should not happen", e);
    }
  }

  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Create a new bootstrap event; called only by sub-classes.
   */
  protected Bootstrap()
  {
    // do not register with active controller, since it doesn't exist yet
    super(false);
  }

  /**
   * Create and enqueue a bootstrap event.
   *
   * @param type bootstrap event type
   * @param controller where to register the bootstrap entity and enqueue event
   * @param name script or application name
   * @param args command-line arguments
   * @param properties object passed through to specific bootstrap implementation
   * @throws RemoteException distributed simulation error
   */
  public static void create(int type, Controller controller, String name, String[] args, Object properties) throws RemoteException
  {
    ClassLoader cll = controller.getClassLoader();
    Entity simEntity;
    switch(type)
    {
      case JistAPI.RUN_CLASS:
        simEntity = new JavaMain();
        break;
      case JistAPI.RUN_BSH:
        simEntity = new Bsh();
        break;
      case JistAPI.RUN_JPY:
        simEntity = new Jython();
        break;
      default:
        throw new RuntimeException("invalid simulation driver type: "+type);
    }
    simEntity._jistMethod_Set__ref(controller.registerEntity(simEntity));
    controller.addEvent(new Event(controller.getSimulationTime(), 
          Bootstrap.method_startSimulation, 
          simEntity._jistMethod_Get__ref(), new Object[] { cll, name, args, properties }));
  }

  /**
   * Entity method (invoked in simulation time) to perform bootstrap event.
   *
   * @param rewriter rewriting class loader instance
   * @param name script or application name
   * @param args command-line arguments
   * @param properties object passed through to specific bootstrap implementation
   */
  public abstract void startSimulation(Rewriter rewriter, String name, String[] args, Object properties);

  //////////////////////////////////////////////////
  // Static method invoker
  //

  /**
   * Shell class to perform static invocations.
   */
  static class StaticRunnableCaller
  {
    /** Run method stub. */
    public static Method method_run;
      
    static
    {
      try
      {
        method_run = StaticRunnableCaller.class.getDeclaredMethod(
            "run",
            new Class[] { Runnable.class });
      }
      catch(NoSuchMethodException e)
      {
        throw new JistException("should not happen", e);
      }
    }

    /** 
     * Run the runnable.
     *
     * @param r given runnable
     */
    public static void run(Runnable r)
    {
      r.run();
    }
  } // class: StaticRunnableCaller

  //////////////////////////////////////////////////
  // Java class bootstrap entity
  //

  /** 
   * A special-purpose Entity used to bootstrap the simulation. It merely calls
   * the usual static main() method of the simulation application.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */

  static class JavaMain extends Bootstrap
  {
    /** {@inheritDoc} */
    public void startSimulation(Rewriter rewriter, String classname, String[] args, Object properties) 
    {
      try
      {
        Class c = Class.forName(classname, true, rewriter);
        Method method = c.getDeclaredMethod("main", new Class[] { String[].class });
        method.invoke(null, new Object[] { args });
      }
      catch(ClassNotFoundException e)
      {
        System.err.println("JiST Java bootstrap - Class not found: "+e.getMessage());
      }
      catch(InvocationTargetException e)
      {
        Throwable target = e.getTargetException();
        if(target instanceof VirtualMachineError) throw (VirtualMachineError)target;
        throw new JistException("application exception in bootstrap event", target);
      }
      catch(Exception e)
      {
        throw new JistException("general failure", e);
      }
    }
  } // class: JavaMain


  //////////////////////////////////////////////////
  // BeanShell script bootstrap entity
  //

  /** 
   * A special-purpose Entity used to bootstrap the simulation. It processes
   * a BeanShell script.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */

  static class Bsh extends Bootstrap
  {
    /** {@inheritDoc} */
    public void startSimulation(Rewriter rewriter, String scriptname, String[] args, Object properties)
    {
      try
      {
        bsh.Interpreter interp = null;
        if(scriptname==null)
        {
          interp = new bsh.Interpreter(new InputStreamReader(System.in), System.out, System.err, true);
        }
        else
        {
          // check that file exists
          File script = new File(scriptname);
          if(!script.exists())
          {
            System.err.println("JiST BeanShell bootstrap - File not found: "+scriptname);
            return;
          }
          scriptname = script.getPath();
          interp = new bsh.Interpreter();
        }
        // configure
        interp.setStrictJava(true);
        interp.setExitOnEOF(false);
        if(scriptname==null)
        {
          interp.eval("void printBanner() { }");
          Main.showVersion(); 
          System.out.println("*** EOF will start simulation loop. ***");
          interp.eval("String getBshPrompt() { return \"jist-bsh> \"; }");
        }
        // set class loader
        interp.setClassLoader(rewriter);
        // set arguments
        String[] args2 = new String[args.length+1];
        args2[0] = scriptname;
        System.arraycopy(args, 0, args2, 1, args.length);
        interp.set("bsh.args", args2);
        // make environment nicer
        interp.eval("import jist.runtime.JistAPI;");
        if(properties!=null)
        {
          interp.eval((String)properties);
        }
        if(scriptname==null)
        {
          // go interactive
          interp.run();
          System.out.println();
        }
        else
        {
          // process the script
          interp.source(scriptname);
        }
      }
      catch(Exception e)
      {
        throw new JistException("exception in beanshell bootstrap", e);
      }
    } // function: startSimulation

  } // class: Bsh



  //////////////////////////////////////////////////
  // Jython script bootstrap entity
  //

  /** 
   * A special-purpose Entity used to bootstrap the simulation. It processes
   * a Jython script.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */

  static class Jython extends Bootstrap
  {
    /** {@inheritDoc} */
    public void startSimulation(Rewriter rewriter, String scriptname, String[] args, Object properties)
    {
      try
      {
        org.python.util.PythonInterpreter interp = null;
        if(scriptname==null)
        {
          try
          {
            // try get a readline-aware console
            interp = new org.python.util.ReadlineConsole();
          }
          catch(NoClassDefFoundError e)
          {
            interp = new org.python.util.InteractiveConsole();
          }
        }
        else
        {
          interp = new org.python.util.PythonInterpreter();
        }
        // set prompts
        interp.exec("import sys; sys.ps1='jist-jpy> '; sys.ps2='      ... '");
        // set sys.argv
        org.python.core.PyList pyargs = new org.python.core.PyList();
        pyargs.append(new org.python.core.PyString(scriptname==null ? "" : scriptname));
        if (args!=null) 
        {
          for(int i=0; i<args.length; i++) 
          {
            pyargs.append(new org.python.core.PyString(args[i]));
          }
        }
        interp.get("sys").__setattr__("argv", pyargs);
        // set classloader
        org.python.core.Py.getSystemState().setClassLoader(rewriter);
        // create nicer environment
        interp.exec("import jist.runtime.JistAPI as JistAPI;");
        if(properties!=null)
        {
          interp.exec((String)properties);
        }
        if(scriptname==null)
        {
          // go interactive
          Main.showVersion(); 
          System.out.println("*** EOF will start simulation loop. ***");
          ((org.python.util.InteractiveConsole)interp).interact(null);
        }
        else
        {
          // process script
          interp.execfile(scriptname);
        }
        interp.cleanup();
      }
      catch(Exception e)
      {
        throw new JistException("exception in jython bootstrap", e);
      }
    } // function: startSimulation
  } // class: Jython

} // class: Bootstrap

