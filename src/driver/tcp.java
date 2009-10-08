//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <tcptest.java Tue 2004/04/06 11:58:09 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package driver;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.misc.Util;
import jist.swans.misc.Mapper;
import jist.swans.net.NetAddress;
import jist.swans.net.NetIp;
import jist.swans.net.PacketLoss;
import jist.swans.trans.TransTcp;
import jist.swans.app.AppJava;

import java.net.*;
import java.io.*;

/**
 * Small TCP test that can be run both inside and outside of JiST.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: tcp.java,v 1.1 2004-11-03 00:16:02 barr Exp $
 * @since JIST1.0
 */

public class tcp
{

  /** default server address. */
  public static final String HOST = "localhost";
  /** default client-server port. */
  public static final int PORT = 3001;
  /** bulk message to transfer. */
  public static byte[] msg_bulk;
  /** number of bytes in bulk message to transfer. */
  public static int NUM_BYTES = 10000;

  /**
   * Simple TCP server.
   */
  public static class Server
  {
    /**
     * TCP server entry point: open TCP socket and receive message from client.
     *
     * @param args command-line parameters
     */
    public static void main(String[] args)
    {
  
      System.out.println("tcp server: starting at t="+JistAPI.getTime());
  
      ServerSocket ss = null;
      Socket s = null;
      InputStream in = null;
      OutputStream out = null;
      
      try
      {
        ss = new ServerSocket(PORT);
      } 
      catch (IOException e) 
      {
        System.err.println("tcp server: Could not listen on port " + PORT);
        System.exit(-1);
      }
  
      try
      {
        s = ss.accept();
      } 
      catch (IOException e) 
      {
        System.err.println("tcp server: Accept failed: " + PORT);
        System.exit(-1);
      }
    
      try
      {
        in = s.getInputStream ();
      }
      catch (IOException e) 
      {
        System.err.println("tcp server: cannot get input stream");
        System.exit(-1);
      }
      
      try
      {
        out = s.getOutputStream ();
      }
      catch (IOException e) 
      {
        System.err.println("tcp server: cannot get input stream");
      
        System.exit(-1);
      }
      try
      {
        byte a = 0;
        System.out.println ("calling read at t = " + JistAPI.getTime());
        int i = 0;
        while ((a = (byte)in.read()) > -1)
        {
          if (a != msg_bulk[i])
          {
            System.out.println ("Bytes received are different!!!");
            break;
          }

          i++;
          if (i == msg_bulk.length)
          {
            System.out.println ("\n####### DONE #######");
            System.out.println ("All " + i + " bytes match.\n");
            break;
          }
        }
        
      }
      catch (IOException e)
      {
        e.printStackTrace ();
      }
      catch (Exception e2)
      {
        e2.printStackTrace ();
      }
        
      try
      {
        JistAPI.sleep (10);
        in.close ();
        out.close ();
        s.close ();
      } 
      catch (IOException e) 
      {
        System.err.println("tcp server: failed while closing");
        System.exit(-1);
      }
      JistAPI.sleep (25);
    }
    
  } // class: Server


  /**
   * Simple TCP client.
   */
  public static class Client
  {
    /**
     * TCP client entry point: open TCP socket and send off message to server.
     *
     * @param args command-line parameters
     */
    public static void main(String[] args)
    {
      
      System.out.println("tcp client: starting at t="+JistAPI.getTime());
    
      Socket s = null;
      InputStream in = null;
      OutputStream out = null;
  
      try
      {
        s = new Socket (HOST, PORT); 
        JistAPI.sleep (10);
    
        in = s.getInputStream ();
        out = s.getOutputStream ();
        out.write (msg_bulk, 0, msg_bulk.length);
        System.out.println("tcp client: sent at t="+JistAPI.getTime() + "(" + msg_bulk.length + " bytes)");
        
        JistAPI.sleep (10);
        in.close ();
        out.close ();
        s.close ();
        
      } 
      catch (UnknownHostException e) 
      {
        System.err.println("tcp client: Unknown host: " + HOST);
        System.exit(1);
      } 
      catch (IOException e) 
      {
        System.err.println("tcp client: No I/O");
        System.exit(1);
      }
      JistAPI.sleep (25);
    }

  } // class: Client


  /**
   * Program entry point: small TCP test that can be run
   * inside and outside of JiST.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    if (args.length > 0)
    {
      Integer numbytes = new Integer (args[0]);
      NUM_BYTES = numbytes.intValue ();
    }
        
    // fill bulk array
    msg_bulk = new byte [NUM_BYTES];
    for (int i = 0; i < msg_bulk.length; i++)
    {
      msg_bulk [i] = (byte)(i % 26 + 65);
    }
    
    try
    {
      // protocol mapper
      Mapper protMap = new Mapper(Constants.NET_PROTOCOL_MAX);
      protMap.mapToNext(Constants.NET_PROTOCOL_TCP);
      // net
      PacketLoss pl = new PacketLoss.Zero();
      NetIp net = new NetIp(NetAddress.LOCAL, protMap, pl, pl);
      // trans
      TransTcp tcp = new TransTcp();

      // hookup
      net.setProtocolHandler(Constants.NET_PROTOCOL_TCP, tcp.getProxy());
      tcp.setNetEntity(net.getProxy());

      // applications
      AppJava server = new AppJava(Server.class);
      server.setTcpEntity(tcp.getProxy());
      AppJava client = new AppJava(Client.class);
      client.setTcpEntity(tcp.getProxy());

      // run apps
      server.getProxy().run(null); 
      JistAPI.sleep(1);
      client.getProxy().run(null); 
    }
    catch(Exception e) 
    { 
      e.printStackTrace(); 
    }
      
    JistAPI.sleep (25);
  }

}

