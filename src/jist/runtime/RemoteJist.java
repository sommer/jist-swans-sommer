//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RemoteJist.java Tue 2004/04/06 11:24:46 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.io.*;
import java.util.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import org.apache.bcel.classfile.*;

/** 
 * All the JiST client-server related remote classes.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: RemoteJist.java,v 1.18 2004-04-06 16:07:43 barr Exp $
 * @since JIST1.0
 */

public class RemoteJist
{

  //////////////////////////////////////////////////
  // PING
  //

  /**
   * Remote ping interface.
   */
  public static interface PingRemote extends Remote
  {
    /**
     * Ping does not nothing, except check that destination is alive.
     *
     * @throws RemoteException rpc failure
     */
    void ping() throws RemoteException;

  } // interface: PingRemote


  /**
   * Ping server.
   */
  public static class Ping 
    extends UnicastRemoteObject 
    implements PingRemote
  {
    /**
     * Create new RMI ping server on given port.
     *
     * @param port RMI ping server listen port
     * @throws RemoteException rpc failure
     */
    public Ping(int port) throws RemoteException
    {
      super(port);
    }

    //////////////////////////////////////////////////
    // PingRemote interface
    //

    /** {@inheritDoc} */
    public void ping() throws RemoteException
    {
    }

  } // class: Ping


  //////////////////////////////////////////////////
  // JOB QUEUE SERVER
  //

  /**
   * A simulation job.
   */
  public static class Job implements Serializable
  {
    /** command-line parameters. */
    public Main.CommandLineOptions options;
    /** simulation properties. */
    public Properties properties;
    /** remote simulation client. */
    public RemoteJist.JistClientRemote client;
    /** memory requirements. */
    public long mem;
    /** {@inheritDoc} */
    public String toString()
    {
      StringBuffer sb = new StringBuffer();
      sb.append(options.sim);
      for(int i=0; i<options.args.length; i++)
      {
        sb.append(" ");
        sb.append(options.args[i]);
      }
      if(mem>0) sb.append("; mem="+mem);
      return sb.toString();
    }
  }


  /**
   * Remote job queue server interface.
   */
  public static interface JobQueueServerRemote extends Remote
  {
    /**
     * Add job to queue.
     * 
     * @param job job to enqueue
     * @param front whether job should be added to the front of the queue
     * @throws RemoteException rpc failure
     */
    void addJob(Job job, boolean front) throws RemoteException;

    /**
     * Wait for job. Returns when there might be a job available.
     *
     * @param maxMem maximum available server memory
     * @throws RemoteException rpc failure
     */
    void waitForJob(long maxMem) throws RemoteException;

    /**
     * Return next job from queue, or null if queue is empty.
     *
     * @param maxMem maximum available server memory
     * @return next job from queue; null if none exists
     * @throws RemoteException rpc failure
     */
    Job getJob(long maxMem) throws RemoteException;

    /**
     * Return RMI handle to queue standard output stream.
     *
     * @return remote output stream RMI handle
     * @throws RemoteException rpc failure
     */
    RemoteIO.RemoteOutputStreamRemote getStdOut() throws RemoteException;

  } // interface: JobQueueServerRemote


