//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Controller.java Sun 2005/03/13 11:10:28 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.util.*;
import java.lang.reflect.*;
import java.rmi.*;
import java.rmi.server.*;
import org.apache.log4j.*;

import jist.runtime.guilog.GuiLog;

/** 
 * Maintains all the data structures of a single simulation thread of
 * execution. The Controller also contains the implementation of static
 * callbacks from the simulation application to the JiST system.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Controller.java,v 1.122 2005-03-13 16:11:54 barr Exp $
 * @since JIST1.0
 */

public final class Controller implements ControllerRemote, Runnable
{

  //////////////////////////////////////////////////
  // method stubs
  //

  /**
   * Method stub for callback to return the current active entity.
   */
  public static final Method method_getTHIS;

  /**
   * Method stub for callback to register an new entity, 
   * creating an entity reference.
   */
  public static final Method method_newEntityReference;

  /**
   * Method stub for callback to get an entity references
   * for an entity.
   */
  public static final Method method_getEntityReference;

  /**
   * Method stub for callback to invoke a method on an entity.
   */
  public static final Method method_entityInvocation;

  /**
   * Method stub for callback to invoke a method on an entity, with
   * continuation -- a blocking call.
   */
  public static final Method method_entityInvocationCont;

  /**
   * Return next frame of incoming event continuation state.
   */
  public static final Method method_popStateInFrame;

  /**
   * Store next frame of outgoing event continuation state.
   */
  public static final Method method_pushStateOutFrame;

  /**
   * Method stub for callback to determine whether current event
   * invocation is in restore mode.
   */
  public static final Method method_isModeRestore;

  /**
   * Method stub for callback to determine whether current event
   * invocation is in save mode.
   */
  public static final Method method_isModeSave;

  static
  {
    try
    {
      method_getTHIS = Controller.class.getDeclaredMethod("getTHIS",
          new Class[] { });
      method_newEntityReference = Controller.class.getDeclaredMethod("newEntityReference", 
          new Class[] { Entity.class });
      method_getEntityReference = Controller.class.getDeclaredMethod("getEntityReference",
          new Class[] { Object.class });
      method_entityInvocation = Controller.class.getDeclaredMethod("entityInvocation", 
          new Class[] { Method.class, Object.class, Object[].class });
      method_entityInvocationCont = Controller.class.getDeclaredMethod("entityInvocationCont",
          new Class[] { Method.class, Object.class, Object[].class });
      method_popStateInFrame = Controller.class.getDeclaredMethod("popStateInFrame",
          new Class[] { });
      method_pushStateOutFrame = Controller.class.getDeclaredMethod("pushStateOutFrame",
          new Class[] { Event.ContinuationFrame.class });
      method_isModeRestore = Controller.class.getDeclaredMethod("isModeRestore",
          new Class[] { });
      method_isModeSave = Controller.class.getDeclaredMethod("isModeSave",
          new Class[] { });
    }
    catch(NoSuchMethodException e)
    {
      throw new JistException("should never happen", e);
    }
  }

  //////////////////////////////////////////////////
  // static fields
  //

  /**
   * Number of controller threads initialized.
   */
  private static int controllerCount = 0;

  /**
   * Active controller, iff controllerCount==1.
   */
  public static final Controller activeController;
  static
  {
    try
    {
      activeController = new Controller();
    }
    catch(RemoteException e)
    {
      throw new RuntimeException("should not happen: could not create static controller");
    }
  }

  /**
   * Controller logger.
   */
  public static final Logger log = Logger.getLogger(Controller.class.getName());

  /**
   * Fast way to determine whether debug logging is turned on.
   */
  private static final boolean isDebugLogging = log.isDebugEnabled();

  /**
   * GUI logger.
   */
  public static final GuiLog guilog = Main.GUILOG_SIZE > 0
    ? new GuiLog(Main.GUILOG_SIZE) : null;

  /**
   * Counts of each event type.
   */
  private static Hashtable eventCounts;

  //////////////////////////////////////////////////
  // instance fields
  //

  /**
   * Pool of pre-allocated events.
   */
  private final Pool.EventPool eventPool = new Pool.EventPool(100);

  /**
   * Pool of pre-allocated continuations.
   */
  private final Pool.ContinuationPool continuationPool = new Pool.ContinuationPool(100);

  /**
   * Queue of simulation events.
   */
  private final Scheduler.Heap events = new Scheduler.Heap();
  //private final Scheduler.Calendar events = new Scheduler.Calendar(1000000000, 1000);


