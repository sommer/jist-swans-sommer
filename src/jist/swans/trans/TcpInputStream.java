//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <TcpInputStream.java Tue 2004/04/06 11:37:14 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.trans;

import java.io.IOException;

/**
 * SWANS Implementation of InputStream for Socket.
 *
 * @author Kelwin Tamtoro &lt;kt222@cs.cornell.edu&gt;
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: TcpInputStream.java,v 1.7 2004-04-06 16:07:51 barr Exp $
 * @since SWANS1.0
 */
public class TcpInputStream extends jist.swans.app.io.InputStream
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
  public TcpInputStream (SocketInterface.TcpSocketInterface entity)
  {
    this.socketEntity = entity;
    isClosed = false;
  }

  /** 
   * Closes this input stream and releases any system 
   * resources associated with the stream.
   *
   * @throws IOException if an I/O error occurs
   */
  public void close () throws IOException
  {
    super.close ();
    isClosed = true;
  }

  /** 
   * Reads the next byte of data from the input stream.
   *
   * @return the next byte of data, or -1 if the end of the stream is reached.
   * @throws IOException if an I/O error occurs
   */
  public int read () throws IOException
  {
    if (isClosed) throw new IOException ("InputStream is closed");
    byte[] ret = (byte[])socketEntity.getBytesFromSocket (1);
    if ((ret == null) || (ret.length <= 0))
    {
      return -1;
    }
    return (int)ret[0];
  }    

  /** 
   * Reads some number of bytes from the input stream and stores 
   * them into the buffer array b.
   *
   * @param b the buffer into which the data is read.
   * @return the total number of bytes read into the buffer, 
   * or -1 is there is no more data because the end of the stream has been reached.
   * @throws IOException if an I/O error occurs
   */
  public int read (byte[] b) throws IOException
  {
    if (isClosed) throw new IOException ("InputStream is closed");
    return read (b, 0, b.length);
  }
  
  /** 
   * Reads up to len bytes of data from the input stream into 
   * an array of bytes.
   *
   * @param b the buffer into which the data is read.
   * @param off the start offset in array b at which the data is written.
   * @param len the maximum number of bytes to read.
   * @return the total number of bytes read into the buffer, 
   * or -1 is there is no more data because the end of the stream has been reached.
   * @throws IOException if an I/O error occurs
   */
  public int read (byte[] b, int off, int len) throws IOException
  {
    if (isClosed) throw new IOException ("InputStream is closed");
    byte[] ret = (byte[])socketEntity.getBytesFromSocket (len);
    if ((ret == null) || (ret.length <= 0))
    {
      return -1;
    }
    System.arraycopy (ret, 0, b, off, ret.length); 
    return ret.length;
  }
      
}// class: TcpInputStream