  /**
   * Job queue server.
   */
  public static class JobQueueServer 
    extends UnicastRemoteObject 
    implements JobQueueServerRemote
  {
    /**
     * RMI name of job queue server.
     */
    public static final String JIST_JOBSERVER_RMI_NAME  = "JistJobServer";

    /**
     * job queue.
     */
    private Vector jobs;

    /**
     * number of server in wait lock.
     */
    private int serversWaiting;

    /**
     * local server output stream.
     */
    private RemoteIO.RemoteOutputStreamReceiver rout;

    /**
     * Start new job queue server.
     *
     * @param port rmi listen port
     * @param out local server output stream
     * @throws RemoteException rpc failure
     */
    public JobQueueServer(int port, PrintStream out) throws RemoteException
    {
      super(port);
      jobs = new Vector();
      serversWaiting = 0;
      this.rout = new RemoteIO.RemoteOutputStreamReceiver(out);
    }

    /**
     * Return stub of remote job queue server.
     *
     * @param n node (host:port) of remote job queue server
     * @return stub of remote job queue server
     * @throws NotBoundException when jist not bound (started) on remote server
     * @throws MalformedURLException never
     * @throws RemoteException rpc failure
     */
    public static JobQueueServerRemote getRemote(Node n) throws NotBoundException, MalformedURLException, RemoteException
    {
      String remotename = "rmi://"+n+"/"+JIST_JOBSERVER_RMI_NAME;
      Remote r = Naming.lookup(remotename);
      return (JobQueueServerRemote)r;
    }

    //////////////////////////////////////////////////
    // helpers
    //

    /** 
     * Show list of currently queued simulation jobs.
     *
     * @throws RemoteException rpc failure
     */
    private synchronized void showJobs() throws RemoteException
    {
      writeln("Job queue has "+jobs.size()+" jobs outstanding and "+serversWaiting+" servers idle.");
      for(int i=0; i<jobs.size(); i++)
      {
        writeln("  "+(i+1)+": "+jobs.elementAt(i));
      }
    }

    /**
     * Write line to output (appending newline).
     *
     * @param s text to output
     * @throws RemoteException rpc failure
     */
    private void writeln(String s) throws RemoteException
    {
      try
      {
        rout.write((s+"\n").getBytes());
        rout.flush();
      }
      catch(IOException e)
      {
        throw new RemoteException(e.getMessage());
      }
    }

    /**
     * Return number of queued jobs that satisfy server constraints.
     *
     * @param maxMem maximum available server memory
     * @return number of queued jobs that satisfy server constraints
     */
    private int numJobsConstraints(long maxMem)
    {
      int count = 0;
      for(int i=0; i<jobs.size(); i++)
      {
        if(satisfyConstraint((Job)jobs.elementAt(i), maxMem)) count++;
      }
      return count;
    }

    /**
     * Determine whether server resources satisfy job constraints.
     *
     * @param j job to verify against constraints
     * @param maxMem maximum available server memory
     * @return whether server resources satisfy job constraints
     */
    private boolean satisfyConstraint(Job j, long maxMem)
    {
      if(maxMem < j.mem+1024*1024) return false;
      return true;
    }


    //////////////////////////////////////////////////
    // JobQueueServerRemote interface
    //

    /** {@inheritDoc} */
    public synchronized void addJob(Job job, boolean front) throws RemoteException
    {
      if(front)
      {
        jobs.add(0, job);
      }
      else
      {
        jobs.add(job);
      }
      showJobs();
      notifyAll();
    }

    /** {@inheritDoc} */
    public synchronized void waitForJob(long maxMem) throws RemoteException
    {
      while(numJobsConstraints(maxMem)==0)
      {
        serversWaiting++;
        try
        {
          showJobs();
          try
          {
            wait(Main.SERVER_QUEUE_RELEASE_INTERVAL);
          }
          catch(InterruptedException e) 
          {
          }
        }
        finally
        {
          serversWaiting--;
        }
      }
    }

    /** {@inheritDoc} */
    public synchronized Job getJob(long maxMem) throws RemoteException
    {
      Job selected = null;
      for(int i=0; i<jobs.size(); i++)
      {
        if(satisfyConstraint((Job)jobs.elementAt(i), maxMem))
        {
          selected = (Job)jobs.remove(i);
          break;
        }
      }
      if(selected!=null) showJobs();
      return selected;
    }

    /** {@inheritDoc} */
    public RemoteIO.RemoteOutputStreamRemote getStdOut() throws RemoteException
    {
      return rout;
    }

  } // class: JobQueueServer


  //////////////////////////////////////////////////
  // Remote ClassLoader and Repository
  //

  /**
   * Remote resource finder interface.
   */
  public static interface ResourceFinderRemote extends Remote
  {
    /**
     * Get bytes of a remote resource.
     *
     * @param name resource name
     * @return bytes of resource
     * @throws RemoteException rpc failure
     */
    byte[] getResourceBytes(String name) throws RemoteException;

    /**
     * Get modification time of resource.
     *
     * @param name resource name
     * @return modification time of resource
     * @throws RemoteException rpc failure
     */
    long getResourceLastModificationDate(String name) throws RemoteException;

  } // interface: ResourceFinderRemote


