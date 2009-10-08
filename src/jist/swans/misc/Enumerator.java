//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Enumerator.java Tue 2004/04/06 11:46:22 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.misc;

/** 
 * Class to generate a sequence; used usually for static initialization
 * of constants.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Enumerator.java,v 1.4 2004-04-06 16:07:48 barr Exp $
 * @since SWANS1.0
 */

public class Enumerator
{

  /**
   * internal sequence counter.
   */
  private static int num;

  /**
   * Reset counter value to given value.
   *
   * @param start value to reset counter to
   * @return the same value that the counter was reset to
   */
  public static int reset(int start)
  {
    num = start;
    return next();
  }

  /**
   * Zero counter.
   *
   * @return zero
   */
  public static int reset()
  {
    return reset(0);
  }

  /**
   * Return next value in sequence.
   *
   * @return next value in sequence
   */
  public static int next()
  {
    return num++;
  }

} // class Enumerator

