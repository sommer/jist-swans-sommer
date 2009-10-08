//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RewriterFlow.java Tue 2004/04/06 11:25:02 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.verifier.structurals.*;
import org.apache.bcel.verifier.exc.*;
import org.apache.log4j.*;
import java.io.*;
import java.util.*;

/** 
 * Perform data flow analysis.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: RewriterFlow.java,v 1.22 2005-01-25 16:33:47 barr Exp $
 * @since JIST1.0
 */

public final class RewriterFlow
{

  /**
   * An accessor class to conveniently query data flow information.
   */
  public static class FlowInfoMap
  {
    /** hashmap: instructionhandle to frames. */
    private HashMap frames;

    /** hashmap: instructionhandle to pointsTo instruction handles. */
    private HashMap pointsTo;

    /**
     * Initialize flow information with given hashmap.
     *
     * @param frames hashmap of frame at beginning of each instruction
     * @param pointsTo hashmap of instructions that pointTo each instruction
     */
    private FlowInfoMap(HashMap frames, HashMap pointsTo)
    {
      this.frames = frames;
    }

    /**
     * Return frame information BEFORE given instruction handle.
     *
     * @param ih BCEL instruction handle (program counter)
     * @return frame BEFORE instruction executes
     */
    public Frame getFrame(InstructionHandle ih)
    {
      return (Frame)frames.get(ih);
    }

  } // class: FlowInfoMap


  /**
   * Utility class to replace BCEL's ReturnaddressType and ignore the return
   * address when checking for type equality for stack merging purposes.
   */
  private static class WildcardReturnaddressType extends ReturnaddressType
  {
    /** singleton WildcardReturnaddressType */
    public static final WildcardReturnaddressType ANY_TARGET = new WildcardReturnaddressType();

    /**
     * Default, private constructor.
     */
    private WildcardReturnaddressType()
    {
      super(null);
    }

    /**
     * Any WildcardReturnaddressType is equal to any other.
     */
    public boolean equals(Object o)
    {
      if(!(o instanceof ReturnaddressType)) return false;
      return true;
    }	

    /** {@inheritDoc} */
    public String toString()
    {
      return "<WildcardReturnaddress>";
    }
  }

  //////////////////////////////////////////////////
  // locals
  //

  /** remaining instructions to process. */
  private Vector remaining;

  /** instruction execution visitor. */
  private ExecutionVisitor ev;

  /** exception handler lookup. */
  private ExceptionHandlers exInfo;

  /** frame information. */
  private HashMap frames;

  /** pointsTo information. */
  private HashMap pointsTo;

  //////////////////////////////////////////////////
  // initialization
  //

  /**
   * Create new rewriter data flow analysis object.
   */
  public RewriterFlow()
  {
    this.ev = new ExecutionVisitor();
  }

  //////////////////////////////////////////////////
  // flow analysis
  //

