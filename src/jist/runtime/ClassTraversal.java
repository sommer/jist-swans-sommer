//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <ClassTraversal.java Tue 2004/04/06 11:22:39 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/** 
 * Facilitates traversal and and modification of a BCEL JavaClass structure.
 * The traversal does not visit all elements of the class, only the ones that
 * are important for JiST. Specifically, it visits (with upcalls) the class,
 * fields, method and each instruction.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: ClassTraversal.java,v 1.12 2004-10-22 04:37:21 barr Exp $
 * @since JIST1.0
 */

public class ClassTraversal
{

  //////////////////////////////////////////////////
  // locals
  //

  /**
   * Traversal upcall instance.
   */
  private Visitor jcti;

  //////////////////////////////////////////////////
  // public methods
  //

  /**
   * Create a new traversal object and initialize it with given traversal
   * upcall.
   *
   * @param jcti object to receive upcalls during traversal
   */
  public ClassTraversal(Visitor jcti)
  {
    this.jcti = jcti;
  }

  /**
   * Accept a class for traversal and processing, and return the resulting
   * (possibly modified) class.
   *
   * @param jc BCEL JavaClass structure to traverse and process
   * @return resulting BCEL JavaClass structure (possibly modified)
   */
  public JavaClass processClass(JavaClass jc) throws ClassNotFoundException
  {
    ClassGen cg = new ClassGen(jc);
    cg = processClassGen(cg);
    return cg.getJavaClass();
  }

  //////////////////////////////////////////////////
  // recursion
  //

  /**
   * Traverse and process a class. Upcalls are made before and after traversal
   * for class-level processing. Traversal involves recursive descent to each
   * method and field of the class.
   *
   * @param cg BCEL class generator object
   * @return processed BCEL class generator object
   */
  protected ClassGen processClassGen(ClassGen cg) throws ClassNotFoundException
  {
    Field[] fields = cg.getFields();
    Method[] methods = cg.getMethods();
    // pre call
    cg = jcti.doClass(cg);
    // fields
    for(int i=0; i<fields.length; i++)
    {
      FieldGen fg = new FieldGen(fields[i], cg.getConstantPool());
      fg = processFieldGen(cg, fg);
      cg.replaceField(fields[i], fg.getField());
    }
    // methods
    for(int i=0; i<methods.length; i++)
    {
      MethodGen mg = new MethodGen(methods[i], cg.getClassName(), cg.getConstantPool());
      mg = processMethodGen(cg, mg);
      mg.setMaxStack();
      mg.setMaxLocals();
      cg.replaceMethod(methods[i], mg.getMethod());
    }
    // post call
    cg = jcti.doClassPost(cg);
    return cg;
  } // function: processClassGen

  /**
   * Traverse and process a field. A single upcall is made for field-level
   * processing.
   *
   * @param cg BCEL class generator object
   * @param fg BCEL field generator object
   * @return processed BCEL field generator object
   */
  protected FieldGen processFieldGen(ClassGen cg, FieldGen fg) throws ClassNotFoundException
  {
    fg = jcti.doField(cg, fg);
    return fg;
  }

  /**
   * Traverse and process a method. Upcalls are made both before and after
   * traversal for method-level processing. Traversal involves recursive
   * descent to each individual instruction.
   *
   * upcalls for each
   * individual instruction, for instruction-level processing.
   *
   * @param cg BCEL class generator object
   * @param mg BCEL method generator object
   * @return processed BCEL method generator object
   */
  protected MethodGen processMethodGen(ClassGen cg, MethodGen mg) throws ClassNotFoundException
  {
    InstructionList il = mg.getInstructionList();
    // pre call
    mg = jcti.doMethod(cg, mg);
    // instructions
    if(il!=null)
    {
      InstructionHandle[] ihs = il.getInstructionHandles();
      for(int i=0; i<ihs.length; i++)
      {
        processInstruction(cg, mg, ihs, i);
      }
      mg.setInstructionList(il);
    }
    // post call
    mg = jcti.doMethodPost(cg, mg);
    return mg;
  } // function: processMethodGen

