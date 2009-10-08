//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <SocketInterface.java Tue 2004/04/06 11:36:47 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.trans;

import java.nio.channels.SocketChannel;
import java.net.InetAddress;
import java.net.SocketAddress;
import jist.swans.app.io.InputStream;
import jist.swans.app.io.OutputStream;
import java.io.IOException;
import java.nio.channels.ServerSocketChannel;

import jist.runtime.JistAPI;
import jist.swans.net.NetAddress;
import jist.swans.trans.TransTcp.TcpMessage;

/**
 * Defines the interface of all socket entity implementations.
 *
 * @author Kelwin Tamtoro &lt;kt222@cs.cornell.edu&gt;
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: SocketInterface.java,v 1.13 2004-04-06 16:07:51 barr Exp $
 * @since SWANS1.0
 */
public interface SocketInterface extends JistAPI.Proxiable
{

  /**
   * Post-constructor call. Since constructors can not be blocking (JiST/Java
   * limitation), we rewrite a regular socket constructor to two calls. This is
   * the second one, and it can be blocking, because it is a regular method.
   *
   * @throws JistAPI.Continuation never; blocking event.   
   */
  void _jistPostInit() throws JistAPI.Continuation;

  /**
   * Sets the reference to the network layer.
   *
   * @param tcpEntity entity reference to transport layer
   */
  void setTcpEntity(TransInterface.TransTcpInterface tcpEntity);

  /**
   * Binds the ServerSocket to a specific address (IP address and port number).
   *
   * @param bindpoint The IP address & port number to bind to.
   * @throws JistAPI.Continuation never; blocking event.
   */
  void bind(SocketAddress bindpoint) throws JistAPI.Continuation;

  /**
   * Closes this socket.
   */
  void close();

  /**
   * Returns the local address of this server socket/socket.
   *
   * @return the address to which this socket is bound, 
   * or null if the socket is unbound.
   * @throws JistAPI.Continuation never; blocking event.
   */
  InetAddress getInetAddress() throws JistAPI.Continuation;

  /**
   * Returns the port on which this socket is listening.
   *
   * @return the port number to which this socket is listening 
   * or -1 if the socket is not bound yet.
   * @throws JistAPI.Continuation never; blocking event.
   */
  int getLocalPort() throws JistAPI.Continuation;

  /**
   * Returns the address of the endpoint this socket is bound to, 
   * or null if it is not bound yet.
   *
   * @return a SocketAddress representing the local endpoint of 
   * this socket, or null if it is not bound yet.
   * @throws JistAPI.Continuation never; blocking event.
   */
  SocketAddress getLocalSocketAddress() throws JistAPI.Continuation;

  /**
   * Gets the value of the SO_RCVBUF option for this ServerSocket, 
   * that is the proposed buffer size that will be used for Sockets 
   * accepted from this ServerSocket.
   *
   * @return the value of the SO_RCVBUF option for this Socket.
   * @throws JistAPI.Continuation never; blocking event.
   */
  int getReceiveBufferSize() throws JistAPI.Continuation;

  /**
   * Tests if SO_REUSEADDR is enabled.
   *
   * @return a boolean indicating whether or not SO_REUSEADDR is enabled.
   * @throws JistAPI.Continuation never; blocking event.
   */
  boolean getReuseAddress() throws JistAPI.Continuation;

  /**
   * Retrieve setting for SO_TIMEOUT.
   *
   * @return the SO_TIMEOUT value 
   * @throws JistAPI.Continuation never; blocking event.
   */
  int getSoTimeout() throws JistAPI.Continuation;

  /**
   * Returns the binding state of the ServerSocket.
   *
   * @return true if the ServerSocket succesfuly bound to an address
   * @throws JistAPI.Continuation never; blocking event.
   */
  boolean isBound() throws JistAPI.Continuation;

  /**
   * Returns the closed state of the ServerSocket.
   *
   * @return true if the socket has been closed
   * @throws JistAPI.Continuation never; blocking event.
   */
  boolean isClosed() throws JistAPI.Continuation;

  /**
   * Sets a default proposed value for the SO_RCVBUF option for 
   * sockets accepted from this ServerSocket.
   *
   * @param size the size to which to set the receive buffer size. 
   * This value must be greater than 0.
   */
  void setReceiveBufferSize(int size);

  /**
   * Enable/disable the SO_REUSEADDR socket option.
   *
   * @param on whether to enable or disable the socket option 
   */
  void setReuseAddress(boolean on);

  /**
   * Enable/disable SO_TIMEOUT with the specified timeout, in milliseconds.
   *
   * @param timeout the specified timeout, in milliseconds 
   */
  void setSoTimeout(int timeout);

  /**
   * Returns the implementation address and implementation 
   * port of this socket as a String.
   *
   * @return a string representation of this socket.
   * @throws JistAPI.Continuation never; blocking event.
   */
  String toString() throws JistAPI.Continuation;

  /**
   * Check and process the incoming packet depending on current state of
   * the socket. 
   *
   * @param msg the incoming TCP message
   * @param src source of packet
   */
  void checkPacketandState(TcpMessage msg, NetAddress src);

