//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Entity.java Tue 2004/04/06 11:22:58 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

/** 
 * An internal interface that all <code>JistAPI.Entity</code> application
 * objects are rewritten to implement. This interface is an internal one
 * (should not be mentioned in simulation program) and allows the JiST system
 * to manage JistAPI.Entity objects.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Entity.java,v 1.15 2004-04-06 16:07:43 barr Exp $
 * @see jist.runtime.JistAPI.Entity
 * @since JIST1.0
 */

public interface Entity extends Timeless
{
  /**
   * Get this entity's self-reference object .
   *
   * @return entity reference object
   */
  EntityRef _jistMethod_Get__ref();

  /**
   * Set this entity's self-reference object.
   *
   * @param ref entity reference object
   */
  void _jistMethod_Set__ref(EntityRef ref);

  /**
   * Dummy implementation of Entity interface.
   */
  class Empty implements Entity
  {
    /**
     * Self-referencing EntityRef of this entity.
     */
    protected EntityRef _jistField__ref;

    /**
     * Create a new entity and register with active controller.
     */
    public Empty()
    {
      this(true);
    }

    /**
     * Create a new entity.
     *
     * @param reg whether to register with active controller
     */
    public Empty(boolean reg)
    {
      if(reg)
      {
        this._jistField__ref = Controller.newEntityReference(this);
      }
    }

    //////////////////////////////////////////////////
    // Entity interface
    //

    /** {@inheritDoc} */
    public EntityRef _jistMethod_Get__ref()
    {
      return _jistField__ref;
    }

    /** {@inheritDoc} */
    public void _jistMethod_Set__ref(EntityRef ref)
    {
      this._jistField__ref = ref;
    }

  } // class: Empty

  /**
   * Entity used for static calls.
   */
  class Static extends Empty
  {
    /**
     * Create a new static-call entity, but do not register with controller
     * (performed manually).
     */
    public Static()
    {
      super(false);
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return "STATIC";
    }

  } // class: Static

} // interface: Entity

