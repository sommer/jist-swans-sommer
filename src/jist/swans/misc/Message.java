//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Message.java Tue 2004/04/06 11:46:33 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.misc;

/**
 * Interface of all packets sent around the various SWANS layers.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Message.java,v 1.6 2004-04-06 16:07:48 barr Exp $
 * @since SWANS1.0
 */

public interface Message extends jist.runtime.JistAPI.Timeless
{

  /** A null/empty message. */
  Message NULL = new MessageBytes(MessageBytes.EMPTY);

  /**
   * Return packet size or Constants.ZERO_WIRE_SIZE.
   *
   * @return packet size
   */
  int getSize();

  /**
   * Store packet into byte array.
   *
   * @param msg destination byte array
   * @param offset byte array starting offset
   */
  void getBytes(byte[] msg, int offset);

} // interface Message

