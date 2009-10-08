//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Sizeof.java Tue 2004/04/06 11:46:54 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.misc;

import java.lang.reflect.*;

/**
 * Compute the static or dynamic size of a type or object.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Sizeof.java,v 1.4 2004-04-06 16:07:49 barr Exp $
 * @since SWANS1.0
 */
public class Sizeof 
{

  /** Size of a pointer. */
  private static final int SIZE_REFERENCE = 4;

  /**
   * Return (dynamic) size of primitive.
   *
   * @param b boolean
   * @return sizeof(boolean)
   */
  public static int inst(boolean b)
  {
    return 1;
  }

  /**
   * Return (dynamic) size of primitive.
   *
   * @param b byte
   * @return sizeof(byte)
   */
  public static int inst(byte b)
  {
    return 1;
  }

  /**
   * Return (dynamic) size of primitive.
   *
   * @param c char
   * @return sizeof(char)
   */
  public static int inst(char c)
  {
    return 2;
  }

  /**
   * Return (dynamic) size of primitive.
   *
   * @param s short
   * @return sizeof(short)
   */
  public static int inst(short s)
  {
    return 2;
  }

  /**
   * Return (dynamic) size of primitive.
   *
   * @param i int
   * @return sizeof(int)
   */
  public static int inst(int i) 
  {
    return 4;
  }

  /**
   * Return (dynamic) size of primitive.
   *
   * @param l long
   * @return sizeof(long)
   */
  public static int inst(long l)
  {
    return 8;
  }

  /**
   * Return (dynamic) size of primitive.
   *
   * @param f float
   * @return sizeof(float)
   */
  public static int inst(float f)
  {
    return 4;
  }

  /**
   * Return (dynamic) size of primitive.
   *
   * @param d double
   * @return sizeof(double)
   */
  public static int inst(double d)
  {
    return 8;
  }

  /**
   * Return (dynamic) size of object instance.
   *
   * @param obj object to size
   * @return size of object
   */
  public static int inst(Object obj)
  {
    if (obj == null) 
    {
      return 0;
    }
    Class c = obj.getClass();
    if (c.isArray()) 
    {
      return array(obj);
    }
    else 
    {
      return type(c);
    }
  }

  /**
   * Return (dynamic) size of an array.
   *
   * @param obj array to compute size
   * @return size of an array
   */
  private static int array(Object obj)
  {
    Class type = obj.getClass().getComponentType();
    int len = Array.getLength(obj);
    if (type.isPrimitive()) 
    {
      return len * type(type);
    }
    else 
    {
      int size = 0;
      for (int i = 0; i < len; i++) 
      {
        size += SIZE_REFERENCE;
        Object o = Array.get(obj, i);
        if(!o.getClass().isArray())
        {
          size += inst(Array.get(obj, i));
        }
      }
      return size;
    }
  }

  /**
   * Return static size of non-primitive type (a class/structure).
   *
   * @param type class or structure to compute size
   * @return size of class or structure
   */
  public static int type(Class type)
  {
    Field[] fields = type.getDeclaredFields();
    int size = 0;
    for (int i = 0; i < fields.length; i++)
    {
      Field f = fields[i];
      if (!type.isInterface() &&
          (f.getModifiers() & Modifier.STATIC) != 0)
      {
        continue;
      }
      size += primitive(f.getType());
    }
    if (type.getSuperclass() != null)
    {
      size += type(type.getSuperclass());
    }
    Class[] cl = type.getInterfaces();
    for (int i = 0; i < cl.length; i++)
    {
      size += type(cl[i]);
    }
    return size;
  }

  /**
   * Return static size of primitive type.
   *
   * @param type class of primitive type
   * @return size of primitive type
   */
  private static int primitive(Class type)
  {
    if (type == Boolean.TYPE) 
    {
      return 1;
    }
    else if (type == Byte.TYPE) 
    {
      return 1;
    }
    else if (type == Character.TYPE) 
    {
      return 2;
    }
    else if (type == Short.TYPE) 
    {
      return 2;
    }
    else if (type == Integer.TYPE) 
    {
      return 4;
    }
    else if (type == Long.TYPE) 
    {
      return 8;
    }
    else if (type == Float.TYPE) 
    {
      return 4;
    }
    else if (type == Double.TYPE) 
    {
      return 8;
    }
    else if (type == Void.TYPE) 
    {
      return 0;
    }
    else 
    {
      return SIZE_REFERENCE;
    }
  }

}