  /**
   * List of registered entities (possibly not completely filled).
   */
  private Entity[] entities;

  /**
   * Number of registered entities.
   */
  private int numEntities;

  /**
   * reference to the entity used for static calls.
   */
  private EntityRef staticEntityRef;

  /**
   * Current event being processed.
   */
  private Event currentEvent;

  /**
   * Current local simulation time.
   */
  private long currentSimulationTime;

  /**
   * Simulation end time.
   */
  private long endSimulationTime;

  /**
   * Outgoing (blocking) call event.
   */
  private Event call;

  /**
   * Callback event for outgoing (blocking) call event.
   */
  private Event callback;

  /**
   * Outgoing callback state for (blocking) call event.
   */
  public Event.ContinuationFrame callState;

  /**
   * Incoming callback state for returning (blocking) call event.
   */
  public Event.ContinuationFrame callbackState;

  /**
   * Incoming caller information for returning (blocking) call event.
   */
  public Event currentCaller;

  /**
   * Controller thread of execution.
   */
  private Thread thread;

  /**
   * Whether we are inside the event loop.
   */
  private boolean isRunning;

  /**
   * Controller class loader.
   */
  private ClassLoader loader;

  /**
   * Simulation time units (number of ticks).
   */
  private long simunitTicks;

  /**
   * Simulation time units string.
   */
  private String simunitString;

  /**
   * JiST application logger.
   */
  private JistAPI.Logger applog;

  /**
   * controller starting time.
   */
  private long startTime;

  /**
   * Non-application exception that aborts simulation, if any.
   */
  private Throwable simulationException;

  /** Blocking sleep singleton entity. */
  public BlockingSleep entityBlockingSleep;

  //////////////////////////////////////////////////
  // threading
  //

  /**
   * Provides a Thread with a slot to store the Controller instance. This thread
   * runs the Controller, and enables the executing simulation to obtain its invoking 
   * Controller instance.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */
  static final class JistThread extends Thread
  {
    /**
     * Slot to store Controller instance.
     */
    public final Controller controller;

    /**
     * Create a thread group with given name.
     *
     * @param controller controller object
     * @param name name for thread group
     */
    public JistThread(Controller controller, String name)
    {
      super(controller, name);
      this.controller = controller;
    }
  } // class: JistThread


  //////////////////////////////////////////////////
  // initialization
  //

  /**
   * Create and initialize Controller instance.
   *
   * @throws RemoteException distributed simulation failure
   * @see jist.runtime.Controller#newController
   */
  private Controller() throws RemoteException
  {
    controllerCount++;
  }

  /**
   * Create and initialize a new Controller instance. May return a single controller, 
   * if JiST is SINGLE_CONTROLLER mode.
   *
   * @param loader class loader (rewriter) to use
   * @return Controller instance
   * @throws RemoteException distributed simulation failure
   * @see Main#SINGLE_CONTROLLER
   */
  public static Controller newController(ClassLoader loader) throws RemoteException
  {
    Controller controller;
    if(Main.SINGLE_CONTROLLER)
    {
      controller = activeController;
    }
    else
    {
      controller = new Controller();
    }
    controller.reset();
    controller.setClassLoader(loader);
    return controller;
  }

  /**
   * Create internal event queue and entity list.
   *
   * @return any unhandled (non-application) exception (such as
   * VirtualMachineError) thrown by previous simulation run
   */
  public Throwable reset()
  {
    isRunning = false;
    events.clear();
    entities = new Entity[10];
    numEntities = 0;
    Entity staticEntity = new Entity.Static();
    staticEntityRef = registerEntity(staticEntity);
    staticEntity._jistMethod_Set__ref(staticEntityRef);
    endSimulationTime = JistAPI.END-1;
    call = null;
    callState = null;
    callbackState = null;
    setSimUnits(1, " ticks");
    currentSimulationTime = 0;
    currentEvent = new Event();
    currentEvent.time = 0;
    loader = null;
    thread = new JistThread(this, "JistController-"+controllerCount);
    entityBlockingSleep = new BlockingSleep(); // self-registering entity
    if(Main.COUNT_EVENTS)
    {
      eventCounts = new Hashtable();
    }
    // todo:
    Throwable t = simulationException;
    simulationException = null;
    return t;
  }

  //////////////////////////////////////////////////
  // execution
  //

