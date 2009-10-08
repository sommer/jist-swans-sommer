//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RemoteIO.java Tue 2004/04/06 11:24:40 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.io.*;
import java.rmi.*;
import java.rmi.server.*;

/** 
 * RMI-based remote input and output streams.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: RemoteIO.java,v 1.10 2004-04-06 16:07:43 barr Exp $
 * @since JIST1.0
 */

public class RemoteIO
{

  //////////////////////////////////////////////////
  // INPUT
  //

  /**
   * Interface for remote input stream.
   */
  public static interface RemoteInputStreamRemote extends Remote
  {
    /** 
     * Read a byte.
     * 
     * @return byte read; -1 for EOF
     * @throws IOException underlying input/output error
     * @throws RemoteException rpc failure
     */
    int read() throws IOException, RemoteException;

    /**
     * Read a sequence of bytes.
     *
     * @param len number of bytes to read
     * @return array of bytes read, less than or equal to len
     * @throws IOException underlying input/output error
     * @throws RemoteException rpc failure
     */
    byte[] read(int len) throws IOException, RemoteException;

    /**
     * Close the underlying remote input stream.
     * @throws IOException underlying input/output error
     * @throws RemoteException rpc failure
     */
    void close() throws IOException, RemoteException;

  } // interface: RemoteInputStreamRemote


  /**
   * Local-side of remote input stream.
   */
  public static class RemoteInputStream extends InputStream
  {
    /**
     * Stub of remote-side of remote input stream.
     */
    private RemoteInputStreamRemote rin;

    /**
     * Create a new local-side remote input stream connected to the given
     * remote input stream stub.
     *
     * @param rin remote input stream stub
     * @throws IOException underlying input/output error
     */
    public RemoteInputStream(RemoteInputStreamRemote rin) throws IOException
    {
      this.rin = rin;
    }

    //////////////////////////////////////////////////
    // inherited from InputStream
    //

    /** {@inheritDoc} */
    public int read() throws IOException
    {
      return rin.read();
    }

    /** {@inheritDoc} */
    public int read(byte[] b) throws IOException
    {
      return read(b, 0, b.length);
    }

    /** {@inheritDoc} */
    public int read(byte[] b, int off, int len) throws IOException
    {
      byte[] b2 = rin.read(len);
      if(b2==null) return -1;
      System.arraycopy(b2, 0, b, off, b2.length);
      return b2.length;
    }

    /** {@inheritDoc} */
    public void close() throws IOException
    {
      rin.close();
    }

  } // class: RemoteInputStream


  /**
   * Remote-side of remote input stream.
   */
  public static class RemoteInputStreamSender 
    extends UnicastRemoteObject 
    implements RemoteInputStreamRemote
  {
    /**
     * Underlying input stream, source of all data.
     */
    private InputStream lin;

    /**
     * Create a new remote input stream server.
     *
     * @param lin underlying source input stream
     * @throws RemoteException rpc failure
     */
    public RemoteInputStreamSender(InputStream lin) throws RemoteException
    {
      this.lin = lin;
    }

    /** {@inheritDoc} */
    public void finalize()
    {
      try
      {
        this.close();
      }
      catch(IOException e) 
      { 
      }
    }

    //////////////////////////////////////////////////
    // RemoteInputStreamRemote interface
    //

    /** {@inheritDoc} */
    public int read() throws IOException, RemoteException
    {
      return lin.read();
    }

    /** {@inheritDoc} */
    public byte[] read(int len) throws IOException, RemoteException
    {
      byte[] b = new byte[len];
      int n = lin.read(b);
      byte[] b2 = new byte[n];
      System.arraycopy(b, 0, b2, 0, n);
      return b2;
    }

    /** {@inheritDoc} */
    public void close() throws IOException, RemoteException
    {
      lin.close();
      UnicastRemoteObject.unexportObject(this, true);
    }

  } // class: RemoteInputStreamSender


