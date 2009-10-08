//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <PriorityList.java Tue 2004/04/06 11:36:31 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.trans;

import java.util.*;
import jist.swans.trans.TransTcp.TcpMessage;

/**
 * Data structure that holds TCP message and sorts
 * them based on their sequence numbers.
 *
 * @author Kelwin Tamtoro &lt;kt222@cs.cornell.edu&gt;
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: PriorityList.java,v 1.5 2004-04-06 16:07:51 barr Exp $
 * @since SWANS1.0
 */
public class PriorityList
{
  
  /** 
   * List of messages.
   */
  private List llist;
  
  /**
   * Constructor.
   */
  public PriorityList()
  {
    llist = new ArrayList();
  }

  /**
   * Insert a message to the possible priority list.
   * Messages are stored in increasing order, ordered by the
   * sequence number.
   *
   * @param msg TCP message to be inserted
   */
  public void insert(TcpMessage msg)
  {
    // dont add if it is already in the list
    if(retrieve(msg.getSeqNum())!=null) return;
    // seek for location to insert
    final int seq = msg.getSeqNum();
    int i;
    for(i=0; i<llist.size() ; i++)
    {
      if(seq<((TcpMessage)llist.get(i)).getSeqNum()) break;
    }
    // insert message
    llist.add(i, msg);
  }

  /**
   * Find a TCP message given the sequence number.
   * 
   * @param seqNum sequence number of message to search
   * @return the position of the message; -1 if not found
   */
  public int find(int seqNum)
  {
    for (int i=0; i<llist.size(); i++)
    {
      if (((TcpMessage)(llist.get(i))).getSeqNum() == seqNum) return i;
    }
    return -1;
  }

  /**
   * Retrieve a message with given sequence number.
   * (Returns null if message is not found)
   * 
   * @param seqNum sequence number of the message
   * @return TCP message
   */
  public TcpMessage retrieve(int seqNum)
  {
    int i = find(seqNum);
    return i==-1 ? null : (TcpMessage)llist.get(i);
  }

  /**
   * Removing a specific message.
   *
   * @param seqNum sequence number of message to be removed
   */
  public void removeMessage(int seqNum)
  {
    int i = find(seqNum);
    if(i!=-1) llist.remove(i);
  }
  
  /**
   * Removing all messages that have number less than 
   * the given number.
   * All messages with sequence number less than the
   * given seqNum will be removed.
   * (If they are equal, the message is not removed)
   *
   * @param seqNum messages with sequence number lower than this
   *               will be removed
   */
  public void removeMessages (int seqNum)
  {
    while(llist.size()>0 && ((TcpMessage)llist.get(0)).getSeqNum()<seqNum)
    {
      llist.remove(0);
    }
  }
  
    
  /**
   * This method is used to print out the list content.
   */
  public void printList ()
  {
    System.out.println ("PriorityList: Printing out list...");
    for (int i = 0; i < llist.size(); i++)
    {
      System.out.println ("@@@ Msg " + i + ": " + ((TcpMessage)llist.get(i)));
    }
  }
}