  /**
   * Return whether the simulation controller is running.
   *
   * @return whether the simulation controller is running
   */
  public boolean isRunning()
  {
    return isRunning;
  }

  /**
   * Return the simulation starting time.
   *
   * @return the simulation starting time
   */
  public long getStartTime()
  {
    return startTime;
  }

  /**
   * Run the simulation controller event loop to completion.
   *
   * @return number of events processed
   */
  private long eventLoop()
  {
    long numEvents = 0;
    try
    {
      while(events.size()>0)
      {
        currentEvent = events.removeFirst();
        currentSimulationTime = currentEvent.time;
        processEvent();
        numEvents++;
        disposeEvent(currentEvent);
      }
    }
    catch(JistException.JistSimulationEndException e) { }
    return numEvents;
  }

  /**
   * Begin execution of simulation events. This method will exit when there are
   * no more events to process, or when the simulation time limit is reached,
   * whichever comes first.
   */
  public void run()
  {
    startTime = System.currentTimeMillis();
    endAt(endSimulationTime);
    log.info((new Node(1)).getHostString()+" starting controller event loop.");
    long numEvents = 0;
    Thread displayThread = null;
    isRunning = true;
    try
    {
      if(log.isInfoEnabled())
      {
        displayThread = startDisplayThread();
      }
      // cycle until end time
      numEvents += eventLoop();
      long lastEventTime = currentSimulationTime;
      // clear out unprocessed events
      while(!events.isEmpty())
      {
        currentEvent = events.removeFirst();
        currentSimulationTime = currentEvent.time;
        if(currentSimulationTime==JistAPI.END) 
        {
          events.insert(currentEvent);
          break;
        }
        if(isDebugLogging)
        {
          log.debug("unprocessed: "+currentEvent);
        }
        disposeEvent(currentEvent);
      }
      // process end-time events
      while(!events.isEmpty())
      {
        currentEvent = events.removeFirst();
        currentSimulationTime = currentEvent.time;
        try
        {
          processEvent();
          numEvents++;
        }
        catch(JistException.JistSimulationEndException e) { }
        disposeEvent(currentEvent);
      }
      // cleanup
      currentEvent = null;
      currentSimulationTime = lastEventTime;
    }
    catch(JistException e)
    {
      System.out.println("Simulation exception!");
      if(Main.EVENT_TRACE)
      {
        System.err.println("STACK TRACE:");
      }
      e.getNested().printStackTrace();
      if(Main.EVENT_TRACE)
      {
        System.err.println("EVENT TRACE:");
        EventLocation.printEventTrace();
      }
      numEvents = -1;
    }
    catch(VirtualMachineError e)
    {
      entities = null;
      events.clear();
      System.gc();
      numEvents = -1;
      simulationException = e;
    }
    catch(Throwable t)
    {
      simulationException = t;
    }
    finally
    {
      isRunning = false;
      if(displayThread!=null)
      {
        displayThread.interrupt();
      }
      if(numEvents>0)
      {
        long endTime = System.currentTimeMillis();
        double seconds = (endTime-startTime)/1000.0;
        log.info("TOTAL: "+Util.getHMS((long)seconds)+" real, "+
            getSimulationTimeString()+" sim, "+
            numEvents+"ev, "+((long)(numEvents/seconds))+"ev/s");
      }
      if(Main.COUNT_EVENTS)
      {
        Enumeration keys = eventCounts.keys();
        while(keys.hasMoreElements())
        {
          Method m = (Method)keys.nextElement();
          int count = ((int[])eventCounts.get(m))[0];
          System.out.println(m.getDeclaringClass().getName()+"."+m.getName()+": "+count);
        }
      }
    }
  }

