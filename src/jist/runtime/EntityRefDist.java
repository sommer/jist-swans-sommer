//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <EntityRefDist.java Tue 2004/04/06 11:23:14 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

/** 
 * Distributed EntityRef.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: EntityRefDist.java,v 1.3 2004-04-06 16:07:43 barr Exp $
 * @since JIST1.0
 */

public class EntityRefDist extends EntityRef
{
  /**
   * Controller owning entity.
   */
  private final ControllerRemote controller;

  /**
   * Initialise a new entity reference with given
   * Controller and Entity IDs.
   *
   * @param controller controller ID
   * @param index entity ID
   */
  public EntityRefDist(ControllerRemote controller, int index)
  {
    super(index);
    this.controller = controller;
  }

  /**
   * Return controller of referenced entity.
   *
   * @return controller of referenced entity
   */
  public ControllerRemote getController()
  {
    return controller;
  }

} // class: EntityRefDist
