//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <MacInfo.java Tue 2004/04/06 11:32:13 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.mac;

import jist.runtime.JistAPI;

/** 
 * Mac properties.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: MacInfo.java,v 1.4 2004-04-06 16:07:48 barr Exp $
 * @since SWANS1.0
 */

public class MacInfo implements JistAPI.Timeless
{
  /**
   * Timeless information unique to this Mac instance.
   */
  protected MacInfoUnique unique;

  /**
   * Timeless information possibly shared among numerous Mac
   * instances (only to save simulation memory).
   */
  protected MacInfoShared shared;

  /**
   * Timeless information unique to a single Mac instance.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  public static class MacInfoUnique implements JistAPI.Timeless
  {
    /**
     * Unique mac address.
     */
    protected MacAddress addr;

    /**
     * Return Mac address.
     *
     * @return mac address
     */
    public MacAddress getAddr()
    {
      return addr;
    }

  } // class: MacInfoUnique


  /**
   * Timeless information possibly shared among numerous Mac
   * instances (only to save simulation memory).
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  public static class MacInfoShared implements JistAPI.Timeless
  {
    /**
     * Mac bandwidth.
     */
    protected int bandwidth;

    /**
     * Return Mac bandwidth.
     *
     * @return Mac bandwidth
     */
    public int getBandwidth()
    {
      return bandwidth;
    }

  } // class: MacInfoShared


  /**
   * Create Mac information object with shared and unique properties.
   *
   * @param unique unique mac properties
   * @param shared shared mac properties (shared only to save memory)
   */
  public MacInfo(MacInfoUnique unique, MacInfoShared shared)
  {
    this.unique = unique;
    this.shared = shared;
  }

  /**
   * Return unique mac properties.
   *
   * @return unique mac properties
   */
  public MacInfoUnique getUnique()
  {
    return unique;
  }

  /**
   * Return shared radio properties.
   *
   * @return shared radio properties
   */
  public MacInfoShared getShared()
  {
    return shared;
  }

} // class: MacInfo