  /**
   * Traverse and process an instruction. A single upcall is made for instruction-level
   * processing.
   *
   * @param cg BCEL class generator object
   * @param mg BCEL field generator object
   * @param ihs array of BCEL instruction handles
   * @param i instruction handle index
   */
  protected void processInstruction(ClassGen cg, MethodGen mg, InstructionHandle[] ihs, int i) throws ClassNotFoundException
  {
    jcti.doInstruction(cg, mg, ihs[i], ihs[i].getInstruction());
  }

  //////////////////////////////////////////////////
  // Visitor interface
  //

  /** 
   * ClassTraversal upcall interface. Specifies upcall methods for processing a
   * class, its fields and methods, and the individual method instructions.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */

  public static interface Visitor
  {

    /**
     * Upcall to process class <b>before</b> field and method processing.
     *
     * @param cg BCEL class generator object to process
     * @return processed BCEL class generator object
     */
    ClassGen doClass(ClassGen cg) throws ClassNotFoundException;

    /**
     * Upcall to process class <b>after</b> field and method processing.
     *
     * @param cg BCEL class generator object to process
     * @return processed BCEL class generator object
     */
    ClassGen doClassPost(ClassGen cg) throws ClassNotFoundException;

    /**
     * Upcall to process field.
     *
     * @param cg BCEL class generator object
     * @param fg BCEL field generator object to process
     * @return processed BCEL field generator object
     */
    FieldGen doField(ClassGen cg, FieldGen fg) throws ClassNotFoundException;

    /**
     * Upcall to process method <b>before</b> instruction processing.
     *
     * @param cg BCEL class generator object
     * @param mg BCEL method generator object to process
     * @return processed BCEL method generator object
     */
    MethodGen doMethod(ClassGen cg, MethodGen mg) throws ClassNotFoundException;

    /**
     * Upcall to process method <b>after</b> instruction processing.
     *
     * @param cg BCEL field generator object
     * @param mg BCEL method generator object to process
     * @return processed BCEL method generator object
     */
    MethodGen doMethodPost(ClassGen cg, MethodGen mg) throws ClassNotFoundException;

    /**
     * Upcall to process instruction. Note that instructions can be inserted and
     * deleted directly using the BCEL API, and therefore this method returns
     * <code>void</code>.
     *
     * @param cg BCEL class generator object
     * @param mg BCEL method generator object
     * @param ih BCEL instruction handle object to process
     * @param i BCEL instruction object to process
     */
    void doInstruction(ClassGen cg, MethodGen mg, InstructionHandle ih, Instruction i) throws ClassNotFoundException;

  } // interface: Visitor


  //////////////////////////////////////////////////
  // Empty Visitor implementation
  //

  /** 
   * Provides a default (empty) implementation of the
   * <code>Visitor</code>.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since JIST1.0
   */

  public static class Empty implements Visitor
  {
    //////////////////////////////////////////////////
    // Visitor interface
    //

    /** {@inheritDoc} */
    public ClassGen doClass(ClassGen cg) throws ClassNotFoundException
    {
      return cg;
    }

    /** {@inheritDoc} */
    public ClassGen doClassPost(ClassGen cg) throws ClassNotFoundException
    {
      return cg;
    }

    /** {@inheritDoc} */
    public FieldGen doField(ClassGen cg, FieldGen fg) throws ClassNotFoundException
    {
      return fg;
    }

    /** {@inheritDoc} */
    public MethodGen doMethod(ClassGen cg, MethodGen mg) throws ClassNotFoundException
    {
      return mg;
    }

    /** {@inheritDoc} */
    public MethodGen doMethodPost(ClassGen cg, MethodGen mg) throws ClassNotFoundException
    {
      return mg;
    }

    /** {@inheritDoc} */
    public void doInstruction(ClassGen cg, MethodGen mg, InstructionHandle ih, Instruction i) throws ClassNotFoundException
    {
    }

  } // class: Empty

} // class: ClassTraversal

