//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <TcpOutputStream.java Tue 2004/04/06 11:37:22 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.trans;

import java.io.IOException;

/**
 * SWANS Implementation of OutputStream for Socket.
 *
 * @author Kelwin Tamtoro &lt;kt222@cs.cornell.edu&gt;
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: TcpOutputStream.java,v 1.7 2004-04-06 16:07:51 barr Exp $
 * @since SWANS1.0
 */
public class TcpOutputStream extends jist.swans.app.io.OutputStream
{

  /**
   * Entity reference to Socket object.
   */
  SocketInterface.TcpSocketInterface socketEntity;

  /**
   * The closed state of the output stream.
   */
  boolean isClosed;
  
  /** 
   * Constructor.
   *
   * @param entity entity reference to Socket
   */
  public TcpOutputStream(SocketInterface.TcpSocketInterface entity)
  {
    this.socketEntity = entity;
    isClosed = false;
  }

  /**
   * Closes this output stream and releases any system 
   * resources associated with this stream.
   *
   * @throws IOException if an I/O error occurs
   */
  public void close () throws IOException
  {
    super.close();
    isClosed = true;
  }
  

  /** 
   * Writes b.length bytes from the specified byte array to 
   * this output stream.
   *
   * @param b the data
   * @throws IOException if an I/O error occurs
   */
  public void write (byte[] b) throws IOException
  {      
    if (isClosed) throw new IOException ("OutputStream is closed");
    socketEntity.queueBytes (b);      
  }
  
  /** 
   * Writes len bytes from the specified byte array starting at 
   * offset off to this output stream.
   *
   * @param b the data
   * @param off the start offset in the data
   * @param len the number of bytes to write
   * @throws IOException if an I/O error occurs
   */
  public void write (byte[] b, int off, int len) throws IOException
  {
    byte[] temp = new byte [len];
    System.arraycopy (b, off, temp, 0, len);
    write (temp);
  }

  /** 
   * Writes the specified byte to this output stream.
   *
   * @param b the byte
   * @throws IOException if an I/O error occurs. In particular, 
   * an IOException may be thrown if the output stream has been closed.
   */
  public void write (int b) throws IOException
  {
    java.lang.Integer temp = new java.lang.Integer (b);
    write (new byte [] {temp.byteValue()});
  }

} // class: TcpOutputStream

