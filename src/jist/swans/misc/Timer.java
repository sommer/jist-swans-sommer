//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Timer.java Tue 2004/04/06 11:46:57 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.misc;

/**
 * Timer expiration interface.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Timer.java,v 1.3 2004-04-06 16:07:49 barr Exp $
 * @since SWANS1.0
 */
public interface Timer
{

  /**
   * Timer expiration processing.
   */
  void timeout();

} // interface: Timer