  /**
   * An RMI-based remote class loader.
   */
  public static class RemoteClassLoader extends ClassLoader
  {
    /**
     * Remote resource finder.
     */
    private ResourceFinderRemote resources;

    /**
     * Hashmap of loaded classes.
     */
    private HashMap loaded;

    /**
     * Create new remote classloader backed by 
     * given remote resource finder.
     *
     * @param resources remote resource finder
     */
    public RemoteClassLoader(ResourceFinderRemote resources) 
    {
      this.resources = resources;
      this.loaded = new HashMap();
    }

    //////////////////////////////////////////////////
    // inherited from ClassLoader
    //

    /** {@inheritDoc} */
    public Class loadClass(String name, boolean resolve)
      throws ClassNotFoundException
    {
      // check for (and delegate) ignored packages
      return Rewriter.isIgnoredStatic(name) 
        ? super.loadClass(name, resolve)
        : this.findClass(name);
    }

    /** {@inheritDoc} */
    protected Class findClass(String name) throws ClassNotFoundException
    {
      try
      {
        Class cl = (Class)loaded.get(name);
        if (cl!=null) return cl;
        byte[] clb = resources.getResourceBytes(name);
        cl = defineClass(name, clb, 0, clb.length);
        loaded.put(name, cl);
        return cl;
      }
      catch(RemoteException e)
      {
        throw new ClassNotFoundException(e.getMessage());
      }
    }

  } // class: RemoteClassLoader



  /**
   * An RMI-based remote BCEL repository.
   */
  public static class RemoteRepository implements org.apache.bcel.util.Repository
  {
    /**
     * Remote resource finder.
     */
    private ResourceFinderRemote resources;

    /**
     * Hashmap of loaded classes.
     */
    private HashMap cache;

    /**
     * Local server output stream.
     */
    private PrintStream out;

    /**
     * Create new remote BCEL repository.
     *
     * @param resources remote resource finder
     * @param out local server output stream
     */
    public RemoteRepository(ResourceFinderRemote resources, PrintStream out)
    {
      this.resources = resources;
      this.out = out;
      clear();
    }

    //////////////////////////////////////////////////
    // inherited from Repository
    //

    /** {@inheritDoc} */
    public void storeClass(JavaClass clazz)
    {
      cache.put(clazz.getClassName(), clazz);
    }

    /** {@inheritDoc} */
    public void removeClass(JavaClass clazz)
    {
      cache.remove(clazz.getClassName());
    }

    /** {@inheritDoc} */
    public JavaClass findClass(String classname)
    {
      return (JavaClass)cache.get(classname);
    }

    /** {@inheritDoc} */
    public JavaClass loadClass(java.lang.String className) throws ClassNotFoundException
    {
      JavaClass jcl = findClass(className);
      if(jcl!=null) return jcl;
      try
      {
        String classfile = Rewriter.classToFileName(className);
        byte[] b = null;
        if(Rewriter.isIgnoredStatic(className))
        {
          // if(out!=null) out.println("  local load: "+className);
          b = Util.getResourceBytes(classfile);
        }
        else
        {
          if(out!=null) out.println("  remote load: "+className);
          b = resources.getResourceBytes(classfile);
        }
        if(b!=null)
        {
          ClassParser cp = new ClassParser(new ByteArrayInputStream(b), classfile);
          jcl = cp.parse();
          jcl.setRepository(this);
        }
      }
      catch(IOException e) 
      { 
        e.printStackTrace(); 
      }
      if(jcl==null) throw new ClassNotFoundException(className);
      storeClass(jcl);
      return jcl;
    }

    /** {@inheritDoc} */
    public JavaClass loadClass(Class cl) throws ClassNotFoundException
    {
      return loadClass(cl.getName());
    }

    /** {@inheritDoc} */
    public void clear()
    {
      cache = new HashMap();
    }

  } // class: RemoteRepository


  //////////////////////////////////////////////////
  // CLIENT INTERFACE
  //

