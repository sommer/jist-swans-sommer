//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <ProxyPoint.java Tue 2004/04/06 11:24:34 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;


/**
 * Firewall and NAT-busting functionality for JiST (and any RMI client). Basic
 * idea is to avoid any incoming connections. All parties make outgoing
 * connections to the central ProxyPoint, and the ProxyPoint connects people up
 * and marshalls information back and forth. None of this is very efficient,
 * but it works well enough to hooks up scores of clients to scores of servers
 * all over the world! In other words, it's one big hack that does the job.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: ProxyPoint.java,v 1.11 2004-04-06 16:07:43 barr Exp $
 * @since JIST1.0
 */

public class ProxyPoint
{

  //////////////////////////////////////////////////
  // constants
  //

  /** default proxy server port. */
  public static final int PROXY_PORT = 3001;

  /** proxy server command constant. */
  public static final byte COMMAND_INVALID   = -1;
  /** proxy server command constant. */
  public static final byte COMMAND_NOOP      = 0;
  /** proxy server command constant. */
  public static final byte COMMAND_LISTEN    = 1;
  /** proxy server command constant. */
  public static final byte COMMAND_CONNECT   = 2;
  /** proxy server command constant. */
  public static final byte COMMAND_CONNECTED = 3;

  /** whether to show some output. */
  public static final boolean DEBUG = true;

  /**
   * Set global RMI proxy factory to proxy sockets to given proxy server. This
   * methods tests the server first with a quick "ping" connection, and then
   * sets the socket factory.
   *
   * @param host proxy server host
   * @param port proxy server port
   * @throws IOException when i/o fails
   */
  public static void setRmiProxy(InetAddress host, int port) throws IOException
  {
    // test proxy first
    try
    {
      Socket s = new Socket(host, port);
      s.setKeepAlive(true);
      ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
      os.writeByte(COMMAND_NOOP);
      os.flush();
      s.close();
    }
    catch(IOException e)
    {
      throw new IOException("connection refused to proxy at "+host+":"+port);
    }
    // set global rmi socket factory
    RMISocketFactory.setSocketFactory(new ProxySocketFactory(host, port));
  }


  //////////////////////////////////////////////////
  // socket factory
  //

  /**
   * An RMI socket factory that returns proxied sockets.
   */
  public static class ProxySocketFactory extends RMISocketFactory
  {
    /** proxy server host and port. */
    private InetSocketAddress proxy;

    /**
     * Create new RMI socket factory for given proxy server.
     *
     * @param host proxy server host
     * @param port proxy server port
     */
    public ProxySocketFactory(InetAddress host, int port)
    {
      proxy = new InetSocketAddress(host, port);
    }

    /** {@inheritDoc} */
    public ServerSocket createServerSocket(int port) throws IOException
    {
      return new ProxyServerSocket(proxy.getAddress(), proxy.getPort(), port);
    }

    /** {@inheritDoc} */
    public Socket createSocket(String host, int port) throws IOException
    {
      return new ProxyClientSocket(proxy.getAddress(), proxy.getPort(), InetAddress.getByName(host), port);
    }

  } // class: ProxySocketFactory


  //////////////////////////////////////////////////
  // sockets
  //

  /**
   * A Socket on the client-side that operates via the ProxyPoint proxy.
   */
  public static class ProxyClientSocket extends Socket
  {
    /** proxied socket end-point. */
    private InetSocketAddress remote;

    /**
     * Create an new proxy socket on the client side.
     *
     * @param proxyhost proxy server host
     * @param proxyport proxy server port
     * @param host proxied endpoint host
     * @param port proxied endpoint port
     * @throws IOException when i/o fails
     */
    public ProxyClientSocket(InetAddress proxyhost, int proxyport, InetAddress host, int port) throws IOException
    {
      super(proxyhost, proxyport);
      try
      {
        setKeepAlive(true);
        remote = new InetSocketAddress(host, port);
        ObjectOutputStream os = new ObjectOutputStream(getOutputStream());
        os.writeByte(COMMAND_CONNECT);
        os.write((new Node(1)).getHost().getAddress());
        os.write(host.getAddress());
        os.writeInt(port);
        os.flush();
        ObjectInputStream is = new ObjectInputStream(getInputStream());
        byte cmd = is.readByte();
        if(cmd!=COMMAND_CONNECTED)
        {
          close();
          throw new IOException("proxy control error");
        }
        remote = new InetSocketAddress(remote.getAddress(), is.readInt());
      }
      catch(IOException e)
      {
        throw new java.net.ConnectException();
      }
    }

    /** {@inheritDoc} */
    public SocketAddress getRemoteSocketAddress()
    {
      return remote;
    }

    /** {@inheritDoc} */
    public InetAddress getInetAddress()
    {
      return remote.getAddress();
    }

    /** {@inheritDoc} */
    public int getPort()
    {
      return remote.getPort();
    }

  } // class: ProxyClientSocket


