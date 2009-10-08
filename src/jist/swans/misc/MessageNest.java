//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <MessageNest.java Tue 2004/04/06 11:46:38 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.misc;

import jist.swans.Constants;

/**
 * Implementation of <code>Message</code> interface that performs
 * message nesting (header, data).
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: MessageNest.java,v 1.5 2004-04-06 16:07:49 barr Exp $
 * @since SWANS1.0
 */

public class MessageNest implements Message
{
  /**
   * Nested message header.
   */
  private Message header;

  /**
   * Nested message body.
   */
  private Message data;

  /**
   * Created new nested message.
   *
   * @param header nested message header
   * @param data nested message body
   */ 
  public MessageNest(Message header, Message data)
  {
    this.header = header;
    this.data = data;
  }

  //////////////////////////////////////////////////
  // Message interface
  //

  /** {@inheritDoc} */
  public int getSize()
  {
    int size = data.getSize();
    if(size==Constants.ZERO_WIRE_SIZE)
    {
      return Constants.ZERO_WIRE_SIZE;
    }
    return header.getSize()+size;
  }

  /** {@inheritDoc} */
  public void getBytes(byte[] msg, int offset)
  {
    header.getBytes(msg, offset);
    data.getBytes(msg, offset+header.getSize());
  }

} // class MessageNest