  /**
   * Start thread to display status of simulation every so often.
   *
   * @return display thread
   */
  public Thread startDisplayThread()
  {
    Runnable runner = new Runnable()
    {
      public void run()
      {
        synchronized(this)
        {
          this.notify();
        }
        while(isRunning)
        {
          try
          {
            Thread.sleep(Main.CONTROLLER_DISPLAY_INTERVAL);
            long simtime = currentEvent.time;
            if(simtime==0) continue;
            if(simtime>endSimulationTime) continue;
            String msg = "sim-time="+(simtime/simunitTicks)+simunitString;
            if(endSimulationTime!=JistAPI.END)
            {
              double percent = Util.round(simtime / (float)endSimulationTime * 100, 2);
              msg += ", "+percent+"%";
            }
            long memused = Util.getUsedMemory();
            msg += " mem="+(memused/1024/1024)+"M";
            msg += " evQ="+events.size();
            long time = System.currentTimeMillis();
            long seconds = (long)((time-startTime)/1000.0);
            msg += " t="+Util.getHMS(seconds);
            double completed = simtime / (float)endSimulationTime;
            if(endSimulationTime!=Long.MAX_VALUE && completed>0.001)
            {
              long remaining = (long)(seconds / completed * (1-completed));
              msg += " ("+Util.getHMS(remaining)+")";
            }
            log.info(msg);
          }
          catch(InterruptedException e) { }
          catch(Exception e)
          {
            endAt(0); // terminate application loop
            e.printStackTrace();
          }
        }
      }
    };
    Thread t = new Thread(runner);
    synchronized(runner)
    {
      t.start();
      // allow display thread to start
      try 
      { 
        runner.wait(); 
      }
      catch(InterruptedException e) { }
    }
    return t;
  }

  /**
   * Process an event. Set the simulation time and event information, and 
   * then invoke the appropriate entity method.
   */
  private void processEvent()
  {
    if(isDebugLogging)
    {
      if(log.isDebugEnabled())
      {
        log.debug(" proc: "+currentEvent);
      }
    }
    if(Main.COUNT_EVENTS)
    {
      int[] count = (int[])eventCounts.get(currentEvent.method);
      if(count==null)
      {
        count = new int[1];
        eventCounts.put(currentEvent.method, count);
      }
      count[0]++;
    }
    try
    {
      // initialize controller for event
      Event.Continuation cont = currentEvent.cont;
      if(cont!=null)
      {
        callbackState = cont.state;
        currentCaller = cont.caller;
      }
      // invoke event
      Object result = null;
      Throwable exception = null;
      try
      {
        result = currentEvent.method.invoke(entities[currentEvent.ref.getIndex()], currentEvent.args);
      }
      catch(InvocationTargetException e)
      {
        exception = e.getTargetException();
        if(exception instanceof JistException) throw (JistException)exception;
        if(exception instanceof VirtualMachineError) throw (VirtualMachineError)exception;
        if(currentCaller==null || call!=null) throw e;
      }
      catch(IllegalArgumentException e)
      {
        throw new NoSuchMethodException("Unable to invoke METHOD: "+currentEvent.method
            +" on target ENTITY: "+entities[currentEvent.ref.getIndex()]);
      }
      // handle blocking calls
      if(call!=null)
      {
        if(isDebugLogging)
        {
          if(log.isDebugEnabled())
          {
            log.debug("saved event state!");
          }
        }
        // create callback event
        callback.cont = continuationPool.get();
        callback.cont.state = callState;
        callback.cont.result = null;
        callback.cont.exception = null;
        callback.cont.caller = currentCaller;
        // hook it onto call event
        call.cont = continuationPool.get();
        call.cont.caller = callback;
        // schedule
        if(Main.SINGLE_CONTROLLER)
        {
          addEvent(call);
        }
        else
        {
          call.ref.getController().addEvent(call);
        }
        // clear controller locals
        call = null;
        callState = null;
        callback = null;
      }
      // return to caller
      else if(currentCaller!=null)
      {
        currentCaller.time = currentSimulationTime;
        currentCaller.cont.result = result;
        currentCaller.cont.exception = exception;
        if(Main.SINGLE_CONTROLLER)
        {
          addEvent(currentCaller);
        }
        else
        {
          currentCaller.ref.getController().addEvent(currentCaller);
        }
      }
      currentCaller = null;
      callbackState = null;
    }
    catch(InvocationTargetException e)
    {
      Throwable t = e.getTargetException();
      if(t instanceof JistException) throw (JistException)t;
      else if(t instanceof VirtualMachineError) throw (VirtualMachineError)t;
      else throw new JistException("application exception propagated to event loop", e.getTargetException());
    }
    catch(JistException e)
    {
      throw e;
    }
    catch(Exception e)
    {
      throw new JistException("unexpected event loop exception", e);
    }
    if(isDebugLogging)
    {
      if(log.isDebugEnabled())
      {
        log.debug("  end: t="+currentSimulationTime+"\n");
      }
    }
  } // function: processEvent

