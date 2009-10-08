//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RewriterVerify.java Tue 2004/04/06 11:25:08 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import java.io.*;
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.verifier.*;

/**
 * Perform the basic Java class verification checks.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: RewriterVerify.java,v 1.4 2004-04-06 16:07:44 barr Exp $
 * @since JIST1.0
 */

public final class RewriterVerify
{

  /**
   * Check a given class for errors.
   *
   * @param name class name
   */
  public static void checkme(String name)
  {
    System.out.println("Now verifying: "+name+"\n");

    org.apache.bcel.verifier.Verifier v = 
      org.apache.bcel.verifier.VerifierFactory.getVerifier(name);
    org.apache.bcel.verifier.VerificationResult vr;

    vr = v.doPass1();
    System.out.println("Pass 1:\n"+vr);

    vr = v.doPass2();
    System.out.println("Pass 2:\n"+vr);

    if (vr == org.apache.bcel.verifier.VerificationResult.VR_OK)
    {
      JavaClass jc = org.apache.bcel.Repository.lookupClass(name);
      for (int i=0; i<jc.getMethods().length; i++)
      {
        vr = v.doPass3a(i);
        System.out.println("Pass 3a, method number "+i+" ['"+jc.getMethods()[i]+"']:\n"+vr);
        vr = v.doPass3b(i);
        System.out.println("Pass 3b, method number "+i+" ['"+jc.getMethods()[i]+"']:\n"+vr);
      }
    }
  }

  /**
   * Small utility program to verify classes provided on the command
   * line. Very useful for when the JVM just throws you a VerificationError
   * exception without any explanation.
   *
   * @param args list of class files to verify
   */
  public static void main(String[] args)
  {
    try
    {
      for(int i=0; i<args.length; i++)
      {
        ClassParser cp = new ClassParser(args[i]);
        JavaClass jcl = cp.parse();
        Repository.addClass(jcl);
      }
      for(int i=0; i<args.length; i++)
      {
        ClassParser cp = new ClassParser(args[i]);
        JavaClass jcl = cp.parse();
        checkme(jcl.getClassName());
      }
    }
    catch(IOException e)
    {
    }
  }

}
