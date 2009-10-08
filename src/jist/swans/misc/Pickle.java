//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Pickle.java Tue 2004/04/06 11:46:44 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.misc;

import java.io.*;
import java.util.*;
import java.net.*;
import sun.misc.HexDumpEncoder;

/**
 * Utility class to simplify the serialization and deserialization
 * of various data types.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Pickle.java,v 1.7 2004-04-06 16:07:49 barr Exp $
 * @since SWANS1.0
 */

public final class Pickle 
{
  //////////////////////////////////////////////////
  // Pretty print byte arrays
  //

  public static void printByteArrayNicely(byte[] a)
  {
    HexDumpEncoder hde = new HexDumpEncoder();
    System.out.print(hde.encode(a));
  }
  
  public static void printByteArrayNicely(byte[] a, int offset, int length) 
  {
    byte[] b = new byte[length];
    System.arraycopy(a, offset, b, 0, length);
    printByteArrayNicely(b);
  }
  
  public static void printlnByteArrayNicely(byte[] a, int offset, int length)
  {
    printByteArrayNicely(a, offset, length);
    System.out.println();
  }

  public static void printlnByteArrayNicely(byte[] a)
  {
    printlnByteArrayNicely(a, 0, a.length);
  }

  /**
   * Utility method to stuff an entire
   * enumeration into a vector
   */
  public static Vector Enum2Vector(Enumeration e) 
  {
    Vector v=new Vector();
    while(e.hasMoreElements()) 
    {
      v.add(e.nextElement());
    }
    return v;
  }


  //////////////////////////////////////////////////
  // Helper methods for dealing with byte[]'s
  //

  /** 
   * Handle "unsigned" byte arrays containing numbers larger than 128
   * (bytes are signed, so convert into ints)
   */
  public static int[] byteToIntArray(byte[] data, int offset, int length) 
  {
    int[] temp = new int[length];
    for (int i = 0; i < length; i++) {
      temp[i] = (int)data[i+offset]<0 
        ? 256+(int)data[i+offset] 
        : (int)data[i+offset];
    }
    return temp;
  }

  public static byte[] intToByteArray(int[] data, int offset, int length) 
  {
    byte[] temp = new byte[length];
    for (int i = 0; i < length; i++) 
    {
      if(data[i+offset]<0 || data[i+offset]>255) 
      {
        throw new RuntimeException("number too large for unsigned byte");
      }
      temp[i] = data[i+offset]>127
        ? (byte)(data[i+offset]-256)
        : (byte)data[i+offset];
    }
    return temp;
  }

  public static int[] byteToIntArray(byte[] data) 
  {
    return byteToIntArray(data, 0, data.length);
  }

  public static byte[] intToByteArray(int[] data) 
  {
    return intToByteArray(data, 0, data.length);
  }

  public static byte[] concat(byte[] b1, byte[] b2)
  {
    byte[] b = new byte[b1.length+b2.length];
    System.arraycopy(b1, 0, b, 0, b1.length);
    System.arraycopy(b2, 0, b, b1.length, b2.length);
    return b;
  }

  //////////////////////////////////////////////////
  // unsigned bytes
  //

  public static final void ubyteToArray(int ubyte, byte[] b, int offset)
  {
    b[offset] = ubyte<128 ? (byte)ubyte : (byte)(ubyte-256);
  }

  public static final int arrayToUByte(byte[] b, int offset)
  {
    return b[offset]<0 ? 256+(int)b[offset] : (int)b[offset];
  }

  //////////////////////////////////////////////////
  // unsigned short
  //

  public static final void ushortToArray(int ushort, byte[] b, int offset)
  {
    ubyteToArray((byte)(ushort>>8), b, offset);
    ubyteToArray((byte)ushort, b, offset+1);
  }

  public static final int arrayToUShort(byte[] b, int offset)
  {
    return (arrayToUByte(b, offset)<<8) + arrayToUByte(b, offset+1);
  }

  //////////////////////////////////////////////////
  // unsigned int
  //

