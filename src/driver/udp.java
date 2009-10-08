//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <udp.java Mon 2005/03/14 17:27:05 barr rimbase.rimonbarr.com>
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
import jist.swans.trans.TransUdp;
import jist.swans.app.AppJava;

import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;

/**
 * Small UDP test that can be run both inside and outside of JiST.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: udp.java,v 1.3 2005-03-14 22:26:47 barr Exp $
 * @since JIST1.0
 */

public class udp
{

  /** default server address. */
  public static final String HOST = "localhost";
  /** default client-server port. */
  public static final int PORT = 3001;

  /**
   * Simple UDP server.
   */
  public static class Server
  {
    /**
     * UDP server entry point: open UDP socket and wait for single packet from
     * client.
     *
     * @param args command-line parameters
     */
    public static void main(String[] args)
    {
      try
      {
        System.out.println("server starting at t="+JistAPI.getTime());
        DatagramSocket socket = new DatagramSocket(PORT);
        byte[] buf = new byte[256];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        int received = 0;
        do
        {
          socket.receive(packet);
          received++;
          System.out.println("received at t="+JistAPI.getTime()+
              " ("+packet.getLength()+" bytes) "
              +(new String(packet.getData(), packet.getOffset(), packet.getLength())));
        }
        while(packet.getLength()>0);
        socket.close();
        System.out.println("server received "+received+" packets.");
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }

  } // class: Server

  /**
   * Simple UDP client.
   */
  public static class Client
  {
    /**
     * UDP client entry point: open UDP socket and send off single packet to
     * server.
     *
     * @param args command-line parameters
     */
    public static void main(String[] args)
    {
      try
      {
        System.out.println("client starting at t="+JistAPI.getTime());
        DatagramSocket socket = new DatagramSocket();
        byte[] buf = "hi".getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(HOST), PORT);
        for(int i=0; i<5; i++)
        {
          System.out.println("sent at t="+JistAPI.getTime());
          System.out.flush();
          socket.send(packet);
          Thread.sleep(1000);
        }
        // sending final packet
        System.out.println("terminate packet sent at t="+JistAPI.getTime());
        System.out.flush();
        buf = new byte[0];
        packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(HOST), PORT);
        socket.send(packet);
        socket.close();
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }
  } // class: Client

  /**
   * Program entry point: small UDP test that can be run
   * inside and outside of JiST.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    try
    {
      // protocol mapper
      Mapper protMap = new Mapper(Constants.NET_PROTOCOL_MAX);
      protMap.mapToNext(Constants.NET_PROTOCOL_UDP);
      // net
      PacketLoss pl = new PacketLoss.Zero();
      NetIp net = new NetIp(NetAddress.LOCAL, protMap, pl, pl);
      // trans
      TransUdp udp = new TransUdp();

      // hookup
      net.setProtocolHandler(Constants.NET_PROTOCOL_UDP, udp.getProxy());
      udp.setNetEntity(net.getProxy());

      // applications
      AppJava server = new AppJava(Server.class);
      server.setUdpEntity(udp.getProxy());
      AppJava client = new AppJava(Client.class);
      client.setUdpEntity(udp.getProxy());

      // run apps
      server.getProxy().run(null); 
      JistAPI.sleep(1);
      client.getProxy().run(null); 
    }
    catch(Exception e) 
    { 
      e.printStackTrace(); 
    }
  }

} // class: udp