  /**
   * LOCAL remote input stream.
   */
  public static class RemoteInputStreamSenderLocal
    implements RemoteInputStreamRemote
  {
    /**
     * Underlying input stream, source of all data.
     */
    private InputStream lin;

    /**
     * Create a new remote input stream server.
     *
     * @param lin underlying source input stream
     */
    public RemoteInputStreamSenderLocal(InputStream lin)
    {
      this.lin = lin;
    }

    /** {@inheritDoc} */
    public void finalize()
    {
      try
      {
        this.close();
      }
      catch(IOException e) 
      {
      }
    }

    //////////////////////////////////////////////////
    // RemoteInputStreamRemote interface
    //

    /** {@inheritDoc} */
    public int read() throws IOException
    {
      return lin.read();
    }

    /** {@inheritDoc} */
    public byte[] read(int len) throws IOException
    {
      byte[] b = new byte[len];
      int n = lin.read(b);
      byte[] b2 = new byte[n];
      System.arraycopy(b, 0, b2, 0, n);
      return b2;
    }

    /** {@inheritDoc} */
    public void close() throws IOException
    {
      lin.close();
    }

  } // class: RemoteInputStreamSenderLocal



  //////////////////////////////////////////////////
  // OUTPUT
  //

  /**
   * Interface for remote output stream.
   */
  public static interface RemoteOutputStreamRemote extends Remote
  {

    /**
     * Write a byte.
     *
     * @param b byte to write
     * @throws IOException underlying input/output error
     * @throws RemoteException rpc failure
     */
    void write(int b) throws IOException, RemoteException;

    /**
     * Write an array of bytes.
     *
     * @param b array of bytes
     * @throws IOException underlying input/output error
     * @throws RemoteException rpc failure
     */
    void write(byte[] b) throws IOException, RemoteException;

    /**
     * Write an array of bytes.
     *
     * @param b array of bytes
     * @param off starting offset within array
     * @param len number of bytes to write
     * @throws IOException underlying input/output error
     * @throws RemoteException rpc failure
     */
    void write(byte[] b, int off, int len) throws IOException, RemoteException;

    /**
     * Flush output.
     * @throws IOException underlying input/output error
     * @throws RemoteException rpc failure
     */
    void flush() throws IOException, RemoteException;

    /**
     * Close the underlying remote output stream.
     * @throws IOException underlying input/output error
     * @throws RemoteException rpc failure
     */
    void close() throws IOException, RemoteException;

  } // interface: RemoteOutputStreamRemote


  /**
   * Local-side of remote output stream.
   */
  public static class RemoteOutputStream extends OutputStream
  {

    /**
     * Stub of remote-side of remote output stream.
     */
    private RemoteOutputStreamRemote rout;

    /**
     * Create a new local-side remote output stream connected to the given
     * remote output stream stub.
     *
     * @param rout remote output stream stub
     */
    public RemoteOutputStream(RemoteOutputStreamRemote rout)
    {
      this.rout = rout;
    }

    //////////////////////////////////////////////////
    // inherited from OutputStream
    //

    /** {@inheritDoc} */
    public void write(int b) throws IOException
    {
      rout.write(b);
    }

    /** {@inheritDoc} */
    public void write(byte[] b) throws IOException
    {
      rout.write(b);
    }

    /** {@inheritDoc} */
    public void write(byte[] b, int off, int len) throws IOException
    {
      rout.write(b, off, len);
    }

    /** {@inheritDoc} */
    public void flush() throws IOException
    {
      rout.flush();
    }

    /** {@inheritDoc} */
    public void close() throws IOException
    {
      rout.close();
    }

  } // class: RemoteOutputStream


  /**
   * Remote-side of remote output stream.
   */
  public static class RemoteOutputStreamReceiver 
    extends UnicastRemoteObject 
    implements RemoteOutputStreamRemote
  {
    /**
     * Underlying output stream, destination of all data.
     */
    private OutputStream lout;

    /**
     * Create a new remote output stream server.
     *
     * @param lout underlying destination output stream
     * @throws RemoteException rpc failure
     */
    public RemoteOutputStreamReceiver(OutputStream lout) throws RemoteException
    {
      this.lout = lout;
    }

    /** {@inheritDoc} */
    public void finalize()
    {
      try
      {
        this.close();
      }
      catch(IOException e) 
      {
      }
    }

    //////////////////////////////////////////////////
    // RemoteOutputStreamRemote interface
    //

    /** {@inheritDoc} */
    public void write(int b) throws IOException, RemoteException
    {
      lout.write(b);
    }

    /** {@inheritDoc} */
    public void write(byte[] b) throws IOException, RemoteException
    {
      lout.write(b);
    }

    /** {@inheritDoc} */
    public void write(byte[] b, int off, int len) throws IOException, RemoteException
    {
      lout.write(b, off, len);
    }

    /** {@inheritDoc} */
    public void flush() throws IOException, RemoteException
    {
      lout.flush();
    }

    /** {@inheritDoc} */
    public void close() throws IOException, RemoteException
    {
      lout.close();
      UnicastRemoteObject.unexportObject(this, true);
    }

  } // class: RemoteOutputStreamReceiver


