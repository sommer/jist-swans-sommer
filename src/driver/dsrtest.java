//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <dsrtest.java Tue 2004/04/06 11:57:46 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package driver;

import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.misc.Util;
import jist.swans.misc.Mapper;
import jist.swans.misc.Location;
import jist.swans.field.Field;
import jist.swans.radio.RadioNoise;
import jist.swans.radio.RadioNoiseIndep;
import jist.swans.radio.RadioInfo;
import jist.swans.mac.MacAddress;
import jist.swans.mac.Mac802_11;
import jist.swans.net.NetAddress;
import jist.swans.net.NetIp;
import jist.swans.net.PacketLoss;
import jist.swans.trans.TransUdp;
import jist.swans.app.AppJava;
import jist.swans.route.RouteDsr;

import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;

public class dsrtest
{

  public static final byte[] SERVER_IP = {1, 2, 3, 4};
  public static final int PORT = 3001;

  public static class Server
  {
    public static void main(String args[])
    {
      try
      {
        System.out.println("server starting at t="+JistAPI.getTime());
        DatagramSocket socket = new DatagramSocket(PORT);
        byte buf[] = new byte[256];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        socket.close();
        System.out.println("received at t="+JistAPI.getTime()+
            " ("+packet.getLength()+" bytes) "
            +(new String(packet.getData(), packet.getOffset(), packet.getLength())));
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  public static class Client
  {
    public static void main(String args[])
    {
      try
      {
        System.out.println("client starting at t="+JistAPI.getTime());
        DatagramSocket socket = new DatagramSocket();
        byte[] buf = "hi".getBytes();
        DatagramPacket packet = new DatagramPacket(
          buf,
          buf.length,
          InetAddress.getByAddress(SERVER_IP),
          PORT);

        System.out.println("sent at t="+JistAPI.getTime());
        System.out.flush();
        socket.send(packet);
        socket.close();
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  private static void TwoNodeTest()
  {
   try
    {
      // field
      Location bounds = new Location.Location2D(200, 200);
      Field field = new Field(bounds);
      RadioInfo.RadioInfoShared radioInfoShared = RadioInfo.createShared(
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
      protMap.mapToNext(Constants.NET_PROTOCOL_DSR);
      protMap.mapToNext(Constants.NET_PROTOCOL_UDP);
      // packet loss
      PacketLoss pl = new PacketLoss.Zero();
      // nodes
      int i=1;
      // radio
      RadioNoise radio1 = new RadioNoiseIndep(i, radioInfoShared);
      RadioNoise radio2 = new RadioNoiseIndep(i+1, radioInfoShared);
      // mac
      Mac802_11 mac1 = new Mac802_11(new MacAddress(i), radio1.getRadioInfo());
      Mac802_11 mac2 = new Mac802_11(new MacAddress(i+1), radio2.getRadioInfo());
      // net
      NetIp net1 = new NetIp(new NetAddress(SERVER_IP), protMap, pl, pl);
      NetIp net2 = new NetIp(new NetAddress(i), protMap, pl, pl);
      // route
      RouteDsr dsr1 = new RouteDsr(net1.getAddress());
      RouteDsr dsr2 = new RouteDsr(net2.getAddress());
      // trans
      TransUdp udp1 = new TransUdp();
      TransUdp udp2 = new TransUdp();

      // field hookup
      Location location1 = new Location.Location2D(0, 0);
      Location location2 = new Location.Location2D(0, 1);
      field.addRadio(radio1.getRadioInfo(), radio1.getProxy(), location1);
      field.addRadio(radio2.getRadioInfo(), radio2.getProxy(), location2);
      // radio hookup
      radio1.setFieldEntity(field.getProxy());
      radio2.setFieldEntity(field.getProxy());
      radio1.setMacEntity(mac1.getProxy());
      radio2.setMacEntity(mac2.getProxy());
      // route hookup
      dsr1.setNetEntity(net1.getProxy());
      dsr2.setNetEntity(net2.getProxy());
      // net hookup
      byte intId1 = net1.addInterface(mac1.getProxy());
      byte intId2 = net2.addInterface(mac2.getProxy());
      net1.setProtocolHandler(Constants.NET_PROTOCOL_DSR, dsr1.getProxy());
      net2.setProtocolHandler(Constants.NET_PROTOCOL_DSR, dsr2.getProxy());
      net1.setRouting(dsr1.getProxy());
      net2.setRouting(dsr2.getProxy());
      // mac hookup
      mac1.setRadioEntity(radio1.getProxy());
      mac2.setRadioEntity(radio2.getProxy());
      mac1.setNetEntity(net1.getProxy(), intId1);
      mac2.setNetEntity(net2.getProxy(), intId2);
      // trans hookup
      udp1.setNetEntity(net1.getProxy());
      udp2.setNetEntity(net2.getProxy());
      net1.setProtocolHandler(Constants.NET_PROTOCOL_UDP, udp1.getProxy());
      net2.setProtocolHandler(Constants.NET_PROTOCOL_UDP, udp2.getProxy());

      // applications
      AppJava server = new AppJava(Server.class);
      server.setUdpEntity(udp1.getProxy());
      AppJava client = new AppJava(Client.class);
      client.setUdpEntity(udp2.getProxy());

      // run apps
      server.getProxy().run(null); 
      JistAPI.sleep(1);
      client.getProxy().run(null);
    }
    catch(Exception e) { e.printStackTrace(); }
  }

  private static void ThreeNodeTest()
  {
   try
    {
      // field
      Location bounds = new Location.Location2D(200, 1500);
      Field field = new Field(bounds);
      RadioInfo.RadioInfoShared radioInfoShared = RadioInfo.createShared(
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
      protMap.mapToNext(Constants.NET_PROTOCOL_DSR);
      protMap.mapToNext(Constants.NET_PROTOCOL_UDP);
      // packet loss
      PacketLoss pl = new PacketLoss.Zero();
      // nodes
      int i=1;
      // radio
      RadioNoise radio1 = new RadioNoiseIndep(i, radioInfoShared);
      RadioNoise radio2 = new RadioNoiseIndep(i+1, radioInfoShared);
      RadioNoise radio3 = new RadioNoiseIndep(i+2, radioInfoShared);
      // mac
      Mac802_11 mac1 = new Mac802_11(new MacAddress(i), radio1.getRadioInfo());
      Mac802_11 mac2 = new Mac802_11(new MacAddress(i+1), radio2.getRadioInfo());
      Mac802_11 mac3 = new Mac802_11(new MacAddress(i+2), radio2.getRadioInfo());
      // net
      NetIp net1 = new NetIp(new NetAddress(SERVER_IP), protMap, pl, pl);
      NetIp net2 = new NetIp(new NetAddress(i), protMap, pl, pl);
      NetIp net3 = new NetIp(new NetAddress(i+1), protMap, pl, pl);
      // route
      RouteDsr dsr1 = new RouteDsr(net1.getAddress());
      RouteDsr dsr2 = new RouteDsr(net2.getAddress());
      RouteDsr dsr3 = new RouteDsr(net3.getAddress());
      // trans
      TransUdp udp1 = new TransUdp();
      TransUdp udp2 = new TransUdp();
      TransUdp udp3 = new TransUdp();

      // field hookup
      Location location1 = new Location.Location2D(0, 0);
      Location location2 = new Location.Location2D(0, 600);
      Location location3 = new Location.Location2D(0, 1200);
      field.addRadio(radio1.getRadioInfo(), radio1.getProxy(), location1);
      field.addRadio(radio2.getRadioInfo(), radio2.getProxy(), location2);
      field.addRadio(radio3.getRadioInfo(), radio3.getProxy(), location3);
      // radio hookup
      radio1.setFieldEntity(field.getProxy());
      radio2.setFieldEntity(field.getProxy());
      radio3.setFieldEntity(field.getProxy());
      radio1.setMacEntity(mac1.getProxy());
      radio2.setMacEntity(mac2.getProxy());
      radio3.setMacEntity(mac3.getProxy());
      // route hookup
      dsr1.setNetEntity(net1.getProxy());
      dsr2.setNetEntity(net2.getProxy());
      dsr3.setNetEntity(net3.getProxy());
      // net hookup
      byte intId1 = net1.addInterface(mac1.getProxy());
      byte intId2 = net2.addInterface(mac2.getProxy());
      byte intId3 = net3.addInterface(mac3.getProxy());
      net1.setProtocolHandler(Constants.NET_PROTOCOL_DSR, dsr1.getProxy());
      net2.setProtocolHandler(Constants.NET_PROTOCOL_DSR, dsr2.getProxy());
      net3.setProtocolHandler(Constants.NET_PROTOCOL_DSR, dsr3.getProxy());
      net1.setRouting(dsr1.getProxy());
      net2.setRouting(dsr2.getProxy());
      net3.setRouting(dsr3.getProxy());
      // mac hookup
      mac1.setRadioEntity(radio1.getProxy());
      mac2.setRadioEntity(radio2.getProxy());
      mac3.setRadioEntity(radio3.getProxy());
      mac1.setNetEntity(net1.getProxy(), intId1);
      mac2.setNetEntity(net2.getProxy(), intId2);
      mac3.setNetEntity(net3.getProxy(), intId3);
      // trans hookup
      udp1.setNetEntity(net1.getProxy());
      udp2.setNetEntity(net2.getProxy());
      udp3.setNetEntity(net3.getProxy());
      net1.setProtocolHandler(Constants.NET_PROTOCOL_UDP, udp1.getProxy());
      net2.setProtocolHandler(Constants.NET_PROTOCOL_UDP, udp2.getProxy());
      net3.setProtocolHandler(Constants.NET_PROTOCOL_UDP, udp3.getProxy());

      // applications
      AppJava server = new AppJava(Server.class);
      server.setUdpEntity(udp1.getProxy());
      AppJava client = new AppJava(Client.class);
      client.setUdpEntity(udp3.getProxy());

      // run apps
      server.getProxy().run(null); 
      JistAPI.sleep(1);
      client.getProxy().run(null);
    }
    catch(Exception e) { e.printStackTrace(); }
  }


  public static void main(String args[])
  {
    TwoNodeTest();
    ThreeNodeTest();
  }
}
