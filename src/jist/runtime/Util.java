//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Util.java Sat 2004/05/22 14:28:42 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import org.apache.bcel.generic.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import java.rmi.*;

/** 
 * Miscellaneous utilities used by JIST.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Util.java,v 1.39 2004-05-22 19:04:53 barr Exp $
 * @since JIST1.0
 */

public final class Util 
{

  /** Escape character used when converting String into Java-safe identifiers. */
  public static final char IDENTIFIER_ESCAPE_CHAR = '_';

  /** An array of Integers for small primitive integers. */
  private static final Integer[] INTS = new Integer[10];

  /**
   * Return whether a given Objects exists within an Object array.
   *
   * @param set an array of objects to test for membership
   * @param item object to test membership
   * @return whether given item exists in the given set
   */
  public static boolean contains(Object[] set, Object item)
  {
    if(set==null || item==null) return false;
    int i=0;
    while (i<set.length)
    {
      if (item.equals(set[i]))
      {
        return true;
      }
      i++;
    }
    return false;
  }

  /**
   * Escape character in string. Given character is replaced
   * with escape characters and hexadecimal representation.
   *
   * @param s string to escape
   * @param c character to replace/escape
   * @return escaped string
   */
  public static String escapeChar(String s, char c)
  {
    int index = s.indexOf(c);
    while(index!=-1)
    {
      s = s.substring(0, index) + 
        IDENTIFIER_ESCAPE_CHAR+getHexCode(c) + 
        s.substring(index+1,s.length());
      index = s.indexOf(c, index+3);
    }
    return s;
  }

  /**
   * Escape string so that it conforms to Java identifier rules.
   *
   * @param s string to escape
   * @return escaped string: can be used as Java identifier
   */
  public static String escapeJavaIdent(String s)
  {
    String escaped = Util.escapeChar(s, IDENTIFIER_ESCAPE_CHAR);
    for(int i=0; i<s.length(); i++)
    {
      char c = s.charAt(i);
      if(!Character.isJavaIdentifierPart(c))
      {
        escaped = Util.escapeChar(escaped, c);
      }
    }
    return escaped;
  }

  /**
   * Return hexadecimal representation of given character.
   *
   * @param c character to convert
   * @return hexadecimal representation of given character
   */
  public static String getHexCode(char c)
  {
    if(Main.ASSERT) Util.assertion(c<256);
    return c<16 ? "0"+Integer.toHexString(c) : Integer.toHexString(c);
  }

  /**
   * Return a set (array of unique objects).
   *
   * @param elements array of objects, possibly with duplicates
   * @return array of objects with duplicates removed; order is not preserved.
   */
  public static Object[] unique(Object[] elements)
  {
    Hashtable h = new Hashtable();
    Object o = new Object();
    for(int i=0; i<elements.length; i++)
    {
      h.put(elements[i], o);
    }
    Object[] el2 = new Object[h.size()];
    Enumeration e = h.keys();
    int i = 0;
    while(e.hasMoreElements())
    {
      el2[i++] = e.nextElement();
    }
    return el2;
  }

  /**
   * Return the set union of two array of objects.
   *
   * @param set1 first set of objects
   * @param set2 second set of objects
   * @return set union of set1 and set2
   */
  public static Object[] union(Object[] set1, Object[] set2)
  {
    Object[] set = new Object[set1.length+set2.length];
    System.arraycopy(set1, 0, set, 0, set1.length);
    System.arraycopy(set2, 0, set, set1.length, set2.length);
    return unique(set);
  }

  /**
   * Same as unique, but for Strings.
   *
   * @param elements array of Strings, possibly with duplicates
   * @return array of Strings with duplicates removed; order is not preserved.
   */
  public static String[] unique(String[] elements)
  {
    Hashtable h = new Hashtable();
    Object o = new Object();
    for(int i=0; i<elements.length; i++)
    {
      h.put(elements[i], o);
    }
    String[] el2 = new String[h.size()];
    Enumeration e = h.keys();
    int i = 0;
    while(e.hasMoreElements())
    {
      el2[i++] = (String)e.nextElement();
    }
    return el2;
  }

  /**
   * Same as union, but for Strings.
   *
   * @param set1 first set of Strings
   * @param set2 second set of Strings
   * @return set union of set1 and set2
   */
  public static String[] union(String[] set1, String[] set2)
  {
    String[] set = new String[set1.length+set2.length];
    System.arraycopy(set1, 0, set, 0, set1.length);
    System.arraycopy(set2, 0, set, set1.length, set2.length);
    return (String[])unique(set);
  }

  /**
   * Start an idle thread. Usually used to prevent JVM from exiting.
   */
  public static void startIdleThread()
  {
    Runnable r = new Runnable()
    {
      public void run()
      {
        try
        {
          while(true)
          {
            Thread.sleep(10);
          }
        }
        catch(InterruptedException e) 
        { 
        }
      }
    };
    (new Thread(r)).start();
  }

  /**
   * Display the currently active threads.
   */
  public static void showThreads()
  {
    Thread[] threads = new Thread[Thread.activeCount()];
    Thread.enumerate(threads);
    for(int i=0; i<threads.length; i++)
    {
      if(threads[i]==null) continue;
      System.out.println("thread: alive="+threads[i].isAlive()+
          " daemon="+threads[i].isDaemon()+
          " info="+threads[i]);
    }
  }

