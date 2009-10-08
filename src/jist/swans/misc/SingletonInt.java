//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <SingletonInt.java Tue 2004/04/06 11:46:50 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.misc;

/** 
 * Class to store a mutable primitive integer with in an Object (because Java
 * has primitives in the first place).
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: SingletonInt.java,v 1.4 2004-04-06 16:07:49 barr Exp $
 * @since SWANS1.0
 */
public class SingletonInt
{
  /** encapsulated integer. */
  public int i;

  /** Array of (commonly requested) Integer object for small integer primitives.
   */
  private static final Integer[] INTS = new Integer[10];
  static
  {
    for(int i=0; i<INTS.length; i++)
    {
      INTS[i] = new Integer(i);
    }
  }

  /**
   * Create a new singleton integer with value zero.
   */
  public SingletonInt()
  {
  }

  /**
   * Create a new singleton integer with given value.
   *
   * @param i initial singleton integer value
   */
  public SingletonInt(int i)
  {
    this.i = i;
  }

  /**
   * Return the Integer for a small integer, hopefully without a new allocation.
   *
   * @param i some integer to convert to an Integer
   * @return Integer object corresponding integer primitive
   */
  public static Integer getSmallInteger(int i)
  {
    if(i<INTS.length) return INTS[i];
    return new Integer(i);
  }

} // class: SingletonInt