  /**
   * A Socket on the server-side that operates via the ProxyPoint proxy.
   */
  public static class ProxyAcceptSocket extends Socket
  {
    /** proxied socket end-point. */
    private InetSocketAddress remote;
    /** outgoing socket to proxy server. */
    private Socket s;

    /**
     * Create an accepted proxied socket on the server side.
     *
     * @param host proxied endpoint host
     * @param port proxied endpoint port
     * @param s socket to proxy server
     * @throws IOException when i/o fails
     */
    public ProxyAcceptSocket(InetAddress host, int port, Socket s) throws IOException
    {
      this.remote = new InetSocketAddress(host, port);
      this.s = s;
    }

    /** {@inheritDoc} */
    public InputStream getInputStream() throws IOException
    {
      return s.getInputStream();
    }

    /** {@inheritDoc} */
    public OutputStream getOutputStream() throws IOException
    {
      return s.getOutputStream();
    }

    /** {@inheritDoc} */
    public void close() throws IOException
    {
      s.close();
    }

    /** {@inheritDoc} */
    public SocketAddress getRemoteSocketAddress()
    {
      return remote;
    }

    /** {@inheritDoc} */
    public InetAddress getInetAddress()
    {
      return remote.getAddress();
    }

    /** {@inheritDoc} */
    public int getPort()
    {
      return remote.getPort();
    }

  } // class: ProxyAcceptSocket


  /**
   * A ServerSocket that operates via the ProxyPoint proxy.
   */
  public static class ProxyServerSocket extends ServerSocket
  {
    /** host and port of proxy server. */
    private InetSocketAddress proxy;
    /** listen port. */
    private int port;
    /** outgoing connection to proxy server. */
    private Socket s;

    /**
     * Create a new proxied ServerSocket.
     *
     * @param proxyhost hostname of proxy server
     * @param proxyport port of proxy server
     * @param port listen port
     * @throws IOException when i/o fails
     */
    public ProxyServerSocket(InetAddress proxyhost, int proxyport, int port) throws IOException
    {
      proxy = new InetSocketAddress(proxyhost, proxyport);
      this.port = port;
      primeProxy();
    }

    /**
     * Open an outgoing connection to the proxy server, if one does not already exist.
     *
     * @throws IOException when i/o fails
     */
    private void primeProxy() throws IOException
    {
      if(s!=null) return;
      s = new Socket(proxy.getAddress(), proxy.getPort());
      s.setKeepAlive(true);
      ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
      os.writeByte(COMMAND_LISTEN);
      os.write((new Node(1)).getHost().getAddress());
      os.writeInt(port);
      os.flush();
      if(port==0)
      {
        ObjectInputStream is = new ObjectInputStream(s.getInputStream());
        port = is.readInt();
      }
    }

    /** {@inheritDoc} */
    public Socket accept() throws IOException
    {
      boolean hangup = false;
      byte cmd = COMMAND_INVALID;
      ObjectInputStream is=null;

      try
      {
        do
        {
          primeProxy();
          try
          {
            is = new ObjectInputStream(s.getInputStream());
            cmd = is.readByte();
          }
          catch(IOException e)
          {
            s=null;
          }
        } while(s==null);

        if(cmd!=COMMAND_CONNECTED)
        {
          s.close();
          throw new IOException("proxy control error");
        }
        Socket tmp = new ProxyAcceptSocket(readInetAddress(is), is.readInt(), s);
        s = null;
        return tmp;
      }
      catch(Exception e)
      {
        throw new IOException(e.getMessage());
      }
    }

    /** {@inheritDoc} */
    public int getLocalPort()
    {
      return port;
    }

  } // class: ProxyServerSocket


  /**
   * A Runnable object that pumps from an InputStream to an OutputStream.
   */
  public static class StreamPump implements Runnable
  {
    /** input stream to read from. */
    private InputStream in;
    /** output stream to write to. */
    private OutputStream out;

    /**
     * Create new stream pump.
     *
     * @param in input stream to read
     * @param out output stream to write to
     */
    public StreamPump(InputStream in, OutputStream out)
    {
      this.in = in;
      this.out = out;
    }

    /**
     * Pumps from input to output, until end-of-file or error.
     */
    public void run()
    {
      final byte[] buf = new byte[10240];
      try
      {
        int n = in.read(buf);
        while(n!=-1)
        {
          out.write(buf, 0, n);
          out.flush();
          n = in.read(buf);
        }
      }
      catch(IOException e)
      {
      }
      try
      {
        out.close();
      }
      catch(IOException e)
      {
      }
      try
      {
        in.close();
      }
      catch(IOException e)
      {
      }
    }

  } // class: ProxyPoint