  /**
   * Perform data flow analysis to determine the frame
   * at each execution point in the method.
   *
   * @param cg BCEL class object
   * @param mg BCEL method object
   * @return flow information
   */
  public FlowInfoMap doFlow(ClassGen cg, MethodGen mg)
  {
    if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("performing data-flow analysis of: "+cg.getClassName()+"."+mg.getName());
    if(mg.isNative()) throw new RuntimeException("cannot perform flow analysis of native method");
    // initialize
    ev.setConstantPoolGen(cg.getConstantPool());
    ConstantPool cp = cg.getConstantPool().getConstantPool();
    frames = new HashMap();
    pointsTo = new HashMap();
    remaining = new Vector();
    exInfo = new ExceptionHandlers(mg);
    Frame f = null;
    // build initial frame
    f = new Frame(mg.getMaxLocals(), mg.getMaxStack());
    int localsOffset = 0;
    if (!mg.isStatic())
    {
      f.getLocals().set(0, mg.getName().equals(Constants.CONSTRUCTOR_NAME)
          ? (Type)new UninitializedObjectType(new ObjectType(mg.getClassName()))
          : (Type)new ObjectType(mg.getClassName()));
      localsOffset++;
    }
    Type[] args = mg.getArgumentTypes();
    for(int j=0; j<args.length; j++)
    {
      Type arg = args[j];
      if(arg.equals(Type.BOOLEAN)) arg=Type.INT;
      if(arg.equals(Type.BYTE)) arg=Type.INT;
      f.getLocals().set(localsOffset++, arg);
      if(arg.getSize()==2)
      {
        f.getLocals().set(localsOffset++, Type.UNKNOWN);
      }
    }
    InstructionHandle start = mg.getInstructionList().getStart();
    frames.put(start, f);
    remaining.add(start);
    // iterate to fixed point
    while(!remaining.isEmpty())
    {
      InstructionHandle ih = (InstructionHandle)remaining.remove(0);
      f = (Frame)frames.get(ih);
      Frame f2 = f.getClone();
      InstructionHandle[] next = execute(ih, f2);
      updatePointsTo(ih, next);
      // regular flow
      for(int i=0; i<next.length; i++)
      {
        if(merge(next[i], f2, cp) && !remaining.contains(next[i]))
        {
          remaining.addElement(next[i]);
        }
      }
      // exception flow
      ExceptionHandler[] ex = exInfo.getExceptionHandlers(ih);
      for(int i=0; i<ex.length; i++)
      {
        f2 = f.getClone();
        OperandStack os = f2.getStack();
        while(!os.isEmpty()) os.pop();
        if(ex[i].getExceptionType()==null)
        {
          os.push(new ObjectType(Throwable.class.getName()));
        }
        else
        {
          os.push(ex[i].getExceptionType());
        }
        InstructionHandle ihHandler = ex[i].getHandlerStart();
        if(merge(ihHandler, f2, cp) && !remaining.contains(ihHandler))
        {
          remaining.addElement(ihHandler);
        }
      }
    }
    return new FlowInfoMap(frames, pointsTo);
  }

  /**
   * Helper flow analysis method: "executes" a given instruction to produce the
   * next frame, and returns all successors of this instruction.
   *
   * @param ih handle of instruction to execute
   * @param f pre-instruction state (is modified to post-instruction state)
   * @return list of instruction successors
   */
  private InstructionHandle[] execute(InstructionHandle ih, Frame f)
  {
    Instruction inst = ih.getInstruction();
    // execute instruction
    ev.setFrame(f);
    inst.accept(ev);
    // return control flow points
    if (inst instanceof ReturnInstruction)
    {
      return new InstructionHandle[] { };
    }
    else if (inst instanceof ATHROW)
    {
      return new InstructionHandle[] { };
    }
    else if (inst instanceof GotoInstruction)
    {
      return new InstructionHandle[] {
        ((GotoInstruction)inst).getTarget(),
      };
    }
    else if (inst instanceof BranchInstruction)
    {
      if (inst instanceof Select)
      {
        InstructionHandle[] matchTargets = ((Select)inst).getTargets();
        InstructionHandle[] ret = new InstructionHandle[matchTargets.length+1];
        ret[0] = ((Select)inst).getTarget();
        System.arraycopy(matchTargets, 0, ret, 1, matchTargets.length);
        return ret;
      }
      else
      {
        return new InstructionHandle[] 
        {
          ih.getNext(),
          ((BranchInstruction)inst).getTarget(),
        };
      }
    }
    else if (inst instanceof JsrInstruction)
    {
      return new InstructionHandle[] 
      {
        ((JsrInstruction)inst).getTarget(),
      };
    }
    else if (inst instanceof RET)
    {
      // rimnode: are there other flow targets?
      return new InstructionHandle[] { };
    }
    else
    {
      return new InstructionHandle[] 
      {
        ih.getNext(), // fall-through
      };
    }
  }

