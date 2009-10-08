//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Rewriter.java Sun 2005/03/13 11:09:51 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import jist.runtime.JistAPI;

/**
 * Add-on SWANS-specific JiST rewriter module.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Rewriter.java,v 1.19 2005-03-13 16:11:54 barr Exp $
 * @since SWANS1.0
 */

public class Rewriter implements JistAPI.CustomRewriter
{

  /**
   * Class beginning with these strings are ignored. The JiST ignored
   * classes do not even arrive at this filter.
   */
  private static final String[] ignoredPackages = new String[]
  {
    "jist.swans.",
  };

  protected ModifyTypeInfo[] typeModifications;

  public Rewriter()
  {
    typeModifications = new ModifyTypeInfo[]
    {
      // sockets
      new ModifyTypeInfo("java.net.DatagramSocket", "jist.swans.app.net.UdpSocket", true),
      new ModifyTypeInfo("java.net.Socket", "jist.swans.app.net.Socket", true),
      new ModifyTypeInfo("java.net.ServerSocket", "jist.swans.app.net.ServerSocket", true),
      // streams
      new ModifyTypeInfo("java.io.InputStream", "jist.swans.app.io.InputStream", false),
      new ModifyTypeInfo("java.io.OutputStream", "jist.swans.app.io.OutputStream", false),
      new ModifyTypeInfo("java.io.FilterInputStream", "jist.swans.app.io.FilterInputStream", false),
      new ModifyTypeInfo("java.io.BufferedInputStream", "jist.swans.app.io.BufferedInputStream", false),
      // readers
      new ModifyTypeInfo("java.io.BufferedReader", "jist.swans.app.io.BufferedReader", false),
      new ModifyTypeInfo("java.io.BufferedWriter", "jist.swans.app.io.BufferedWriter", false),
      // threading
      new ModifyTypeInfo("java.lang.Thread", "jist.swans.app.lang.SimtimeThread", false),
    };
  }

  /** {@inheritDoc} */
  public JavaClass process(JavaClass jcl) throws ClassNotFoundException
  {
    if(isIgnored(jcl.getClassName())) return jcl;
    jcl = (new jist.runtime.ClassTraversal(new ModifyTypeTraversal(typeModifications))).processClass(jcl);
    return jcl;
  }

