//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <EventLocation.java Tue 2004/04/06 11:23:33 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.lang.reflect.Method;

/**
 * Extends Event to include its originating location. Note: These events should
 * only be used for debugging purposes, and not for running full-speed
 * simulations, since generating the source location is slow.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: EventLocation.java,v 1.12 2004-04-06 16:07:43 barr Exp $
 * @since JIST1.0
 */

class EventLocation extends Event
{
  //////////////////////////////////////////////////
  // locals
  //

  /**
   * Filename where event was generated.
   */
  private String file;

  /**
   * Line where event was generated.
   */
  private int line;

  /**
   * Event that generated this event.
   */
  private Event source;
  
  //////////////////////////////////////////////////
  // initialization
  //

  /**
   * Create an empty Event with location information, used by 
   * EventPool.
   */
  public EventLocation()
  {
    compute();
  }

  /**
   * Create an initialized Event with location information.
   *
   * @param time simulation time at which the event should be processed
   * @param method method to be invoked for event processing
   * @param ref entity reference to receive event
   * @param args arguments to be passed to entity for processing
   */
  public EventLocation(long time, Method method, EntityRef ref, Object[] args)
  {
    super(time, method, ref, args);
    compute();
  }

  //////////////////////////////////////////////////
  // event origin
  //

  /**
   * Initialize event structure with information about where it originated.
   */
  public void compute()
  {
    if(Main.EVENT_LOCATION) computeLocation();
    if(Main.EVENT_TRACE) computeSource();
  }

  /**
   * Computes the originating source location (if possible) for an event. 
   * This is performed by throwing an exception, catching it, and inspecting
   * the stack. Consequently, it is slow (generating the exception, walking
   * the stack) and should be turned on only for debugging purposes. It may
   * be possible to use the rewriter to glean this information instead, but
   * this is so much simpler, and good enough.
   */
  private void computeLocation()
  {
    StackTraceElement[] stack = (new Throwable()).getStackTrace();
    for(int i=0; i<stack.length; i++)
    {
      StackTraceElement el = stack[i];
      if(el.getClassName().startsWith(Main.class.getPackage().getName())) continue;
      if(el.getClassName().startsWith("$Proxy")) continue;
      //if(el.getFileName()==null) continue;
      file = el.getFileName();
      line = el.getLineNumber();
      break;
    }
    if(file==null)
    {
      file = "unknown";
      line = -1;
    }
  }

  /**
   * Computes the originating source runtime events (if possible) for an event.
   * This is performed by looking at current event in the active controller.
   */
  private void computeSource()
  {
    // find source
    try
    {
      source = Controller.getActiveController().getCurrentEvent();
    }
    catch(NullPointerException e)
    {
      source = null;
    }
    // cut-off causality depth (for GC!)
    EventLocation ev = this;
    int depth = Main.EVENT_TRACE_DEPTH-1;
    if(depth<0) return;
    while(ev!=null && depth>0)
    {
      depth--;
      ev = ev.getSource() instanceof EventLocation
        ? (EventLocation)ev.getSource()
        : null;
    }
    if(ev!=null) ev.source = null;
  }

  //////////////////////////////////////////////////
  // public methods
  //

  /**
   * Return string representation of Event including its originating location.
   *
   * @return string representation of Event including originating location
   */
  public String toString()
  {
    return (file==null ? "" : ("{"+file+":"+line+"} "))+super.toString();
  }

  /**
   * Return the event that generated this event.
   *
   * @return event that generated this event.
   */
  public Event getSource()
  {
    return source;
  }

  /**
   * Display event causality trace on System error stream.
   */
  public static void printEventTrace()
  {
    Event ev = Controller.getActiveController().getCurrentEvent();
    do
    {
      System.err.println("- "+ev);
      ev = ev instanceof EventLocation
        ? ((EventLocation)ev).getSource()
        : null;
    } while(ev!=null);
  }

} // class: EventLocation