  /**
   * Remote JiST client interface.
   */
  public static interface JistClientRemote 
    extends PingRemote, ResourceFinderRemote, Remote
  {
    /**
     * Return remote stdout.
     * @return remote stdout
     * @throws RemoteException rpc failure
     */
    RemoteIO.RemoteOutputStreamRemote getStdOut() throws RemoteException;

    /**
     * Return remote stderr.
     * @return remote stderr
     * @throws RemoteException rpc failure
     */
    RemoteIO.RemoteOutputStreamRemote getStdErr() throws RemoteException;

    /**
     * Signal client that simulation is done.
     * @throws RemoteException rpc failure
     */
    void done() throws RemoteException;

  } // interface: JistClientRemote


  /**
   * Remote JiST client RMI "server".
   */
  public static class JistClient 
    extends UnicastRemoteObject 
    implements RemoteJist.JistClientRemote
  {

    /**
     * Remote stub for local stdout.
     */
    private RemoteIO.RemoteOutputStreamRemote rout;

    /**
     * Remote stub for local stderr.
     */
    private RemoteIO.RemoteOutputStreamRemote rerr;

    /**
     * Create new Jist client RMI "server".
     * @throws RemoteException rpc failure
     */
    public JistClient() throws RemoteException
    {
      rout = new RemoteIO.RemoteOutputStreamReceiver(System.out);
      rerr = new RemoteIO.RemoteOutputStreamReceiver(System.err);
    }

    //////////////////////////////////////////////////
    // JistClient interface
    //

    /** {@inheritDoc} */
    public RemoteIO.RemoteOutputStreamRemote getStdOut() throws RemoteException
    {
      return rout;
    }

    /** {@inheritDoc} */
    public RemoteIO.RemoteOutputStreamRemote getStdErr() throws RemoteException
    {
      return rerr;
    }

    /** {@inheritDoc} */
    public synchronized void done() throws RemoteException
    {
      UnicastRemoteObject.unexportObject(this, true);
      rout = null;
      rerr = null;
      notify();
    }

    //////////////////////////////////////////////////
    // ResourceFinderRemote interface
    //

    /** {@inheritDoc} */
    public byte[] getResourceBytes(String name) throws RemoteException
    {
      return Util.getResourceBytes(name);
    }

    /** {@inheritDoc} */
    public long getResourceLastModificationDate(String name) throws RemoteException
    {
      try
      {
        return ClassLoader.getSystemClassLoader().getResource(name).openConnection().getLastModified();
      }
      catch(Exception e)
      {
        return 0;
      }
    }

    //////////////////////////////////////////////////
    // PingRemote interface
    //

    /** {@inheritDoc} */
    public void ping() throws RemoteException
    {
    }

  } // class: JistClient

  /**
   * Local JiST client.
   */
  public static class JistClientLocal
    implements RemoteJist.JistClientRemote
  {

    /**
     * Remote stub for local stdout.
     */
    private RemoteIO.RemoteOutputStreamRemote rout;

    /**
     * Remote stub for local stderr.
     */
    private RemoteIO.RemoteOutputStreamRemote rerr;

    /**
     * Create new Jist client RMI "server".
     *
     * @throws RemoteException never
     */
    public JistClientLocal() throws RemoteException
    {
      rout = new RemoteIO.RemoteOutputStreamReceiverLocal(System.out);
      rerr = new RemoteIO.RemoteOutputStreamReceiverLocal(System.err);
    }

    //////////////////////////////////////////////////
    // JistClient
    //

    /** {@inheritDoc} */
    public RemoteIO.RemoteOutputStreamRemote getStdOut()
    {
      return rout;
    }

    /** {@inheritDoc} */
    public RemoteIO.RemoteOutputStreamRemote getStdErr()
    {
      return rerr;
    }

    /** {@inheritDoc} */
    public synchronized void done()
    {
      rout = null;
      rerr = null;
      notify();
    }

    //////////////////////////////////////////////////
    // ResourceFinderRemote
    //

    /** {@inheritDoc} */
    public byte[] getResourceBytes(String name)
    {
      return Util.getResourceBytes(name);
    }

    /** {@inheritDoc} */
    public long getResourceLastModificationDate(String name)
    {
      try
      {
        return ClassLoader.getSystemClassLoader().getResource(name).openConnection().getLastModified();
      }
      catch(Exception e)
      {
        return 0;
      }
    }

    //////////////////////////////////////////////////
    // PingRemote interface
    //

    /** {@inheritDoc} */
    public void ping()
    {
    }

  } // class: JistClientLocal

} // class: RemoteJist