  // ControllerRemote interface
  /** {@inheritDoc} */
  public void start()
  {
    thread.setContextClassLoader(loader);
    thread.start();
  }

  /**
   * Wait for simulation to finish.
   * @throws InterruptedException interrupted simulation thread
   */
  public void join() throws InterruptedException
  {
    thread.join();
  }

  //////////////////////////////////////////////////
  // events
  //

  /**
   * Return event currently being processed.
   *
   * @return event currently being processed
   */
  public Event getCurrentEvent()
  {
    return currentEvent;
  }

  /**
   * Create an event using the event pool, using the current simulation time
   * and the given invocation parameters.
   *
   * @param method invocation method for event
   * @param ref invocation entity reference for event
   * @param args invocation parameters for event
   * @return event with given parameters and current simulation time
   */
  private Event createEvent(Method method, EntityRef ref, Object[] args)
  {
    Event ev = eventPool.get();
    ev.time = currentSimulationTime;
    ev.method = method;
    ev.ref = ref;
    ev.args = args;
    return ev;
  }

  /**
   * Create an event using the event pool, using the given simulation time
   * and the given invocation parameters.
   *
   * @param method invocation method for event
   * @param ref invocation entity reference for event
   * @param args invocation parameters for event
   * @param time simulation time for event
   * @return event with given parameters and simulation time
   */
  private Event createEvent(Method method, EntityRef ref, Object[] args, long time)
  {
    Event ev = eventPool.get();
    ev.time = time;
    ev.method = method;
    ev.ref = ref;
    ev.args = args;
    return ev;
  }

  /**
   * Create and register an outgoing call event. This event will be kept
   * by the active Controller until the state saving is complete, and then
   * scheduled (back in the processing loop).
   *
   * @param method invocation method for event
   * @param ref invocation entity reference for event
   * @param args invocation parameters for event
   */
  public void registerCallEvent(Method method, EntityRef ref, Object[] args)
  {
    Event ev = eventPool.get();
    ev.time = currentSimulationTime;
    ev.method = method;
    ev.ref = ref;
    ev.args = args;
    if(Main.ASSERT) Util.assertion(call==null);
    call = ev;
    callback = createEvent(currentEvent.method, currentEvent.ref, currentEvent.args);
    callState = Event.ContinuationFrame.BASE;
  }

  /**
   * Dispose event into the event pool, and clear any references in the event
   * object to allow for GC.
   *
   * @param ev event instance to dispose
   */
  public void disposeEvent(Event ev)
  {
    if(!Main.EVENT_TRACE && !Main.EVENT_LOCATION)
    {
      ev.ref = null;
      ev.args = null;
      ev.method = null;
      if(ev.cont!=null)
      {
        ev.cont.state = null;
        ev.cont.result = null;
        ev.cont.exception = null;
        ev.cont.caller = null;
        continuationPool.put(ev.cont);
        ev.cont = null;
      }
      eventPool.put(ev);
    }
  }

  /**
   * Helper method to log events as they are scheduled.
   *
   * @param ev scheduled event
   */
  private void logEventSched(Event ev)
  {
    if(isDebugLogging)
    {
      if(log.isDebugEnabled())
      {
        log.debug(" schd: "+ev);
      }
      if(guilog!=null)
      {
        guilog.add(ev, currentEvent);
      }
    }
  }

  // ControllerRemote interface
  /** {@inheritDoc} */
  public void addEvent(Event ev)
  {
    events.insert(ev);
    if(isDebugLogging) logEventSched(ev);
  }

  // ControllerRemote interface
  /** {@inheritDoc} */
  public void addEvent(Method meth, EntityRef ref, Object[] params)
  {
    Event ev = createEvent(meth, ref, params);
    events.insert(ev);
    if(isDebugLogging) logEventSched(ev);
  }

  // ControllerRemote interface
  /** {@inheritDoc} */
  public void addEvent(Method meth, EntityRef ref, Object[] params, long time)
  {
    Event ev = createEvent(meth, ref, params, time);
    events.insert(ev);
    if(isDebugLogging) logEventSched(ev);
  }

  //////////////////////////////////////////////////
  // time
  //

  /**
   * Get current simulation time. The simulation time is the base simulation
   * time of the current event, plus all the sleeps incurred during processing.
   *
   * @return current simulation time
   */
  public long getSimulationTime()
  {
    return currentSimulationTime;
  }

