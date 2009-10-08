//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <FieldInterface.java Tue 2004/04/06 11:30:59 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.field;

import jist.swans.radio.RadioInfo;
import jist.swans.misc.Location;
import jist.swans.misc.Message;

import jist.runtime.JistAPI;

/** 
 * Interface for Field entities.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: FieldInterface.java,v 1.17 2004-04-06 16:07:47 barr Exp $
 * @since SWANS1.0
 */

public interface FieldInterface extends JistAPI.Proxiable
{

  //////////////////////////////////////////////////
  // communication
  //

  /**
   * Transmit physical-layer message. Called from radio entity.
   *
   * @param srcInfo source radio information
   * @param msg physical layer packet
   * @param duration transmission duration
   */
  void transmit(RadioInfo srcInfo, Message msg, long duration);

  //////////////////////////////////////////////////
  // radio management
  //

  /**
   * Move radio to different location on field.
   *
   * @param id unique radio identifier
   * @param loc new radio coordinates
   */
  void moveRadio(Integer id, Location loc);

  /**
   * Move radio to different relative location on field.
   *
   * @param id unique radio identifier
   * @param delta radio offset coordinates
   */
  void moveRadioOff(Integer id, Location delta);

} // interface: FieldInterface

