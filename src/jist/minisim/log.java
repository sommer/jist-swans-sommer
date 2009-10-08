//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <log.java Tue 2004/04/06 11:28:03 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.minisim;

import jist.runtime.JistAPI;

/**
 * Test JiST custom logging capability.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: log.java,v 1.3 2004-04-06 16:07:42 barr Exp $
 * @since JIST1.0
 */

public class log
{
  /**
   * Program entry point: Write something to the JiST log.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    JistAPI.log("show me the log!");
  }

} // class: log