  /**
   * Pump data between two sockets in both directions, and close both if one fails.
   * This method is not particularly efficient... it creates three threads to deal with
   * the work, rather than performing selects, and non-blocking i/o.
   *
   * @param s1 first socket to pump
   * @param s2 second socket to pump
   * @throws IOException when i/o fails (before pumping begins)
   */
  public static void pumpSocket(final Socket s1, final Socket s2) throws IOException
  {
    final InputStream in1 = s1.getInputStream(), in2 = s2.getInputStream();
    final OutputStream out1 = s1.getOutputStream(), out2 = s2.getOutputStream();
    Runnable r = new Runnable()
    {
      public void run()
      {
        Thread t1 = new Thread(new StreamPump(in1, out2));
        Thread t2 = new Thread(new StreamPump(in2, out1));
        t1.start();
        t2.start();
        try
        {
          t1.join();
        }
        catch(InterruptedException e)
        {
        }
        try
        {
          t2.join();
        }
        catch(InterruptedException e)
        {
        }
        try
        {
          s1.close();
        }
        catch(IOException e)
        {
        }
        try
        {
          s2.close();
        }
        catch(IOException e)
        {
        }
      }
    };
    Thread t = new Thread(r);
    t.start();
  }

  /**
   * Read and parse IP address from input stream.
   *
   * @param is input stream to read
   * @return IP address read
   * @throws IOException when i/o fails
   */
  public static InetAddress readInetAddress(ObjectInputStream is) throws IOException
  {
    byte[] b = new byte[4];
    is.readFully(b);
    return InetAddress.getByAddress(b);
  }

  //////////////////////////////////////////////////
  // locals
  //

  /** proxy server listen socket. */
  private ServerSocket ss;
  /** listen sockets: SocketAddress -- Socket. */
  private Hashtable listen;

  //////////////////////////////////////////////////
  // proxy logic
  //

  /**
   * Create a new ProxyPoint server object.
   *
   * @param port port for incoming connections
   * @throws IOException when i/o fails
   */
  public ProxyPoint(int port) throws IOException
  {
    ss = new ServerSocket(port);
    listen = new Hashtable();
    if(DEBUG) System.out.println("Proxying on port: "+port);
  }

  /**
   * Return an unused proxied port number for given address.
   *
   * @param addr machine requesting unused port
   * @return unused proxied port at given address
   */
  private int getUnusedPort(InetAddress addr)
  {
    int port;
    Random r = new Random();
    do
    {
      port = r.nextInt(Short.MAX_VALUE-10)+1;
    } while(listen.get(new InetSocketAddress(addr, port))!=null);
    return port;
  }

  /**
   * Block for and process a single incoming connection.
   */
  public void accept()
  {
    try
    {
      final Socket s = ss.accept();
      Thread t = new Thread(new Runnable() {
        public void run() 
        {
          try 
          {
            ObjectInputStream is = new ObjectInputStream(s.getInputStream());
            byte cmd = is.readByte();
            switch(cmd)
            {
              case COMMAND_NOOP:
                s.close();
                break;
              case COMMAND_LISTEN:
                {
                  InetAddress addr = readInetAddress(is);
                  int port = is.readInt();
                  if(port==0)
                  {
                    port = getUnusedPort(addr);
                    ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
                    os.writeInt(port);
                    os.flush();
                  }
                  InetSocketAddress remote = new InetSocketAddress(addr, port);
                  if(DEBUG) System.out.println("listen: "+remote);
                  listen.put(remote, s);
                  break;
                }
              case COMMAND_CONNECT:
                {
                  InetAddress src = readInetAddress(is);
                  InetAddress dst = readInetAddress(is);
                  int port = is.readInt();
                  InetSocketAddress remote = new InetSocketAddress(dst, port);
                  if(DEBUG) System.out.println("connect: "+remote);
                  Socket server = (Socket)listen.remove(remote);
                  int tries = 3;
                  long delay = 3000;
                  while(server==null && tries>0)
                  {
                    tries--;
                    try
                    {
                      Thread.sleep(delay);
                    }
                    catch(InterruptedException e)
                    {
                    }
                    server = (Socket)listen.remove(remote);
                  }
                  if(server!=null)
                  {
                    ObjectOutputStream osClient = new ObjectOutputStream(s.getOutputStream());
                    ObjectOutputStream osServer = new ObjectOutputStream(server.getOutputStream());
                    osClient.writeByte(COMMAND_CONNECTED);
                    osClient.writeInt(server.getPort());
                    osClient.flush();
                    osServer.writeByte(COMMAND_CONNECTED);
                    osServer.write(src.getAddress());
                    osServer.writeInt(s.getPort());
                    osServer.flush();
                    pumpSocket(s, server);
                  }
                  else
                  {
                    s.close();
                  }
                  break;
                }
              default:
                throw new RuntimeException("invalid operation");
            }
          }
          catch(Exception e)
          {
          }
        }
      });
      t.start();
    }
    catch(Exception e)
    {
    }
  }

  /**
   * Entry point to start a proxy point server.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    try
    {
      ProxyPoint pp = new ProxyPoint(PROXY_PORT);
      while(true) pp.accept();
    }
    catch(IOException e)
    {
      e.printStackTrace();
    }
  }

} // class: ProxyPoint