  /**
   * Concatenate array of Strings separated by given delimeter.
   *
   * @param strings array of strings to concatenate
   * @param delim delimeter to insert between each pair of strings
   * @return delimited concatenation of strings
   */
  public static String stringJoin(String[] strings, String delim)
  {
    StringBuffer sb = new StringBuffer();
    int i=0;
    while(i<strings.length-1)
    {
      sb.append(strings[i++]);
      sb.append(delim);
    }
    if(i<strings.length)
    {
      sb.append(strings[i]);
    }
    return sb.toString();
  }

  /**
   * Return a range of integers in an array: [0, max).
   *
   * @param max range maximum limit
   * @return integer array containing [0, max)
   */
  public static int[] getRange(int max)
  {
    int[] result = new int[max];
    for(int i=0; i<max; i++)
    {
      result[i] = i;
    }
    return result;
  }

  /**
   * Method that prints the type of the object
   * passed to it. Useful for rewritter debugging as in:
   * <code>
   *   DUP
   *   INVOKE Util.printType
   * </code>
   *
   * @param o some object
   * @return object passed in (to keep stack unchanged)
   */
  public static Object printType(Object o)
  {
    System.out.println(o.getClass().getName());
    return o;
  }

  /**
   * Method stub for printType method.
   */
  public static Method method_printType;

  static
  {
    try
    {
      method_printType = Util.class.getDeclaredMethod(
          "printType",
          new Class[] { Object.class });
    }
    catch(NoSuchMethodException e)
    {
      throw new JistException("should not happen", e);
    }
  }

  /**
   * Round-off double to given number of decimal places.
   *
   * @param num number to round
   * @param decimal decimal places
   * @return rounded number
   */
  public static double round(double num, int decimal)
  {
    double factor = Math.pow(10, decimal);
    return Math.rint(num*factor)/factor;
  }

  /**
   * Return the number of memory bytes used, which is computed by subtracting
   * the 'free' memory from 'total' memory.
   *
   * @return number of bytes of memory used
   */
  public static long getUsedMemory()
  {
    long total = Runtime.getRuntime().totalMemory();
    long free = Runtime.getRuntime().freeMemory();
    return total - free;
  }

  /**
   * Display a stack trace.
   */
  public static void showStack()
  {
    StackTraceElement[] stack = (new Throwable()).getStackTrace();
    for(int i=0; i<stack.length; i++)
    {
      StackTraceElement el = stack[i];
      System.out.println(el);
    }
  }

  /**
   * Read an entire stream and return byte[].
   *
   * @param in inputstream to devour
   * @return byte[] of data read
   * @throws IOException on error from given inputstream
   */
  public static byte[] readAll(InputStream in) throws IOException
  {
    final int CHUNK = 16*1024;
    int total = 0;
    byte[] buf = new byte[CHUNK*2];
    int read = in.read(buf);
    if(read>0) total+=read;
    while(read!=-1)
    {
      if(buf.length-total<CHUNK)
      {
        byte[] buf2 = new byte[buf.length*2];
        System.arraycopy(buf, 0, buf2, 0, total);
        buf = buf2;
      }
      read = in.read(buf, total, CHUNK);
      if(read>0) total+=read;
    }
    byte[] buf2 = new byte[total];
    System.arraycopy(buf, 0, buf2, 0, total);
    return buf2;
  }

  /**
   * Get data of given named resource as byte array.
   *
   * @param name resource to retrieve
   * @return resource data as byte array
   */
  public static byte[] getResourceBytes(String name)
  {
    try
    {
      InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream(name);
      if(is==null) return null;
      byte[] b = Util.readAll(is);
      is.close();
      return b;
    }
    catch(IOException e)
    {
      return null;
    }
  }

  /**
   * Return seconds as string of hours, minutes and seconds.
   *
   * @param second number of seconds
   * @return string in HHhMMmSSs format
   */
  public static String getHMS(long second)
  {
    long hour = second/3600;
    second = second % 3600;
    long minute = second/60;
    second = second % 60;
    String time = "";
    if(hour>0)
    {
      time += hour+"h";
    }
    if(minute>0)
    {
      time += minute+"m";
    }
    time += second+"s";
    return time;
  }

  /**
   * Validate condition.
   *
   * @param cond condition to validate
   */
  public static void assertion(boolean cond)
  {
    if(!cond) throw new AssertionError("assertion");
  }

  /**
   * Lookup method in a class by name.
   *
   * @param c class to scan
   * @param name method name
   * @return method, if found and unique
   * @throws NoSuchMethodException if method not found or not unique
   */
  public static Method findUniqueMethod(Class c, String name) throws NoSuchMethodException
  {
    Method[] methods = c.getDeclaredMethods();
    Method m = null;
    for(int i=0; i<methods.length; i++)
    {
      if(name.equals(methods[i].getName()))
      {
        if(m!=null)
        {
          throw new NoSuchMethodException("method name not unique: "+name);
        }
        m=methods[i];
      }
    }
    if(m==null)
    {
      throw new NoSuchMethodException("method not found: "+name);
    }
    return m;
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

  /**
   * Return the sign of a long.
   *
   * @return sign of a long.
   */
  public static int sign(long l)
  {
    return l>0 ? 1 : l<0 ? -1 : 0;
  }

  public static String unqualifiedName(String s)
  {
    if(s.indexOf('.')==-1) return s;
    return s.substring(s.lastIndexOf('.')+1);
  }

} // class: Util

