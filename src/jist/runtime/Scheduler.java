//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Scheduler.java Sun 2005/03/13 11:10:39 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

/** 
 * Event scheduler interface and implementations.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Scheduler.java,v 1.14 2005-03-13 16:11:54 barr Exp $
 * @since JIST1.0
 */

abstract class Scheduler
{

  /**
   * Return size of event queue.
   *
   * @return number of events in event queue
   */
  public abstract int size();

  /**
   * Return whether event queue is empty.
   *
   * @return whether event queue is empty
   */
  public abstract boolean isEmpty();

  /**
   * Insert event into event queue.
   *
   * @param ev event to insert
   */
  public abstract void insert(Event ev);

  /**
   * Remove first event in queue.
   *
   * @return first event (removed) from queue
   */
  public abstract Event removeFirst();

  /**
   * Peek at first event in queue.
   *
   * @return first event (still) in queue
   */
  public abstract Event peekFirst();

  /**
   * Clear all events in the queue.
   */
  public void clear()
  {
    while(!isEmpty()) removeFirst();
  }

  /** 
   * Implements an array-based heap of Events. In addition to the regular heap
   * functionality, there are methods for extracting elements other than the min.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @version $Id: Scheduler.java,v 1.14 2005-03-13 16:11:54 barr Exp $
   * @since JIST1.0
   */
  static final class Heap extends Scheduler
  {

    /** Initial size of internal heap array. */
    public static final int INIT_LENGTH = 10;

    /** Internal array of heap items. */
    private Event[] items;

    /** Number of elements in heap. */
    private int size;

    /** Collapse size. */
    private int halveSize;

    //////////////////////////////////////////////////
    // public interface
    //

    /**
     * Create a new, empty heap with given comparator.
     */
    public Heap()
    {
      items = new Event[INIT_LENGTH];
      size = 0;
    }

    /**
     * Find parent location in heap-ordered array.
     *
     * @param i location to find parent of
     * @return parent location
     */
    private int parent(int i)
    {
      return (i-1)/2;
    }

    /**
     * Find left child location in heap-ordered array.
     *
     * @param i location to find left child of
     * @return left child location
     */
    private int left(int i)
    {
      return i*2 + 1;
    }

    /**
     * Find right child location in heap-ordered array.
     * 
     * @param i location to find right child of
     * @return right child location
     */
    private int right(int i)
    {
      return i*2 + 2;
    }

    /**
     * Swap data in two array locations.
     *
     * @param i first location
     * @param j second location
     */
    private void swap(int i, int j)
    {
      Event temp = items[i];
      items[i] = items[j];
      items[j] = temp;
    }

    /** {@inheritDoc} */
    public void insert(Event ev)
    {
      try
      {
        // bubble the value to the right spot
        int i = size, parent;
        Event parentItem;
        while(i>0 && ev.time<(parentItem=items[parent=(i-1)/2]).time)
        {
          items[i] = parentItem;
          i = parent;
        }
        // insert item
        items[i]=ev;
        size++;
      }
      catch(ArrayIndexOutOfBoundsException e)
      {
        // check internal array size
        if(size!=items.length) throw e;
        // expand and reinsert
        doubleCapacity();
        insert(ev);
      }
    }

    /** {@inheritDoc} */
    public int size()
    {
      return size;
    }

    /** {@inheritDoc} */
    public boolean isEmpty()
    {
      return size==0;
    }

    /** {@inheritDoc} */
    public Event peekFirst()
    {
      // check heap size
      if(size==0)
      {
        throw new ArrayIndexOutOfBoundsException("heap is empty");
      }
      // return item
      return items[0];
    }

    /** {@inheritDoc} */
    public Event removeFirst()
    {
      // get item
      Event item = items[0];
      // fill in the gap
      size--;
      items[0] = items[size];
      items[size] = null;
      // reorder heap array
      heapify(0);
      if(size<halveSize) halveCapacity();
      return item;
    }

    /**
     * Return the element stored a given location in the internal
     * heap array and remove it from the heap.
     *
     * @param i index into the internal heap array
     * @return datum object removed from heap
     */
    public Event removeIndex(int i)
    {
      // check heap size
      if(size<i)
      {
        throw new ArrayIndexOutOfBoundsException("heap has less than "+i+" items");
      }
      // get item
      Event item = items[i];
      // fill in the gap
      size--;
      items[i] = items[size];
      items[size] = null;
      // reorder heap array
      heapify(i);
      if(size<halveSize) halveCapacity();
      return item;
    }

    /**
     * Establish heap order in internal array from given index.
     *
     * @param i index into internal heap array
     */
    private void heapify(int i)
    {
      while(true)
      {
        // left and right children
        int left = (i*2)+1;
        int right = left + 1;
        // find "largest" element
        int largest;
        if(left<size && items[left].time<items[i].time)
        {
          largest = left;
        }
        else
        {
          largest = i;
        }
        if(right<size && items[right].time<items[largest].time)
        {
          largest = right;
        }
        // swap and recurse, if necessary
        if(largest==i) return;
        Event tmp = items[i];
        items[i] = items[largest];
        items[largest] = tmp;
        i = largest;
      }
    }

