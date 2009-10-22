package de.fau.cs7.driver.traci;

import org.apache.log4j.Logger;
import jist.swans.Constants;
import jist.swans.misc.Message;
import jist.swans.misc.Util;
import jist.swans.misc.Mapper;
import jist.swans.misc.Location;
import jist.swans.app.AppInterface;
import jist.swans.app.net.UdpSocket;
import jist.swans.field.Field;
import jist.swans.field.PathLoss;
import jist.swans.radio.RadioNoise;
import jist.swans.radio.RadioNoiseIndep;
import jist.swans.radio.RadioInfo;
import jist.swans.route.RouteAodv;
import jist.swans.trans.TransUdp;
import jist.swans.trans.TransInterface.SocketHandler;
import jist.swans.trans.TransInterface.TransUdpInterface;
import jist.swans.mac.MacAddress;
import jist.swans.mac.MacDumb;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.net.NetIp;
import jist.swans.net.PacketLoss;
import jist.runtime.JistAPI;
import jist.runtime.JistAPI.Continuation;
import de.fau.cs7.traci.TraCIClientSwans;

public class TraCITestSwans {

	protected static interface TestAppInterface {
		public void sendMessage();
	}

	public static class TestApp implements AppInterface, SocketHandler, TestAppInterface {

		public static class TestMessage implements Message {

			public static final int SIZE = 50;

			public int getSize() { 
				return SIZE; 
			}
			
			public void getBytes(byte[] b, int offset) {
				throw new RuntimeException("not implemented");
			}

			public String toString() {
				return "hi!";
			}
			
		}

		private boolean dead = false;
		private Object self;
		protected int traciId;
		protected TransUdpInterface udpEntity;
		protected TraCIClientSwans traciClient;

		public TestApp(int traciId) {
			this.traciId = traciId;

			this.self = JistAPI.proxyMany(this, new Class[] { AppInterface.class, SocketHandler.class, TestAppInterface.class });
		}

		public void setUdpEntity(TransUdpInterface udpEntity) {
			this.udpEntity = udpEntity;
		}

		public void setTraCIClient(TraCIClientSwans traciClient) {
			this.traciClient = traciClient;
		}

		public AppInterface getAppProxy() {
			return (AppInterface)self;
		}

		public void run() {
			run(null);			
		}
		
		public void run(String[] args) {
			udpEntity.addSocketHandler(5000, this);

			try {
				if (traciId == 0) traciClient.commandStopNode(traciId, "5", 100, (byte)0, 6, 60);
			}
			catch (java.io.IOException e) {
			}

			((TestAppInterface)self).sendMessage();
		}

		public void die() {
			dead = true;
		}
		
		public void sendMessage() {
			if (dead) return;

			TestMessage msg = new TestMessage();
			udpEntity.send(msg, NetAddress.ANY, 5000, 5000, (byte)0);
			logInfo("sent message");

			JistAPI.sleep(10 * Constants.SECOND);
			((TestAppInterface)self).sendMessage();
		}

		public void receive(Message msg, NetAddress src, int srcPort) throws Continuation {
			if (!(msg instanceof TestMessage)) {
				throw new RuntimeException("received unknown message");
			}
			
			logInfo("received message: "+msg);
		}

	}

	public static final Logger log = Logger.getLogger(TraCITestSwans.class.getName());

	protected static void logDebug(String s) {
		if(!log.isDebugEnabled()) return;
		log.debug("t="+(float)JistAPI.getTime()/Constants.SECOND+": "+s);
	}

	protected static void logInfo(String s) {
		if(!log.isInfoEnabled()) return;
		log.info("t="+(float)JistAPI.getTime()/Constants.SECOND+": "+s);
	}

	protected static class NodeCont {
		public TestApp app;
		public RadioNoise radio;
		public NodeCont(TestApp app, RadioNoise radio) {this.app=app; this.radio=radio;}
	}

