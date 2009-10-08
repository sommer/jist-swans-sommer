//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <LogInterface.java Tue 2004/04/06 11:59:38 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime.guilog;

import jist.runtime.Event;

/**
 * @author Mark Fong
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: LogInterface.java,v 1.5 2004-07-23 07:43:01 mjf21 Exp $
 * @since SWANS1.0
 **/

public interface LogInterface
{
  /**
   * Adds an event to the GUI.
   *
   * @param id event id is added to the table
   * @param parent id's parent event
   */
  void add(Event id, Event parent);

  /**
   * Deletes an event from the GUI.
   *
   * @param id event that is deleted from the GUI.
   */
  void del(Event id);
}
