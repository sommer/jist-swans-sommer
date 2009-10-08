//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Socket.java Tue 2004/04/06 11:45:57 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app.net;

import java.nio.channels.SocketChannel;
import java.net.SocketImplFactory;
import java.net.SocketImpl;
import java.net.InetAddress;
import java.net.SocketAddress;
import jist.swans.app.io.InputStream;
import jist.swans.app.io.OutputStream;
import java.io.IOException;

import jist.swans.app.AppInterface;
import jist.swans.trans.SocketInterface;
import jist.swans.trans.TcpSocket;
import jist.runtime.JistAPI;

/**
 * The SWANS target of all java.net.Socket calls.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: Socket.java,v 1.7 2004-04-06 16:07:47 barr Exp $
 * @since SWANS1.0
 */
public class Socket
{

  //////////////////////////////////////////////////
  // Private Variables
  //

  /** supporting socket entity. */
  private SocketInterface.TcpSocketInterface socketEntity;


  //////////////////////////////////////////////////
  // Constructors
  //

  /**
   * @see java.net.Socket
   */
  public Socket()
  { 
    TcpSocket newSocketEntity = new TcpSocket();
    socketEntity = newSocketEntity.getProxy();
  }

  /**
   * @see java.net.Socket
   */
  public Socket(InetAddress address, int port)
  {
    TcpSocket newSocketEntity = new TcpSocket(address, port);
    socketEntity = newSocketEntity.getProxy();
  }

  /**
   * @see java.net.Socket
   */
  public Socket(InetAddress host, int port, boolean stream)
  {
    TcpSocket newSocketEntity = new TcpSocket(host, port, stream);
    socketEntity = newSocketEntity.getProxy();
  }

  /**
   * @see java.net.Socket
   */
  public Socket(InetAddress address, int port, InetAddress localAddr, int localPort)
  {
    TcpSocket newSocketEntity = new TcpSocket(address, port, localAddr, localPort);
    socketEntity = newSocketEntity.getProxy();
  }

  /**
   * @see java.net.Socket
   */
  protected Socket(SocketImpl impl)
  {
    throw new RuntimeException("not implemented");
  }

  /**
   * @see java.net.Socket
   */
  public Socket(String host, int port)
  {
    TcpSocket newSocketEntity = new TcpSocket(host, port);
    socketEntity = newSocketEntity.getProxy();
  }

  /**
   * @see java.net.Socket
   */
  public Socket(String host, int port, boolean stream)
  {
    throw new RuntimeException("not implemented");
  }

  /**
   * @see java.net.Socket
   */
  public Socket(String host, int port, InetAddress localAddr, int localPort)
  {
    TcpSocket newSocketEntity = new TcpSocket(host, port, localAddr, localPort);
    socketEntity = newSocketEntity.getProxy();
  }

  /**
   * @see java.net.Socket
   */
  public Socket(TcpSocket socket)
  {
    this.socketEntity = socket.getProxy();
  }

  /**
   * Method call after socket initialization. Since constructors can not be
   * blocking, we performing blocking events in this method, which is called
   * immediately after the constructor.
   */
  public void _jistPostInit()
  {
    socketEntity.setTcpEntity(((AppInterface.TcpApp)JistAPI.proxy(JistAPI.THIS, AppInterface.TcpApp.class)).getTcpEntity());
    socketEntity._jistPostInit();
  }


  //////////////////////////////////////////////////
  // Socket methods
  //

  /**
   * @see java.net.Socket
   */
  public void bind(SocketAddress bindpoint)
  {
    socketEntity.bind(bindpoint);
  }

  /**
   * @see java.net.Socket
   */
  public void close()
  {
    JistAPI.sleep(1000);
    socketEntity.close();
  }

  /**
   * @see java.net.Socket
   */
  public void connect(SocketAddress endpoint)
  {
    socketEntity.connect(endpoint);
  }

  /**
   * @see java.net.Socket
   */
  public void connect(SocketAddress endpoint, int timeout)
  {
    socketEntity.connect(endpoint, timeout);
  }

  /**
   * @see java.net.Socket
   */
  public SocketChannel getChannel()
  {
    return socketEntity.getChannel();
  }

  /**
   * @see java.net.Socket
   */
  public InetAddress getInetAddress()
  {
    return socketEntity.getInetAddress();
  }

  /**
   * @see java.net.Socket
   */
  public InputStream getInputStream()
  {
    return socketEntity.getInputStream();
  }

  /**
   * @see java.net.Socket
   */
  public boolean getKeepAlive()
  {
    return socketEntity.getKeepAlive();
  }

  /**
   * @see java.net.Socket
   */
  public InetAddress getLocalAddress()
  {
    return socketEntity.getLocalAddress();
  }

