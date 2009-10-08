//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <UdpSocket.java Sun 2005/03/13 11:09:24 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app.net;

import jist.swans.app.AppInterface;
import jist.swans.trans.TransInterface;
import jist.swans.net.NetAddress;
import jist.swans.misc.Message;
import jist.swans.misc.MessageBytes;
import jist.swans.Constants;

import jist.runtime.JistAPI;
import jist.runtime.Channel;

import java.net.DatagramPacket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.InetSocketAddress;

/**
 * The SWANS target of all java.net.DatagramSocket calls.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: UdpSocket.java,v 1.6 2005-03-13 16:11:54 barr Exp $
 * @since SWANS1.0
 */

// todo: implement socket options
public class UdpSocket
{
  /** local port. */
  private int lport;
  /** local address. */
  private InetAddress laddr;
  /** blocking channel represents application-kernel boundary; used to wait on packets. */
  private Channel channel;
  /** UDP socket handler. */
  private TransInterface.SocketHandler callback;
  /** UDP entity. */
  private TransInterface udpEntity;
  /** whether socket is bound. */
  private boolean isBound;

  /**
   * @see java.net.DatagramSocket
   */
  public UdpSocket()
  {
    this(new InetSocketAddress(0));
  }

  /**
   * @see java.net.DatagramSocket
   */
  protected UdpSocket(DatagramSocketImpl impl)
  {
    throw new RuntimeException("not implemented");
  }

  /**
   * @see java.net.DatagramSocket
   */
  public UdpSocket(int port)
  {
    this(new InetSocketAddress(port));
  }

  /**
   * @see java.net.DatagramSocket
   */
  public UdpSocket(int port, InetAddress addr)
  {
    this(new InetSocketAddress(addr, port));
  }

  /**
   * @see java.net.DatagramSocket
   */
  public UdpSocket(SocketAddress addr)
  {
    InetSocketAddress inetAddr = (InetSocketAddress)addr;
    this.lport = inetAddr.getPort();
    this.laddr = inetAddr.getAddress();
    this.channel = JistAPI.createChannel();
    this.callback = new UdpSocketCallback(channel);
    isBound = false;
  }

  /**
   * Post-constructor call. Since constructors can not be blocking (JiST/Java
   * limitation), we rewrite a regular socket constructor to two calls. This is
   * the second one, and it can be blocking, because it is a regular method.
   */
  public void _jistPostInit()
  {
    this.udpEntity = ((AppInterface.UdpApp)
        JistAPI.proxy(JistAPI.THIS, AppInterface.UdpApp.class)).getUdpEntity();
    bind(new InetSocketAddress(laddr, lport));
  }

  /**
   * @see java.net.DatagramSocket
   */
  public void bind(SocketAddress addr)
  {
    InetSocketAddress inetAddr = (InetSocketAddress)addr;
    this.lport = inetAddr.getPort();
    this.laddr = inetAddr.getAddress();
    if(isBound)
    {
      udpEntity.delSocketHandler(lport);
    }
    udpEntity.addSocketHandler(((InetSocketAddress)addr).getPort(), callback);
    isBound = true;
  }

  /**
   * @see java.net.DatagramSocket
   */
  public void close()
  {
    if(isBound)
    {
      udpEntity.delSocketHandler(lport);
      isBound = false;
    }
  }

  /**
   * @see java.net.DatagramSocket
   */
  public void receive(DatagramPacket p)
  {
    DatagramPacket lp = (DatagramPacket)channel.receive();
    p.setAddress(lp.getAddress());
    p.setPort(lp.getPort());
    p.setSocketAddress(lp.getSocketAddress());
    // rimnote: sending the actual (mutable) data array over... 
    //    might become a problem if applications modify.
    p.setData(lp.getData(), lp.getOffset(), lp.getLength());
  }

  /**
   * @see java.net.DatagramSocket
   */
  public void send(DatagramPacket p)
  {
    udpEntity.send(new MessageBytes(p.getData(), p.getOffset(), p.getLength()),
        new NetAddress(p.getAddress()), p.getPort(),
        lport,
        Constants.NET_PRIORITY_NORMAL); 
  }


  //////////////////////////////////////////////////
  // socket handler
  //

  /**
   * Callback for incoming UDP packets.
   */
  public static class UdpSocketCallback implements TransInterface.SocketHandler
  {
    /**
     * Blocking channel, shared with UdpSocket.
     */
    private Channel channel;

    /**
     * Create new socket callback on given UDP socket channel.
     *
     * @param channel blocking channel used to deliver packets.
     */
    public UdpSocketCallback(Channel channel)
    {
      this.channel = channel;
    }

    /** {@inheritDoc} */
    public void receive(Message msg, NetAddress src, int srcPort)
    {
      MessageBytes bytes = (MessageBytes)msg;
      DatagramPacket p = new DatagramPacket(
          bytes.getBytes(), bytes.getOffset(), bytes.getLength(),
          src.getIP(), srcPort);
      p.setSocketAddress(new InetSocketAddress(src.getIP(), srcPort));
      channel.sendNonBlock(p, true, false);
    }
  }

} // class: UdpSocket

