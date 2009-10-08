//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Node.java Tue 2004/04/06 11:24:10 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.Serializable;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.StringTokenizer;

/** 
 * Stores and manipulates host:port information.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Node.java,v 1.12 2004-04-06 16:07:43 barr Exp $
 * @since JIST1.0
 */

public final class Node implements Serializable
{

  /**
   * Internet address of node.
   */
  private InetAddress host;

  /**
   * Port of jist node.
   */
  private short port;

  /**
   * hash of this object.
   */
  private int hashCode;

  /**
   * cached toString() of this object.
   */
  private String cacheToString;

  /**
   * Internet address of local host.
   */
  private static InetAddress localhost;
  
  //////////////////////////////////////////////////
  // Constructors
  //

  /**
   * Instantiate node object with given host and port.
   *
   * @param host Internet address of node instance
   * @param port port of node instance
   */
  public Node(InetAddress host, short port) 
  {
    this.host = host;
    this.port = port;
    calcHashCode();
  }

  /**
   * Instantiate node object with given host and port.
   *
   * @param host Internet address or name of node instance
   * @param port port of node instance
   * @throws UnknownHostException thrown if named host can not be resolved
   */
  public Node(String host, short port) throws UnknownHostException 
  {
    try 
    {
      this.host = InetAddress.getByName(host);
    }
    catch(UnknownHostException e) // maybe is x.x.x.x format
    {
      try 
      {
        int[] addr = new int[4];
        StringTokenizer st = new StringTokenizer(host, ".");
        if(st.countTokens()!=4) throw new RuntimeException();
        for(int i=0; i<addr.length; i++)
        {
          String n = (String)st.nextToken().trim();
          addr[i] = Integer.parseInt(n);
        }
        this.host = InetAddress.getByAddress(Pickle.intToByteArray(addr));
      }
      catch(Exception e1)
      {
        throw new UnknownHostException(host);
      }
    }
    this.port = port;
    calcHashCode();
  }

  /**
   * Instantiate node object with given host and port.
   *
   * @param addr Internet address of node instance as quad-byte array
   * @param port port of node instance
   * @throws UnknownHostException never
   */
  public Node(byte[] addr, short port) 
    throws UnknownHostException
  {
    this.host = InetAddress.getByAddress(addr);
    this.port = port;
    calcHashCode();
  }

  /**
   * Instantiate node object with given port on the local host.
   *
   * @param port port of node instance
   */
  public Node(short port)
  {
    this(getLocalHost(), port);
  }

  /**
   * Instantiate node object with given host and port. Convenience 
   * method to accept integer ports and convert to short.
   *
   * @param host Internet address of node instance
   * @param port port of node instance
   */
  public Node(InetAddress host, int port)
  {
    this(host, (short)port);
  }

  /**
   * Instantiate node object with host string and port. Convenience
   * method to accept integer ports and convert to short.
   *
   * @param host Internet address string of node instance
   * @param port port of node instance
   * @throws UnknownHostException invalid host name or IP address
   */
  public Node(String host, int port) 
    throws UnknownHostException
  {
    this(host, (short)port);
  }

  /**
   * Instantiate node object with given port on local host. Convenience
   * method to accept integer ports and convert to short.
   *
   * @param port port of node instance
   */
  public Node(int port)
  {
    this((short)port);
  }

  /**
   * Instantiate node object from serialized information in byte array
   * at given offset. The byte array must contain 4-bytes for the
   * Internet address and 2-bytes for the port.
   *
   * @param node byte array with node information
   * @param offset location of information within byte array
   */
  public Node(byte[] node, int offset)  // 6 bytes
  {
    this(Pickle.arrayToInetAddress(node, offset), Pickle.arrayToShort(node, offset+4));
  }

  //////////////////////////////////////////////////
  // Serialization
  //

  /**
   * Serialize node instance into byte array.
   * The byte array will be of length 6.
   *
   * @return serialized node information
   */
  public byte[] toByteArray()
  {
    byte[] b=new byte[6];
    toByteArray(b, 0);
    return b;
  }

  /**
   * Serialize node instance into byte array. The serialized
   * byte representation of a node occupies 6 bytes.
   *
   * @param b byte array to place node information into
   * @param offset starting location within byte array.
   */
  public void toByteArray(byte[] b, int offset)
  {
    Pickle.InetAddressToArray(host, b, offset);
    Pickle.shortToArray(port, b, offset+4);
  }

  /**
   * Parse String (host:port) into a Node.
   *
   * @param s serialized node string in format host:port
   * @return node instance
   * @throws UnknownHostException thrown if host name within
   *   string can not be resolved
   */
  public static Node parse(String s) 
    throws UnknownHostException 
  {
    try 
    {
      s = " "+s.trim();
      StringTokenizer st = new StringTokenizer(s, ":");
      while (st.hasMoreTokens()) 
      {
        String host = (String)st.nextToken().trim();
        if(host.equals("")) host=null;
        int port = Integer.parseInt((String)st.nextToken());
        if(host==null)
        {
          return new Node(port);
        }
        else
        {
          return new Node(host, port);
        }
      }
    }
    catch(UnknownHostException e)
    {
      throw e;
    }
    catch(Exception e) {}
    throw new IllegalArgumentException("unable to parse Node from: "+s);
  }

