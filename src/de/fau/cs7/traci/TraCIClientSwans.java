/*
 * TraCIClientSwans - TraCI client for JiST / SWANS
 * Copyright (C) 2009 Christoph Sommer <christoph.sommer@informatik.uni-erlangen.de>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 */

package de.fau.cs7.traci;

import de.uniluebeck.itm.tcpip.Socket;
import de.uniluebeck.itm.tcpip.Storage;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.*;
import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.Mobility;
import jist.swans.field.FieldInterface;
import jist.swans.misc.Location;

public class TraCIClientSwans implements Mobility {

	protected class TraCIMobilityInfo implements MobilityInfo {
		protected FieldInterface mobility_f;
		protected Integer mobility_id;
		protected int traci_id;
		protected Object mobility_object;
		protected Location position;
		protected String road_id;
		protected double speed;
		protected double angle;
		protected double allowed_speed;
	}

	public interface ModuleCreator {
		Object createModule(byte domain, int traciId);
		void destroyModule(Object module);
	}

	public class TraCIController implements JistAPI.Entity {
		private TraCIClientSwans client;
		
		public TraCIController(TraCIClientSwans client) {
			JistAPI.log("foo");
			logDebug("TraCIController: create");
			this.client = client;
		}
		
		public void step() {
			logDebug("TraCIController: step");
			// perform timestep
			client.executeOneTimestep();
			if (client.stopSimulation) JistAPI.end();

			// move all radios
			for (Iterator it = client.mobilityInfoByTraciId.values().iterator(); it.hasNext();) {
				TraCIMobilityInfo i = (TraCIMobilityInfo)it.next();
				logInfo("moveRadio "+i.traci_id+" (MID "+i.mobility_id+")");
				i.mobility_f.moveRadio(i.mobility_id, i.position);
			}

			// wait 1s
			JistAPI.sleep(1*Constants.SECOND);

			step();			
		}
		
		public void start() {
			step();
		}
		
	}

	protected static final byte CMD_FILE_SEND = (byte)0x75;

	protected static final byte CMD_SIMSTEP = (byte)0x01;
	protected static final byte CMD_SUBSCRIBELIFECYCLES = (byte)0x61;
	protected static final byte CMD_UNSUBSCRIBELIFECYCLES = (byte)0x62;
	protected static final byte CMD_OBJECTCREATION = (byte)0x63;
	protected static final byte CMD_OBJECTDESTRUCTION = (byte)0x64;
	protected static final byte CMD_SUBSCRIBEDOMAIN = (byte)0x65;
	protected static final byte CMD_UNSUBSCRIBEDOMAIN = (byte)0x66;
	protected static final byte CMD_UPDATEOBJECT = (byte)0x67;

	protected static final byte POSITION_NONE = (byte)0x00;
	protected static final byte POSITION_2D = (byte)0x01;
	protected static final byte POSITION_2_5D = (byte)0x02;
	protected static final byte POSITION_3D = (byte)0x03;
	protected static final byte POSITION_ROADMAP = (byte)0x04;

	protected static final byte TYPE_BOUNDINGBOX = (byte)0x05;
	protected static final byte TYPE_POLYGON = (byte)0x06;
	protected static final byte TYPE_UBYTE = (byte)0x07;
	protected static final byte TYPE_BYTE = (byte)0x08;
	protected static final byte TYPE_INTEGER = (byte)0x09;
	protected static final byte TYPE_FLOAT = (byte)0x0A;
	protected static final byte TYPE_DOUBLE = (byte)0x0B;
	protected static final byte TYPE_STRING = (byte)0x0C;
	protected static final byte TYPE_TLPHASELIST = (byte)0x0D;
	protected static final byte TYPE_STRINGLIST = (byte)0x0E;

	protected static final byte RTYPE_OK = (byte)0x00;
	protected static final byte RTYPE_NOTIMPLEMENTED = (byte)0x01;
	protected static final byte RTYPE_ERR = (byte)0xFF;

	protected static final byte DOM_ROADMAP = (byte)0x00;
	protected static final byte DOM_VEHICLE = (byte)0x01;
	protected static final byte DOM_TRAFFICLIGHTS = (byte)0x02;
	protected static final byte DOM_POI = (byte)0x03;
	protected static final byte DOM_POLYGON = (byte)0x04;

