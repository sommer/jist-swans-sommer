//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <PathLoss.java Wed 2004/06/23 09:18:00 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.field;

import jist.swans.radio.RadioInfo;
import jist.swans.misc.Location;
import jist.swans.misc.Util;

import jist.swans.Constants;

/** 
 * Interface for performing pathloss calculations.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: PathLoss.java,v 1.22 2004-06-23 17:15:54 barr Exp $
 * @since SWANS1.0
 */

public interface PathLoss
{

  //////////////////////////////////////////////////
  // interface
  //

  /**
   * Compute the path loss.
   *
   * @param srcRadio source radio information
   * @param srcLocation source location
   * @param dstRadio destination radio information
   * @param dstLocation destination location
   * @return path loss (units: dB)
   */
  double compute(RadioInfo srcRadio, Location srcLocation, 
      RadioInfo dstRadio, Location dstLocation);


  //////////////////////////////////////////////////
  // implementations
  //

  /** 
   * Computes free-space path loss. Equivalent to GloMoSim code.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  final class FreeSpace implements PathLoss
  {
    // PathLoss interface
    /** {@inheritDoc} */
    public double compute(RadioInfo srcRadio, Location srcLocation, 
        RadioInfo dstRadio, Location dstLocation)
    {
      double dist = srcLocation.distance(dstLocation);
      double pathloss = - srcRadio.getShared().getGain() - dstRadio.getShared().getGain();
      double valueForLog = 4.0 * Math.PI * dist / srcRadio.getShared().getWaveLength();
      if (valueForLog > 1.0)
      {
        pathloss += Util.log((float)valueForLog) / Constants.log10 * 20.0;
      }
      return pathloss;
    }

  } // class: FreeSpace

  /** 
   * Computes two-ray path loss. Equivalent to GloMoSim code.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  final class TwoRay implements PathLoss
  {
    // PathLoss interface
    /** {@inheritDoc} */
    public double compute(RadioInfo srcRadio, Location srcLocation, 
        RadioInfo dstRadio, Location dstLocation)
    {
      double dist = srcLocation.distance(dstLocation);
      double pathloss = - srcRadio.getShared().getGain() - dstRadio.getShared().getGain();
      double planeEarthLoss = (dist * dist) / 
        (srcLocation.getHeight() * dstLocation.getHeight());
      double freeSpaceLoss = 4.0 * Math.PI * dist / srcRadio.getShared().getWaveLength();
      if (planeEarthLoss > freeSpaceLoss)
      {
        if (planeEarthLoss > 1.0)
        {
          pathloss += 20.0 * Math.log(planeEarthLoss) / Constants.log10;
        }
      }
      else
      {
        if (freeSpaceLoss > 1.0)
        {
          pathloss += 20.0 * Math.log(freeSpaceLoss) / Constants.log10;
        }
      }
      return pathloss;
    }
  } // class: TwoRay

  // todo: MITRE's pathloss format
  // Time (nearest whole second)    Node A     Node B     Path Loss (dB) Range (meters)
  // End of file is indicated by a -1 in the first column.  (And nothing else on the line.)  

} // class: PathLoss

