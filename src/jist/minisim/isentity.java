//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <isentity.java Tue 2004/04/06 11:27:55 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.minisim;

import java.lang.reflect.*;
import jist.runtime.JistAPI;

/**
 * Test JiST isEntity API.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: isentity.java,v 1.3 2004-04-06 16:07:42 barr Exp $
 * @since JIST1.0
 */

public class isentity
{

  /** a dummy regular entity. */
  public static class myEntity implements JistAPI.Entity 
  { 
  }

  /** an entity interface. */
  public interface myObjectInterface extends JistAPI.Proxiable 
  { 
  }

  /** a dummy proxy entity. */
  public static class myObject implements myObjectInterface 
  { 
  }

  /**
   * Program entry point: Test different kinds of references 
   * with JistAPI isEntity.
   *
   * @param args command-line parameters
   */
  public static void main(String[] args)
  {
    System.out.println("object:    "+JistAPI.isEntity(new Object()));
    System.out.println("entity:    "+JistAPI.isEntity(new myEntity()));
    System.out.println("proxiable: "+JistAPI.isEntity(new myObject()));
    System.out.println("proxied:   "+JistAPI.isEntity(
          JistAPI.proxy(new myObject(), myObjectInterface.class)));

  }
}