	protected static final byte DOMVAR_COUNT = (byte)0x01;
	protected static final byte DOMVAR_POSITION = (byte)0x02;
	protected static final byte DOMVAR_BOUNDINGBOX = (byte)0x03;
	protected static final byte DOMVAR_SPEED = (byte)0x04;
	protected static final byte DOMVAR_CURTLPHASE = (byte)0x05;
	protected static final byte DOMVAR_NEXTTLPHASE = (byte)0x06;
	protected static final byte DOMVAR_TYPE = (byte)0x07;
	protected static final byte DOMVAR_LAYER = (byte)0x08;
	protected static final byte DOMVAR_SHAPE = (byte)0x09;
	protected static final byte DOMVAR_MAXCOUNT = (byte)0x0A;
	protected static final byte DOMVAR_EQUIPPEDCOUNT = (byte)0x0B;
	protected static final byte DOMVAR_EQUIPPEDCOUNTMAX = (byte)0x0C;
	protected static final byte DOMVAR_NAME = (byte)0x0D;
	protected static final byte DOMVAR_ROUTE = (byte)0x0E;
	protected static final byte DOMVAR_ALLOWED_SPEED = (byte)0x0F;
	protected static final byte DOMVAR_AIRDISTANCE = (byte)0x10;
	protected static final byte DOMVAR_DRIVINGDISTANCE = (byte)0x11;
	protected static final byte DOMVAR_EXTID = (byte)0x12;
	protected static final byte DOMVAR_ANGLE = (byte)0x13;
	protected static final byte DOMVAR_SIMTIME = (byte)0x14;
	protected static final byte DOMVAR_CO2EMISSION = (byte)0x20;
	protected static final byte DOMVAR_COEMISSION = (byte)0x21;
	protected static final byte DOMVAR_HCEMISSION = (byte)0x22;
	protected static final byte DOMVAR_PMXEMISSION = (byte)0x23;
	protected static final byte DOMVAR_NOXEMISSION = (byte)0x24;
	protected static final byte DOMVAR_FUELCONSUMPTION = (byte)0x25;
	protected static final byte DOMVAR_NOISEEMISSION = (byte)0x26;
	protected static final byte CMD_STOP = (byte)0x12;
	protected static final byte CMD_CHANGEROUTE = (byte)0x30;
	protected static final byte CMD_DISTANCEREQUEST = (byte)0x72;
	protected static final byte CMD_SETMAXSPEED = (byte)0x11;

	protected static final byte REQUEST_AIRDIST = (byte)0x00;
	protected static final byte REQUEST_DRIVINGDIST = (byte)0x01;

	protected static final byte CMD_SET_TL_VARIABLE = (byte)0xc2;
	protected static final byte TL_PHASE_INDEX = (byte)0x22;
	protected static final byte TL_PROGRAM = (byte)0x23;

	public static final Logger log = Logger.getLogger(TraCIClientSwans.class.getName());

	protected String host;
	protected int port;
	protected String launchConfig;
	protected ModuleCreator moduleCreator;

	protected boolean autoShutdown;
	
	protected TraCIMobilityInfo last_mobility_info; /**< stored by ::init(), retrieved by ::processObjectCreation() */

	protected boolean stopSimulation;
	protected int nextCallsUntilStepRequired;
	protected int nextCallsUntilStepLeft;
	protected Socket socket;
	protected HashMap mobilityInfoByMobilityId; /**< (mobility_id -> TraCIMobilityInfo) */
	protected HashMap mobilityInfoByTraciId; /**< (mobility_id -> TraCIMobilityInfo) */

	public TraCIClientSwans(String host, int port, String launchConfig) {
		logInfo("initializing TraCI client");
		this.host = host;
		this.port = port;
		this.launchConfig = launchConfig;

		autoShutdown = true;

		stopSimulation = false;
		nextCallsUntilStepRequired = 0;
		nextCallsUntilStepLeft = 0;
		mobilityInfoByMobilityId = new HashMap();
		mobilityInfoByTraciId = new HashMap();
	}

	public void setModuleCreator(ModuleCreator moduleCreator) {
		this.moduleCreator = moduleCreator;
	}

