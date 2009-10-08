//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <JistException.java Tue 2004/04/06 11:23:55 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.io.*;
import java.lang.reflect.Method;

/** 
 * Encodes the RuntimeExceptions related to JiST system operations.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: JistException.java,v 1.17 2004-04-06 16:07:43 barr Exp $
 * @since JIST1.0
 */

class JistException extends Error
{

  //////////////////////////////////////////////////
  // locals
  //

  /**
   * Nested exception that caused Jist exception.
   */
  private Throwable nested=null;

  /**
   * Stack trace of nested exception that caused Jist exception.
   */
  private String nestedStackTrace="";
  
  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Initialize jist exception with error string
   * and nested exception.
   *
   * @param s error string
   * @param t nested exception
   */
  public JistException(String s, Throwable t) 
  {
    super(s);
    nested=t;
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    if(t!=null)
    {
      t.printStackTrace(pw);
      pw.close();
      nestedStackTrace = sw.getBuffer().toString();
    }
  }
  
  //////////////////////////////////////////////////
  // public methods
  //

  /**
   * Return exception error message.
   *
   * @return exception error message
   */
  public String getMessage() 
  {
    String s=super.getMessage();
    if(nested!=null)
      s+="\n"+nested.getMessage();
    return s;
  }
  
  /**
   * Show stack trace on System error stream.
   */
  public void printStackTrace() 
  {
    super.printStackTrace();
    if(nestedStackTrace!=null) System.out.println(nestedStackTrace);
  }

  /**
   * Return nested exception.
   *
   * @return nested exception
   */
  public Throwable getNested()
  {
    return nested;
  }

  //////////////////////////////////////////////////
  // end of simulation
  //

  /**
   * End of simulation Jist exception.
   */
  public static class JistSimulationEndException extends JistException
  {
    /**
     * Method stub field for ending simulation.
     */
    public static Method method_end;

    static
    {
      try
      {
        method_end = JistSimulationEndException.class.getDeclaredMethod(
            "end", 
            new Class[] { });
      }
      catch(NoSuchMethodException e)
      {
        throw new JistException("should not happen", e);
      }
    }

    /**
     * Create new exception object to denote end of simulation.
     */
    public JistSimulationEndException()
    {
      super("end of simulation", null);
    }

    /**
     * Ends simulation immediately when called. Usually call to this method is
     * scheduled via the JistAPI.end() method.
     */
    public static void end()
    {
      throw new JistSimulationEndException();
    }
  }

} // class: JistException

