//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <ServerSocket.java Tue 2004/04/06 11:45:48 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app.net;

import java.nio.channels.ServerSocketChannel;
import java.net.SocketImplFactory;
import java.net.InetAddress;
import java.net.SocketAddress;

import jist.swans.app.AppInterface;
import jist.swans.trans.SocketInterface;
import jist.swans.trans.TcpServerSocket;
import jist.swans.trans.TcpSocket;
import jist.runtime.JistAPI;

/**
 * The SWANS target of all java.net.ServerSocket calls.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: ServerSocket.java,v 1.8 2004-04-06 16:07:47 barr Exp $
 * @since SWANS1.0
 */

public class ServerSocket
{

  //////////////////////////////////////////////////
  // Private Variables
  //

  /** supporting serversocket entity. */
  private SocketInterface.TcpServerSocketInterface serverSocketEntity;

  /**
   * @see java.net.ServerSocket
   */
  public ServerSocket() 
  {
    TcpServerSocket newServerSocketEntity = new TcpServerSocket();
    serverSocketEntity = newServerSocketEntity.getProxy();
  }

  /**
   * @see java.net.ServerSocket
   */
  public ServerSocket(int port)
  {
    TcpServerSocket newServerSocketEntity = new TcpServerSocket(port);
    serverSocketEntity = newServerSocketEntity.getProxy();
  }

  /**
   * @see java.net.ServerSocket
   */
  public ServerSocket(int port, int backlog)
  {
    TcpServerSocket newServerSocketEntity = new TcpServerSocket(port, backlog);
    serverSocketEntity = newServerSocketEntity.getProxy();
  }

  /**
   * @see java.net.ServerSocket
   */
  public ServerSocket(int port, int backlog, InetAddress bindAddr)
  {
    TcpServerSocket newServerSocketEntity = new TcpServerSocket(port, backlog, bindAddr);
    serverSocketEntity = newServerSocketEntity.getProxy();
  }

  /**
   * Method call after socket initialization. Since constructors can not be
   * blocking, we performing blocking events in this method, which is called
   * immediately after the constructor.
   */
  public void _jistPostInit()
  {
    serverSocketEntity.setTcpEntity(((AppInterface.TcpApp)JistAPI.proxy(JistAPI.THIS, AppInterface.TcpApp.class)).getTcpEntity());
    serverSocketEntity._jistPostInit();
  }

  /**
   * @see java.net.ServerSocket
   */
  public Socket accept()
  {
    TcpSocket socket = serverSocketEntity.accept();
    return new Socket(socket);
  }

  /**
   * @see java.net.ServerSocket
   */
  public void bind(SocketAddress endpoint)
  {
    serverSocketEntity.bind(endpoint);
  }

  /**
   * @see java.net.ServerSocket
   */
  public void bind(SocketAddress endpoint, int backlog)
  {
    serverSocketEntity.bind(endpoint, backlog);
  }

  /**
   * @see java.net.ServerSocket
   */
  public void close()
  {
    JistAPI.sleep(1000);
    serverSocketEntity.close();
  }

  /**
   * @see java.net.ServerSocket
   */
  public ServerSocketChannel getChannel()
  {
    return serverSocketEntity.getChannel();
  }

  /**
   * @see java.net.ServerSocket
   */
  public InetAddress getInetAddress()
  {
    return serverSocketEntity.getInetAddress();
  }

  /**
   * @see java.net.ServerSocket
   */
  public int getLocalPort()
  {
    return serverSocketEntity.getLocalPort();
  }

  /**
   * @see java.net.ServerSocket
   */
  public SocketAddress getLocalSocketAddress()
  {
    return serverSocketEntity.getLocalSocketAddress();
  }

  /**
   * @see java.net.ServerSocket
   */
  public int getReceiveBufferSize()
  {
    return serverSocketEntity.getReceiveBufferSize();
  }

  /**
   * @see java.net.ServerSocket
   */
  public boolean getReuseAddress()
  {
    return serverSocketEntity.getReuseAddress();
  }

  /**
   * @see java.net.ServerSocket
   */
  public int getSoTimeout()
  {
    return serverSocketEntity.getSoTimeout();
  }

  /**
   * @see java.net.ServerSocket
   */
  protected void implAccept(Socket s)
  {
    throw new RuntimeException("not implemented");
  }

  /**
   * @see java.net.ServerSocket
   */
  public boolean isBound()
  {
    return serverSocketEntity.isBound();
  }

  /**
   * @see java.net.ServerSocket
   */
  public boolean isClosed()
  {
    return serverSocketEntity.isClosed();
  }

  /**
   * @see java.net.ServerSocket
   */
  public void setReceiveBufferSize(int size)
  {
    serverSocketEntity.setReceiveBufferSize(size);
  }

  /**
   * @see java.net.ServerSocket
   */
  public void setReuseAddress(boolean on)
  {
    serverSocketEntity.setReuseAddress(on);
  }

  /**
   * @see java.net.ServerSocket
   */
  public static void setSocketFactory(SocketImplFactory fac)
  {
    throw new RuntimeException("not implemented");
  }

  /**
   * @see java.net.ServerSocket
   */
  public void setSoTimeout(int timeout)
  {
    serverSocketEntity.setSoTimeout(timeout);
  }

  /**
   * @see java.net.ServerSocket
   */
  public String toString()
  {
    return serverSocketEntity.toString();
  }

} // class: ServerSocket