	public void start() throws RuntimeException {

		if (moduleCreator == null) throw new RuntimeException("no moduleCreator set");

		try {

			socket = new Socket(host, port);
			socket.connect();
			sendFile("sumo-launchd.launch.xml", launchConfig);
			subscribeLifecycles();
			subscribeVehicleDomain();
			
			TraCIController controller = new TraCIController(this);
			controller.start();

		} catch (java.net.UnknownHostException e) {
			throw new RuntimeException(e);
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized MobilityInfo init(FieldInterface f, Integer id, Location loc) {
		logDebug("MobilityInfo::init "+id);

		last_mobility_info = new TraCIMobilityInfo();
		last_mobility_info.mobility_f = f;
		last_mobility_info.mobility_id = id;

		return last_mobility_info;
	}

	public synchronized void next(FieldInterface f, Integer id, Location loc, MobilityInfo info) {
	}

	protected synchronized void finalize() throws Throwable {
		try {
			socket.close();
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		} finally {
			super.finalize();
		}
	}

	protected void logDebug(String s) {
		if(!log.isDebugEnabled()) return;
		log.debug("t="+(float)JistAPI.getTime()/Constants.SECOND+": "+s);
	}

	protected void logInfo(String s) {
		if(!log.isInfoEnabled()) return;
		log.info("t="+(float)JistAPI.getTime()/Constants.SECOND+": "+s);
	}

	protected void sendTraCICommand(byte commandId, Storage cmd) throws java.io.IOException {
			Storage buf = new Storage();
			int len = cmd.size();
			if (1 + 1 + len <= 0xFF) {
				buf.writeUnsignedByte(1 + 1 + len);
			} else {
				buf.writeUnsignedByte(0);
				buf.writeInt(1 + 4 + 1 + len);
			}
			buf.writeUnsignedByte(commandId);
			buf.writeBytes(cmd.getBytes());

			socket.sendExact(buf);

			logDebug("sending command "+commandId);
	}

	protected Storage queryTraCI(byte commandId, Storage cmd) throws java.io.IOException {
		sendTraCICommand(commandId, cmd);

		Storage msg = socket.receiveExact();
		byte cmdLength = (byte)msg.readUnsignedByte();
		byte commandResp = (byte)msg.readUnsignedByte(); 
		if (commandResp != commandId) throw new RuntimeException("Expected response to command "+commandId+", but got one for command "+commandResp);
		byte result = (byte)msg.readUnsignedByte();
		String description = msg.readStringUTF8();
		if (result != RTYPE_OK) throw new RuntimeException("non-OK response to command "+commandId+". Error message was: \""+description+"\"");
		
		return msg;

	}

	protected void sendFile(String fname, String contents) throws java.io.IOException {
		Storage cmd = new Storage();
		cmd.writeStringUTF8(fname);
		cmd.writeStringUTF8(contents);
		queryTraCI(CMD_FILE_SEND, cmd);
	}


	protected void subscribeLifecycles() throws java.io.IOException {
		Storage cmd = new Storage();
		cmd.writeUnsignedByte(DOM_VEHICLE);
		queryTraCI(CMD_SUBSCRIBELIFECYCLES, cmd);
	}

	protected void subscribeVehicleDomain() throws java.io.IOException {
		Storage cmd = new Storage();
		cmd.writeUnsignedByte(DOM_VEHICLE);
		cmd.writeUnsignedByte(6);
		cmd.writeUnsignedByte(DOMVAR_SIMTIME);
		cmd.writeUnsignedByte(TYPE_DOUBLE);
		cmd.writeUnsignedByte(DOMVAR_POSITION);
		cmd.writeUnsignedByte(POSITION_2D);
		cmd.writeUnsignedByte(DOMVAR_POSITION);
		cmd.writeUnsignedByte(POSITION_ROADMAP);
		cmd.writeUnsignedByte(DOMVAR_SPEED);
		cmd.writeUnsignedByte(TYPE_FLOAT);
		cmd.writeUnsignedByte(DOMVAR_ANGLE);
		cmd.writeUnsignedByte(TYPE_FLOAT);
		cmd.writeUnsignedByte(DOMVAR_ALLOWED_SPEED);
		cmd.writeUnsignedByte(TYPE_FLOAT);
		queryTraCI(CMD_SUBSCRIBEDOMAIN, cmd);
	}

	protected void processObjectCreation(byte domain, int traciId) throws RuntimeException {
		logInfo("handling creation of node: "+traciId);
		if (domain != DOM_VEHICLE) throw new RuntimeException("Expected DOM_VEHICLE, but got " + domain);

		last_mobility_info = null;
		Object o = moduleCreator.createModule(domain, traciId);
		if (last_mobility_info == null) throw new RuntimeException("moduleCreator.createModule failed to call TraCIClientSwans::init");

		last_mobility_info.traci_id = traciId;
		last_mobility_info.mobility_object = o;
		mobilityInfoByMobilityId.put(last_mobility_info.mobility_id, last_mobility_info);
		mobilityInfoByTraciId.put(Integer.valueOf(last_mobility_info.traci_id), last_mobility_info);
	}

	protected void processObjectDestruction(byte domain, int traciId) throws RuntimeException {
		logInfo("handling destruction of node: "+traciId);
		if (domain != DOM_VEHICLE) throw new RuntimeException("Expected DOM_VEHICLE, but got " + domain);

		TraCIMobilityInfo mobilityInfo = (TraCIMobilityInfo)mobilityInfoByTraciId.get(Integer.valueOf(traciId));
		if (mobilityInfo == null) throw new RuntimeException("processObjectDestruction called for unknown node #"+traciId);
		
		moduleCreator.destroyModule(mobilityInfo.mobility_object);
		
		mobilityInfoByMobilityId.remove(mobilityInfo.mobility_id);
		mobilityInfoByTraciId.remove(Integer.valueOf(mobilityInfo.traci_id));
	}

	protected Location traci2swans(float px, float py) {
		return new Location.Location2D(px, py);
	}

	protected float swans2traci_px(Location loc) {
		return loc.getX();
	}

	protected float swans2traci_py(Location loc) {
		return loc.getY();		
	}

	protected void processUpdateObject(byte domain, int traciId, Storage buf) throws RuntimeException {
		logDebug("handling update of node: "+traciId);
		if (domain != DOM_VEHICLE) throw new RuntimeException("Expected DOM_VEHICLE, but got " + domain);

		double targetTime = buf.readDouble();
		float px = buf.readFloat();
		float py = buf.readFloat();
		String edge = buf.readStringUTF8();
		float speed = buf.readFloat();
		float angle = buf.readFloat();
		float allowed_speed = buf.readFloat();

		TraCIMobilityInfo mobilityInfo = (TraCIMobilityInfo)mobilityInfoByTraciId.get(Integer.valueOf(traciId));
		if (mobilityInfo == null) throw new RuntimeException("processUpdateObject called for unknown node #"+traciId);

		mobilityInfo.position = traci2swans(px, py);
		mobilityInfo.road_id = edge;
		mobilityInfo.speed = speed;
		mobilityInfo.angle = angle;
		mobilityInfo.allowed_speed = allowed_speed;		
	}

	protected void executeOneTimestep() throws RuntimeException {
		try {
			double targetTime = 1;
			Storage cmd = new Storage();
			cmd.writeDouble(targetTime);
			cmd.writeUnsignedByte(POSITION_NONE);
			Storage buf = queryTraCI(CMD_SIMSTEP, cmd);

			while (buf.validPos()) {
				byte cmdLength = (byte)buf.readUnsignedByte();
				byte commandId = (byte)buf.readUnsignedByte();
				logDebug("processing "+cmdLength+" bytes of cmd #"+commandId);

				if (commandId == CMD_OBJECTCREATION) {
					byte domain = (byte)buf.readUnsignedByte();
					int nodeId = (byte)buf.readInt();
					processObjectCreation(domain, nodeId);
				}
				else if (commandId == CMD_OBJECTDESTRUCTION) {
					byte domain = (byte)buf.readUnsignedByte();
					int nodeId = (byte)buf.readInt();
					processObjectDestruction(domain, nodeId);
					if (autoShutdown && (mobilityInfoByTraciId.size() <= 0)) {
						stopSimulation = true;
					}
				}
				else if (commandId == CMD_UPDATEOBJECT) {
					byte domain = (byte)buf.readUnsignedByte();
					int nodeId = (byte)buf.readInt();
					processUpdateObject(domain, nodeId, buf);
				}
				else {
					throw new RuntimeException("Unknown command: "+commandId);
				}
			}
		} catch (java.io.IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void commandSetMaximumSpeed(int nodeId, float maxSpeed) throws java.io.IOException {
		Storage cmd = new Storage();
		cmd.writeInt(nodeId);
		cmd.writeFloat(maxSpeed);
		Storage buf = queryTraCI(CMD_SETMAXSPEED, cmd);
		if (!buf.validPos()) throw new java.io.IOException("expected only an OK response, but received additional bytes");
	}

	public void commandChangeRoute(int nodeId, String roadId, double travelTime) throws java.io.IOException {
		Storage cmd = new Storage();
		cmd.writeInt(nodeId);
		cmd.writeStringUTF8(roadId);
		cmd.writeDouble(travelTime);
		Storage buf = queryTraCI(CMD_CHANGEROUTE, cmd);
		if (!buf.validPos()) throw new java.io.IOException("expected only an OK response, but received additional bytes");
	}

	public float commandDistanceRequest(float pos1x, float pos1y, float pos2x, float pos2y, boolean returnDrivingDistance) throws java.io.IOException {
		//position1 = omnet2traci(position1);
		//position2 = omnet2traci(position2);
		byte disttype = returnDrivingDistance ? REQUEST_DRIVINGDIST : REQUEST_AIRDIST;

		Storage cmd = new Storage();
		cmd.writeUnsignedByte(POSITION_2D);
		cmd.writeFloat(pos1x);
		cmd.writeFloat(pos1y);
		cmd.writeUnsignedByte(POSITION_2D);
		cmd.writeFloat(pos2x);
		cmd.writeFloat(pos2y);
		cmd.writeUnsignedByte(disttype);
		Storage buf = queryTraCI(CMD_DISTANCEREQUEST, cmd);

		byte cmdLength = (byte)buf.readUnsignedByte();
		byte commandId = (byte)buf.readUnsignedByte();
		if (commandId != CMD_DISTANCEREQUEST) {
			throw new java.io.IOException("Expected response to CMD_DISTANCEREQUEST, but got "+commandId);
		}

		byte flag = (byte)buf.readUnsignedByte();
		if (flag != disttype) {
			throw new java.io.IOException("Received wrong distance type: "+flag);
		}

		float distance = buf.readFloat();

		if (!buf.validPos()) throw new java.io.IOException("expected only a distance type and a distance, but received additional bytes");

		return distance;
	}

	public void commandStopNode(int nodeId, String roadId, float pos, byte laneid, float radius, double waittime) throws java.io.IOException {
		Storage cmd = new Storage();
		cmd.writeInt(nodeId);
		cmd.writeUnsignedByte(POSITION_ROADMAP);
		cmd.writeStringUTF8(roadId);
		cmd.writeFloat(pos);
		cmd.writeUnsignedByte(laneid);
		cmd.writeFloat(radius);
		cmd.writeDouble(waittime);
		Storage buf = queryTraCI(CMD_STOP, cmd);

		// read additional CMD_STOP sent back in response
		byte cmdLength = (byte)buf.readUnsignedByte();
		byte commandId = (byte)buf.readUnsignedByte(); if (commandId != CMD_STOP) throw new java.io.IOException("Expected response to CMD_STOP, but got "+commandId);
		int nodeId_r = buf.readInt(); if (nodeId_r != nodeId) throw new java.io.IOException("Received response to CMD_STOP for wrong nodeId: expected "+nodeId+", but got "+nodeId_r);
		byte posType_r = (byte)buf.readUnsignedByte(); if (posType_r != POSITION_ROADMAP) throw new java.io.IOException("Received response to CMD_STOP containing POSITION_ROADMAP: expected "+POSITION_ROADMAP+", but got "+posType_r);
		String roadId_r = buf.readStringUTF8(); if (roadId_r != roadId) throw new java.io.IOException("Received response to CMD_STOP for wrong roadId: expected "+roadId+", but got "+roadId_r);
		float pos_r = buf.readFloat(); 
		byte laneid_r = (byte)buf.readUnsignedByte(); 
		float radius_r = buf.readFloat(); 
		double waittime_r = buf.readDouble(); 

		if (!buf.validPos()) throw new java.io.IOException("expected only a response to CMD_STOP, but received additional bytes");
	}

	public void commandSetTrafficLightProgram(String trafficLightId, String program) throws java.io.IOException {
		Storage cmd = new Storage();
		cmd.writeUnsignedByte(TL_PROGRAM);
		cmd.writeStringUTF8(trafficLightId);
		cmd.writeUnsignedByte(TYPE_STRING);
		cmd.writeStringUTF8(program);
		Storage buf = queryTraCI(CMD_SET_TL_VARIABLE, cmd);
		if (!buf.validPos()) throw new java.io.IOException("expected only an OK response, but received additional bytes");
	}

	public void commandSetTrafficLightPhaseIndex(String trafficLightId, int index) throws java.io.IOException {
		Storage cmd = new Storage();
		cmd.writeUnsignedByte(TL_PHASE_INDEX);
		cmd.writeStringUTF8(trafficLightId);
		cmd.writeUnsignedByte(TYPE_INTEGER);
		cmd.writeInt(index);
		Storage buf = queryTraCI(CMD_SET_TL_VARIABLE, cmd);
		if (!buf.validPos()) throw new java.io.IOException("expected only an OK response, but received additional bytes");
	}

	/*
	public static void main(String[] args) throws RuntimeException {
		String message="Starting up...";
		logInfo(message);

		TraCIClientSwans client = new TraCIClientSwans("127.0.0.1", 9999);
		for (int i = 0; i < 10; i++) {
			logInfo("step");
			client.executeOneTimestep();
		}
	}
	*/

}
