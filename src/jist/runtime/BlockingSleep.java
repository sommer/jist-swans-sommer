//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <BlockingSleep.java Thu 2005/02/24 21:19:21 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.lang.reflect.Method;
import java.rmi.RemoteException;

/**
 * Implements API support for blocking sleeps.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: BlockingSleep.java,v 1.1 2005-02-25 04:43:58 barr Exp $
 * @since JIST1.0
 */
public class BlockingSleep extends Entity.Empty implements JistAPI.Entity
{

  //////////////////////////////////////////////////
  // entity method stubs
  //

  /**
   * Jist method stub for (blocking) sleep method.
   */
  public static Method _jistMethodStub_sleep_28J_29V;

  static 
  {
    try
    {
      _jistMethodStub_sleep_28J_29V =
        BlockingSleep.class.getDeclaredMethod(
            "sleep",
            new Class[] { Long.TYPE });
    }
    catch(NoSuchMethodException e)
    {
      throw new JistException("should not happen", e);
    }
  }

  /**
   * Create new BlockingSleepEntity.
   * @see JistAPI
   */
  // intentionally prevent out-of-package initialization; use JistAPI
  BlockingSleep()
  {
  }

  /**
   * Blocking sleep implementation.
   *
   * @param i number of simulation ticks to sleep
   */
  public void sleep(long i) throws JistAPI.Continuation
  {
    JistAPI_Impl.sleep(i);
  }

} // class: BlockingSleep