  /**
   * LOCAL remote output stream.
   */
  public static class RemoteOutputStreamReceiverLocal
    implements RemoteOutputStreamRemote
  {
    /**
     * Underlying output stream, destination of all data.
     */
    private OutputStream lout;

    /**
     * Create a new remote output stream server.
     *
     * @param lout underlying destination output stream
     */
    public RemoteOutputStreamReceiverLocal(OutputStream lout)
    {
      this.lout = lout;
    }

    /** {@inheritDoc} */
    public void finalize()
    {
      try
      {
        this.close();
      }
      catch(IOException e) 
      { 
      }
    }

    //////////////////////////////////////////////////
    // RemoteOutputStreamRemote interface
    //

    /** {@inheritDoc} */
    public void write(int b) throws IOException
    {
      lout.write(b);
    }

    /** {@inheritDoc} */
    public void write(byte[] b) throws IOException
    {
      lout.write(b);
    }

    /** {@inheritDoc} */
    public void write(byte[] b, int off, int len) throws IOException
    {
      lout.write(b, off, len);
    }

    /** {@inheritDoc} */
    public void flush() throws IOException
    {
      lout.flush();
    }

    /** {@inheritDoc} */
    public void close() throws IOException
    {
      lout.close();
    }

  } // class: RemoteOutputStreamReceiverLocal


  //////////////////////////////////////////////////
  // PrintStream
  //

  /**
   * A special PrintStream that *does* throw exceptions. 
   * Used for creating a substitute System.stdout, which fail 
   * abruptly on detecting client/server network failure.
   */
  public static class PrintStreamWithExceptions extends PrintStream
  {

    /** underlying PrintStream. */
    private PrintStream out;

    /**
     * Create new PrintStream that throws exceptions on error.
     *
     * @param out underlying PrintStream
     */
    public PrintStreamWithExceptions(PrintStream out) 
    {
      super(out); 
      this.out=out; 
      check(); 
    }

    /**
     * Throw exception on error in underlying PrintStream.
     */
    private void check()
    {
      if(out.checkError()) throw new RuntimeException("IOException");
    }

    //////////////////////////////////////////////////
    // inherited from PrintStream
    //

    /** {@inheritDoc} */
    public synchronized void close() 
    { 
      out.close(); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void flush() 
    { 
      out.flush(); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void print(boolean b) 
    { 
      out.print(b); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void print(char c) 
    { 
      out.print(c); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void print(char[] s) 
    { 
      out.print(s); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void print(double d) 
    { 
      out.print(d); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void print(float f) 
    { 
      out.print(f); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void print(int i) 
    { 
      out.print(i); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void print(long l) 
    { 
      out.print(l); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void print(Object obj) 
    { 
      out.print(obj); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void print(String s) 
    { 
      out.print(s); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void println() 
    { 
      out.println(); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void println(boolean x) 
    { 
      out.println(x); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void println(char x) 
    { 
      out.println(x); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void println(char[] x) 
    { 
      out.println(x); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void println(double x) 
    { 
      out.println(x); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void println(float x) 
    { 
      out.println(x); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void println(int x) 
    { 
      out.println(x); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void println(long x) 
    { 
      out.println(x); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void println(Object x) 
    { 
      out.println(x); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void println(String x) 
    { 
      out.println(x); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void write(byte[] buf, int off, int len) 
    { 
      out.write(buf, off, len); 
      check(); 
    }
    /** {@inheritDoc} */
    public synchronized void write(int b) 
    { 
      out.write(b); 
      check(); 
    }

  } // class: PrintStreamWithExceptions

} // class: RemoteIO