  public static final void uintToArray(long uint, byte[] b, int offset)
  {
    ushortToArray((int)(uint>>16), b, offset);
    ushortToArray((int)uint, b, offset+2);
  }

  public static final long arrayToUInt(byte[] b, int offset)
  {
    return (arrayToUShort(b, offset)<<16) + arrayToUShort(b, offset+2);
  }



  /**
   * Integer: size = 4
   */
  public static void integerToArray(int integer, byte[] b, int offset)
  {
    b[offset]   = (byte)integer;
    b[offset+1] = (byte)(integer>>8);
    b[offset+2] = (byte)(integer>>16);
    b[offset+3] = (byte)(integer>>24);
  }

  public static int arrayToInteger(byte[] b, int offset)
  {
    int[] i=byteToIntArray(b, offset, 4);
    return i[0] + (i[1]<<8) + (i[2]<<16) + (i[3]<<24);
  }

  /**
   * Short: size = 2
   */
  public static void shortToArray(short i, byte[] b, int offset)
  {
    b[offset]   = (byte)i;
    b[offset+1] = (byte)(i>>8);
  }

  public static short arrayToShort(byte[] b, int offset)
  {
    int[] i = byteToIntArray(b, offset, 2);
    return (short)(i[0]+(i[1]<<8));
  }

  /**
   * InetAddress: size = 4
   */
  public static void InetAddressToArray(InetAddress inet, byte[] b, int offset)
  {
    System.arraycopy(inet.getAddress(), 0, b, offset, 4);
  }

  public static InetAddress arrayToInetAddress(byte[] addr, int offset)
  {
    int[] i = byteToIntArray(addr, offset, 4);
    String s=i[0]+"."+i[1]+"."+i[2]+"."+i[3];
    try 
    {
      return InetAddress.getByName(s);
    }
    catch(UnknownHostException e)
    {
      throw new RuntimeException("unknown host: "+s);
    }
  }

  /**
   * String: size = variable
   */
  public static int getLength(byte[] b, int offset)
  {
    int len = arrayToInteger(b, offset);
    return Math.max(0, len)+4;
  }

  public static byte[] stringToArray(String s)
  {
    byte[] out = null;
    if(s==null) 
    {
      out = new byte[4];
      integerToArray(-1, out, 0);
    }
    else 
    {
      byte[] sb = s.getBytes();
      out = new byte[sb.length+4];
      integerToArray(sb.length, out, 0);
      System.arraycopy(sb, 0, out, 4, sb.length);
    }
    return out;
  }

  public static String arrayToString(byte[] b, int offset)
  {
    int len = arrayToInteger(b, offset);
    if(len==-1)
    {
      return null;
    }
    else 
    {
      return new String(b, offset+4, len);
    }
  }

  /**
   * Object: size = variable
   */
  public static byte[] objectToArray(Object s)
  {
    try 
    {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(s);
      oos.close();
      baos.close();
      byte[] sb = baos.toByteArray();
      byte[] out = new byte[sb.length+4];
      integerToArray(sb.length, out, 0);
      System.arraycopy(sb, 0, out, 4, sb.length);
      return out;
    }
    catch(IOException e) 
    {
      e.printStackTrace();
      throw new RuntimeException("unable to serialize packet", e);
    }
  }

  public static Object arrayToObject(byte[] b, int offset)
  {
    try 
    {
      int len = arrayToInteger(b, offset);
      ByteArrayInputStream bais = new ByteArrayInputStream(b, offset+4, len);
      ObjectInputStream ois = new ObjectInputStream(bais);
      return ois.readObject();
    }
    catch(IOException e) 
    {
      e.printStackTrace();
      throw new RuntimeException("unable to deserialize packet (io error)", e);
    }
    catch(ClassNotFoundException e) 
    {
      e.printStackTrace();
      throw new RuntimeException("unable to deserialize packet (class not found)", e);
    }
  }

  public static Object arrayToObject(byte[] b)
  {
    return arrayToObject(b, 0);
  }

  public static byte[] messageBytes(Message m)
  {
    byte[] b = new byte[m.getSize()];
    m.getBytes(b, 0);
    return b;
  }

} // class: Pickle