  /**
   * Return time string in simulation time units.
   *
   * @return time string in simulation time units
   */
  public String getSimulationTimeString()
  {
    long time = getSimulationTime();
    if(time==JistAPI.END)
    {
      return "END";
    }
    else
    {
      return (time/simunitTicks)+simunitString;
    }
  }

  /**
   * Advance the current simulation time.
   *
   * @param i number of steps to advance simulation time
   */
  public void advanceSimulationTime(long i)
  {
    currentSimulationTime += i;
    if(isDebugLogging)
    {
      if(log.isDebugEnabled())
      {
        log.debug(" advancing simulation time to t="+currentSimulationTime);
      }
    }
  }

  // ControllerRemote interface
  /** {@inheritDoc} */
  public void endAt(long time)
  {
    endSimulationTime = time;
    JistAPI_Impl.callStaticAt(JistException.JistSimulationEndException.method_end, null, time);
  }

  // ControllerRemote interface
  /** {@inheritDoc} */
  public void setSimUnits(long ticks, String name)
  {
    simunitTicks = ticks;
    simunitString = name;
  }

  //////////////////////////////////////////////////
  // entities
  //

  /**
   * Register an entity with the Controller.
   * 
   * @param entity entity to register with current controller
   * @return entity reference to given entity
   */
  public synchronized EntityRef registerEntity(Entity entity)
  {
    EntityRef ref;
    if(Main.SINGLE_CONTROLLER)
    {
      ref = new EntityRef(numEntities);
    }
    else
    {
      ref = new EntityRefDist(this, numEntities);
    }
    if(numEntities==entities.length)
    {
      Entity[] entities2 = new Entity[entities.length*2];
      System.arraycopy(entities, 0, entities2, 0, entities.length);
      entities = entities2;
    }
    entities[numEntities++] = entity;
    return ref;
  }

  /**
   * Return an entity owned by this Controller.
   *
   * @param index local entity identifier
   * @return requested entity object
   */
  public Entity getEntity(int index)
  {
    return entities[index];
  }

  // ControllerRemote interface
  /** {@inheritDoc} */
  public Class getEntityClass(int index) throws RemoteException
  {
    return getEntity(index).getClass();
  }

  /**
   * Return the entity reference to the "static" entity.
   *
   * @return entity reference of the static entity
   */
  public EntityRef getStaticEntityRef()
  {
    return staticEntityRef;
  }

  /**
   * Return string of entity at given index.
   *
   * @param index entity id
   * @return string of entity at given index
   * @throws RemoteException distributed simulation failure
   */
  public String toStringEntity(int index) throws RemoteException
  {
    return getEntity(index).toString();
  }

  /**
   * String of given object or entity.
   *
   * @param o object or entity to stringify
   * @return string of given object to stringify
   * @throws RemoteException distributed simulation failure
   */
  public static String toString(Object o) throws RemoteException
  {
    try
    {
      if(JistAPI_Impl.isEntity(o))
      {
        EntityRef ref = null;
        if(o instanceof EntityRef)
        {
          ref = (EntityRef)o;
        }
        else if(Proxy.isProxyClass(o.getClass()) && Proxy.getInvocationHandler(o) instanceof EntityRef)
        {
          ref = (EntityRef)Proxy.getInvocationHandler(o);
        }
        else
        {
          throw new RuntimeException("strange entity object");
        }
        return "entity:"+ref.getController().toStringEntity(ref.getIndex());
      }
      else
      {
        if(o.getClass().isArray())
        {
          StringBuffer sb = new StringBuffer();
          sb.append("[");
          for(int i=0; i<Array.getLength(o); i++)
          {
            if(i>0) sb.append(",");
            sb.append(Controller.toString(Array.get(o,i)));
          }
          sb.append("]");
          return sb.toString();
        }
        else
        {
          return o.toString();
        }
      }
    }
    catch(NullPointerException e)
    {
      return "null";
    }
  }

  //////////////////////////////////////////////////
  // application callbacks (JistAPI)
  //

  /**
   * Return active controller instance. This is the controller instance from
   * the controller slot of the thread group of the current thread.
   *
   * @return active Controller instance
   */
  public static Controller getActiveController()
  {
    if(Main.SINGLE_CONTROLLER)  // (controllerCount==1)
    {
      // note: this "optimization" does not seem to make any difference
      return activeController;
    }
    else
    {
      return ((JistThread)Thread.currentThread()).controller;
    }
  }

