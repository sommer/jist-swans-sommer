//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Event.java Fri 2004/07/23 20:04:58 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.util.Comparator;
import java.rmi.RemoteException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** 
 * Stores the details of an entity invocation for processing at 
 * the appropriate simulation time.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Event.java,v 1.34 2004-07-27 15:46:47 barr Exp $
 * @since JIST1.0
 */
public class Event 
{
  //////////////////////////////////////////////////
  // locals
  //

  /**
   * Simulation time at which the event should be processed.
   */
  public long time;

  /**
   * Reference to entity that will receive the event.
   */
  public EntityRef ref;
  
  /**
   * Method to be invoked for event processing.
   */
  public Method method;

  /**
   * Arguments to be passed to entity method for processing.
   */
  public Object[] args;

  /**
   * Any event continuation information. Used only for continuation 
   * calls and callbacks - blocking events.
   */
  public Continuation cont;


  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Simulation event empty constructor, used for pooling.
   */
  public Event()
  {
  }

  /**
   * Simulation event constructor.
   *
   * @param time simulation time at which the event should be processed
   * @param method method to be invoked for event processing
   * @param ref entity reference to receive event
   * @param args arguments to be passed to entity for processing
   */
  public Event(long time, Method method, EntityRef ref, Object[] args)
  {
    set(time, method, ref, args);
  }

  /**
   * Copy constructor.
   *
   * @param ev event to copy
   */
  public Event(Event ev)
  {
    this(ev.time, ev.method, ev.ref, 
        ev.args); // possibly not safe to reference same args
    this.cont = ev.cont; // possibly not safe to reference same continuation
  }

  //////////////////////////////////////////////////
  // public methods
  //

  /**
   * Simulation event assignment, used for pooling.
   *
   * @param time simulation time at which the event should be processed
   * @param method method to be invoked for event processing
   * @param ref entity reference to receive event
   * @param args arguments to be passed to entity for processing
   */
  public void set(long time, Method method, EntityRef ref, Object[] args)
  {
    this.time = time;
    this.ref = ref;
    this.method = method;
    this.args = args;
  }

  /**
   * Return event type as string.
   *
   * @return event type string
   */
  public String getTypeString()
  {
    return cont==null ? "EVNT" : cont.state==null ? "CALL" : "CBCK";
  }

  /**
   * Return event time as string
   *
   * @return event time as string
   */
  public String getTimeString()
  {
    return Long.toString(time);
  }

  /**
   * Return event target method modifiers as string. public, abstract and final
   * are not interesting. Return only whether method is static or not.
   *
   * @return event modifiers as string
   */
  public String getModifiersString()
  {
    StringBuffer s = new StringBuffer();
    // public: all events are public
    // abstract: scheduled events can not be abstract
    // final: not important, omit
    // native: not important, omit
    if(Modifier.isStatic(method.getModifiers()))
    {
      s.append("static ");
    }
    return s.toString().trim();
  }

  /**
   * Return event target class as string. Note that the unqualified class name
   * is returned.
   *
   * @return unqualified event target class name.
   */
  public String getClassString()
  {
    try
    {
      return Util.unqualifiedName(
          ref.getController().getEntityClass(ref.getIndex()).getName());
    }
    catch(RemoteException e)
    {
      return "RMI FAILURE";
    }
    catch(NullPointerException e)
    {
      return Util.unqualifiedName(method.getDeclaringClass().getName());
    }
  }

  /**
   * Return event target method as string.
   *
   * @return event target method name.
   */
  public String getMethodString()
  {
    if(method==null) return "NOOP";
    return method.getName();
  }

  /**
   * Return event arguments as string
   *
   * @return comma-separated event arguments as string
   */
  public String getArgsString()
  {
    try
    {
      if(args==null) return "";
      StringBuffer s = new StringBuffer();
      for(int i=0; i<args.length; i++)
      {
        if(i>0) s.append(", ");
        s.append(args[i]==null ? "null" : Controller.toString(args[i]));
      }
      return s.toString();
    }
    catch(RemoteException e)
    {
      return "RMI FAILURE";
    }
  }

  /**
   * Return event continuation contents. Note that only a summary of
   * continuation information is returned.
   *
   * @return summary of event continuation payload
   */
  public String getContinuationString()
  {
    if(cont==null) return "";
    StringBuffer s = new StringBuffer();
    if(cont.state!=null) s.append("state ");
    if(cont.result!=null) s.append("result ");
    if(cont.caller!=null) s.append("caller ");
    return s.toString().trim();
  }

  /**
   * Return event target entity as string.
   *
   * @return event target entity as string.
   */
  public String getEntityString()
  {
    try
    {
      return Controller.toString(ref);
    }
    catch(RemoteException e)
    {
      return "RMI FAILURE";
    }
  }

  /**
    * Return string representation of Event.
    *
    * @return string representation
    */
  // overrides java.lang.Object
  public String toString()
  {
    if(method==null) return "NOOP EVENT";
    StringBuffer s = new StringBuffer();
    s.append("t="+getTimeString()+": ");
    s.append(getTypeString()+" ");
    s.append(getClassString()+"."+getMethodString());
    s.append("("+getArgsString()+") cont=[ "
        +getContinuationString()+" ] on "
        +getEntityString());
    return s.toString();
  } // function: toString


