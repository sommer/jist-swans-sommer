//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <block.java Tue 2004/04/06 11:27:12 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.minisim;

import jist.runtime.JistAPI;
import jist.runtime.Channel;

/**
 * Blocking channel entity demo/test. Note how the send and receive co-routines
 * block at the channel send and receive calls. The channel also provides
 * methods for non-blocking sends, and dropping packets if the channel is full.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: block.java,v 1.7 2004-04-06 16:07:42 barr Exp $
 * @since JIST1.0
 */

public class block implements JistAPI.Entity
{
  /**
   * Communication channel entity.
   */
  private Channel c = JistAPI.createChannel();

  /**
   * Schedule two messages; one with the receive first
   * and one with the send first.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    block b = new block();
    // do blocking receive-send
    b.receive();
    JistAPI.sleep(1);
    b.send();
    JistAPI.sleep(1);
    // do blocking send-receive
    b.send();
    JistAPI.sleep(1);
    b.receive();
  }

  /**
   * Send a message along the channel.
   */
  public void send()
  {
    System.out.println("send called at t="+JistAPI.getTime());
    c.send("foo");
    System.out.println("send continues at t="+JistAPI.getTime());
  }

  /**
   * Receive a message from the channel.
   */
  public void receive()
  {
    System.out.println("receive called at t="+JistAPI.getTime());
    Object o = c.receive();
    System.out.println("receive continues at t="+JistAPI.getTime()+" with object="+o);
  }

} // class block