  /**
   * Whether class is ignored by SWANS rewriter.
   *
   * @param classname qualified name of class to check
   * @return whether class is ignored by SWANS rewriter
   */
  public static boolean isIgnored(String classname)
  {
    if(jist.runtime.Rewriter.isIgnoredStatic(classname)) return true;
    for(int i=0; i<ignoredPackages.length; i++)
    {
      if(classname.indexOf('.')!=-1 && classname.startsWith(ignoredPackages[i]))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Used by ModifyTypeTraversal to guide modification.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  private static class ModifyTypeInfo
  {
    public ModifyTypeInfo(String oldName, String newName, boolean addJistPostInit)
    {
      this.oldName = oldName;
      this.oldType = new ObjectType(oldName);
      this.newName = newName;
      this.newType = new ObjectType(newName);
      this.addJistPostInit = addJistPostInit;
    }
    /** name of type to replace. */
    public String oldName;
    /** type to replace. */
    public Type oldType;
    /** name of replacement type. */
    public String newName;
    /** replacement type. */
    public Type newType;
    /** whether to add a postinit call after constructor. */
    public boolean addJistPostInit;
  }

  /**
   * Class traversal that substitutes types in all the common places.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  public static class ModifyTypeTraversal extends jist.runtime.ClassTraversal.Empty
  {

    /** class instruction factory. */
    private InstructionFactory ifc;
    /** type modification info. */
    private ModifyTypeInfo[] modifications;

    /**
     * Create a type-modifying traversal object.
     *
     * @param oldName name of old type
     * @param newName name of new type
     * @param addJistPostInit whether to add a _jistPostInit call 
     *   after an initializer call is modified.
     */
    public ModifyTypeTraversal(ModifyTypeInfo[] modifications)
    {
      this.modifications = modifications;
    }

    /**
     * Replace BCEL type.
     *
     * @param t type to replace
     * @return replaced type
     */
    private Type transformType(Type t)
    {
      for(int i=0; i<modifications.length; i++)
      {
        if(modifications[i].oldType.equals(t)) return modifications[i].newType;
      }
      return t;
    }

    private boolean shouldTransformType(Type t)
    {
      for(int i=0; i<modifications.length; i++)
      {
        if(modifications[i].oldType.equals(t)) return true;
      }
      return false;
    }

    /**
     * Replace BCEL type array.
     *
     * @param t type array to substitute
     * @return array with types replaced
     */
    private Type[] transformTypes(Type[] t)
    {
      for(int i=0; i<t.length; i++)
      {
        t[i] = transformType(t[i]);
      }
      return t;
    }

    private boolean shouldTransformTypes(Type[] t)
    {
      for(int i=0; i<t.length; i++)
      {
        if(shouldTransformType(t[i])) return true;
      }
      return false;
    }

    /**
     * Replace type by string.
     *
     * @param n type string to substitute
     * @return replaced type string
     */
    private String transformName(String n)
    {
      for(int i=0; i<modifications.length; i++)
      {
        if(modifications[i].oldName.equals(n)) return modifications[i].newName;
      }
      return n;
    }

    private boolean shouldTransformName(String n)
    {
      for(int i=0; i<modifications.length; i++)
      {
        if(modifications[i].oldName.equals(n)) return true;
      }
      return false;
    }

    private boolean isIgnored(String classname)
    {
      for(int i=0; i<modifications.length; i++)
      {
        if(modifications[i].oldName.equals(classname)) return false;
        if(modifications[i].newName.equals(classname)) return false;
      }
      return Rewriter.isIgnored(classname);
    }

    private boolean shouldAddJistPostInit(String oldName)
    {
      for(int i=0; i<modifications.length; i++)
      {
        if(modifications[i].oldName.equals(oldName)) return modifications[i].addJistPostInit;
      }
      return false;
    }

    /** {@inheritDoc} */
    public ClassGen doClass(ClassGen cg)
    {
      ifc = new InstructionFactory(cg.getConstantPool());
      return cg;
    }

    /** {@inheritDoc} */
    public FieldGen doField(ClassGen cg, FieldGen fg)
    {
      if(shouldTransformType(fg.getType())) fg.setType(transformType(fg.getType()));
      return fg;
    }

    /** {@inheritDoc} */
    public MethodGen doMethod(ClassGen cg, MethodGen mg)
    {
      if(shouldTransformTypes(mg.getArgumentTypes())) mg.setArgumentTypes(transformTypes(mg.getArgumentTypes()));
      if(shouldTransformType(mg.getReturnType())) mg.setReturnType(transformType(mg.getReturnType()));
      return mg;
    }

    /** {@inheritDoc} */
    public void doInstruction(ClassGen cg, MethodGen mg, InstructionHandle ih, Instruction inst)
    {
      ConstantPoolGen cpg = cg.getConstantPool();
      // modify field access
      if(inst instanceof FieldInstruction)
      {
        FieldInstruction fi = (FieldInstruction)inst;
        if(!isIgnored(fi.getClassName(cpg)) &&
            (shouldTransformName(fi.getClassName(cpg)) 
             || shouldTransformType(fi.getType(cpg))))
        {
          short itype = 
            inst instanceof GETFIELD ? org.apache.bcel.Constants.GETFIELD :
            inst instanceof PUTFIELD ? org.apache.bcel.Constants.PUTFIELD :
            inst instanceof GETSTATIC ? org.apache.bcel.Constants.GETSTATIC :
            inst instanceof PUTSTATIC ? org.apache.bcel.Constants.PUTSTATIC :
            -1;
          ih.swapInstruction(ifc.createFieldAccess(
                transformName(fi.getClassName(cpg)),
                fi.getName(cpg),
                transformType(fi.getType(cpg)), itype));
        }
      }
      // modify invocations
      if(inst instanceof InvokeInstruction)
      {
        InvokeInstruction ii = (InvokeInstruction)inst;
        if(!isIgnored(ii.getClassName(cpg)) &&
            (shouldTransformName(ii.getClassName(cpg))
             || shouldTransformType(ii.getReturnType(cpg))
             || shouldTransformTypes(ii.getArgumentTypes(cpg))))
        {
          short itype =
            inst instanceof INVOKEINTERFACE ? org.apache.bcel.Constants.INVOKEINTERFACE :
            inst instanceof INVOKESPECIAL ? org.apache.bcel.Constants.INVOKESPECIAL :
            inst instanceof INVOKESTATIC ? org.apache.bcel.Constants.INVOKESTATIC :
            inst instanceof INVOKEVIRTUAL ? org.apache.bcel.Constants.INVOKEVIRTUAL :
            -1;
          ih.swapInstruction(ifc.createInvoke(
                transformName(ii.getClassName(cpg)), 
                ii.getMethodName(cpg),
                transformType(ii.getReturnType(cpg)), 
                transformTypes(ii.getArgumentTypes(cpg)), itype));
          if(org.apache.bcel.Constants.CONSTRUCTOR_NAME.equals(ii.getMethodName(cpg))
              && shouldAddJistPostInit(ii.getClassName(cpg)))
          {
            InstructionList il = new InstructionList();
            il.append(InstructionConstants.DUP);
            il.append(ifc.createInvoke(
                  transformName(ii.getClassName(cpg)), "_jistPostInit",
                  Type.VOID, Type.NO_ARGS, org.apache.bcel.Constants.INVOKEVIRTUAL));
            mg.getInstructionList().append(ih, il);
          }
        }
      }
      // modify object creation
      if(inst instanceof NEW)
      {
        NEW ni = (NEW)inst;
        if(shouldTransformType(ni.getType(cpg)))
        {
          ih.swapInstruction(ifc.createNew(
                (ObjectType)transformType(ni.getType(cpg))));
        }
      }
      // modify array creation
      if(inst instanceof ANEWARRAY)
      {
        ANEWARRAY nai = (ANEWARRAY)inst;
        if(shouldTransformType(nai.getType(cpg)))
        {
          ih.swapInstruction(ifc.createNewArray(
                transformType(nai.getType(cpg)), (short)1));
          // todo: what about multi-dimensional arrays
        }
      }
      // todo: what about checkcast?
      // todo: any other typed instructions that should be modified?
    }

  } // class: ModifyTypeTraversal

} // class: Rewriter

