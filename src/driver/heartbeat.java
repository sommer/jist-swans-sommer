//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <heartbeat.java Tue 2004/04/06 11:57:52 barr pompom.cs.cornell.edu>
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
import jist.swans.field.Mobility;
import jist.swans.field.Spatial;
import jist.swans.field.Fading;
import jist.swans.field.PathLoss;
import jist.swans.radio.RadioNoiseIndep;
import jist.swans.radio.RadioInfo;
import jist.swans.mac.MacAddress;
import jist.swans.mac.MacDumb;
import jist.swans.net.NetAddress;
import jist.swans.net.NetIp;
import jist.swans.net.PacketLoss;
import jist.swans.app.AppHeartbeat;

import jist.runtime.JistAPI;

/**
 * SWANS demo/test: heartbeat application.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: heartbeat.java,v 1.16 2004-11-22 16:51:05 barr Exp $
 */

public class heartbeat
{

  /** random waypoint pause time. */
  public static final int PAUSE_TIME = 30;
  /** random waypoint granularity. */
  public static final int GRANULARITY = 10;
  /** random waypoint minimum speed. */
  public static final int MIN_SPEED = 2;
  /** random waypoint maximum speed. */
  public static final int MAX_SPEED = 10;

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
  public static void createNode(int i,
      Field field, Placement placement,
      RadioInfo.RadioInfoShared radioInfoShared, Mapper protMap,
      PacketLoss plIn, PacketLoss plOut)
  {
    // create entities
    RadioNoiseIndep radio = new RadioNoiseIndep(i, radioInfoShared);
    MacDumb mac = new MacDumb(new MacAddress(i), radio.getRadioInfo());
    //Mac802_11 mac = new Mac802_11(new MacAddress(i), radio.getRadioInfo());
    NetIp net = new NetIp(new NetAddress(i), protMap, plIn, plOut);
    AppHeartbeat app = new AppHeartbeat(i, true);

    // hookup entities
    field.addRadio(radio.getRadioInfo(), radio.getProxy(), placement.getNextLocation());
    field.startMobility(radio.getRadioInfo().getUnique().getID());
    radio.setFieldEntity(field.getProxy());
    radio.setMacEntity(mac.getProxy());
    mac.setRadioEntity(radio.getProxy());
    byte intId = net.addInterface(mac.getProxy());
    mac.setNetEntity(net.getProxy(), intId);
    net.setProtocolHandler(Constants.NET_PROTOCOL_HEARTBEAT, app.getNetProxy());
    app.setNetEntity(net.getProxy());
    app.getAppProxy().run(null);
  }

  /**
   * Initialize simulation field.
   *
   * @param nodes number of nodes
   * @param length length of field
   * @return simulation field
   */
  public static Field createSim(int nodes, int length)
  {
    Location.Location2D bounds = new Location.Location2D(length, length);
    Placement placement = new Placement.Random(bounds);
    Mobility mobility = new Mobility.RandomWaypoint(bounds, PAUSE_TIME, GRANULARITY, MAX_SPEED, MIN_SPEED);
    Spatial spatial = new Spatial.HierGrid(bounds, 5);
    Fading fading = new Fading.None();
    PathLoss pathloss = new PathLoss.FreeSpace();
    Field field = new Field(spatial, fading, pathloss, mobility, Constants.PROPAGATION_LIMIT_DEFAULT);

    RadioInfo.RadioInfoShared radioInfoShared = RadioInfo.createShared(
        Constants.FREQUENCY_DEFAULT, Constants.BANDWIDTH_DEFAULT,
        Constants.TRANSMIT_DEFAULT, Constants.GAIN_DEFAULT,
        Util.fromDB(Constants.SENSITIVITY_DEFAULT), Util.fromDB(Constants.THRESHOLD_DEFAULT),
        Constants.TEMPERATURE_DEFAULT, Constants.TEMPERATURE_FACTOR_DEFAULT, Constants.AMBIENT_NOISE_DEFAULT);

    Mapper protMap = new Mapper(Constants.NET_PROTOCOL_MAX);
    protMap.mapToNext(Constants.NET_PROTOCOL_HEARTBEAT);
    PacketLoss pl = new PacketLoss.Zero();

    for(int i=0; i<nodes; i++)
    {
      createNode(i, field, placement, radioInfoShared, protMap, pl, pl);
    }

    return field;
  }

  /**
   * Benchmark entry point: heartbeat test.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    if(args.length<3)
    {
      System.out.println("syntax: swans driver.heartbeat <nodes> <length> <time>");
      System.out.println("    eg: swans driver.heartbeat 5 100 5");
      return;
    }

    int nodes = Integer.parseInt(args[0]);
    int length = Integer.parseInt(args[1]);
    int time = Integer.parseInt(args[2]);
    float density = nodes / (float)(length/1000.0 * length/1000.0);

    System.out.println("nodes   = "+nodes);
    System.out.println("size    = "+length+" x "+length);
    System.out.println("time    = "+time+" seconds");
    System.out.print("Creating simulation nodes... ");
    Field f = createSim(nodes, length);
    System.out.println("done.");

    System.out.println("Average density = "+f.computeDensity()*1000*1000+"/km^2");
    System.out.println("Average sensing = "+f.computeAvgConnectivity(true));
    System.out.println("Average receive = "+f.computeAvgConnectivity(false));
    JistAPI.endAt(time*Constants.SECOND);
  }

}

