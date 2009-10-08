//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <EntityRef.java Sun 2005/03/13 11:10:16 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.rmi.RemoteException;

/** 
 * Stores a reference to a (possibly remote) Entity object. A reference
 * consists of a serialized reference to a Controller and an index within that
 * Controller.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: EntityRef.java,v 1.25 2005-03-13 16:11:54 barr Exp $
 * @since JIST1.0
 */

public class EntityRef implements InvocationHandler
{
  /**
   * NULL reference constant.
   */
  public static final EntityRef NULL = new EntityRefDist(null, -1);

  /**
   * Entity index within Controller.
   */
  private final int index;

  /**
   * Initialise a new entity reference with given
   * Controller and Entity IDs.
   *
   * @param index entity ID
   */
  public EntityRef(int index)
  {
    this.index = index;
  }

  /**
   * Return entity reference hashcode.
   *
   * @return entity reference hashcode
   */
  public int hashCode()
  {
    return index;
  }

  /**
   * Test object equality.
   *
   * @param o object to test equality
   * @return object equality
   */
  public boolean equals(Object o)
  {
    if(o==null) return false;
    if(!(o instanceof EntityRef)) return false;
    EntityRef er = (EntityRef)o;
    if(index!=er.index) return false;
    return true;
  }

  /**
   * Return controller of referenced entity.
   *
   * @return controller of referenced entity
   */
  public ControllerRemote getController()
  {
    if(Main.SINGLE_CONTROLLER)
    {
      return Controller.activeController;
    }
    else
    {
      throw new RuntimeException("multiple controllers");
    }
  }

  /**
   * Return index of referenced entity.
   *
   * @return index of referenced entity
   */
  public int getIndex()
  {
    return index;
  }

  /**
   * Return toString of referenced entity.
   *
   * @return toString of referenced entity
   */
  public String toString()
  {
    try
    {
      return "EntityRef:"+getController().toStringEntity(getIndex());
    }
    catch(java.rmi.RemoteException e)
    {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return class of referenced entity.
   *
   * @return class of referenced entity
   */
  public Class getClassRef()
  {
    try
    {
      return getController().getEntityClass(getIndex());
    }
    catch(java.rmi.RemoteException e)
    {
      throw new RuntimeException(e);
    }
  }

  //////////////////////////////////////////////////
  // proxy entities
  //

  /** boolean type for null return. */
  private static final Boolean   RET_BOOLEAN   = new Boolean(false);
  /** byte type for null return. */
  private static final Byte      RET_BYTE      = new Byte((byte)0);
  /** char type for null return. */
  private static final Character RET_CHARACTER = new Character((char)0);
  /** double type for null return. */
  private static final Double    RET_DOUBLE    = new Double((double)0);
  /** float type for null return. */
  private static final Float     RET_FLOAT     = new Float((float)0);
  /** int type for null return. */
  private static final Integer   RET_INTEGER   = new Integer(0);
  /** long type for null return. */
  private static final Long      RET_LONG      = new Long(0);
  /** short type for null return. */
  private static final Short     RET_SHORT     = new Short((short)0);

  /**
   * Called whenever a proxy entity reference is invoked. Schedules the call
   * at the appropriate Controller.
   *
   * @param proxy proxy entity reference object whose method was invoked
   * @param method method invoked on entity reference object
   * @param args arguments of the method invocation
   * @return result of blocking event; null return for non-blocking events
   * @throws Throwable whatever was thrown by blocking events; never for non-blocking events
   */
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
  {
    try
    {
      if(Rewriter.isBlockingRuntimeProxy(method))
        // todo: make Object methods blocking
        //|| method.getDeclaringClass()==Object.class)
      {
        return blockingInvoke(proxy, method, args);
      }
      else
      {
        // schedule a simulation event
        if(Main.SINGLE_CONTROLLER)
        {
          Controller.activeController.addEvent(method, this, args);
        }
        else
        {
          getController().addEvent(method, this, args);
        }
        return null;
      }
    }
    catch(RemoteException e)
    {
      throw new JistException("distributed simulation failure", e);
    }
  }

  /**
   * Helper method: called whenever a BLOCKING method on proxy entity reference
   * is invoked. Schedules the call at the appropriate Controller.
   *
   * @param proxy proxy entity reference object whose method was invoked
   * @param method method invoked on entity reference object
   * @param args arguments of the method invocation
   * @return result of blocking event
   * @throws Throwable whatever was thrown by blocking events
   */
  private Object blockingInvoke(Object proxy, Method method, Object[] args) throws Throwable
  {
    Controller c = Controller.getActiveController();
    if(c.isModeRestoreInst())
    {
      // restore complete
      if(Controller.log.isDebugEnabled())
      {
        Controller.log.debug("restored event state!");
      }
      // return callback result
      return c.clearRestoreState();
    }
    else
    {
      // calling blocking method
      c.registerCallEvent(method, this, args);
      // todo: darn Java; this junk slows down proxies
      Class ret = method.getReturnType();
      if(ret==Void.TYPE)
      {
        return null;
      }
      else if(ret.isPrimitive())
      {
        String retName = ret.getName();
        switch(retName.charAt(0))
        {
          case 'b':
            switch(retName.charAt(1))
            {
              case 'o': return RET_BOOLEAN;
              case 'y': return RET_BYTE;
              default: throw new RuntimeException("unknown return type");
            }
          case 'c': return RET_CHARACTER;
          case 'd': return RET_DOUBLE;
          case 'f': return RET_FLOAT;
          case 'i': return RET_INTEGER;
          case 'l': return RET_LONG;
          case 's': return RET_SHORT;
          default: throw new RuntimeException("unknown return type");
        }
      }
      else
      {
        return null;
      }
    }
  }

} // class: EntityRef
