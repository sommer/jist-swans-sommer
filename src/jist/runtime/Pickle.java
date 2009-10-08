//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Pickle.java Tue 2004/04/06 11:24:16 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.io.*;
import java.util.*;
import java.net.*;
import sun.misc.HexDumpEncoder;

/** 
 * Support for pickling and unpickling common data objects.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Pickle.java,v 1.10 2004-04-06 16:07:43 barr Exp $
 * @since JIST1.0
 */

final class Pickle 
{

  //////////////////////////////////////////////////
  // Pretty print byte arrays
  //

  /**
   * Print a hex-dump of a byte-array to System standard output stream.
   *
   * @param a array to print
   */
  public static void printByteArrayNicely(byte[] a)
  {
    HexDumpEncoder hde = new HexDumpEncoder();
    System.out.print(hde.encode(a));
  }
  
  /**
   * Print a hex-dump of some portion of a byte-array to System standard output stream.
   *
   * @param a array to print
   * @param offset starting offset
   * @param length number of bytes of print
   */
  public static void printByteArrayNicely(byte[] a, int offset, int length) 
  {
    byte[] b = new byte[length];
    System.arraycopy(a, offset, b, 0, length);
    printByteArrayNicely(b);
  }
  
  /**
   * Print a hex-dump of a byte-array to System.out followed by a new line.
   *
   * @param a array to print
   */
  public static void printlnByteArrayNicely(byte[] a)
  {
    printlnByteArrayNicely(a, 0, a.length);
  }

  /**
   * Print a hex-dump of some portion of a byte-array to System.out followed by
   * a new line.
   *
   * @param a array to print
   * @param offset starting offset
   * @param length number of bytes of print
   */
  public static void printlnByteArrayNicely(byte[] a, int offset, int length)
  {
    printByteArrayNicely(a, offset, length);
    System.out.println();
  }


  /**
   * Utility method to stuff an entire enumeration into a vector.
   *
   * @param e input enumeration
   * @return vector containing all elements of enumeration
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

  //
  // Handle "unsigned" byte arrays containing numbers larger than 128
  // (bytes are signed, so convert into ints)
  //

  /**
   * Convert unsigned bytes to ints.
   *
   * @param data array of unsigned bytes
   * @param offset location in array to begin conversion
   * @param length number of bytes to convert
   * @return unsigned byte array as int array
   */
  public static int[] byteToIntArray(byte[] data, int offset, int length) 
  {
    int[] temp = new int[length];
    for (int i = 0; i < length; i++) 
    {
      temp[i] = (int)data[i+offset]<0 
        ? 256+(int)data[i+offset] 
        : (int)data[i+offset];
    }
    return temp;
  }

  /**
   * Convert ints into unsigned bytes.
   *
   * @param data array of unsigned ints
   * @param offset locationin array to begin conversion
   * @param length number of bytes to convert
   * @return array of ints as unsigned byte array
   */
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

  /**
   * Convert unsigned bytes to ints.
   *
   * @param data array of unsigned bytes
   * @return unsigned byte array as int array
   */
  public static int[] byteToIntArray(byte[] data) 
  {
    return byteToIntArray(data, 0, data.length);
  }

  /**
   * Convert ints into unsigned bytes.
   *
   * @param data array of unsigned ints
   * @return array of ints as unsigned byte array
   */
  public static byte[] intToByteArray(int[] data) 
  {
    return intToByteArray(data, 0, data.length);
  }

  /**
   * Concatenate two byte arrays.
   *
   * @param b1 first byte array
   * @param b2 second byte array
   * @return concatenated byte array
   */
  public static byte[] concat(byte[] b1, byte[] b2)
  {
    byte[] b = new byte[b1.length+b2.length];
    System.arraycopy(b1, 0, b, 0, b1.length);
    System.arraycopy(b2, 0, b, b1.length, b2.length);
    return b;
  }

  //
  // Integer: size = 4
  //

  /**
   * Store integer as bytes.
   *
   * @param integer int to convert
   * @param b destination byte array
   * @param offset offset within array to store integer at (4 bytes)
   */
  public static void integerToArray(int integer, byte[] b, int offset)
  {
    b[offset]   = (byte)integer;
    b[offset+1] = (byte)(integer>>8);
    b[offset+2] = (byte)(integer>>16);
    b[offset+3] = (byte)(integer>>24);
  }

  /**
   * Reconstruct integer from bytes.
   *
   * @param b byte array
   * @param offset offset within array to read integer from (4 bytes)
   * @return reconstructed integer
   */
  public static int arrayToInteger(byte[] b, int offset)
  {
    int[] i=byteToIntArray(b, offset, 4);
    return i[0] + (i[1]<<8) + (i[2]<<16) + (i[3]<<24);
  }

  //
  // Short: size = 2
  //

  /**
   * Store integer as bytes.
   *
   * @param i short to convert
   * @param b destination byte array
   * @param offset offset within array to store short at (2 bytes)
   */
  public static void shortToArray(short i, byte[] b, int offset)
  {
    b[offset]   = (byte)i;
    b[offset+1] = (byte)(i>>8);
  }

  /**
   * Reconstruct short from bytes.
   *
   * @param b byte array
   * @param offset offset within array to read short from (2 bytes)
   * @return reconstructed short
   */
  public static short arrayToShort(byte[] b, int offset)
  {
    int[] i = byteToIntArray(b, offset, 2);
    return (short)(i[0]+(i[1]<<8));
  }

  //
  // InetAddress: size = 4
  //

  /**
   * Store Internet address as bytes.
   *
   * @param inet Internet address  to convert
   * @param b destination byte array
   * @param offset offset within array to store address at (4 bytes)
   */
  public static void InetAddressToArray(InetAddress inet, byte[] b, int offset)
  {
    System.arraycopy(inet.getAddress(), 0, b, offset, 4);
  }

  /**
   * Reconstruct Internet address from bytes.
   *
   * @param b byte array
   * @param offset offset within array to read address from (4 bytes)
   * @return reconstructed Internet address
   */
  public static InetAddress arrayToInetAddress(byte[] b, int offset)
  {
    int[] i = byteToIntArray(b, offset, 4);
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

  //
  // String: size = variable
  //

  /**
   * Store string as bytes.
   *
   * @param s string to convert
   * @return string as byte array
   */
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

  /**
   * Reconstruct string from bytes.
   *
   * @param b byte array
   * @param offset offset within array to read string from (4 bytes + string)
   * @return reconstructed string
   */
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

  //
  // Object: size = variable
  //

  /**
   * Store object as bytes.
   *
   * @param s object to convert
   * @return object as byte array
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
      throw new JistException("unable to serialize packet", e);
    }
  }

  /**
   * Reconstruct object from bytes.
   *
   * @param b byte array
   * @param offset offset within array to read object from (4 bytes + object)
   * @return reconstructed object
   */
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
      throw new JistException("unable to deserialize packet (io error)", e);
    }
    catch(ClassNotFoundException e) 
    {
      e.printStackTrace();
      throw new JistException("unable to deserialize packet (class not found)", e);
    }
  }

  /**
   * Reconstruct object from bytes.
   *
   * @param b byte array
   * @return reconstructed object
   */
  public static Object arrayToObject(byte[] b)
  {
    return arrayToObject(b, 0);
  }

} // class: Pickle