  /**
   * Return class loader used by this Controller instance.
   *
   * @return class loader used by this Controller instance.
   */
  public ClassLoader getClassLoader()
  {
    return loader;
  }

  /**
   * Set the Controller class loader (rewriter).
   *
   * @param loader class loader / rewriter for controller
   */
  public void setClassLoader(ClassLoader loader)
  {
    this.loader = loader;
  }

  /**
   * Return current entity.
   *
   * @return current entity
   */
  public static EntityRef getTHIS()
  {
    return getActiveController().currentEvent.ref;
  }

  /**
   * Application callback method to register an new entity, creating an entity
   * reference.
   * 
   * @param entity newly created entity to be registered
   * @return new entity reference to give entity
   */
  public static EntityRef newEntityReference(Entity entity)
  {
    return getActiveController().registerEntity(entity);
  }

  /**
   * Application callback method to return the entity reference for a given
   * object. If an Entity is provided, then the corresponding EntityRef is
   * returned. Otherwise, an EntityRef is assumed, and returned verbatim.
   *
   * @param o object of either Entity or EntityRef type
   * @return EntityRef of given object
   */
  public static EntityRef getEntityReference(Object o)
  {
    return o instanceof Entity 
      ? ((Entity)o)._jistMethod_Get__ref()
      : (EntityRef)o;
  }

  /**
   * Application callback method to perform (intercept) an entity method
   * invocation at the appropriate simulation time.
   *
   * @param meth method to be invoked
   * @param ref reference to entity being invoked
   * @param params invocation parameters
   */
  public static void entityInvocation(Method meth, EntityRef ref, Object[] params)
  {
    if(Main.SINGLE_CONTROLLER)
    {
      activeController.addEvent(meth, ref, params);
    }
    else
    {
      try
      {
        ref.getController().addEvent(meth, ref, params);
      }
      catch(RemoteException e)
      {
        throw new JistException("distributed simulation failure", e);
      }
    }
  }

  /**
   * Application callback method to perform (intercept) an entity method
   * invocation at the appropriate simulation time.
   *
   * @param meth method to be invoked
   * @param entity entity to be invoked
   * @param params invocation parameters
   */
  public static void entityInvocation(Method meth, Entity entity, Object[] params)
  {
    entityInvocation(meth, entity._jistMethod_Get__ref(), params);
  }

  /**
   * Application callback method to perform (intercept) an entity method
   * invocation at the appropriate simulation time.
   *
   * @param meth method to be invoked
   * @param callee object (either entity or entityreference) to be invoked
   * @param params invocation parameters
   */
  public static void entityInvocation(Method meth, Object callee, Object[] params)
  {
    entityInvocation(meth,
        callee instanceof EntityRef 
          ? (EntityRef)callee 
          : ((Entity)callee)._jistMethod_Get__ref(), 
        params);
  }

  /**
   * Application callback method to perform (intercept) a BLOCKING entity 
   * method invocation at the appropriate simulation time. This method
   * will be called once when the blocking call is initiated, and once
   * when the callback is restoring its state. This ping-pong is necessary
   * to capture the execution stack -- no other way in pure Java.
   *
   * @param meth method to be invoked
   * @param ref reference to entity being invoked
   * @param params invocation parameters
   * @return whatever was returned by blocking event
   * @throws JistAPI.Continuation never (marker for rewriter)
   * @throws Throwable whatever was thrown by blocking event
   */
  public static Object entityInvocationCont(Method meth, EntityRef ref, Object[] params) throws JistAPI.Continuation, Throwable
  {
    Controller c = getActiveController();
    if(c.isModeRestore())
    {
      // restore complete
      if(isDebugLogging)
      {
        if(log.isDebugEnabled())
        {
          log.debug("restored event state!");
        }
      }
      // return callback result
      return c.clearRestoreState();
    }
    else
    {
      // calling blocking method
      c.registerCallEvent(meth, ref, params);
      return null;
    }
  }

  /**
   * Application callback method to perform (intercept) a BLOCKING entity 
   * method invocation at the appropriate simulation time. This method
   * will be called once when the blocking call is initiated, and once
   * when the callback is restoring its state. This ping-pong is necessary
   * to capture the execution stack -- no other way in pure Java.
   *
   * @param meth method to be invoked
   * @param entity entity to be invoked
   * @param params invocation parameters
   * @return whatever was returned by blocking event
   * @throws JistAPI.Continuation never (marker for rewriter)
   * @throws Throwable whatever was thrown by blocking event
   */
  public static Object entityInvocationCont(Method meth, Entity entity, Object[] params) throws JistAPI.Continuation, Throwable
  {
    return entityInvocationCont(meth, entity._jistMethod_Get__ref(), params);
  }