  /**
   * @see java.net.Socket
   */
  public int getLocalPort()
  {
    return socketEntity.getLocalPort();
  }

  /**
   * @see java.net.Socket
   */
  public SocketAddress getLocalSocketAddress()
  {
    return socketEntity.getLocalSocketAddress();
  }

  /**
   * @see java.net.Socket
   */
  public boolean getOOBInline()
  {
    return socketEntity.getOOBInline();
  }

  /**
   * @see java.net.Socket
   */
  public OutputStream getOutputStream()
  {
    return socketEntity.getOutputStream();
  }

  /**
   * @see java.net.Socket
   */
  public int getPort()
  {
    return socketEntity.getPort();
  }

  /**
   * @see java.net.Socket
   */
  public int getReceiveBufferSize()
  {
    return socketEntity.getReceiveBufferSize();
  }

  /**
   * @see java.net.Socket
   */
  public SocketAddress getRemoteSocketAddress()
  {
    return socketEntity.getRemoteSocketAddress();
  }

  /**
   * @see java.net.Socket
   */
  public boolean getReuseAddress()
  {
    return socketEntity.getReuseAddress();
  }

  /**
   * @see java.net.Socket
   */
  public int getSendBufferSize()
  {
    return socketEntity.getSendBufferSize();
  }

  /**
   * @see java.net.Socket
   */
  public int getSoLinger()
  {
    return socketEntity.getSoLinger();
  }

  /**
   * @see java.net.Socket
   */
  public int getSoTimeout()
  {
    return socketEntity.getSoTimeout();
  }

  /**
   * @see java.net.Socket
   */
  public boolean getTcpNoDelay()
  {
    return socketEntity.getTcpNoDelay();
  }

  /**
   * @see java.net.Socket
   */
  public int getTrafficClass()
  {
    return socketEntity.getTrafficClass();
  }

  /**
   * @see java.net.Socket
   */
  public boolean isBound()
  {
    return socketEntity.isBound();
  }

  /**
   * @see java.net.Socket
   */
  public boolean isClosed()
  {
    return socketEntity.isClosed();
  }

  /**
   * @see java.net.Socket
   */
  public boolean isConnected()
  {
    return socketEntity.isConnected();
  }

  /**
   * @see java.net.Socket
   */
  public boolean isInputShutdown()
  {
    return socketEntity.isInputShutdown();
  }

  /**
   * @see java.net.Socket
   */
  public boolean isOutputShutdown()
  {
    return socketEntity.isOutputShutdown();
  }

  /**
   * @see java.net.Socket
   */
  public void sendUrgentData(int data)
  {
    socketEntity.sendUrgentData(data);
  }

  /**
   * @see java.net.Socket
   */
  public void setKeepAlive(boolean on)
  {
    socketEntity.setKeepAlive(on);
  }

  /**
   * @see java.net.Socket
   */
  public void setOOBInline(boolean on)
  {
    socketEntity.setOOBInline(on);
  }

  /**
   * @see java.net.Socket
   */
  public void setReceiveBufferSize(int size)
  {
    socketEntity.setReceiveBufferSize(size);
  }

  /**
   * @see java.net.Socket
   */
  public void setReuseAddress(boolean on)
  {
    socketEntity.setReuseAddress(on);
  }

  /**
   * @see java.net.Socket
   */
  public void setSendBufferSize(int size)
  {
    socketEntity.setSendBufferSize(size);
  }

  /**
   * @see java.net.Socket
   */
  public static void setSocketImplFactory(SocketImplFactory fac)
  {
    throw new RuntimeException("not implemented");
  }

  /**
   * @see java.net.Socket
   */
  public void setSoLinger(boolean on, int linger)
  {
    socketEntity.setSoLinger(on, linger);
  }

  /**
   * @see java.net.Socket
   */
  public void setSoTimeout(int timeout)
  {
    socketEntity.setSoTimeout(timeout);
  }

  /**
   * @see java.net.Socket
   */
  public void setTcpNoDelay(boolean on)
  {
    socketEntity.setTcpNoDelay(on);
  }

  /**
   * @see java.net.Socket
   */
  public void setTrafficClass(int tc)
  {
    socketEntity.setTrafficClass(tc);
  }

  /**
   * @see java.net.Socket
   */
  public void shutdownInput() throws IOException
  {
    socketEntity.shutdownInput();
  }

  /**
   * @see java.net.Socket
   */
  public void shutdownOutput() throws IOException
  {
    socketEntity.shutdownOutput();
  }

  /**
   * @see java.net.Socket
   */
  public String toString()
  {
    return socketEntity.toString();
  }

} // class Socket