  /**
   * Parse string (host:port) into a Node, using the default port if
   * the string does not contain a port.
   *
   * @param s serialized node string in format host[:port]
   * @param defaultPort default port to use if port omitted
   * @return parsed node object
   * @throws UnknownHostException thrown if host name within string
   *   can not be resolved.
   */
  public static Node parse(String s, int defaultPort) 
    throws UnknownHostException
  {
    try 
    {
      s = " "+s.trim();
      StringTokenizer st = new StringTokenizer(s, ":");
      while (st.hasMoreTokens()) 
      {
        String host = (String)st.nextToken().trim();
        if(host.equals("")) host=null;
        int port = defaultPort;
        try 
        {
          port = Integer.parseInt((String)st.nextToken());
        }
        catch (Exception e) {}
        if(host==null)
        {
          return new Node(port);
        }
        else
        {
          return new Node(host, port);
        }
      }
    }
    catch(UnknownHostException e)
    {
      throw e;
    }
    catch(Exception e) {}
    throw new IllegalArgumentException("unable to parse Node from: "+s);
  }


  //////////////////////////////////////////////////
  // Accessors; toString, hash, equals
  //

  /**
   * Return host Internet address of current node.
   *
   * @return host Internet address
   */
  public InetAddress getHost() 
  { 
    return host;
  }

  /**
   * Return host string of current node.
   *
   * @return host string
   */
  public String getHostString()
  { 
    int[] i = Pickle.byteToIntArray(host.getAddress());
    return i[0]+"."+i[1]+"."+i[2]+"."+i[3];
  }

  /**
   * Return port of current node.
   *
   * @return node port
   */
  public short getPort() 
  {
    return port;
  }

  /**
   * Return string representation of node in format host:port.
   *
   * @return string representation of node in format host:port
   */
  public String toString()
  {
    if(cacheToString==null)
    {
      cacheToString = getHostString()+":"+port;
    }
    return cacheToString;
  }

  /**
   * Determine whether this node is the same as another.
   *
   * @param o object to test equality
   * @return whether this node is equal to given object
   */
  public boolean equals(Object o) 
  {
    if(!(o instanceof Node)) return false;
    Node n=(Node)o;
    if(!host.equals(n.getHost())) return false;
    if(port!=n.getPort()) return false;
    return true;
  }

  /**
   * Compute and cache node hash code.
   */
  private void calcHashCode() 
  {
    hashCode = host.hashCode()+port;
  }

  /**
   * Return node hash code.
   *
   * @return node hash code
   */
  public int hashCode()
  {
    return hashCode;
  }
  
  //////////////////////////////////////////////////
  // Determine local host
  //

  /**
   * Local host (loopback) Internet address string.
   */
  private static final String loopback = "127.0.0.1";

  /**
   * Find local host Internet address. Attempts to discover
   * local host IP address using a number of techniques.
   *
   * @return Internet address of localhost, or null if it can
   *   not be discovered.
   */
  private static InetAddress getLocalHost()
  {
    if (localhost!=null)
    {
      return localhost;
    }
    localhost = getLocalHostLinux("eth0");
    if(localhost!=null && !loopback.equals(localhost.getHostAddress())) 
    {
      return localhost;
    }
    localhost = getLocalHostLinux("eth1");
    if(localhost!=null && !loopback.equals(localhost.getHostAddress()))
    {
      return localhost;
    }
    localhost = getLocalHostJava();
    if(localhost!=null && !loopback.equals(localhost.getHostAddress()))
    {
      return localhost;
    }
    return localhost;
  }

  /**
   * Find local host Internet address using (portable) Java
   * mechanisms.
   *
   * @return Internet address of localhost, or null if it can
   *   not be discovered.
   */
  private static InetAddress getLocalHostJava()
  {
    InetAddress local = null;
    try 
    {
      local = InetAddress.getByName(loopback);  // works more often than next line!
      if(localhost!=null && !loopback.equals(localhost.getHostAddress()))
      {
        local = InetAddress.getLocalHost();
      }
    }
    catch (UnknownHostException e) 
    { 
    }
    return local;
  }

  /**
   * Find local host Internet address using Unix mechanisms.
   * Specifically, this method invokes 'ifconfig', and parses
   * its output, searching for the IP address of the given interface.
   *
   * @param iface network interface to query 'ifconfig'
   * @return Internet address of localhost, or null if it can
   *   not be discovered.
   */
  public static InetAddress getLocalHostLinux(String iface)
  {
    try 
    {
      final String command = "/sbin/ifconfig "+iface;
      Process p = Runtime.getRuntime().exec(command);
      BufferedReader stdInput = new BufferedReader(new 
          InputStreamReader(p.getInputStream()));
      /*
       * This is kinda hack... Get the ip address from
       * second line of /sbin/ifconfig eth0, after the 
       * 'inet addr:xx.xxx.xxx.xxx'
       */
      stdInput.readLine();
      String l = stdInput.readLine();
      if(l==null) return null;
      if(!l.trim().toLowerCase().startsWith("inet addr")) return null;
      StringTokenizer results = new StringTokenizer(l, " ");
      results.nextToken();
      String addr = results.nextToken().substring(5);
      return InetAddress.getByName(addr);
    }
    catch(IOException e)
    {
      return null;
    }
  }

} // class: Node