  /**
   * Application callback method to perform (intercept) a BLOCKING entity 
   * method invocation at the appropriate simulation time. This method
   * will be called once when the blocking call is initiated, and once
   * when the callback is restoring its state. This ping-pong is necessary
   * to capture the execution stack -- no other way in pure Java.
   *
   * @param meth method to be invoked
   * @param callee object (either entity or entityreference) to be invoked
   * @param params invocation parameters
   * @return whatever was returned by blocking event
   * @throws JistAPI.Continuation never (marker for rewriter)
   * @throws Throwable whatever was thrown by blocking event
   */
  public static Object entityInvocationCont(Method meth, Object callee, Object[] params) throws JistAPI.Continuation, Throwable
  {
    return entityInvocationCont(meth,
        callee instanceof EntityRef ? (EntityRef)callee : ((Entity)callee)._jistMethod_Get__ref(), 
        params);
  }

  //////////////////////////////////////////////////
  // continuation state save/restore
  //

  /**
   * Return whether the event invocation is currently in restore mode.
   *
   * @return true if currently restoring an event execution stack
   */
  public static boolean isModeRestore()
  {
    return getActiveController().callbackState != null;
  }

  /**
   * Return whether the event invocation is currently in restore mode.
   *
   * @return true if currently restoring an event execution stack
   */
  public boolean isModeRestoreInst()
  {
    return callbackState != null;
  }

  /**
   * Return whether the event invocation is currently in save mode.
   *
   * @return true if current save an event execution stack
   */
  public static boolean isModeSave()
  {
    return getActiveController().call != null;
  }

  /**
   * Return whether the event invocation is currently in save mode.
   *
   * @return true if current save an event execution stack
   */
  public boolean isModeSaveInst()
  {
    return call != null;
  }

  /**
   * Clear restore state of current event, and callback result.
   *
   * @return callback result
   * @throws Throwable whatever was thrown by the blocking event
   */
  public Object clearRestoreState() throws Throwable
  {
    callbackState = null;
    if(currentEvent.cont.exception!=null)
    {
      throw currentEvent.cont.exception;
    }
    else
    {
      return currentEvent.cont.result;
    }
  }

  /**
   * Pop a frame from the incoming execution stack.
   *
   * @return next frame from the incoming execution stack.
   */
  public static Event.ContinuationFrame popStateInFrame()
  {
    Controller c = getActiveController();
    Event.ContinuationFrame f = c.callbackState;
    c.callbackState = c.callbackState.next;
    if(isDebugLogging)
    {
      if(log.isDebugEnabled())
      {
        log.debug("popStateIn: "+f.getClass());
      }
    }
    return f;
  }

  /**
   * Push a frame onto the outgoing execution stack.
   *
   * @param f next frame of outgoing execution stack.
   */
  public static void pushStateOutFrame(Event.ContinuationFrame f)
  {
    if(isDebugLogging)
    {
      if(log.isDebugEnabled())
      {
        log.debug("pushStateOut "+f.getClass());
      }
    }
    Controller c = getActiveController();
    f.next = c.callState;
    c.callState = f;
  }

  /**
   * Switch caller (return) event.
   *
   * @param caller new caller (return) event
   * @return old caller (return) event
   */
  public Event switchCaller(Event caller)
  {
    Event tmp = currentCaller;
    this.currentCaller = caller;
    return tmp;
  }

  //////////////////////////////////////////////////
  // logging
  //

  /**
   * Set logging implementation.
   *
   * @param logger logging implementation
   */
  public void setLog(JistAPI.Logger logger)
  {
    this.applog = logger;
  }

  /**
   * Set logging implementation.
   *
   * @param loggerClass logging class
   * @throws InstantiationException invalid logger class
   * @throws IllegalAccessException invalid logger class
   */
  public void setLog(Class loggerClass) 
    throws InstantiationException, IllegalAccessException
  {
    setLog((JistAPI.Logger)loggerClass.newInstance());
  }

  // ControllerRemote interface
  /** {@inheritDoc} */
  public void log(String s)
  {
    if(applog!=null)
    {
      applog.log(s);
    }
  }

} // class: Controller

