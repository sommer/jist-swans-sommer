//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <memory.java Mon 2005/07/11 08:55:09 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package driver;

import jist.swans.Constants;
import jist.swans.misc.Util;
import jist.swans.misc.Mapper;
import jist.swans.misc.Location;
import jist.swans.field.Field;
import jist.swans.field.Placement;
import jist.swans.radio.RadioNoise;
import jist.swans.radio.RadioNoiseIndep;
import jist.swans.radio.RadioInfo;
import jist.swans.mac.MacAddress;
import jist.swans.mac.Mac802_11;
import jist.swans.net.NetAddress;
import jist.swans.net.NetIp;
import jist.swans.net.PacketLoss;
import jist.swans.trans.TransUdp;
import jist.swans.trans.TransTcp;
import jist.swans.app.AppJava;

/**
 * SWANS memory benchmark.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: memory.java,v 1.12 2005-07-11 12:02:35 barr Exp $
 */

public class memory
{

  /**
   * Initialize simulation node.
   *
   * @param i node number
   * @param field simulation field
   * @param placement node placement model
   * @param radioInfoShared shared radio information
   * @param protMap shared protocol map
   * @param plIn incoming packet loss model
   * @param plOut outgoing packet loss model
   */
  private static void createNode(int i, Field field, Placement placement,
      RadioInfo.RadioInfoShared radioInfoShared, Mapper protMap,
      PacketLoss plIn, PacketLoss plOut)
  {
    // radio
    RadioNoise radio = new RadioNoiseIndep(i, radioInfoShared);
    // mac
    Mac802_11 mac = new Mac802_11(new MacAddress(i), radio.getRadioInfo());
    // net
    NetIp net = new NetIp(new NetAddress(i), protMap, plIn, plOut);
    // trans
    TransUdp udp = new TransUdp();
    TransTcp tcp = new TransTcp();
    // app
    AppJava app = null;
    try
    {
      app = new AppJava("driver.udp");
    }
    catch(ClassNotFoundException e) { throw new RuntimeException(e); }
    catch(NoSuchMethodException e) { throw new RuntimeException(e); }
    // field hookup
    field.addRadio(radio.getRadioInfo(), radio.getProxy(), placement.getNextLocation());
    // radio hookup
    radio.setFieldEntity(field.getProxy());
    radio.setMacEntity(mac.getProxy());
    // net hookup
    byte intId = net.addInterface(mac.getProxy());
    net.setProtocolHandler(Constants.NET_PROTOCOL_UDP, udp.getProxy());
    // mac hookup
    mac.setRadioEntity(radio.getProxy());
    mac.setNetEntity(net.getProxy(), intId);
    // trans hookup
    udp.setNetEntity(net.getProxy());
    tcp.setNetEntity(net.getProxy());
    // app hookup
    app.setUdpEntity(udp.getProxy());
    app.setTcpEntity(tcp.getProxy());
  }

  /**
   * Initialize simulation field.
   *
   * @param numnodes number of nodes
   */
  public static void createSim(int numnodes)
  {
    try
    {
      Location bounds = new Location.Location2D(200, 200);
      // field
      Field field = new Field(bounds);
      // placement
      Placement placement = new Placement.Random(bounds);
      // radio info
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
      protMap.mapToNext(Constants.NET_PROTOCOL_UDP);
      // packet loss
      PacketLoss pl = new PacketLoss.Zero();
      // nodes
      for(int i=0; i<numnodes; i++)
      {
        if(i%1000==0) System.out.print(".");
        createNode(i, field, placement, radioInfoShared, protMap, pl, pl);
      }
      System.out.println();
      System.out.flush();
      Util.printMemoryStats();
      // memprof.memprof.dumpHeap("rimon");
    }
    catch(Exception e) { e.printStackTrace(); }
  }

  /**
   * Benchmark entry point: SWANS memory benchmark.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    if(args.length<1)
    {
      System.out.println("syntax: memory <nodes>");
      return;
    }
    int nodes = Integer.valueOf(args[0]).intValue();
    System.out.println("Creating "+nodes+" nodes");
    createSim(nodes);
  }

} // class: memory

