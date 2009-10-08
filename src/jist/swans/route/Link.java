//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Link.java Tue 2004/04/06 11:33:11 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.route;

import jist.swans.net.NetAddress;

/**
 * Contains a directed pair of Network addresses.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Link.java,v 1.5 2004-04-06 16:07:50 barr Exp $
 * @since SWANS1.0
 */
public class Link
{
  /** link source address. */
  public final NetAddress src;
  /** link destination address. */
  public final NetAddress dst;

  /**
   * Create link.
   *
   * @param src link source address
   * @param dst link destination address
   */
  public Link(NetAddress src, NetAddress dst)
  {
    if(src==null) throw new IllegalArgumentException("null src");
    if(dst==null) throw new IllegalArgumentException("null dst");
    this.src = src;
    this.dst = dst;
  }

  /**
   * Return link in reverse direction.
   *
   * @return link in reverse direction
   */
  public Link reverse()
  {
    return new Link(dst, src);
  }

  /** {@inheritDoc} */
  public boolean equals(Object o)
  {
    if(!(o instanceof Link)) return false;
    Link l = (Link)o;
    if(!src.equals(l.src)) return false;
    if(!dst.equals(l.dst)) return false;
    return true;
  }

  /** {@inheritDoc} */
  public int hashCode()
  {
    return src.hashCode()+dst.hashCode()*2;
  }

  /** {@inheritDoc} */
  public String toString()
  {
    return src+"-"+dst;
  }

} // class: Link