	public static Object createNode(int i, int traciId, TraCIClientSwans traciClient, Field field, RadioInfo.RadioInfoShared radioInfoShared, Mapper protMap, PacketLoss plIn, PacketLoss plOut) {

		// create entities
		RadioNoise radio = new RadioNoiseIndep(i, radioInfoShared);
		MacDumb mac = new MacDumb(new MacAddress(i), radio.getRadioInfo());
		NetAddress netAddr = new NetAddress(i);
		NetIp net = new NetIp(netAddr, protMap, plIn, plOut);
		RouteAodv route = new RouteAodv(netAddr);
		route.getProxy().start();
		TransUdp udp = new TransUdp();
		TestApp app = new TestApp(traciId);

		// hookup entities
		Location location = new Location.Location2D(0, 0);
		field.addRadio(radio.getRadioInfo(), radio.getProxy(), location);
		field.startMobility(radio.getRadioInfo().getUnique().getID());
		radio.setFieldEntity(field.getProxy());
		radio.setMacEntity(mac.getProxy());
		mac.setRadioEntity(radio.getProxy());
		byte intId = net.addInterface(mac.getProxy());
		mac.setNetEntity(net.getProxy(), intId);
		net.setProtocolHandler(Constants.NET_PROTOCOL_UDP, udp.getProxy());
		net.setRouting(route.getProxy());
		route.setNetEntity(net.getProxy());
		udp.setNetEntity(net.getProxy());
		app.setUdpEntity(udp.getProxy());
		app.setTraCIClient(traciClient);

		// start application
		app.getAppProxy().run();
		
		// this will get passed to deleteNode
		NodeCont nc = new NodeCont(app, radio);
		return nc;
	}

	public static void deleteNode(Field field, Object o) {
		NodeCont nc = (NodeCont)o;
		nc.app.die();
		field.delRadio(nc.radio.getRadioInfo().getUnique().getID());
	}

	public static void createSim() {
		Location.Location2D bounds = new Location.Location2D(2000, 2000);

		final Field field = new Field(bounds);
		field.setPathLoss(new PathLoss.FreeSpace());

		final RadioInfo.RadioInfoShared radioInfoShared = RadioInfo.createShared(Constants.FREQUENCY_DEFAULT, Constants.BANDWIDTH_DEFAULT, 11, Constants.GAIN_DEFAULT, Util.fromDB(Constants.SENSITIVITY_DEFAULT), Util.fromDB(Constants.THRESHOLD_DEFAULT), Constants.TEMPERATURE_DEFAULT, Constants.TEMPERATURE_FACTOR_DEFAULT, Constants.AMBIENT_NOISE_DEFAULT);

		final Mapper protMap = new Mapper(Constants.NET_PROTOCOL_MAX);
		protMap.mapToNext(Constants.NET_PROTOCOL_UDP);

		final PacketLoss plIn = new PacketLoss.Zero();
		final PacketLoss plOut = new PacketLoss.Zero();

		logInfo("Starting TraCI client...");
		String cwd = System.getProperty("user.dir")+"/de/fau/cs7/driver/traci";
		final TraCIClientSwans mobility = new TraCIClientSwans("127.0.0.1", 9999, "<?xml version=\"1.0\"?>\n<launch id=\"quad\">\n<basedir path=\""+cwd+"\" /><copy file=\"quad.net.xml\" />\n <copy file=\"quad.rou.xml\" />\n <copy file=\"quad.sumo.cfg\" type=\"config\" />\n </launch>");
		mobility.setModuleCreator(new TraCIClientSwans.ModuleCreator() {
			protected int last_node_id = 0;

			public Object createModule(byte domain, int traciId) {
				return createNode(++last_node_id, traciId, mobility, field, radioInfoShared, protMap, plIn, plOut);
			}
			public void destroyModule(Object module) {
				deleteNode(field, module);
			}
		});
		mobility.start();
		logInfo("Started");

		field.setMobility(mobility);

		logInfo("done creating sim");
	}

	public static void main(String[] args) {
		logDebug("Hello World");
		createSim();
	}
}