  /**
   * Defines the interface for TcpSocket specific implementation.
   */
  public interface TcpSocketInterface extends SocketInterface
  {

    /**
     * Connects this socket to the server.
     *
     * @param endpoint the SocketAddress
     * @throws JistAPI.Continuation never; blocking event.   
     */
    void connect(SocketAddress endpoint) throws JistAPI.Continuation;

    /**
     * Connects this socket to the server with a specified timeout value.
     *
     * @param endpoint the SocketAddress
     * @param timeout the timeout value to be used in milliseconds
     * @throws JistAPI.Continuation never; blocking event.   
     */
    void connect(SocketAddress endpoint, int timeout) throws JistAPI.Continuation;

    /**
     * Returns the unique SocketChannel object associated with this socket, if any.
     *
     * @return the socket channel associated with this socket, or 
     * null if this socket was not created for a channel
     * @throws JistAPI.Continuation never; blocking event.   
     */
    SocketChannel getChannel() throws JistAPI.Continuation;

    /**
     * Returns an input stream for this socket.
     *
     * @return an input stream for reading bytes from this socket.
     * @throws JistAPI.Continuation never; blocking event.   
     */
    InputStream getInputStream() throws JistAPI.Continuation;

    /**
     * Tests if SO_KEEPALIVE is enabled.
     *
     * @return a boolean indicating whether or not SO_KEEPALIVE is enabled.
     * @throws JistAPI.Continuation never; blocking event.   
     */
    boolean getKeepAlive() throws JistAPI.Continuation;

    /**
     * Gets the local address to which the socket is bound.
     *
     * @return the local address to which the socket is bound 
     * or InetAddress.anyLocalAddress() if the socket is not bound yet.
     * @throws JistAPI.Continuation never; blocking event.   
     */
    InetAddress getLocalAddress() throws JistAPI.Continuation;

    /**
     * Tests if OOBINLINE is enabled.
     *
     * @return a boolean indicating whether or not OOBINLINE is enabled.
     * @throws JistAPI.Continuation never; blocking event.   
     */
    boolean getOOBInline() throws JistAPI.Continuation;

    /**
     * Returns an output stream for this socket.
     *
     * @return an output stream for writing bytes to this socket.
     * @throws JistAPI.Continuation never; blocking event.   
     */
    OutputStream getOutputStream() throws JistAPI.Continuation;

    /**
     * Returns the remote port to which this socket is connected.
     *
     * @return the remote port number to which this socket is connected, 
     * or 0 if the socket is not connected yet.
     * @throws JistAPI.Continuation never; blocking event.   
     */
    int getPort() throws JistAPI.Continuation;

    /**
     * Returns the address of the endpoint this socket is 
     * connected to, or null if it is unconnected.
     *
     * @return a SocketAddress representing the local endpoint 
     * of this socket, or null if it is not bound yet.
     * @throws JistAPI.Continuation never; blocking event.   
     */
    SocketAddress getRemoteSocketAddress() throws JistAPI.Continuation;

    /**
     * Get value of the SO_SNDBUF option for this Socket, 
     * that is the buffer size used by the platform for 
     * output on this Socket.
     *
     * @return the value of the SO_SNDBUF option for this Socket.
     * @throws JistAPI.Continuation never; blocking event.   
     */
    int getSendBufferSize() throws JistAPI.Continuation;

    /**
     * Returns setting for SO_LINGER.
     *
     * @return the setting for SO_LINGER.
     * @throws JistAPI.Continuation never; blocking event.   
     */
    int getSoLinger() throws JistAPI.Continuation;

    /**
     * Tests if TCP_NODELAY is enabled.
     *
     * @return a boolean indicating whether or not TCP_NODELAY is enabled.
     * @throws JistAPI.Continuation never; blocking event.   
     */
    boolean getTcpNoDelay() throws JistAPI.Continuation;

    /**
     * Gets traffic class or type-of-service in the IP 
     * header for packets sent from this Socket.
     *
     * @return the traffic class or type-of-service already set 
     * @throws JistAPI.Continuation never; blocking event.   
     */
    int getTrafficClass() throws JistAPI.Continuation;

    /**
     * Returns the connection state of the socket.
     *
     * @return true if the socket successfuly connected to a server
     * @throws JistAPI.Continuation never; blocking event.   
     */
    boolean isConnected() throws JistAPI.Continuation;

    /**
     * Returns wether the read-half of the socket connection is closed.
     *
     * @return true if the input of the socket has been shutdown
     * @throws JistAPI.Continuation never; blocking event.   
     */
    boolean isInputShutdown() throws JistAPI.Continuation;

    /**
     * Returns wether the write-half of the socket connection is closed.
     *
     * @return true if the output of the socket has been shutdown
     * @throws JistAPI.Continuation never; blocking event.   
     */
    boolean isOutputShutdown() throws JistAPI.Continuation;

    /**
     * Send one byte of urgent data on the socket.
     *
     * @param data The byte of data to send 
     */
    void sendUrgentData(int data);

