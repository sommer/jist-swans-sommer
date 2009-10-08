//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <aodvtest.java Tue 2004/04/06 11:57:37 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package driver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import jist.swans.Constants;
import jist.swans.misc.Util;
import jist.swans.misc.Mapper;
import jist.swans.misc.Location;
import jist.swans.app.AppJava;
import jist.swans.field.Field;
import jist.swans.field.Mobility;
import jist.swans.radio.RadioNoise;
import jist.swans.radio.RadioNoiseIndep;
import jist.swans.radio.RadioInfo;
import jist.swans.route.RouteAodv;
import jist.swans.trans.TransUdp;

import jist.swans.mac.MacAddress;
//import jist.swans.mac.Mac802_11;
import jist.swans.mac.MacDumb;
import jist.swans.net.NetAddress;
import jist.swans.net.NetIp;
import jist.swans.net.PacketLoss;

import jist.runtime.JistAPI;

public class aodvtest
{

  public static final int CLIENT_ID = 1;
  public static final int INTERM_ID = 2;
  public static final int SERVER_ID = 3;

  public static final int PORT = 3001;

  /** random waypoint pause time. */
  public static final int PAUSE_TIME = 30;
  /** random waypoint granularity. */
  public static final int GRANULARITY = 10;
  /** random waypoint minimum speed. */
  public static final int MIN_SPEED = 20;
  /** random waypoint maximum speed. */
  public static final int MAX_SPEED = 100;


  public static class MyServer
  {
    public static void main(String[] args)
    {
      try
      {
        System.out.println("server starting at t=" + JistAPI.getTime());
        DatagramSocket socket = new DatagramSocket(PORT);
        byte buf[] = new byte[256];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        socket.close();
        System.out.println(
          "server received at t="
            + JistAPI.getTime()
            + " ("
            + packet.getLength()
            + " bytes) "
            + (new String(packet.getData(),
              packet.getOffset(),
              packet.getLength())));
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  public static class MyClient 
  {
    
    public static void main(String args[])
    {

      try
      {
        JistAPI.sleep(1*Constants.SECOND);
        System.out.println("client starting at t=" + JistAPI.getTime());
        DatagramSocket socket = new DatagramSocket();
        byte[] buf = "hi".getBytes();

        InetAddress serverIP = new NetAddress(SERVER_ID).getIP();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, serverIP, PORT);
        //      DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(HOST), PORT);
        System.out.println("sent at t=" + JistAPI.getTime());
        System.out.flush();
        
        socket.send(packet);
        /*
        for (int i=0; i<10; i++)
        {
          socket.send(packet);
          JistAPI.sleep(100*Constants.MILLI_SECOND);
        }
        */
        
        socket.close();
        
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
    
    public static void sendMessage()
    {
      
    }
  }

  /**
   * Create node at location (x,y)
   * 
   * @param i node id
   * @param field 
   * @param x node's x-coordinate
   * @param y node's y-coordinate
   * @param radioInfoShared
   * @param protMap protocol mapper
   * @param appClass class of application to run at this node
   */
  public static void createNode(
    int i,
    Field field,
    float x,
    float y,
    RadioInfo.RadioInfoShared radioInfoShared,
    Mapper protMap,
    Class appClass,
    PacketLoss plIn, PacketLoss plOut)
  {
    //create entities
    RadioNoise radio = new RadioNoiseIndep(i, radioInfoShared);
    MacDumb mac = new MacDumb(new MacAddress(i), radio.getRadioInfo());
    NetAddress netAddr = new NetAddress(i);
    NetIp net = new NetIp(netAddr, protMap, plIn, plOut);
    RouteAodv route = new RouteAodv(netAddr);
    route.getProxy().start();
    //route.setDebugMode(true);
    TransUdp udp = new TransUdp();

    //hookup entities
    Location location = new Location.Location2D(x, y);
    field.addRadio(radio.getRadioInfo(), radio.getProxy(), location);
    field.startMobility(radio.getRadioInfo().getUnique().getID());

    //radio hookup
    radio.setFieldEntity(field.getProxy());
    radio.setMacEntity(mac.getProxy());

    //mac hookup
    mac.setRadioEntity(radio.getProxy());
    byte intId = net.addInterface(mac.getProxy());
    //System.out.println("node "+i+"intId="+intId);
    mac.setNetEntity(net.getProxy(), intId);

    //net hookup
    net.setProtocolHandler(Constants.NET_PROTOCOL_UDP, udp.getProxy());
    net.setProtocolHandler(Constants.NET_PROTOCOL_AODV, route.getProxy());
    net.setRouting(route.getProxy());

    //routing hookup
    route.setNetEntity(net.getProxy());

    //trans hookup
    udp.setNetEntity(net.getProxy());

    //application
    AppJava app = null;
    if (appClass != null)
    {
      try
      {
        app = new AppJava(appClass);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      app.setUdpEntity(udp.getProxy());
      app.getProxy().run(null);
    }

  }

  public static void createSim()
  {
    Location.Location2D bounds = new Location.Location2D(2000, 2000);
    //Placement placement = new Placement.Random(bounds);
    Mobility mobility = new Mobility.Static();
    //Mobility mobility = new Mobility.RandomWaypoint(bounds, PAUSE_TIME, GRANULARITY, MAX_SPEED, MIN_SPEED);
    Field field = new Field(bounds);
    field.setMobility(mobility);
    RadioInfo.RadioInfoShared radioInfoShared =
      RadioInfo.createShared(
        Constants.FREQUENCY_DEFAULT,
        Constants.BANDWIDTH_DEFAULT,
        Constants.TRANSMIT_DEFAULT,
        Constants.GAIN_DEFAULT,
        Util.fromDB(Constants.SENSITIVITY_DEFAULT),
        Util.fromDB(Constants.THRESHOLD_DEFAULT),
        Constants.TEMPERATURE_DEFAULT,
        Constants.TEMPERATURE_FACTOR_DEFAULT,
        Constants.AMBIENT_NOISE_DEFAULT);

    // protocol mapper
    Mapper protMap = new Mapper(Constants.NET_PROTOCOL_MAX);
    protMap.mapToNext(Constants.NET_PROTOCOL_UDP);
    protMap.mapToNext(Constants.NET_PROTOCOL_AODV);

    // packet loss
    PacketLoss plIn = new PacketLoss.Zero();
    PacketLoss plOut = new PacketLoss.Zero();

    //create client node
    System.out.println("Starting client node...");
    createNode(
      CLIENT_ID,
      field,
      0,
      0,
      radioInfoShared,
      protMap,
      MyClient.class,
      plIn, plOut);

    //create intermediate node
    System.out.println("Starting intermediate node...");
    createNode(INTERM_ID, field, 400, 400, radioInfoShared, protMap, null, plIn, plOut);

    //create intermediate node
    //System.out.println("attempting to start another intermediate node ");
    //createNode(4, field, 800, 800, radioInfoShared, protMap, null, plIn, plOut);

    //create intermediate node
    //System.out.println("attempting to start another intermediate node ");
    //createNode(5, field, 600, 600, radioInfoShared, protMap, null);

    //create server node
    
    System.out.println("Startin server node...");
    createNode(
      SERVER_ID,
      field,
      800,
      800,
      radioInfoShared,
      protMap,
      MyServer.class,
      plIn, plOut);

    System.out.println("done creating sim");

  }

  public static void main(String[] args)
  {
    System.out.println("Hello World!!");
    createSim();
  }
}
