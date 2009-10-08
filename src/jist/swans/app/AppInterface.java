//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <AppInterface.java Wed 2005/03/09 15:45:35 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app;

import jist.swans.trans.TransInterface;
import jist.swans.app.lang.SimtimeThread;

import jist.runtime.JistAPI;

/** 
 * Interface for Application entities.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: AppInterface.java,v 1.8 2005-03-13 14:43:34 barr Exp $
 * @since SWANS1.0
 */

public interface AppInterface extends JistAPI.Proxiable
{

  /**
   * Run application.
   */
  void run();

  /**
   * Run application.
   *
   * @param args command-line parameters
   */
  void run(String[] args);

  /**
   * Application that supports TCP sockets.
   */
  public static interface TcpApp
  {
    /**
     * Return application TCP entity.
     *
     * @return application TCP entity
     * @throws JistAPI.Continuation not thrown; marker for rewriter
     */
    TransInterface.TransTcpInterface getTcpEntity() throws JistAPI.Continuation;
  }

  /**
   * Application that supports UDP sockets.
   */
  public static interface UdpApp
  {
    /**
     * Return application UDP entity.
     *
     * @return application UDP entity
     * @throws JistAPI.Continuation not thrown; marker for rewriter
     */
    TransInterface.TransUdpInterface getUdpEntity() throws JistAPI.Continuation;
  }

  /**
   * Application that supports threading.
   */
  public interface ThreadedApp
  {
    /**
     * Get current thread from thread context.
     *
     * @return thread entity
     */
    public SimtimeThread getCurrentThread();

    /**
     * Set current thread in thread context.
     *
     * @param thread thread entity
     */
    public void setCurrentThread(SimtimeThread t);
  }

} // interface: AppInterface