    /**
     * Enable/disable SO_KEEPALIVE.
     *
     * @param on whether or not to have socket keep alive turned on
     */
    void setKeepAlive(boolean on);

    /**
     * Enable/disable OOBINLINE (receipt of TCP urgent data) 
     * By default, this option is disabled and TCP urgent data 
     * received on a socket is silently discarded.
     *
     * @param on true to enable OOBINLINE, false to disable. 
     */
    void setOOBInline(boolean on);

    /**
     * Sets the SO_SNDBUF option to the specified value for this Socket.
     *
     * @param size the size to which to set the send buffer size. 
     * This value must be greater than 0.
     */
    void setSendBufferSize(int size);

    /**
     * Enable/disable SO_LINGER with the specified linger time in seconds.
     *
     * @param on whether or not to linger on.
     * @param linger how long to linger for, if on is true.
     */
    void setSoLinger(boolean on, int linger);

    /**
     * Enable/disable TCP_NODELAY (disable/enable Nagle's algorithm).
     *
     * @param on true to enable TCP_NODELAY, false to disable.
     */
    void setTcpNoDelay(boolean on);

    /**
     * Sets traffic class or type-of-service octet in 
     * the IP header for packets sent from this Socket.
     *
     * @param tc an int value for the bitset.
     */
    void setTrafficClass(int tc);

    /**
     * Places the input stream for this socket at "end of stream".
     *
     * @throws JistAPI.Continuation never; blocking event.   
     * @throws IOException if an I/O error occurs when shutting down this socket.
     */
    void shutdownInput() throws JistAPI.Continuation, IOException;

    /**
     * Disables the output stream for this socket.
     *
     * @throws JistAPI.Continuation never; blocking event.   
     * @throws IOException if an I/O error occurs when shutting down this socket.
     */
    void shutdownOutput() throws JistAPI.Continuation, IOException;

    /** 
     * This method is called to send bytes to the other side.
     * What this method does is storing the bytes in the send buffer and
     * then call sendPackets method to send the bytes.
     *
     * @param data bytes to be stored in the send buffer
     * @return the number of bytes actually stored
     * @throws JistAPI.Continuation never; blocking event.   
     */
    int queueBytes(byte[] data) throws JistAPI.Continuation;
  
    /**
     * This method is called by the input stream to retrieve
     * data from the transport layer.
     *
     * @param length number of bytes to retrieve
     * @return byte array containing data from socket
     * @throws JistAPI.Continuation never; blocking event.   
     */
    byte[] getBytesFromSocket(int length) throws JistAPI.Continuation;

    /**
     * Creates packets to be sent based on the receiver's 
     * advertised window (managing flow control).
     */
    void constructPackets();

    /**
     * Schedule a retransmission for a message.
     *
     * @param seqNum sequence number of message to retransmit
     * @param time wait time before attempting to retransmit
     */
    void startRetransmitTimer(int seqNum, long time);

    /**
     * Attempt to retransmit because the timer times out.
     *
     * @param seqNum sequence number of message to retransmit
     * @param time wait time before attempting to retransmit
     */
    void retransmitTimerTimeout(int seqNum, long time);

    /**
     * Methods for persist timer (sender sending packets to probe 
     * receiver window).
     *
     * @param seqNum number of the probe message
     */
    void startPersistTimer(int seqNum);

    /**
     * Execution when persist timer times out (sending probe message).
     *
     * @param timerId ID of the persist timer
     * @param seqNum number of the probe message
     */
    void persistTimerTimeout(int timerId,int seqNum);

    /**
     * Methods for reset timer (this timer is used when
     * establishing or closing connection --> If no reply is
     * received, connection is reset).
     */
    void startResetTimer();

    /**
     * Execution when reset timer times out (closing the socket).
     *
     * @param timerId ID of the reset timer
     */
    void resetTimerTimeout(int timerId);

    /**
     * Methods for timer during TIME_WAIT state.
     */
    void startTimeWaitTimer();

    /**
     * Execution when time wait timer times out (closing the socket).
     */
    void timeWaitTimerTimeout();

  }

  /**
   * Defines the interface for TcpServerSocket specific implementation.
   */
  public interface TcpServerSocketInterface extends SocketInterface
  {

    /**
     * Listens for a connection to be made to this socket and accepts it.
     *
     * @return the new TcpSocket
     * @throws JistAPI.Continuation never; blocking event.
     */
    TcpSocket accept() throws JistAPI.Continuation;

    /**
     * Binds the ServerSocket to a specific address (IP address and port number).
     *
     * @param endpoint The IP address & port number to bind to.
     * @param backlog The listen backlog length.
     * @throws JistAPI.Continuation never; blocking event.
     */
    void bind(SocketAddress endpoint, int backlog) throws JistAPI.Continuation;

    /**
     * Returns the unique ServerSocketChannel object associated 
     * with this socket, if any.
     *
     * @return the server-socket channel associated with this socket, 
     * or null if this socket was not created for a channel
     * @throws JistAPI.Continuation never; blocking event
     */
    ServerSocketChannel getChannel() throws JistAPI.Continuation;

  }

} // interface: SocketInterface