  /**
   * Test object equality.
   *
   * @param o object to perform equality test
   * @return whether object is equal to event
   */
  public boolean equals(Object o)
  {
    if(o==null) return false;
    if(!(o instanceof Event)) return false;
    Event e = (Event)o;
    if(time!=e.time) return false;
    if(ref==null)
    {
      if (e.ref!=null) return false;
    }
    else if(!ref.equals(e.ref)) return false;
    if(method==null)
    {
      if (e.method!=null) return false;
    }
    else if(!method.equals(e.method)) return false;
    if(args==null)
    {
      if(e.args!=null) return false;
    }
    else
    {
      if(args.length!=e.args.length) return false;
      for(int i=0; i<args.length; i++)
      {
        if(args[i]==null)
        {
          if(e.args[i]!=null) return false;
        }
        else if(!args[i].equals(e.args[i])) return false;
      }
    }
    if(cont==null)
    {
      if(e.cont!=null) return false;
    }
    else if(!cont.equals(e.cont)) return false;
    return true;
  }

  /**
   * Return entity reference hashcode.
   *
   * @return entity reference hashcode
   */
  public int hashCode()
  {
    int hash = (int)time%Integer.MAX_VALUE + ref.hashCode() + method.hashCode();
    if(args!=null)
    {
      hash += args.length;
      for(int i=0; i<args.length; i++)
      {
        if(args[i]!=null) hash+=args[i].hashCode();
      }
    }
    // possibly also add continuation here
    return hash;
  }

  //////////////////////////////////////////////////
  // Event comparison
  //

  /**
   * Comparator used to compare two events in terms of their
   * timestamp. Used by the event queue to order the events 
   * in order of their simulation time.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */
  public static class EventComparator implements Comparator
  {
    /**
     * Compare two Event objects in time. Note that this does not conform to
     * the usual interface: only returns -1 and 1 (1 is returned in place of
     * the usual 0 - performance).
     *
     * @param o1 first event
     * @param o2 second event
     * @return o1.time before e2.time ? -1 : 1
     */
    public int compare(Object o1, Object o2)
    {
      Event e1 = (Event)o1, e2 = (Event)o2;
      return e1.time < e2.time ? -1 : 1;
    }
  }


  //////////////////////////////////////////////////
  // Continuations
  //

  /** 
   * Stores a continuation information for blocking event calls.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @version $Id: Event.java,v 1.34 2004-07-27 15:46:47 barr Exp $
   * @since JIST1.0
   */
  public static class Continuation
  {

    //////////////////////////////////////////////////
    // locals
    //

    /** 
     * State of the event execution before blocking call. 
     */
    public ContinuationFrame state;

    /** 
     * Returning result of the blocking call.
     */
    public Object result;

    /**
     * Return exception of the blocking call.
     */
    public Throwable exception;

    /** 
     * Caller event information.
     */
    public Event caller;

    //////////////////////////////////////////////////
    // public methods
    //

    /**
     * Return string representation of Contination information.
     *
     * @return string representation
     */
    // override: java.lang.Object
    public String toString()
    {
      StringBuffer s = new StringBuffer();
      s.append("result=");
      s.append(result);
      s.append(", state=\n");
      ContinuationFrame curr = state;
      while(curr!=null)
      {
        s.append("   ");
        s.append(curr.toString());
        s.append("\n");
        curr = curr.next;
      }
      return s.toString();
    } // function: toString

  } // class: Continuation


  /** 
   * Stores a single stack frame for event calls with continuation.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */

  public static class ContinuationFrame
  {

    //////////////////////////////////////////////////
    // constants
    //

    /** 
     * frame at bottom of stack .
     */
    public static final ContinuationFrame BASE = new ContinuationFrame();

    /** 
     * field stub for pointer to next stack frame.
     */
    public static Field field_next;

    /** 
     * field stub for program "counter" location.
     */
    public static Field field_pc;

    static
    {
      try
      {
        field_next = ContinuationFrame.class.getDeclaredField("next");
        field_pc = ContinuationFrame.class.getDeclaredField("pc");
      }
      catch(NoSuchFieldException e)
      {
        throw new JistException("should not happen", e);
      }
    }

    //////////////////////////////////////////////////
    // locals
    //

    /** 
     * pointer to next stack frame.
     */
    public ContinuationFrame next;

    /** 
     * program "counter" location where frame information captured.
     */
    public int pc;

    /**
     * Test whether to events are equal.
     *
     * @param o object to perform equality test
     * @return whether object is equal to event
     */
    public boolean equals(Object o)
    {
      if(o==null) return false;
      if(!(o instanceof ContinuationFrame)) return false;
      ContinuationFrame cf = (ContinuationFrame)o;
      if(pc!=cf.pc) return false;
      if(next==null && cf.next!=null) return false;
      return next.equals(cf.next);
    }

  } // class: ContinuationFrame

} // class: Event
