//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Pool.java Tue 2004/04/06 11:24:22 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

/** 
 * Contains the various classes that pool objects for performance.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Pool.java,v 1.6 2004-04-06 16:07:43 barr Exp $
 * @since JIST1.0
 */

public abstract class Pool
{

  /** 
   * Implements an object pool for the massively used Object array objects. 
   * Note that, for performance reasons, this class is intentionally
   * NOT thread-safe.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */

  public static class ArrayPool extends Pool
  {

    /**
     * Size of Object arrays pooled.
     */
    private final int size;

    /**
     * Private array pool of Object arrays of size 'size'.
     */
    private final Object[][] pool;

    /**
     * Index into array pool.
     */
    private int count;

    /**
     * Create new object array pool of given size, for
     * array of given size.
     *
     * @param arraysize object array size pooled
     * @param poolsize size of object array pool
     */
    public ArrayPool(int arraysize, int poolsize)
    {
      size = arraysize;
      pool = new Object[size][];
      for(count=0; count<pool.length; count++)
      {
        pool[count] = new Object[size];
      }
    }

    /**
     * Return an array from the pool, 
     * or create a new one if pool is empty.
     *
     * @return initialized Object array
     */
    public Object[] get()
    {
      if(count>0)
      {
        return pool[--count];
      }
      else
      {
        return new Object[size];
      }
    }

    /**
     * Place a free (otherwise unused) array object back in the pool, if there
     * is space. Remember to any clear reference from the array to allow 
     * garbage collection to occur.
     *
     * @param o reference to free array object
     */
    public void put(Object[] o)
    {
      if(count<pool.length)
      {
        pool[count++] = o;
      }
    }

    /**
     * Small program to test performance of ArrayPool implementation.
     *
     * @param args command line parameters
     */
    public static void main(String[] args)
    {
      // initialize
      final int size=100, arraysize=1;
      long num=10000000;
      Object[][] stuff = new Object[size][];
      long startTime, endTime;
      for(int i=0; i<size; i++)
      {
        stuff[i] = new Object[arraysize];
      }
      for(int repeat=0; repeat<5; repeat++)
      {
        // regular allocation
        startTime = System.currentTimeMillis();
        for(int i=0; i<num; i++)
        {
          stuff[i%size] = new Object[arraysize];
        }
        endTime = System.currentTimeMillis();
        System.out.println(" new: "+((endTime-startTime)/1000.0));
        // pooled
        Object lock = new Object();
        ArrayPool ap = new ArrayPool(arraysize, 100);
        startTime = System.currentTimeMillis();
        for(int i=0; i<num; i++)
        {
          Thread.currentThread();
          int index=i%size;
          ap.put(stuff[index]);
          stuff[index] = ap.get();
        }
        endTime = System.currentTimeMillis();
        System.out.println("pool: "+((endTime-startTime)/1000.0));
      }
    } // function: main

  } // class: ArrayPool


  /** 
   * Implements an object pool for the massively used Continuation objects. 
   * Note that, for performance reasons, this class is intentionally
   * NOT thread-safe.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */

  public static class ContinuationPool extends Pool
  {

    /**
     * Private object pool.
     */
    private final Event.Continuation[] pool;

    /**
     * Index into object pool.
     */
    private int count;

    /**
     * Create new object pool of fixed given size.
     *
     * @param size object pool size
     */
    public ContinuationPool(int size)
    {
      pool = new Event.Continuation[size];
      for(count=0; count<pool.length; count++)
      {
        pool[count] = new Event.Continuation();
      }
    }

    /**
     * Return a Continuation from pool, or create new one if pool empty.
     *
     * @return uninitialized Continuation object
     */
    public Event.Continuation get()
    {
      if(count>0)
      {
        return pool[--count];
      }
      else
      {
        return new Event.Continuation();
      }
    }

    /**
     * Place a free (otherwise unused) Continuation object back in the pool,
     * if there is space.
     *
     * @param cont reference to a free Continuation object
     */
    public void put(Event.Continuation cont)
    {
      if(count<pool.length)
      {
        pool[count++] = cont;
      }
    }

  } // class: ContinuationPool


  /** 
   * Implements an object pool for the massively used Event objects. 
   * Note that, for performance reasons, this class is intentionally
   * NOT thread-safe.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */

  public static class EventPool extends Pool
  {

    /**
     * Private object pool.
     */
    private final Event[] pool;

    /**
     * Index into object pool.
     */
    private int count;

    /**
     * Create new object pool of fixed given size.
     *
     * @param size object pool size
     */
    public EventPool(int size)
    {
      pool = new Event[size];
      for(count=0; count<pool.length; count++)
      {
        pool[count] = Main.EVENT_LOCATION || Main.EVENT_TRACE ? new EventLocation() : new Event();
      }
    }

    /**
     * Return an event from pool, or create new one if pool empty.
     *
     * @return uninitialized Event object
     */
    public Event get()
    {
      if(count>0)
      {
        if(Main.EVENT_LOCATION || Main.EVENT_TRACE)
        {
          EventLocation ev = (EventLocation)pool[--count];
          ev.compute();
          return ev;
        }
        else
        {
          return pool[--count];
        }
      }
      else
      {
        return Main.EVENT_LOCATION || Main.EVENT_TRACE ? new EventLocation() : new Event();
      }
    }

    /**
     * Place a free (otherwise unused) Event object back in the pool,
     * if there is space.
     *
     * @param e reference to free event object
     */
    public void put(Event e)
    {
      if(count<pool.length)
      {
        pool[count++]=e;
      }
    }

  } // class: EventPool

} // class: Pool

