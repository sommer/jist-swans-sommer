//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <ProxyEntity.java Sun 2006/05/14 14:13:47 barr jist>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.util.*;
import java.lang.reflect.*;
import java.rmi.*;

/**
 * Implementation of a dynamic Proxy Entity for an object, an entity that
 * forwards all of its calls (according to a mutual interface) to a regular
 * object, but in simulation time.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: ProxyEntity.java,v 1.29 2006-05-14 18:32:45 barr Exp $
 * @since JIST1.0
 * @see EntityRef
 */

final class ProxyEntity extends Entity.Empty
{

  //////////////////////////////////////////////////
  // proxy entity handler
  //

  /**
   * Handles all invocations from the Controller on the proxy entity and passes
   * them on to the target object. This handler is bypassed if the target
   * object is Proxiable (i.e. it can receive calls from the Controller and
   * manage it's own EntityRef object)
   */
  public static class ProxyEntityHandler implements InvocationHandler
  {
    /**
     * Target object of the proxy entity.
     */
    private Object target;

    /**
     * Intialize the proxy entity handler with a given target.
     *
     * @param target target object of the proxy entity
     */
    public ProxyEntityHandler(Object target)
    {
      this.target = target;
    }

    /**
     * Called whenever a proxy entity method is invoked.
     *
     * @param proxy proxy entity object whose method was invoked
     * @param method method invoked on entity object
     * @param args arguments of the method invocation
     * @return result of invocation (will always be null)
     */
    public Object invoke(Object proxy, Method method, Object[] args)
    {
      try
      {
        // invoke method on target object of proxy entity
        return method.invoke(target, args);
      }
      catch(InvocationTargetException e)
      {
        Throwable t = e.getTargetException();
        if(t instanceof JistException) throw (JistException)t;
        if(t instanceof VirtualMachineError) throw (VirtualMachineError)t;
        throw new JistException("application error escaped to proxy", e);
      }
      catch(Exception e)
      {
        throw new JistException("unexpected error in proxy", e);
      }
    }

  } // class: ProxyEntityHandler

  //////////////////////////////////////////////////
  // proxy creation
  //

  /**
   * Check recursively whether a given class implements a given interface.
   *
   * @param c class to check
   * @param ci interface to check for
   * @return whether class implements interface
   */
  public static boolean doesImplement(Class c, Class ci)
  {
    return ci.isInterface() && ci.isAssignableFrom(c);
  }

  /**
   * Verifies the given proxy target and proxy interfaces are valid. Checks
   * performed include: verify that target implements the interface, the
   * interface does not expose non-final or non-static fields, the interface
   * does not expose non-void methods, the interface does not expose methods
   * that declare exceptions.
   *
   * @param proxyTarget target object for the proxy entity
   * @param proxyInterface interface to be supported by the proxy entity
   * @return array of error strings, or null if there are no errors found
   */
  protected static String[] isValidProxyEntity(Object proxyTarget, Class proxyInterface)
  {
    if(proxyTarget instanceof EntityRef) return null;
    Vector errors = null;
    // check that target implements interface
    if(!doesImplement(proxyTarget.getClass(), proxyInterface))
    {
      if(errors==null) errors = new Vector();
      errors.add("target object of proxy entity does not implement interface: "+
          proxyInterface.getName());
    }
    // validate interface
    Method[] methods = proxyInterface.getMethods();
    for(int j=0; j<methods.length; j++)
    {
      // determine if method is blocking
      boolean blocking = false;
      int numExceptions = 0;
      Class[] exceptions = methods[j].getExceptionTypes();
      for(int k=0; k<exceptions.length; k++)
      {
        if(Event.ContinuationFrame.class.equals(exceptions[k])
            || JistAPI.Continuation.class.equals(exceptions[k]))
        {
          blocking = true;
          continue;
        }
        numExceptions++;
      }
      // ensure each non-blocking method does not declare exceptions
      if(numExceptions>0 && !blocking)
      {
        if(errors==null) errors = new Vector();
        errors.add("proxy entity implements interface '"+
            proxyInterface.getName()+
            "' that has method that declares exceptions: "+
            methods[j].getName());
      }
      // ensure each non-blocking method returns void
      if(!Void.TYPE.equals(methods[j].getReturnType()) && !blocking)
      {
        if(errors==null) errors = new Vector();
        errors.add("proxy entity implements interface '"+
            proxyInterface.getName()+
            "' that has method with non-void return type: "+
            methods[j].getReturnType().getName());
      }
    }
    Field[] fields = proxyInterface.getFields();
    // ensure that there are no non-static or non-void fields
    for(int j=0; j<fields.length; j++)
    {
      System.out.println(fields[j]);
      int mods = fields[j].getModifiers();
      if(!(Modifier.isFinal(mods) && Modifier.isStatic(mods)))
      {
        if(errors==null) errors = new Vector();
        errors.add("proxy entity implements interface '"+
            proxyInterface.getName()+
            "' that has non-final or non-static field: "+
            fields[j].getName());
      }
    }
    // return errors if any, otherwise null
    if(errors!=null)
    {
      String[] errorsArray = new String[errors.size()];
      errors.toArray(errorsArray);
      return errorsArray;
    }
    return null;
  }

  /**
   * Dynamically create a proxy entity for the given target object and interface.
   *
   * @param proxyTarget target object for proxy entity
   * @param proxyInterface interface supported by proxy entity
   * @return proxy entity
   */
  public static Object create(Object proxyTarget, Class[] proxyInterface)
  {
    for(int i=0; i<proxyInterface.length; i++)
    {
      String[] errors = isValidProxyEntity(proxyTarget, proxyInterface[i]);
      if(errors!=null)
      {
        throw new RuntimeException("Invalid proxy target interface:\n"+
            Util.stringJoin(errors, "\n"));
      }
    }
    // check whether target is already an Entity, or whether we need to proxy it
    EntityRef ref = null;
    if(proxyTarget instanceof JistAPI.Proxiable)
    {
      ref = ((Entity)proxyTarget)._jistMethod_Get__ref();
      if(ref==null)
      {
        ref = Controller.newEntityReference((Entity)proxyTarget);
        ((Entity)proxyTarget)._jistMethod_Set__ref(ref);
      }
    }
    else if(proxyTarget instanceof EntityRef)
    {
      ref = (EntityRef)proxyTarget;
    }
    else
    {
      // create proxy entity object
      ProxyEntity e = new ProxyEntity();
      // create proxy entity object invocation handler
      Proxy pe = (Proxy)Proxy.newProxyInstance(
          proxyTarget.getClass().getClassLoader(),
          proxyInterface,
          new ProxyEntityHandler(proxyTarget));
      // register with controller and get EntityRef
      ref = Controller.newEntityReference((Entity)pe);
      e._jistMethod_Set__ref(ref);
    }
    // create proxy entity reference invocation handler
    Proxy per = (Proxy)Proxy.newProxyInstance(
        proxyInterface[0].getClassLoader(),
        proxyInterface, ref);
    return per;
  }

} // class: ProxyEntity