    /**
     * Double the capacity of the internal array.
     */
    private void doubleCapacity()
    {
      try
      {
        Event[] items2 = new Event[items.length*2];
        System.arraycopy(items, 0, items2, 0, size);
        items = items2;
        halveSize = items.length/4;
      }
      catch(Exception e)
      {
        // should never occur
        e.printStackTrace();
      }
    }

    /**
     * Halve the capacity of the internal array.
     */
    private void halveCapacity()
    {
      try
      {
        int newLength = items.length/2;
        if(newLength<INIT_LENGTH)
        {
          halveSize = 0;
          return;
        }
        if(Main.ASSERT) Util.assertion(size<=newLength);
        Event[] items2 = new Event[newLength];
        System.arraycopy(items, 0, items2, 0, size);
        items = items2;
        halveSize = items.length/4;
      }
      catch(Exception e)
      {
        // should never occur
        e.printStackTrace();
      }
    }

  } // class: Heap



  /**
   * Implementation of calendar queue event scheduler.
   * See R.Brown. "Calendar queues: A fast O(1) priority queue 
   *  implementation for the simulation event set problem." 
   *  Comm. of ACM, 31(10):1220-1227, Oct 1988
   */

  // todo: calendar resizing
  static final class Calendar extends Scheduler
  {
    /**
     * Calendar days.
     */
    private Heap[] bins;

    /**
     * Event queue size.
     */
    private int size;
    
    /**
     * Last "day" where event was found.
     */
    private int lastBin;

    /**
     * Width of a single bin (length of a day).
     */
    private long binWidth; 

    /**
     * Maximum time threshold of lastBin.
     */
    private long binMax;

    /**
     * Create a calendar scheduler with given number of bins and bin width.
     *
     * @param width total width of all bins (length of a year)
     * @param nbins number of bins (number of days in a year)
     */
    public Calendar(long width, int nbins)
    {
      if(Main.ASSERT) Util.assertion(width>nbins);
      bins = new Heap[nbins];
      for(int i=0; i<bins.length; i++)
      {
        bins[i] = new Heap();
      }
      binWidth = width/nbins;
      size = 0;
      lastBin = 0;
      binMax = binWidth;
    }

    /**
     * Return mapped bin index for a given time.
     *
     * @param time event time
     * @return mapped bin index
     */
    private int getBin(long time)
    {
      return (int)((time / binWidth) % bins.length);
    }

    /**
     * Return bin with minimum time event -- linear scan.
     *
     * @return bin with minimum time event
     */
    private int minBin()
    {
      int min = -1;
      long minTime = Long.MAX_VALUE;
      for(int i=0; i<bins.length; i++)
      {
        Heap bin = bins[i];
        long time;
        if(!bin.isEmpty() && (time=bin.peekFirst().time)<minTime)
        {
          min = i;
          minTime = time;
        }
      }
      return min;
    }

    /**
     * Scan for and return next bin with an event with the next calendar year,
     * or use minBin (above) to find bin with minimum event using direct
     * search.
     *
     * @return mapped index of bin with next event
     */
    private int nextBin()
    {
      // fast case
      int i = lastBin;
      do
      {
        Heap bin = bins[i];
        if(!bin.isEmpty() && bin.peekFirst().time<binMax)
        {
          return lastBin = i;
        }
        i=(i+1)%bins.length;
        binMax+=binWidth;
      } while(i!=lastBin);
      // linear scan
      lastBin = minBin();
      binMax = bins[lastBin].peekFirst().time;
      binMax = binMax - binMax%binWidth + binWidth;
      return lastBin;
    }

    /** {@inheritDoc} */
    public void insert(Event e)
    {
      size++;
      bins[getBin(e.time)].insert(e);
    }

    /** {@inheritDoc} */
    public Event removeFirst()
    {
      if(Main.ASSERT) Util.assertion(size>0);
      size--;
      return bins[nextBin()].removeFirst();
    }

    /** {@inheritDoc} */
    public Event peekFirst()
    {
      return bins[nextBin()].peekFirst();
    }

    /** {@inheritDoc} */
    public int size()
    {
      return size;
    }

    /** {@inheritDoc} */
    public boolean isEmpty()
    {
      return size==0;
    }

  } // class: Calendar



  //////////////////////////////////////////////////
  // testing
  //

  /**
   * Simple scheduler test... Schedule random time events and then dequeue and check
   * that they are coming in order.
   *
   * @param s scheduler to test
   */
  private static void test(Scheduler s)
  {
    int num = 500000;
    java.util.Random rand = new java.util.Random();
    System.out.println("inserting "+num+" random elements.");
    for(int i=0; i<num; i++)
    {
      Event ev = new Event();
      ev.time = rand.nextLong();
      while(ev.time<0)
      {
        ev.time = rand.nextLong();
      }
      s.insert(ev);
    }
    System.out.println("checking order.");
    Event ev = s.removeFirst();
    long min = ev.time;
    while(!s.isEmpty())
    {
      ev = s.removeFirst();
      if(ev.time < min) throw new RuntimeException("incorrect ordering");
      min = ev.time;
    }
    System.out.println("seems to work!");
  }

  /**
   * Test various schedular implementations.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args)
  {
    System.out.println("testing heap scheduler implementation.");
    test(new Heap());
    System.out.println("testing heap calendar implementation.");
    test(new Calendar(Long.MAX_VALUE/10000, 1000));
  }

} // class: Scheduler