  /**
   * Helper flow analysis method: merges two frames ala the VM spec, 
   * and stores result.
   *
   * @param ih location to merge information into
   * @param f frame to merge in
   * @param cp class constant pool
   * @return whether merging actually changed anything
   */
  private boolean merge(InstructionHandle ih, Frame f, ConstantPool cp)
  {
    try
    {
      f = new Frame(f.getLocals().getClone(), removeRetTargets(f.getStack().getClone()));
      Frame old = (Frame)frames.get(ih);
      if(old==null)
      {
        frames.put(ih, f);
        return true;
      }
      else
      {
        LocalVariables newVars = old.getLocals(), oldVars = newVars.getClone();
        newVars.merge(f.getLocals());
        OperandStack newStack = old.getStack(), oldStack = newStack.getClone();
        newStack.merge(f.getStack());
        return !oldVars.equals(newVars) || !oldStack.equals(newStack);
      }
    }
    catch(StructuralCodeConstraintException e)
    {
      System.err.println("merging stacks for instruction: \n  "+ih.toString(true)+"\n  "+ih.getInstruction().toString(cp));
      throw e;
    }
  }

  /**
   * Replace any ReturnaddressTypes (placed on the stack by JSR)
   * with WildcardReturnaddressTypes, so as to pass the verification
   * performed during stack merge.
   *
   * @param os input operand stack, not modified
   * @return modified operand stack
   */
  private OperandStack removeRetTargets(OperandStack os)
  {
    os = os.getClone();
    OperandStack noRETs = new OperandStack(os.maxStack());
    while(!os.isEmpty())
    {
      Type t = os.pop();
      if(t instanceof ReturnaddressType)
      {
        t = WildcardReturnaddressType.ANY_TARGET;
      }
      noRETs.push(t);
    }
    return reverseStack(noRETs);
  }

  /**
   * Reverse the operand stack.
   *
   * @param os input operand stack, not modified
   * @return reversed operand stack
   */
  private OperandStack reverseStack(OperandStack os)
  {
    os = os.getClone();
    OperandStack rev = new OperandStack(os.maxStack());
    while(!os.isEmpty()) rev.push(os.pop());
    return rev;
  }

  /**
   * Update table of which instructions "point to" which.
   *
   * @param from predecessor instruction
   * @param to list of successor instructions
   */
  private void updatePointsTo(InstructionHandle from, InstructionHandle[] to)
  {
    for(int i=0; i<to.length; i++)
    {
      HashSet s = (HashSet)pointsTo.get(to);
      if(s==null)
      {
        s = new HashSet();
        pointsTo.put(to, s);
      }
      s.add(from);
    }
  }

  /**
   * Small utility program to dump the methods of a given class file
   * with flow information between every instruction.
   *
   * @param args list of filenames to process
   */
  public static void main(String[] args)
  {
    RewriterFlow flow = new RewriterFlow();
    try
    {
      for(int i=0; i<args.length; i++)
      {
        JavaClass jcl = (new ClassParser(args[i])).parse();
        ClassGen cg = new ClassGen(jcl);
        System.out.println("***** CLASS: "+cg.getClassName());
        Method[] methods = cg.getMethods();
        for(int j=0; j<methods.length; j++)
        {
          try
          {
            MethodGen mg = new MethodGen(methods[j], cg.getClassName(), cg.getConstantPool());
            System.out.println("***** METHOD: "+mg.getName()+mg.getSignature()+" *****");
            if(mg.isNative()) continue;
            FlowInfoMap flowinfo = flow.doFlow(cg, mg);
            InstructionHandle ih = mg.getInstructionList().getStart();
            while(ih!=null)
            {
              System.out.println(flowinfo.getFrame(ih));
              System.out.print("instruction "+ih.getPosition()+": ");
              System.out.println(ih.getInstruction().toString(cg.getConstantPool().getConstantPool()));
              System.out.println();
              ih = ih.getNext();
            }
          }
          catch(Exception e)
          {
            System.out.println("Error computing flow of: "+methods[j]);
            e.printStackTrace();
          }
        }
      }
    }
    catch(IOException e)
    {
      System.out.println(e);
    }
  } // method: main

} // class: RewriterFlow

