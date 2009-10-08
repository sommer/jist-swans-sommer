//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Rewriter.java Sun 2005/09/25 21:39:20 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.runtime;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.verifier.structurals.*;
import org.apache.log4j.*;
import java.io.*;
import java.util.*;
import java.rmi.server.*;

/** 
 * A custom class loader used by the JIST system to modify standard Java
 * applications at load-time to run under "Simulation Time". This
 * implementation uses the Apache Byte-Code Engineering Library (BCEL).
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Rewriter.java,v 1.118 2005-10-04 11:52:47 barr Exp $
 * @since JIST1.0
 */

public final class Rewriter extends ClassLoader
{

  //////////////////////////////////////////////////
  // Constants
  //

  /**
   * Name of the Java initialization method.
   */
  public static final String JAVA_MAIN_NAME = "main";

  /**
   * Prefix of automatically generated methods.
   */
  public static final String JIST_METHOD_PREFIX = "_jistMethod_";

  /**
   * Prefix of automatically generated fields.
   */
  public static final String JIST_FIELD_PREFIX = "_jistField_";

  /**
   * Prefix of generated method stub fields.
   */
  public static final String JIST_METHODSTUB_PREFIX = "_jistMethodStub_";

  /**
   * Prefix of generated field set accessor method.
   */
  public static final String JIST_METHOD_SET = JIST_METHOD_PREFIX+"Set_";

  /**
   * Prefix of generated field get accessor method.
   */
  public static final String JIST_METHOD_GET = JIST_METHOD_PREFIX+"Get_";

  /**
   * Suffix of the entity self reference field.
   */
  public static final String JIST_REF_SUFFIX = "_ref";

  /**
   * Name of the entity self reference field.
   */
  public static final String JIST_ENTITYREF = JIST_FIELD_PREFIX+JIST_REF_SUFFIX;

  /**
   * Suffix of the field used to trigger static intialization.
   */
  public static final String JIST_STATIC_TRIGGER_SUFFIX = "_staticTrigger";

  /**
   * Name of the field used to trigger static initialization.
   */
  public static final String JIST_STATIC_TRIGGER = JIST_FIELD_PREFIX+JIST_STATIC_TRIGGER_SUFFIX;

  /**
   * Prefix for all classes to capture continuation state.
   */
  public static final String JIST_CONTINUATION_STATE_PREFIX = "_jistcont";

  /**
   * Logger instance.
   */
  public static final Logger log = Logger.getLogger(Rewriter.class.getName());

  /**
   * Class cache prefix.
   */
  public static final String CACHE_PREFIX = "jistRewriterCache-";
  
  /**
   * List of objects that are pre-defined to be timeless.
   */
  public static final String[] timeless =
  {
    Boolean.class.getName(),
    Character.class.getName(),
    Float.class.getName(),
    Double.class.getName(),
    Byte.class.getName(),
    Short.class.getName(),
    Integer.class.getName(),
    Long.class.getName(),
    String.class.getName(),
  };

  /**
   * Hashtable containing the timeless list for rapid lookup.
   */
  private static Hashtable timelessHash;

  static
  {
    timelessHash = new Hashtable();
    for(int i=0; i<timeless.length; i++)
    {
      timelessHash.put(timeless[i], timeless[i]);
    }
  }

  /**
   * List of methods that are pre-defined to be blocking.
   */
  public static java.lang.reflect.Member[] blockingMethod;

  static
  {
    try
    {
      blockingMethod = new java.lang.reflect.Method[]
      {
        // these won't be auto-detected b/c jist.runtime isIgnored()
        Controller.method_entityInvocationCont,
        Channel._jistMethodStub_receive_28_29Ljava_2elang_2eObject_3b,
        Channel._jistMethodStub_send_28Ljava_2elang_2eObject_3b_29V,
        Channel._jistMethodStub_send_28Ljava_2elang_2eObject_3bZZ_29V,
      };
    }
    catch(Exception e)
    {
      throw new JistException("should not happen", e);
    }
  }

  /**
   * List of methods that are pre-defined to be continuable.
   */
  public static java.lang.reflect.Member[] continuableMethod;

  static
  {
    try
    {
      continuableMethod = new java.lang.reflect.Method[]
      {
        // these won't be auto-detected b/c jist.runtime isIgnored()
        JistAPI_Impl.method_sleepBlock,
        JistAPI_Impl.method_sleepBlockAPI,
      };
    }
    catch(Exception e)
    {
      throw new JistException("show not happen", e);
    }
  }

  /**
   * List of methods that are pre-defined to have every method blocking.
   */
  public static final Class[] blockingClass = 
  {
  };

  /**
   * Class beginning with these strings are ignored, (used only when
   * processedPackages is null).
   */
  private static final String[] ignoredPackages = new String[]
  {
    "java.",
    "javax.",
    "sun.",
    "com.sun.",
    "jist.runtime.",
    "org.apache.bcel.",
    "org.apache.log4j.",
    "jargs.gnu.",
    "bsh.",
    "org.python.",
  };

  /**
   * Mutex access to BCEL repository object.
   */
  private static Object repositoryLock = new Object();

  //////////////////////////////////////////////////
  // state
  //

  /**
   * Classes beginning with these strings are processed and all others are
   * ignored; or, if this variable is null, then ignore packages in
   * ignoredPackages and process all others.
   */
  private String[] processedPackages;

  /**
   * Rewrite cache. This is not just for performance...
   * Must have for type-safety.
   */
  private HashMap rewritten;

  /**
   * Class disk-based cache directory.
   */
  private String cacheDir;

  /**
   * Resource locator (possibly remote).
   */
  private RemoteJist.ResourceFinderRemote resources;

  /**
   * (Possibly remote) Jist repository.
   */
  private org.apache.bcel.util.Repository jistRepository;

  /**
   * Time that the rewriter class was compiled.
   */
  private long rewriterTime;

  /**
   * Methods should be rewritten to be Continuable.
   */
  private HashSet continuable;

  /**
   * Methods that are blocking.
   */
  private HashSet blocking;

  /**
   * Installed rewrite traversal visitors.
   */
  private Vector rewriters;

  /**
   * Classpath lookup cache: (string to JavaClass).
   */
  private HashMap lookupCache;

  /**
   * Regular method call graph: (methodsig mapped to hashset(methodsig)).
   */
  private HashMap calledBy;

  //////////////////////////////////////////////////
  // initialization
  //

  /**
   * Method stub of the generic static method initializer method.
   */
  public static java.lang.reflect.Method method_initializeMethodStubs;
  static
  {
    try
    {
      method_initializeMethodStubs = Rewriter.class.getDeclaredMethod(
          "initializeMethodStubs", 
          new Class[] { String.class });
    }
    catch(NoSuchMethodException e)
    {
      throw new JistException("should not happen", e);
    }
  }

  /**
   * Initialize an Rewriter instance, with a list of packages to
   * be processed (others are ignored).
   *
   * @param processedPackages list of packages to process, or null to process
   * all packages, except those in the (default) ignoredPackages list.
   * @param cacheDir directory for rewritten classfile cache
   * @param resources remote resources finder
   * @param serverOut server machine output stream
   */
  public Rewriter(String[] processedPackages, String cacheDir, RemoteJist.ResourceFinderRemote resources, PrintStream serverOut)
  {
    this.processedPackages = processedPackages;
    this.cacheDir = cacheDir;
    this.resources = resources;
    synchronized(repositoryLock)
    {
      this.jistRepository = new RemoteJist.RemoteRepository(resources, serverOut);
    }
    this.lookupCache = new HashMap();
    this.calledBy = new HashMap();
    this.rewriterTime = Repository.lookupClassFile(Rewriter.class.getName()).getTime();
    this.rewritten = new HashMap();
    this.rewriters = new Vector();
    this.continuable = new HashSet();
    this.blocking = new HashSet();
    for(int i=0; i<blockingClass.length; i++)
    {
      java.lang.reflect.Method[] methods = blockingClass[i].getDeclaredMethods();
      for(int j=0; j<methods.length; j++)
      {
        blocking.add(getSignature(methods[j]).intern());
        if(log.isDebugEnabled())
        {
          log.debug("Found blocking method (blockingClass): "+getSignature(methods[j]));
        }
      }
    }
    for(int i=0; i<blockingMethod.length; i++)
    {
      blocking.add(getSignature(blockingMethod[i]).intern());
      if(log.isDebugEnabled())
      {
        log.debug("Found blocking method (blockingMethod): "+getSignature(blockingMethod[i]));
      }
    }
    for(int i=0; i<continuableMethod.length; i++)
    {
      addContinuable(getSignature(continuableMethod[i]));
    }
  }

  //////////////////////////////////////////////////
  // class loader functions
  //

  /**
   * Find and load class. This method is invoked 
   * by the VM.
   * 
   * @param name qualified class name (e.g. java.lang.String)
   * @param resolve not relevant
   * @throws ClassNotFoundException thrown if class can not be found
   * @throws VerifyError throw if given class does not satify various
   *   JIST programming constraints.
   * @return requested class object
   */
  public Class loadClass(String name, boolean resolve)
    throws ClassNotFoundException, VerifyError
  {
    if(log.isDebugEnabled()) log.debug("** loading class "+name);
    // check for (and delegate) ignored packages
    return isIgnored(name) 
      ? super.loadClass(name, resolve)
      : this.findClass(name);
  }

  /**
   * Called by loadClass and actually does all the rewriting work,
   * but checks in caches first.
   *
   * @param name qualified class name (e.g. java.lang.String)
   * @throws ClassNotFoundException thrown if class can not be found
   * @throws VerifyError throw if given class does not satify various
   *   JIST programming constraints.
   * @return requested class object
   */
  protected Class findClass(String name)
    throws ClassNotFoundException, VerifyError
  {
    log.debug("** LOADING CLASS: "+name);
    // first look in the rewrite memory cache
    Class cl = (Class)rewritten.get(name);
    if (cl!=null) return cl;

    // then check the disk-based cache
    if(Main.REWRITE_CACHE && cacheDir!=null) cl = getDiskRewrittenClass(name);
    if (cl==null) 
    {
      synchronized(repositoryLock)
      {
        // initialize bcel repository, just in case it is accessed internally during rewriting
        org.apache.bcel.util.Repository oldRepository = Repository.getRepository();
        Repository.setRepository(jistRepository);
        // try to find requested class (unmodified)
        JavaClass jcl = lookupJavaClass(name);
        // collect statistics (pre-rewrite)
        int preTotalSize = 0, preConstSize = 0;
        if(log.isDebugEnabled())
        {
          preTotalSize = jcl.getBytes().length;
          preConstSize = getConstantPoolSize(jcl);
        }

        // rewrite
        if(!isDoNotRewrite(jcl))
        {
          jcl = rewriteClass(jcl);

          // emit statistics (post-rewrite)
          if(log.isDebugEnabled())
          {
            // processed using script: bin/processClassSizeOuput.py
            String sizeinfo = "** REWRITING_SIZE_STATS: {";
            sizeinfo += "'name': '"+name+"', ";
            sizeinfo += "'preTotal': "+preTotalSize+", ";
            sizeinfo += "'postTotal': "+jcl.getBytes().length+", ";
            sizeinfo += "'preConst': "+preConstSize+", ";
            sizeinfo += "'postConst': "+getConstantPoolSize(jcl)+", ";
            sizeinfo += "'type': ";
            if(isEntity(jcl))
              sizeinfo += 1;
            else if(isTimeless(jcl))
              sizeinfo += 2;
            else
              sizeinfo += 0;
            sizeinfo += ", ";
            sizeinfo += "}";
            log.debug(sizeinfo);
          }
        }
        else
        {
          // skip rewritting
          if(log.isInfoEnabled())
          {
            log.info("** Rewriting class SKIPPED: "+name);
          }
        }
        // load it into the VM
        cl = define(jcl);
        Repository.setRepository(oldRepository);
      }
    }
    else
    {
      rewritten.put(name.intern(), cl);
    }
    return cl;
  }

  /**
   * Search the class path and return requested BCEL JavaClass object. This is
   * the ONLY point through which the Rewriter retrieves new JavaClass objects.
   * Therefore, it is also a convenient point to perform caching and
   * incremental maintenance of the call-graph.
   *
   * @param name classname
   * @throws ClassNotFoundException when class not found
   * @return BCEL JavaClass object
   */
  public JavaClass lookupJavaClass(String name) throws ClassNotFoundException
  {
    // look in cache
    JavaClass jcl = (JavaClass)lookupCache.get(name);
    if(jcl!=null) return jcl;
    // otherwise, look in repository (eventually disk)
    if(log.isDebugEnabled())
    {
      log.debug("retrieving class from repository: "+name);
    }
    jcl = Repository.lookupClass(name);
    if(jcl==null) throw new ClassNotFoundException(name);
    // process installed rewriters
    if(!(isIgnored(name) || isDoNotRewrite(jcl)))
    {
      for(int i=rewriters.size()-1; i>=0; i--)
      {
        JistAPI.CustomRewriter cr = (JistAPI.CustomRewriter)rewriters.elementAt(i);
        jcl = cr.process(jcl);
      }
    }
    // put in cache
    // note: must come before call graph processing to ensure termination
    lookupCache.put(name.intern(), jcl);
    // process super
    JavaClass sup = jcl.getSuperClass();
    if(sup!=null) lookupJavaClass(sup.getClassName());
    // process callgraph
    if(!isIgnored(name))
    {
      if(log.isDebugEnabled()) log.debug("** updating call graph after loading: "+name);
      updateCallGraphAndContinuable(jcl);
    }
    // return javaclass object
    return jcl;
  }

  /**
   * Reset the JavaClass lookup cache.
   */
  public void clearLookupCache()
  {
    Repository.clearCache();
    lookupCache = new HashMap();
  }

  /**
   * Load a given BCEL class into the JVM.
   *
   * @param jcl BCEL Java class
   * @return loaded class object
   */
  public Class define(JavaClass jcl)
  {
    String name = jcl.getClassName();
    if(log.isDebugEnabled()) log.debug("** converting class structure back into bytecode for "+name);
    byte[] b = jcl.getBytes();
    if(Main.REWRITE_CACHE && cacheDir!=null) putDiskRewrittenClass(name, b);
    Class cl = defineClass(name, b, 0, b.length);
    rewritten.put(name, cl);
    return cl;
  }

  /**
   * Performs JiST rewriting of given JavaClass file.
   *
   * @param jcl BCEL JavaClass structure to rewrite
   * @throws VerifyError throw if given class does not satify various
   *   JIST programming constraints.
   * @return rewritten class object
   * @throws ClassNotFoundException failure to find referenced class
   */
  protected JavaClass rewriteClass(JavaClass jcl) throws ClassNotFoundException, VerifyError
  {
    String name = jcl.getClassName();
    log.info("** Rewriting class: "+name);

    // update continuable method list
    computeContinuableFixedPoint();

    // verification
    {
      if(log.isDebugEnabled()) log.debug("** verifying class "+name);
      RewriterTraversalVerifyAll va = new RewriterTraversalVerifyAll(this);
      (new ClassTraversal(va)).processClass(jcl);
      String[] errors = va.getErrors();
      if(errors.length>0)
      {
        for(int i=0; i<errors.length; i++)
        {
          if(log.isDebugEnabled()) log.debug("verification error: "+errors[i]);
        }
        throw new VerifyError(errors[0]);
      }
    }
    if(isEntity(jcl))
    {
      if(log.isDebugEnabled()) log.debug("** verifying entity "+name);
      RewriterTraversalVerifyEntity ve = new RewriterTraversalVerifyEntity(this);
      (new ClassTraversal(ve)).processClass(jcl);
      String[] errors = ve.getErrors();
      if(errors.length>0)
      {
        for(int i=0; i<errors.length; i++)
        {
          if(log.isDebugEnabled()) log.debug("verification error: "+errors[i]);
        }
        throw new VerifyError(errors[0]);
      }
    }

    // REWRITING

    // entity modifications
    if(isEntity(jcl))
    {
      if(log.isDebugEnabled()) log.debug("** adding accessor methods for public entity fields "+name);
      jcl = (new ClassTraversal(new RewriterTraversalAddAccessorMethods())).processClass(jcl);
      if(log.isDebugEnabled()) log.debug("** adding method stubs for entity "+name);
      jcl = (new ClassTraversal(new RewriterTraversalAddEntityMethodStubs(this))).processClass(jcl);
      if(log.isDebugEnabled()) log.debug("** adding entity self reference field and accessors for entity "+name);
      jcl = (new ClassTraversal(new RewriterTraversalAddSelfEntityRef(true))).processClass(jcl);
      if(log.isDebugEnabled()) log.debug("** adding entity interface to entity "+name);
      jcl = (new ClassTraversal(new RewriterTraversalImplementEntityInterface(true))).processClass(jcl);
    }
    // timeless modifications
    if(isTimeless(jcl))
    {
      if(log.isDebugEnabled()) log.debug("** adding timeless interface to timeless object "+name);
      jcl = (new ClassTraversal(new RewriterTraversalImplementTimelessInterface())).processClass(jcl);
    }
    // common modifications
    if(jcl.isClass())
    {
      if(log.isDebugEnabled()) log.debug("** modifying all entity references in class "+name);
      jcl = (new ClassTraversal(new RewriterTraversalModifyEntityReferences(this))).processClass(jcl);
      if(log.isDebugEnabled()) log.debug("** removing field access from class "+name);
      jcl = (new ClassTraversal(new RewriterTraversalRemoveFieldAccess(this))).processClass(jcl);
      if(log.isDebugEnabled()) log.debug("** modifying all entity creation in class "+name);
      jcl = (new ClassTraversal(new RewriterTraversalModifyEntityCreation(this))).processClass(jcl);
      if(log.isDebugEnabled()) log.debug("** modifying all entity invocations in class "+name);
      jcl = (new ClassTraversal(new RewriterTraversalModifyEntityInvocation(this))).processClass(jcl);
      if(log.isDebugEnabled()) log.debug("** replacing all Jist API calls "+name);
      jcl = (new ClassTraversal(new RewriterTraversalTranslateAPICalls())).processClass(jcl);
    }
    // proxy entity modifications
    if(isProxiable(jcl))
    {
      // note: the verifier checks that an entity can not implement the proxiable interface 
      //   (i.e. only non-entities pass through here)
      if(log.isDebugEnabled()) log.debug("** adding entity self reference field and accessors for proxiable object "+name);
      jcl = (new ClassTraversal(new RewriterTraversalAddSelfEntityRef(false))).processClass(jcl);
      if(log.isDebugEnabled()) log.debug("** adding entity interface to proxiable object "+name);
      jcl = (new ClassTraversal(new RewriterTraversalImplementEntityInterface(false))).processClass(jcl);
    }
    // continuable method modifications
    if(log.isDebugEnabled()) log.debug("** modifying continuable methods: "+name);
    jcl = (new ClassTraversal(new RewriterTraversalContinuableMethods(this))).processClass(jcl);
    return jcl;
  }


  //////////////////////////////////////////////////
  // disk rewriter class cache
  //

  /**
   * Read a cached rewritten class file from disk.
   * 
   * @param name class name
   * @return rewritten class, or null if it does not exist/invalid
   */
  protected Class getDiskRewrittenClass(String name)
  {
    try
    {
      File f = new File(cacheDir, CACHE_PREFIX+name);
      long cachetime = f.lastModified();
      // ensure cached class is newer than original
      long cltime = resources.getResourceLastModificationDate(classToFileName(name));
      if(cltime>cachetime) return null;
      // ensure cached class is newer than rewriter(s)
      if(rewriterTime>cachetime) return null;
      for(int i=0; i<rewriters.size(); i++)
      {
        long time = resources.getResourceLastModificationDate(
            classToFileName(rewriters.elementAt(i).getClass().getName()));
        if(time>cachetime) return null;
      }
      // return rewritten bits from disk
      FileInputStream fin = new FileInputStream(f);
      int size = (int)f.length(), read=0;
      byte[] b = new byte[size];
      while(read<size)
      {
        read += fin.read(b, read, size-read);
      }
      if(log.isDebugEnabled()) log.debug("** loading rewritten class from cache: "+name);
      return defineClass(name, b, 0, b.length);
    }
    catch(Exception e)
    {
      if(log.isDebugEnabled()) log.debug("unable to read cache class file: "+e);
      return null;
    }
  }

  /**
   * Write a rewritten class file to the disk cache.
   *
   * @param name class name
   * @param b class bytecode
   */
  protected void putDiskRewrittenClass(String name, byte[] b)
  {
    try
    {
      File f = new File(cacheDir, CACHE_PREFIX+name);
      FileOutputStream fos = new FileOutputStream(f);
      fos.write(b);
      fos.close();
    }
    catch(Exception e)
    {
      log.info("unable to cache rewritten class file: "+e);
    }
  }

  //////////////////////////////////////////////////
  // class analysis functions
  //

  /**
   * Add signature to list of continables. Verify that it is not a constructor,
   * and return whether it is newly discovered.
   *
   * @param sig continuable to add
   * @return whether this is a newly found continuable method
   * @throws VerifyError if signature is of an initialization method
   */
  public boolean addContinuable(String sig) throws VerifyError
  {
    boolean added = continuable.add(sig.intern());
    if(added && log.isDebugEnabled())
    {
      log.debug("Found continuable method: "+sig);
    }
    if(added && sig.indexOf(Constants.CONSTRUCTOR_NAME)!=-1)
    {
      throw new VerifyError("continuable constructor detected: "+sig);
    }
    return added;
  }

  /**
   * Update call-graph with methods of a new class.
   *
   * @param jcl BCEL class object to analyze
   * @throws ClassNotFoundException failure to load class in call graph
   */
  public void updateCallGraphAndContinuable(JavaClass jcl) throws ClassNotFoundException
  {
    ConstantPoolGen cpg = new ConstantPoolGen(jcl.getConstantPool());
    Method[] methods = jcl.getMethods();
    // loop through all methods looking for blocking
    for(int i=0; i<methods.length; i++)
    {
      MethodGen mg = new MethodGen(methods[i], jcl.getClassName(), cpg);
      if(isBlocking(mg))
      {
        boolean added = blocking.add(getSignature(mg).intern());
        if(added && log.isDebugEnabled())
        {
          log.debug("Found blocking method: "+getSignature(mg));
        }
      }
    }
    // loop through all methods looking for continuables
    for(int i=0; i<methods.length; i++)
    {
      MethodGen mg = new MethodGen(methods[i], jcl.getClassName(), cpg);
      // check for explicit continuables
      if(isContinuable(mg))
      {
        addContinuable(getSignature(mg));
      }
      String mySig = getSignature(mg);
      // loop through every instruction
      InstructionList il = mg.getInstructionList();
      if(il==null) continue;
      Instruction[] instructions = il.getInstructions();
      for(int j=0; j<instructions.length; j++)
      {
        Instruction inst = instructions[j];
        // find invocation instructions
        if(inst instanceof InvokeInstruction)
        {
          InvokeInstruction ii = (InvokeInstruction)inst;
          // first we recurse. terminating b/c of lookupCache
          lookupJavaClass(ii.getClassName(cpg));
          // now, classify the call are entity call or regular method call
          if(ii instanceof INVOKEVIRTUAL && isEntity(ii.getClassName(cpg))
              || ii instanceof INVOKEINTERFACE)
          {
            // regular or proxy entity call
            if(isBlocking(ii.getClassName(cpg), ii.getMethodName(cpg),
                  ii.getReturnType(cpg), ii.getArgumentTypes(cpg))) 
            {
              // entity call to blocking method
              addContinuable(getSignature(mg));
            }
          }
          else
          {
            // add to call-graph
            String isig = getSignature(ii, cpg);
            HashSet callers = (HashSet)calledBy.get(isig);
            if(callers==null)
            {
              callers = new HashSet();
              calledBy.put(isig.intern(), callers);
            }
            callers.add(mySig.intern());
          }
        } // if instanceof InvokeInstruction
      } // foreach instruction
    } // foreach method
  } // function: updateCallGraphAndContinuable

  /**
   * Compute continuable fixed-point using call-graph.
   */
  public void computeContinuableFixedPoint()
  {
    boolean changed = true;
    while(changed)
    {
      changed = false;
      Iterator it = ((HashSet)continuable.clone()).iterator();
      while(it.hasNext())
      {
        HashSet callers = (HashSet)calledBy.get(it.next());
        if(callers==null) continue;
        Iterator it2 = callers.iterator();
        while(it2.hasNext())
        {
          changed |= addContinuable((String)it2.next());
        }
      }
    }
  } // function: performContinuableFixedPoint

  /**
   * Return all the classes that are statically, directly referenced by this
   * classes.
   *
   * @param classname name of class to inspect
   * @return array of classes statically referenced directly
   * @throws ClassNotFoundException failure to find referenced class
   */
  public String[] getAllClassReferences(String classname)
    throws ClassNotFoundException
  {
    JavaClass jcl = lookupJavaClass(classname);
    ConstantPool cp = jcl.getConstantPool();
    Constant[] entries = cp.getConstantPool();
    Vector result = new Vector();
    for(int i=0; i<entries.length; i++)
    {
      if(entries[i] instanceof ConstantClass)
      {
        result.add(((ConstantClass)entries[i]).getBytes(cp).replace('/', '.'));
      }
    }
    String[] result2 = new String[result.size()];
    result.copyInto(result2);
    return result2;
  }

  /**
   * Return all the classes that are statically, recursively (directly and
   * indirectly) referenced by this class.
   * 
   * @param classname name of class to inspect
   * @return array of classes statically referenced directly or indirectly
   * @throws ClassNotFoundException failure to find referenced class
   */
  public String[] getAllClassReferencesRecursively(String classname) throws ClassNotFoundException
  {
    HashSet list = new HashSet();
    Stack toProcess = new Stack();
    toProcess.add(classname);
    String[] stringArray = new String[0];
    while(!toProcess.isEmpty())
    {
      String s = (String)toProcess.pop();
      String[] refs = getAllClassReferences(s);
      list.add(s);
      for(int i=0; i<refs.length; i++)
      {
        if(list.contains(refs[i])) continue;
        if(isIgnored(refs[i])) continue;
        toProcess.push(refs[i]);
      }
    }
    return (String[])list.toArray(stringArray);
  }

  /**
   * Return an array of the names of all interfaces implemented by a given class.
   *
   * @param jcl BCEL JavaClass object to inspect
   * @throws ClassNotFoundException interface classes not found
   * @return array of the names of all interfaces implemented.
   */
  public String[] getInterfaceNames(JavaClass jcl) throws ClassNotFoundException
  {
    String[] interfaces = jcl.getInterfaceNames();
    // collect all direct interfaces
    String supercl = jcl.getSuperclassName();
    while(supercl!=null && !supercl.equals(Object.class.getName()))
    {
      jcl = lookupJavaClass(supercl);
      interfaces = Util.union(interfaces, jcl.getInterfaceNames());
      supercl = jcl.getSuperclassName();
    }
    // expand to find all indirect interfaces
    String[] interfaces2 = interfaces;
    do
    {
      interfaces = interfaces2;
      for(int i=0; i<interfaces.length; i++)
      {
        interfaces2 = Util.union(interfaces2, 
            lookupJavaClass(interfaces[i]).getInterfaceNames());
      }
    } while(interfaces2.length!=interfaces.length);
    interfaces = interfaces2;
    return interfaces;
  }

  /**
   * Find class that declares a method, or null otherwise.
   *
   * @param classname classname to initiate search from
   * @param methodname name of method being sought
   * @param methodreturn BCEL return type of method being sought
   * @param methodargs BCEL argument type of method being sought
   * @throws ClassNotFoundException parent class not found
   * @return name of class that declares given method
   */
  public String getDeclaringClass(String classname, String methodname, Type methodreturn, Type[] methodargs) throws ClassNotFoundException
  {
    if(classname==null) return null;
    // check locally
    JavaClass jcl = lookupJavaClass(classname);
    if(containsMethod(jcl, methodname, methodreturn, methodargs))
    {
      return jcl.getClassName();
    }
    // recurse to parent
    String sup = jcl.getSuperclassName();
    if(!classname.equals(sup))
    {
      String result = getDeclaringClass(jcl.getSuperclassName(), methodname, methodreturn, methodargs);
      if(result!=null) return result;
    }
    // recurse through interfaces
    String[] interf = jcl.getInterfaceNames();
    for(int i=0; i<interf.length; i++)
    {
      String result = getDeclaringClass(interf[i], methodname, methodreturn, methodargs);
      if(result!=null) return result;
    }
    return null;
  }

  /**
   * Return whether method contained (declared) within class.
   *
   * @param jcl BCEL JavaClass object to inspect
   * @param methodname method name
   * @param methodreturn method return BCEL type
   * @param methodargs method parameter BCEL types
   * @return whether method is contained within class
   */
  public static boolean containsMethod(JavaClass jcl, String methodname, Type methodreturn, Type[] methodargs)
  {
    String sig = Type.getMethodSignature(methodreturn, methodargs);
    Method[] methods = jcl.getMethods();
    for(int i=0; i<methods.length; i++)
    {
      if(!methodname.equals(methods[i].getName())) continue;
      if(!sig.equals(methods[i].getSignature())) continue;
      return true;
    }
    return false;
  }

  //////////////////////////////////////////////////
  // static rewriter helper functions
  //

  /**
   * Determine whether a class should be rewritten or
   * left alone. If processedPackages is defined, then we 
   * only process packages with those prefixes. Otherwise,
   * any package that does not begin with a prefix in the 
   * <code>ignoredPackages</code> array is rewritten.
   *
   * @param classname given class name
   * @return whether the given class should be rewritten
   */
  public boolean isIgnored(String classname)
  {
    if(processedPackages!=null)
    {
      for(int i=0; i < processedPackages.length; i++) 
      {
        if(processedPackages[i].length()==0)
        {
          // default package
          // note: does not deal with inner classes in default package
          if(classname.indexOf('.')==-1)
          {
            return false;
          }
        }
        else 
        {
          // regular package
          if(classname.startsWith(processedPackages[i]))
          {
            return false;
          }
        }
      }
      return true;
    }
    else
    {
      return isIgnoredStatic(classname);
    }
  }

  /**
   * Determine whether a class should be rewritten or
   * left alone. Any package that does not begin with
   * a prefix in the <code>ignoredPackages</code> array is 
   * rewritten.
   *
   * @param classname given class name
   * @return whether the given class should be rewritten
   */
  public static boolean isIgnoredStatic(String classname)
  {
    for(int i=0; i < ignoredPackages.length; i++)
    {
      if(classname.indexOf('.')!=-1 && classname.startsWith(ignoredPackages[i]))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Determine whether given class is an entity. A class is an entity if
   * and only if it implements the <code>JistAPI.Entity</code> interface.
   * 
   * @see JistAPI.Entity
   * @param c BCEL class object
   * @return whether given class is an entity
   */
  public boolean isEntity(JavaClass c) throws ClassNotFoundException
  {
    if(c==null) return false;
    if(c.isInterface()) return false;
    return Util.contains(getInterfaceNames(c), JistAPI.Entity.class.getName());
  }

  /**
   * Determine whether given class is an entity. A class is an entity if
   * and only if it implements the <code>JistAPI.Entity</code> interface.
   *
   * @see JistAPI.Entity
   * @param name class name
   * @throws ClassNotFoundException class could not be loaded
   * @return whether given class is an entity
   */
  public boolean isEntity(String name) throws ClassNotFoundException
  {
    return isEntity(lookupJavaClass(name));
  }

  /**
   * Determine whether given class is an entity. A class is an entity (at runtime)
   * if and only if it implement the <code>Entity</code> interface.
   *
   * @see JistAPI.Entity
   * @param c runtime class object
   * @return whether given class is an entity
   */
  public static boolean isEntityRuntime(Class c)
  {
    return Util.contains(c.getClasses(), Entity.class);
  }

  /**
   * Determine whether given class is timeless. A class is timeless if
   * and only if it implements the <code>JistAPI.Timeless</code> interface,
   * or the object is immutable, as determined automatically or through
   * membership in the static timeless list.
   * 
   * @see JistAPI.Timeless
   * @param c BCEL class object
   * @return whether given class is an timeless
   */
  public boolean isTimeless(JavaClass c) throws ClassNotFoundException
  {
    return Util.contains(getInterfaceNames(c), JistAPI.Timeless.class.getName())
      || timelessHash.containsKey(c.getClassName());
    // todo: or, if a class is determined to be immutable by static analysis!
  }

  /**
   * Determine whether given class is timeless. A class is timeless if
   * and only if it implements the <code>JistAPI.Timeless</code> interface,
   * or it is determined to be immutable.
   *
   * @see JistAPI.Entity
   * @param name class name
   * @return whether given class is an entity
   */
  public boolean isTimeless(String name) throws ClassNotFoundException
  {
    return isTimeless(lookupJavaClass(name));
  }

  /**
   * Determine whether given type is timeclass. A type is timeless if
   * and only if it is a primitive type or it is a timeless class
   *
   * @param type type
   * @return whether t is a timeless
   */
  public boolean isTimeless(Type type) throws ClassNotFoundException
  {
    if(type instanceof ObjectType)
    {
      return isTimeless(((ObjectType)type).getClassName());
    }
    // todo: arrays allowed to pass as timeless
    // return !(type instanceof ArrayType);
    return true;
  }

  /**
   * Determine whether given class is timeless. A class is timeless (at runtime)
   * if and only if it implements the <code>Timeless</code> interface.
   *
   * @see JistAPI.Timeless
   * @param c runtime class object
   * @return whether given class is an timeless
   */
  public static boolean isTimelessRuntime(Class c)
  {
    return Util.contains(c.getClasses(), Timeless.class);
  }

  /**
   * Determine whether given class is an entity. A class is an entity if
   * and only if it implements the <code>JistAPI.Entity</code> interface.
   * 
   * @see JistAPI.Entity
   * @param c BCEL class object
   * @return whether given class is an entity
   */
  public boolean isProxiable(JavaClass c) throws ClassNotFoundException
  {
    return c.isClass() && !c.isAbstract() &&
      Util.contains(getInterfaceNames(c), JistAPI.Proxiable.class.getName());
  }

  /**
   * Determine whether given class is proxiable. A class is proxiable if
   * and only if it implements the <code>JistAPI.Proxiable</code> interface.
   *
   * @see JistAPI.Entity
   * @param name class name
   * @throws ClassNotFoundException class could not be loaded
   * @return whether given class is an entity
   */
  public boolean isProxiable(String name) throws ClassNotFoundException
  {
    return isProxiable(lookupJavaClass(name));
  }

  /**
   * Whether a given method has blocking semantics (whether the caller needs to
   * call with continuation). A method is blocking iff it declares that it
   * throws a <code>JistAPI.Continuation</code>.
   *
   * @param mg method object
   * @return whether method is blocking
   */
  public static boolean isBlocking(MethodGen mg)
  {
    return Util.contains(mg.getExceptions(), JistAPI.Continuation.class.getName());
  }

  /**
   * Whether a specified method has blocking semantics (whether the caller
   * needs to call with continuation). A method is blocking iff it declares
   * that it throws a <code>JistAPI.Continuation</code>.
   *
   * @param classname class name
   * @param methodname method name
   * @param methodreturn method return type
   * @param methodargs method parameter types
   * @throws ClassNotFoundException could not find parent classes
   * @return whether method is blocking
   */
  public boolean isBlocking(String classname, String methodname, Type methodreturn, Type[] methodargs) throws ClassNotFoundException
  {
    classname = getDeclaringClass(classname, methodname, methodreturn, methodargs);
    if(classname==null) return false;
    return blocking.contains(getSignature(classname, methodname, methodreturn, methodargs));
  }

  /**
   * Whether a given regular entity method has blocking semantics (whether the
   * caller needs to call with continuation). A regular (non-proxy) entity
   * method is blocking, at runtime, iff it declares that it throws a
   * <code>Event.ContinuationFrame</code>.
   *
   * @param m method object
   * @return whether regular entity method is blocking
   */
  public static boolean isBlockingRuntime(java.lang.reflect.Method m)
  {
    return Util.contains(m.getExceptionTypes(), Event.ContinuationFrame.class);
  }

  /**
   * Whether a given proxy entity method has blocking semantics (whether the
   * caller needs to call with continuation). A proxy entity method is
   * blocking, at runtime, iff it declares that it throws a
   * <code>JistAPI.Continuation</code>.
   *
   * @param m method object
   * @return whether proxy entity method is blocking
   */
  public static boolean isBlockingRuntimeProxy(java.lang.reflect.Method m)
  {
    return Util.contains(m.getExceptionTypes(), JistAPI.Continuation.class);
  }

  /**
   * Whether a given method is marked as "continuable". Continuable methods are
   * methods that calls blocking methods (those that declare throws
   * Continuation) or other continuable methods in the same entity.
   *
   * @param mg BCEL MethodGen object
   * @return whether method is "continuable"
   */
  public boolean isContinuable(MethodGen mg)
  {
    return Util.contains(mg.getExceptions(), JistAPI.Continuable.class.getName())
      || continuable.contains(getSignature(mg));
  }

  /**
   * Whether a given invocation target is "continuable". Continuable methods
   * are methods that calls blocking methods (those that declare throws 
   * Continuation) or other continuable methods in the same entity.
   *
   * @param ii invocation instruction
   * @param cpg class constant pool
   * @return whether invocation target is "continuable"
   */
  public boolean isContinuable(InvokeInstruction ii, ConstantPoolGen cpg)
  {
    return continuable.contains(getSignature(ii, cpg));
  }

  /**
   * Determine whether given class should not be rewritten. A class can turn
   * off the rewriter by implementing the <code>JistAPI.DoNotRewrite</code>
   * interface.
   * 
   * @see JistAPI.DoNotRewrite
   * @param c BCEL class object
   * @return whether given class has rewriting disabled
   */
  public boolean isDoNotRewrite(JavaClass c) throws ClassNotFoundException
  {
    if(c==null) return false;
    return Util.contains(getInterfaceNames(c), JistAPI.DoNotRewrite.class.getName());
  }

  /**
   * Modify the name of a class.
   *
   * @param jcl BCEL class object
   * @param name new name for class
   */
  public static void setClassName(JavaClass jcl, String name)
  {
    jcl.setClassName(name);
    ConstantPool cp = jcl.getConstantPool();
    ConstantClass cc = (ConstantClass)cp.getConstant(jcl.getClassNameIndex(), Constants.CONSTANT_Class);
    ConstantUtf8 nm = (ConstantUtf8)cp.getConstant(cc.getNameIndex(), Constants.CONSTANT_Utf8);
    nm.setBytes(name.replace('.', '/'));
  }

  /**
   * Emit the access flags to Rewriter log.
   *
   * @param a access flags to log
   */
  public static void logAccessFlags(AccessFlags a)
  {
    if(log.isDebugEnabled())
    {
      String s = "access flags: ";
      if(a.isAbstract()) s += ("abstract ");
      if(a.isFinal()) s += ("final ");
      if(a.isInterface()) s += ("interface ");
      if(a.isNative()) s += ("native ");
      if(a.isPrivate()) s += ("private ");
      if(a.isProtected()) s += ("protected ");
      if(a.isPublic()) s += ("public ");
      if(a.isStatic()) s += ("static ");
      if(a.isStrictfp()) s += ("strictfp ");
      if(a.isSynchronized()) s += ("synchronized ");
      if(a.isTransient()) s += ("transient ");
      if(a.isVolatile()) s += ("volatile ");
      log.debug(s);
    }
  }

  /**
   * Return the reference type of given type. The reference
   * type of an object type is an EntityRef if the object is
   * an entity. All other types (non-entity objects, arrays
   * and primitives) are their own reference types. Array
   * types are processed recursively.
   * 
   * @param t Java type
   * @return reference type of given type
   * @see EntityRef
   */
  public Type getRefType(Type t) throws ClassNotFoundException
  {
    if(t instanceof ObjectType 
        && isEntity(((ObjectType)t).getClassName()))
    {
      return new ObjectType(EntityRef.class.getName()); 
    }
    if(t instanceof ArrayType)
    {
      ArrayType at = (ArrayType)t;
      return new ArrayType(getRefType(at.getBasicType()), at.getDimensions());
    }
    return t;
  }

  /**
   * Return an array of reference types for a given array of 
   * types. The reference type of an object type is an EntityRef 
   * if the object is an entity. All other types (non-entity 
   * objects, arrays and primitives) are their own reference types. 
   * Array types are processed recursively.
   *
   * @param t array of Java types
   * @return array of reference type of given array
   */
  public Type[] getRefTypes(Type[] t) throws ClassNotFoundException
  {
    for(int i=0; i<t.length; i++)
    {
      t[i] = getRefType(t[i]);
    }
    return t;
  }

  /**
   * Return whether this type is a non-entity. (i.e. whether it needs to be
   * converted into an entity reference)
   *
   * @param t type
   * @return whether t is a non-entity
   */
  public boolean isRefType(Type t) throws ClassNotFoundException
  {
    if(t instanceof ObjectType
        && isEntity(((ObjectType)t).getClassName()))
    {
      return false;
    }
    if(t instanceof ArrayType)
    {
      ArrayType at = (ArrayType)t;
      return isRefType(((ArrayType)t).getBasicType());
    }
    return true;
  }

  /**
   * Return whether all types in array are non-entities. (i.e. whether anything
   * in the array needs to be converted to an entity reference)
   * 
   * @param t array of types
   * @return whether all types in t are non-entities
   */
  public boolean isRefTypes(Type[] t) throws ClassNotFoundException
  {
    for(int i=0; i<t.length; i++)
    {
      if(!isRefType(t[i]))
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Return the BCEL Type object for a corresponding Java 
   * Class object.
   *
   * @param c type given as Java class object
   * @return given type as a BCEL Type object
   */
  public static Type getType(Class c)
  {
    return Type.getType(getSignature(c));
  }

  /**
   * Return a BCEL Type object array corresponding to Java
   * Class object array.
   *
   * @param c type array given as Java class objects
   * @return given type array converted to BCEL Type objects
   */
  public static Type[] getTypes(Class[] c)
  {
    Type[] t = new Type[c.length];
    for(int i=0; i<t.length; i++)
    {
      t[i] = Rewriter.getType(c[i]);
    }
    return t;
  }

  /**
   * Return a Class object for a given primitive Java Type.
   *
   * @param t primitive type given as BCEL Type object
   * @return primitive type as Java class object
   */
  public static Class getPrimitiveObjectType(BasicType t)
  {
    char c = t.getSignature().charAt(0);
    switch(c)
    {
      case 'Z': return Boolean.class;
      case 'C': return Character.class;
      case 'B': return Byte.class;
      case 'S': return Short.class;
      case 'I': return Integer.class;
      case 'J': return Long.class;
      case 'F': return Float.class;
      case 'D': return Double.class;
      default: throw new RuntimeException("unknown primitive type: "+t);
    }
  }

  /**
   * Return name of method used to convert a primitive type wrapper object to a
   * primitive. (eg. doubleValue to convert Double to double.)
   *
   * @param t primitive BCEL type object
   * @return name of method used to convert a primitive type wrapper object to
   *   a primitive type.
   */
  public static String getPrimitiveObjectConversionMethod(BasicType t)
  {
    char c = t.getSignature().charAt(0);
    switch(c)
    {
      case 'Z': return "booleanValue";
      case 'C': return "charValue";
      case 'B': return "byteValue";
      case 'S': return "shortValue";
      case 'I': return "intValue";
      case 'J': return "longValue";
      case 'F': return "floatValue";
      case 'D': return "doubleValue";
      default: throw new RuntimeException("unknown primitive type: "+t);
    }
  }

  /**
   * Return the internal Java signature string of a given class.
   *
   * @param c given class type to convert
   * @return internal Java signature of given class type
   */
  public static String getSignature(Class c)
  {
    if(c.equals(Byte.TYPE))
      return "B";
    else if(c.equals(Character.TYPE))
      return "C";
    else if(c.equals(Double.TYPE))
      return "D";
    else if(c.equals(Float.TYPE))
      return "F";
    else if(c.equals(Integer.TYPE))
      return "I";
    else if(c.equals(Long.TYPE))
      return "J";
    else if(c.equals(Short.TYPE))
      return "S";
    else if(c.equals(Boolean.TYPE))
      return "Z";
    else if(c.equals(Void.TYPE))
      return "V";
    else if(c.isArray())
      return "["+getSignature(c.getComponentType());
    else
      return 'L'+c.getName()+';';
  }

  /**
   * Return the internal Java signature string of a method.
   *
   * @param args method argument types
   * @param ret method return type
   * @return internal Java signature of method with given 
   *   parameter and return types
   */
  public static String getSignature(Class[] args, Class ret)
  {
    String sig = "";
    for(int i=0; i<args.length; i++)
    {
      sig += getSignature(args[i]);
    }
    sig = "("+sig+")";
    if(ret!=null) sig += getSignature(ret);
    return sig;
  }

  /**
   * Return a method signature for given BCEL methodgen object.
   *
   * @param mg BCEL MethodGen object
   * @return Java method signature
   */
  public static String getSignature(MethodGen mg)
  {
    return mg.getClassName()+"."+mg.getName()+
      mg.getSignature().replace('/','.');
  }

  /**
   * Return a method signature from an invocation instruction.
   *
   * @param ii invocation instruction
   * @param cpg class constant pool 
   * @return method signature
   */
  public static String getSignature(InvokeInstruction ii, ConstantPoolGen cpg)
  {
    return getSignature(ii.getClassName(cpg), ii.getMethodName(cpg), 
          ii.getReturnType(cpg), ii.getArgumentTypes(cpg));
  }

  /**
   * Return a method signature for given Java reflection method object.
   *
   * @param m Java reflection method object
   * @return Java method signature
   */
  public static String getSignature(java.lang.reflect.Member m)
  {
    if(m instanceof java.lang.reflect.Method)
    {
      java.lang.reflect.Method mm = (java.lang.reflect.Method)m;
      return mm.getDeclaringClass().getName()+"."+mm.getName()+
        getSignature(mm.getParameterTypes(), mm.getReturnType());
    }
    if(m instanceof java.lang.reflect.Constructor)
    {
      java.lang.reflect.Constructor mm = (java.lang.reflect.Constructor)m;
      return mm.getDeclaringClass().getName()+"."+mm.getName()+
        getSignature(mm.getParameterTypes(), null);
    }
    throw new RuntimeException("invalid member type: "+m);
  }

  /**
   * Return a method signature for the given information.
   * 
   * @param classname class name
   * @param methodname method name
   * @param methodreturn method return BCEL type
   * @param methodargs method argument BCEL types
   * @return method signature string
   */
  public static String getSignature(String classname, String methodname, Type methodreturn, Type[] methodargs)
  {
    return classname+"."+methodname+
      Type.getMethodSignature(methodreturn, methodargs).replace('/','.');
  }

  /**
   * Return the name of the method stub field for a given method.
   *
   * @param mg BCEL method generator object
   * @return name of method stub field for given method
   */
  public String getMethodStubFieldName(MethodGen mg) throws ClassNotFoundException
  {
    return getMethodStubFieldName(mg.getName(), 
        Type.getMethodSignature(mg.getReturnType(), 
          getRefTypes(mg.getArgumentTypes())));
  }

  /**
   * Return the name of the method stub field for a given method.
   *
   * @param m Java method object
   * @return name of method stub field for given method
   */
  public static String getMethodStubFieldName(java.lang.reflect.Method m)
  {
    return getMethodStubFieldName(
        m.getName(), getSignature(m.getParameterTypes(), m.getReturnType()));
  }

  /**
   * Return the name of the method stub field for a method with given
   * name and signature (argument and return types).
   *
   * @param methodName name of method
   * @param sig method signature (internal Java representation of method
   *   arguments and return types)
   * @return name of method stub field for method with given name and signature
   */
  public static String getMethodStubFieldName(String methodName, String sig)
  {
    return JIST_METHODSTUB_PREFIX+Util.escapeJavaIdent(methodName + sig.replace('/', '.'));
  }

  /**
   * Return the name of the class used to store the continuation of a method at a given "program counter" point.
   * The name needs to be a valid class name, to be in the same package as the method's class, to be unique
   * among other (class, method name, method arguments, pc) values.
   *
   * @param mg method being continued
   * @param pc "program counter" point within method
   * @return name of class used to store continuation state
   */
  public static String getContinutationClassName(MethodGen mg, int pc)
  {
    String pkgDot = mg.getClassName();
    int pkgIndex = pkgDot.lastIndexOf('.');
    String cl;
    if(pkgIndex==-1)
    {
      cl = pkgDot;
      pkgDot = "";
    }
    else
    {
      cl = pkgDot.substring(pkgIndex+1);
      pkgDot = pkgDot.substring(0, pkgIndex+1);
    }
    return pkgDot+Rewriter.JIST_CONTINUATION_STATE_PREFIX+"_"
      +cl+"_"+mg.getName()
      +"_"+Integer.toHexString(Util.escapeJavaIdent(mg.getSignature()).hashCode())
      +"_"+pc;
  }

  /**
   * Return the class name of the given Type or array type, 
   * or null for primitive types, or arrays of primitives.
   *
   * @param t BCEL Type object
   * @return class name of type, or null for primitives
   */
  public static String getTypeClassname(Type t)
  {
    if(t instanceof ObjectType)
    {
      return ((ObjectType)t).getClassName();
    }
    else if(t instanceof ArrayType)
    {
      return getTypeClassname(((ArrayType)t).getBasicType());
    }
    else
    {
      return null;
    }
  }

  /**
   * Return the (byte) size of the constant pool.
   *
   * @param jcl BCEL Java class object
   * @return size of constant pool in bytes
   */
  public static int getConstantPoolSize(JavaClass jcl)
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try
    {
      jcl.getConstantPool().dump(new DataOutputStream(baos));
    }
    catch(IOException e) 
    {
    }
    return baos.size();
  }

  //////////////////////////////////////////////////
  // runtime rewriting support functions
  //

  /**
   * Generic method for initializing method stubs of entities at runtime. This
   * method uses reflection to simply initialize the static stubs in
   * a generic manner at load time with Method objects. It is called by each 
   * entity in its class initializer. The same (or more specific) code could 
   * have been placed in each class initializer, but this saves a lot of bytecode
   * generation during transformation, and has no performance implications.
   *
   * @param classname class name that should have its static stubs initialized
   *   with Method objects
   */
  public static void initializeMethodStubs(String classname)
  {
    if(log.isDebugEnabled()) log.debug("** generic runtime method stub field initializer called for "+classname);
    try
    {
      Rewriter rewriter = (Rewriter)Controller.getActiveController().getClassLoader();
      Class cl = Class.forName(classname, true, rewriter);
      java.lang.reflect.Method[] methods = cl.getDeclaredMethods();
      for(int i=0; i<methods.length; i++)
      {
        // skip the self-reference accessors
        if(methods[i].getName().equals("_jistMethod_Set__ref")) continue;
        if(methods[i].getName().equals("_jistMethod_Get__ref")) continue;
        // skip any static methods
        if(java.lang.reflect.Modifier.isStatic(methods[i].getModifiers())) continue;
        if(log.isDebugEnabled()) log.debug("initializing stub field for method: "+methods[i]);
        // turn off access checks
        methods[i].setAccessible(true);
        // find appropriate method stub field
        java.lang.reflect.Field methodStubField = cl.getField(
            getMethodStubFieldName(methods[i]));
        // paranoia check
        if(methodStubField.get(null)!=null)
        {
          throw new RuntimeException("static method field stub already assigned: "+methodStubField);
        }
        // set method stub field to point to its appropriate method
        methodStubField.set(null, methods[i]);
      }
    }
    catch(Throwable e)
    {
      throw new JistException("filling in entity ref stubs", e);
    }
  }

  /**
   * Prime the rewriter doing a breadth-first search loading
   * (and rewrite) of all classes transitively statically referenced.
   *
   * @param classname root class of the breadth-first traversal
   * @return number of classes loaded
   * @throws ClassNotFoundException when requested or (directly or indirectly) referenced classes 
   *   cannot be loaded
   */
  public int prime(String classname) throws ClassNotFoundException
  {
    if(log.isDebugEnabled()) log.debug("** priming class rewriter starting with: "+classname);
    String[] list = getAllClassReferencesRecursively(classname);
    for(int i=0; i<list.length; i++)
    {
      Class c = loadClass(list[i], false);
      // fire the static initializer (side-effect)
      try
      {
        if(Rewriter.isEntityRuntime(c))
        {
          c.getDeclaredField(Rewriter.JIST_STATIC_TRIGGER).get(null);
        }
      }
      catch(Exception ex)
      {
        throw new JistException("invoking static triggers", ex);
      }
    }
    clearLookupCache();
    return list.length;
  }


  /**
   * Install a new rewrite traversal handler.
   *
   * @param rewrite rewriter traversal handler
   */
  public void installPreRewriteTraversal(ClassTraversal.Visitor rewrite)
  {
    rewriters.add(rewrite);
  }


  /**
   * Convert class name into a filename.
   *
   * @param classname class name to converted
   * @return file name containing given class
   */
  public static String classToFileName(String classname)
  {
    return classname.replace('.', '/')+".class";
  }

} // class: Rewriter


/**
 * Traversal object that ensures that a given entity obeys the specific coding
 * norms of JIST applications. Specifically, JIST applications may not have
 * entity objects that have static, abstract or native methods. All entity
 * state should be declared private. All non-private methods should return
 * void.
 * 
 * @see JistAPI.Entity
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @since JIST1.0
 */

class RewriterTraversalVerifyEntity extends ClassTraversal.Empty 
{

  /** primary rewriter. */
  private Rewriter rewriter;
  /** accumulated errors. */
  private Vector errors;
  /** class constant pool. */
  private ConstantPoolGen cpg;

  /**
   * Create rewriter object to perform entity verification.
   *
   * @param rewriter reference to primary rewriter
   */
  public RewriterTraversalVerifyEntity(Rewriter rewriter)
  {
    this.rewriter = rewriter;
  }

  /**
   * Return accumulated entity verification errors.
   *
   * @return array of accumulated entity verification errors
   */
  public String[] getErrors()
  {
    String[] serrors = new String[errors.size()];
    errors.copyInto(serrors);
    return serrors;
  }

  /** {@inheritDoc} */
  public ClassGen doClass(ClassGen cg)
  {
    errors = new Vector();
    cpg = cg.getConstantPool();
    return cg;
  }

  /** {@inheritDoc} */
  public ClassGen doClassPost(ClassGen cg) throws ClassNotFoundException
  {
    if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("number of errors found in class "+cg.getClassName()+": "+errors.size());
    if(rewriter.isProxiable(cg.getJavaClass()))
      errors.add("Proxiable entity interface found on Entity class: "+cg.getClassName());
    return cg;
  }

  /** {@inheritDoc} */
  public FieldGen doField(ClassGen cg, FieldGen fg)
  {
    if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("verifying field "+fg.getName());
    if(fg.isStatic())
      errors.add("Illegal static field in entity: "+fg.getName());
    if(!fg.isPrivate())
      errors.add("Illegal non-private field in entity: "+fg.getName());
    return fg;
  }

  /** {@inheritDoc} */
  public MethodGen doMethod(ClassGen cg, MethodGen mg) throws ClassNotFoundException
  {
    if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("verifying method "+mg.getName());
    if(mg.isStatic() &&
        !(mg.getName().equals(Rewriter.JAVA_MAIN_NAME) &&
          mg.getArgumentTypes().length==1)
        )
      errors.add("Illegal static method in entity: "+mg.getName());
    if(mg.isAbstract())
      errors.add("Illegal abstract method in entity: "+mg.getName());
    if(mg.isNative())
      errors.add("Illegal native method in entity: "+mg.getName());
    if(!mg.isPrivate() && !rewriter.isBlocking(mg) && !mg.getReturnType().equals(Type.VOID))
      errors.add("Non-private method has non-void return: "+mg.getName());
    if(!mg.getName().equals(Constants.CONSTRUCTOR_NAME) &&
        !mg.getName().equals(Rewriter.JAVA_MAIN_NAME))
    {
      Type[] types = mg.getArgumentTypes();
      for(int i=0; i<types.length; i++)
      {
        if(!rewriter.isTimeless(types[i]))
        {
          errors.add("Illegal non-timeless parameter to entity method: "+
              mg.getClassName()+"."+mg.getName()+", type="+types[i]);
        }
      }
    }
    return mg;
  }

} // class: RewriterTraversalVerifyEntity


/**
 * Traversal object that ensures that a given class obeys the specific coding
 * norms of JIST applications. Specifically, JistAPI.THIS may not appear in a
 * non-entity or static method.
 * 
 * @see JistAPI
 * @see JistAPI.Entity
 * @see JistAPI#THIS
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @since JIST1.0
 */

class RewriterTraversalVerifyAll extends ClassTraversal.Empty 
{

  /** accumulated verification errors. */
  private Vector errors;
  /** class constant pool. */
  private ConstantPoolGen cpg;
  /** primary rewriter. */
  private Rewriter rewriter;

  /**
   * Create rewriter object for general verification.
   *
   * @param rewriter reference to primary rewriter
   */
  public RewriterTraversalVerifyAll(Rewriter rewriter)
  {
    this.rewriter = rewriter;
  }

  /**
   * Return accumulated errors.
   *
   * @return array of accumulated errors
   */
  public String[] getErrors()
  {
    String[] serrors = new String[errors.size()];
    errors.copyInto(serrors);
    return serrors;
  }

  /** {@inheritDoc} */
  public ClassGen doClass(ClassGen cg)
  {
    errors = new Vector();
    cpg = cg.getConstantPool();
    return cg;
  }

  /** {@inheritDoc} */
  public MethodGen doMethod(ClassGen cg, MethodGen mg)
  {
    if(rewriter.isContinuable(mg) && Constants.CONSTRUCTOR_NAME.equals(mg.getName()))
    {
      errors.add("illegal to have continuation within object constructor: "+mg.getClassName());
    }
    return mg;
  }

} // class: RewriterTraversalVerifyAll 


/**
 * Traversal object that creates both set and get accessor methods for 
 * each public field. Finally, all fields are converted to protected access.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @since JIST1.0
 */

class RewriterTraversalAddAccessorMethods extends ClassTraversal.Empty
{

  /** class instruction factory. */
  private InstructionFactory ifc;

  /** {@inheritDoc} */
  public ClassGen doClass(ClassGen cg)
  {
    ifc = new InstructionFactory(cg.getConstantPool());
    return cg;
  }

  /** {@inheritDoc} */
  public FieldGen doField(ClassGen cg, FieldGen fg)
  {
    // add set accessor
    if(fg.isPublic())
    {
      if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("adding set accessor method for "+cg.getClassName()+"."+fg.getName());
      InstructionList il = new InstructionList();
      if(!fg.isStatic())
        il.append(ifc.createThis());
      il.append(ifc.createLoad(fg.getType(), fg.isStatic() ? 0 : 1));
      il.append(ifc.createFieldAccess(
            cg.getClassName(), fg.getName(), fg.getType(),
            fg.isStatic() ? Constants.PUTSTATIC : Constants.PUTFIELD));
      il.append(InstructionConstants.RETURN);
      MethodGen mg = new MethodGen(Constants.ACC_PUBLIC, Type.VOID,
          new Type[] { fg.getType() }, new String[] { "_x" },
          Rewriter.JIST_METHOD_SET+fg.getName(),
          cg.getClassName(), il, cg.getConstantPool());
      mg.isStatic(fg.isStatic());
      mg.setMaxStack();
      mg.setMaxLocals();
      cg.addMethod(mg.getMethod());
    }
    // add get accessor
    if(fg.isPublic())
    {
      if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("adding get accessor method for "+cg.getClassName()+"."+fg.getName());
      InstructionList il = new InstructionList();
      if(!fg.isStatic())
        il.append(ifc.createThis());
      il.append(ifc.createFieldAccess(
            cg.getClassName(), fg.getName(), fg.getType(),
            fg.isStatic() ? Constants.GETSTATIC : Constants.GETFIELD));
      il.append(ifc.createReturn(fg.getType()));
      MethodGen mg = new MethodGen(Constants.ACC_PUBLIC, fg.getType(),
          new Type[] { }, new String[] { },
          Rewriter.JIST_METHOD_GET+fg.getName(),
          cg.getClassName(), il, cg.getConstantPool());
      mg.isStatic(fg.isStatic());
      mg.setMaxStack();
      mg.setMaxLocals();
      cg.addMethod(mg.getMethod());
    }
    // convert fields to private access
    if(fg.isPublic())
    {
      if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("converting field to private access "+cg.getClassName()+"."+fg.getName());
      fg.isPrivate(false);
      fg.isProtected(true);
      fg.isPublic(false);
    }
    return fg;
  } // function: doInstruction

} // class: RewriterTraversalAddAccessorMethods


/**
 * Traversal object that ensures all entity field access operations are 
 * converted to method invocations. The traversal replaces all 
 * <code>getfield</code>, <code>getstatic</code>, <code>putfield</code> and
 * <code>putstatic</code> instructions into the appropriate method invocations.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @since JIST1.0
 */

class RewriterTraversalRemoveFieldAccess extends ClassTraversal.Empty
{

  /** class instruction factory. */
  private InstructionFactory ifc;
  /** primary rewriter. */
  private Rewriter rewriter;

  /**
   * Create rewriter object to convert all remote field accesses to method accesses.
   *
   * @param rewriter reference to primary rewriter
   */
  public RewriterTraversalRemoveFieldAccess(Rewriter rewriter)
  {
    this.rewriter = rewriter;
  }

  /** {@inheritDoc} */
  public ClassGen doClass(ClassGen cg)
  {
    ifc = new InstructionFactory(cg.getConstantPool());
    return cg;
  }

  /**
   * Determine whether instruction is local field access.
   *
   * @param cg class object
   * @param cpg class constant pool
   * @param inst instruction to test
   * @return whether given instruction is of a local field access.
   */
  private boolean isLocalNonPublicFieldAccess(ClassGen cg, ConstantPoolGen cpg, FieldInstruction inst)
  {
    // check local
    if(!cg.getClassName().equals(inst.getClassName(cpg))) return false;
    // find field
    Field[] fields = cg.getFields();
    for(int i=0; i<fields.length; i++)
    {
      if(fields[i].getName().equals(inst.getFieldName(cpg)))
      {
        // check non-public
        return fields[i].isPrivate() || fields[i].isProtected();
      }
    }
    return false;
  }

  /** {@inheritDoc} */
  public void doInstruction(ClassGen cg, MethodGen mg, InstructionHandle ih, Instruction inst) throws ClassNotFoundException
  {
    if(inst instanceof FieldInstruction)
    {
      FieldInstruction fieldinst = (FieldInstruction)inst;
      ConstantPoolGen cpg = cg.getConstantPool();
      if(rewriter.isEntity(fieldinst.getClassName(cpg)) &&
          !rewriter.isIgnored(fieldinst.getClassName(cpg)) &&
          !fieldinst.getFieldName(cg.getConstantPool()).startsWith(Rewriter.JIST_FIELD_PREFIX) &&
          !isLocalNonPublicFieldAccess(cg, cpg, fieldinst) 
          )
      {
        if(inst instanceof GETFIELD || inst instanceof GETSTATIC)
        {
          inst = ifc.createInvoke(fieldinst.getClassName(cpg),
              Rewriter.JIST_METHOD_GET+fieldinst.getFieldName(cpg),
              fieldinst.getType(cpg), new Type[] { },
              inst instanceof GETFIELD ? Constants.INVOKEVIRTUAL : Constants.INVOKESTATIC);
        }
        if(inst instanceof PUTFIELD || inst instanceof PUTSTATIC)
        {
          inst = ifc.createInvoke(fieldinst.getClassName(cpg),
              Rewriter.JIST_METHOD_SET+fieldinst.getFieldName(cpg),
              Type.VOID, new Type[] { fieldinst.getType(cpg) },
              inst instanceof PUTFIELD ? Constants.INVOKEVIRTUAL : Constants.INVOKESTATIC);
        }
        if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("replacing field access instruction ("+fieldinst.toString(cg.getConstantPool().getConstantPool())+") in "+cg.getClassName()+"."+mg.getName());
        ih.swapInstruction(inst);
      }
    }
  } // method: doInstruction

} // class: RewriterTraversalRemoveFieldAccess


/**
 * Traversal object that converts any references to entity objects within a
 * class into EntityRef objects. This means that all method parameter and
 * method return types, as well as all field types, that implement the
 * JistAPI.Entity interface are converted into the EntityRef type.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @since JIST1.0
 * @see Controller#newEntityReference
 * @see JistAPI.Entity
 * @see EntityRef
 */

class RewriterTraversalModifyEntityReferences extends ClassTraversal.Empty
{

  /** class instruction factory. */
  private InstructionFactory ifc;
  /** primary rewriter. */
  private Rewriter rewriter;

  /**
   * Create rewriter object to modify all entity references.
   *
   * @param rewriter reference to primary rewriter
   */
  public RewriterTraversalModifyEntityReferences(Rewriter rewriter)
  {
    this.rewriter = rewriter;
  }

  /** {@inheritDoc} */
  public ClassGen doClass(ClassGen cg)
  {
    ifc = new InstructionFactory(cg.getConstantPool());
    return cg;
  }

  /** {@inheritDoc} */
  public FieldGen doField(ClassGen cg, FieldGen fg) throws ClassNotFoundException
  {
    // convert entity fields into entity references
    if(!rewriter.isRefType(fg.getType()))
    {
      if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("changing type to entity reference for field "+cg.getClassName()+"."+fg.getName());
      fg.setType(rewriter.getRefType(fg.getType()));
    }
    return fg;
  }

  /** {@inheritDoc} */
  public MethodGen doMethod(ClassGen cg, MethodGen mg) throws ClassNotFoundException
  {
    // convert entity parameters to entity reference parameteres
    if(!rewriter.isRefTypes(mg.getArgumentTypes()))
    {
      if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("changing parameters to entity references for method "+cg.getClassName()+"."+mg.getName());
      mg.setArgumentTypes(rewriter.getRefTypes(mg.getArgumentTypes()));
    }
    // and also return entities to entity references
    if(!rewriter.isRefType(mg.getReturnType()))
    {
      if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("changing return type to entity reference for method "+cg.getClassName()+"."+mg.getName());
      mg.setReturnType(rewriter.getRefType(mg.getReturnType()));
    }
    return mg;
  }

  /** {@inheritDoc} */
  public void doInstruction(ClassGen cg, MethodGen mg, InstructionHandle ih, Instruction inst) throws ClassNotFoundException
  {
    ConstantPoolGen cpg = cg.getConstantPool();
    // modify field access instructions to entities
    if(inst instanceof FieldInstruction)
    {
      FieldInstruction finst = (FieldInstruction)inst;
      if(!rewriter.isRefType(finst.getType(cpg)))
      {
        short itype = 
          inst instanceof GETFIELD ? Constants.GETFIELD :
          inst instanceof PUTFIELD ? Constants.PUTFIELD :
          inst instanceof GETSTATIC ? Constants.GETSTATIC :
          inst instanceof PUTSTATIC ? Constants.PUTSTATIC :
          -1;
        if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("changing entity reference for field instruction in "+cg.getClassName()+"."+mg.getName());
        ih.swapInstruction(ifc.createFieldAccess(
              finst.getClassName(cpg), finst.getName(cpg),
              rewriter.getRefType(finst.getType(cpg)), itype));
      }
    }
    // modify entity method invocation
    if(inst instanceof InvokeInstruction)
    {
      InvokeInstruction invinst = (InvokeInstruction)inst;
      if(!rewriter.isRefType(invinst.getReturnType(cpg)) ||
          !rewriter.isRefTypes(invinst.getArgumentTypes(cpg)))
      {
        short itype =
          inst instanceof INVOKEINTERFACE ? Constants.INVOKEINTERFACE :
          inst instanceof INVOKESPECIAL ? Constants.INVOKESPECIAL :
          inst instanceof INVOKESTATIC ? Constants.INVOKESTATIC :
          inst instanceof INVOKEVIRTUAL ? Constants.INVOKEVIRTUAL :
          -1;
        if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("changing types to entity references for invocation instruction ("+inst+") in "+cg.getClassName()+"."+mg.getName());
        ih.swapInstruction(ifc.createInvoke(
              invinst.getClassName(cpg), invinst.getMethodName(cpg),
              rewriter.getRefType(invinst.getReturnType(cpg)),
              rewriter.getRefTypes(invinst.getArgumentTypes(cpg)),
              itype));
      }
    }
    // modify entity array creation
    if(inst instanceof ANEWARRAY)
    {
      ANEWARRAY nainst = (ANEWARRAY)inst;
      if(!rewriter.isRefType(nainst.getType(cpg)))
      {
        if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("modifying entity array new instruction in "+cg.getClassName()+"."+mg.getName());
        ih.swapInstruction(ifc.createNewArray(
              rewriter.getRefType(nainst.getType(cpg)), (short)1));
      }
      // rimnote: what about multi-dimensional arrays
    }
    // checkcast
    if(inst instanceof CHECKCAST)
    {
      CHECKCAST ci = (CHECKCAST)inst;
      if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("modifying entity check cast instruction in "+cg.getClassName()+"."+mg.getName());
      if(!rewriter.isRefType(ci.getType(cpg)))
      {
        ih.swapInstruction(ifc.createCast(
              Type.OBJECT, rewriter.getRefType(ci.getType(cpg))));
      }
    }
    // rimnote: any other typed instructions that should be modified?
  }

} // class: RewriterTraversalModifyEntityReferences 


/**
 * Traversal object that ensures all entity creation operations are modified to
 * result in an EntityRef on the stack, instead of the original Entity. The way
 * this is done is by searching for constructor (&lt;init&gt;) method calls on
 * entity objects. At the end of this method call there is an instance of the
 * object on the stack. We use this instance to call
 * <code>Controller.newRef(Entity)</code>, which creates and returns a new
 * entityRef that refers to the current entity. Additionally, this traversal
 * object modifies entity array creation.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @since JIST1.0
 * @see JistAPI.Entity
 * @see EntityRef
 * @see Controller#newEntityReference
 */

class RewriterTraversalModifyEntityCreation extends ClassTraversal.Empty
{
  /** primary rewriter. */
  private Rewriter rewriter;
  /** class instruction factory. */
  private InstructionFactory ifc;
  /** class constant pool. */
  private ConstantPoolGen cpg;
  /** number of (nested) new instructions. */
  private int NEWs;

  /**
   * Create rewriter object to modify entity creation points.
   *
   * @param rewriter reference to primary rewriter
   */
  public RewriterTraversalModifyEntityCreation(Rewriter rewriter)
  {
    this.rewriter = rewriter;
  }

  /** {@inheritDoc} */
  public ClassGen doClass(ClassGen cg)
  {
    cpg = cg.getConstantPool();
    ifc = new InstructionFactory(cpg);
    return cg;
  }

  /** {@inheritDoc} */
  public MethodGen doMethod(ClassGen cg, MethodGen mg)
  {
    NEWs=0;
    return mg;
  }

  /** {@inheritDoc} */
  public void doInstruction(ClassGen cg, MethodGen mg, InstructionHandle ih, Instruction inst) throws ClassNotFoundException
  {
    if(inst instanceof NEW)
    {
      NEWs++;
    }
    // modify entity creation to result in entityref on stack
    if(inst instanceof INVOKESPECIAL)
    {
      INVOKESPECIAL is = (INVOKESPECIAL)inst;
      if(is.getMethodName(cpg).equals(Constants.CONSTRUCTOR_NAME))
      {
        if(rewriter.isEntity(is.getClassName(cpg)) && NEWs>0)
        {
          InstructionList il = new InstructionList();
          il.append(ifc.createInvoke(
                is.getClassName(cpg),
                Rewriter.JIST_METHOD_GET+Rewriter.JIST_REF_SUFFIX,
                new ObjectType(EntityRef.class.getName()),
                Type.NO_ARGS,
                Constants.INVOKEVIRTUAL));
          if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("modifying entity creation point in "+cg.getClassName()+"."+mg.getName());
          mg.getInstructionList().append(ih, il);
        }
        NEWs--;
        if(NEWs<0) NEWs=0;
      }
    }
    // todo: sometimes the actual entity object (as opposed to the entity
    //   reference) is needed for this.x() calls, so search for non-entity
    //   invocations, and convert back in those cases using local controller.
  }
}
 

/**
 * Traversal object that intercepts any method invocation instructions to
 * entity objects. This conversion is a little bit tricky, because it involves
 * a bit more bytecode generation. The basic idea has three parts. First, the
 * appropriate method object is selected from the corresponding method stub
 * field. Second, corresponding entity reference (or entity, in the case of a
 * "this." call) is passed along. Lastly, all the invocation parameters on the
 * stack are placed an object array of the appropriate size. Primitive types
 * are first converted into their object equivalents.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @since JIST1.0
 * @see Controller#newEntityReference
 * @see JistAPI.Entity
 * @see EntityRef
 */

class RewriterTraversalModifyEntityInvocation extends ClassTraversal.Empty
{

  /** class instruction factory. */
  private InstructionFactory ifc;
  /** class constant pool. */
  private ConstantPoolGen cpg;
  /** primary rewriter. */
  private Rewriter rewriter;

  /**
   * Create rewriter object to modify entity invocation points.
   *
   * @param rewriter reference to primary rewriter
   */
  public RewriterTraversalModifyEntityInvocation(Rewriter rewriter)
  {
    this.rewriter = rewriter;
  }

  /** {@inheritDoc} */
  public ClassGen doClass(ClassGen cg)
  {
    ifc = new InstructionFactory(cg.getConstantPool());
    cpg = cg.getConstantPool();
    return cg;
  }

  /**
   * Insert code to pack method parameters into object array.
   *
   * @param il instruction list
   * @param args method arguments
   * @param mg method object
   * @param ih instruction handle
   */
  private void packParametersIntoArray(InstructionList il, 
      Type[] args, MethodGen mg, InstructionHandle ih) throws ClassNotFoundException
  {
    if(args.length==0)
    {
      il.append(new ACONST_NULL());
    }
    else
    {
      // create argument array
      il.append(new PUSH(cpg, args.length));
      il.append(new ANEWARRAY(cpg.addClass(Type.OBJECT)));
      int maxlocals = mg.getMaxLocals();
      il.append(ifc.createStore(new ArrayType(Type.OBJECT, 1), maxlocals));
      // fill argument array from stack
      for(int i=args.length-1; i>=0; i--)
      {
        // convert to object, if necessary
        if(args[i] instanceof BasicType)
        {
          String oname = Rewriter.getPrimitiveObjectType(
              (BasicType)args[i]).getName();
          il.append(ifc.createNew(oname));
          il.append(InstructionConstants.DUP);
          il.append(ifc.createStore(Type.OBJECT, maxlocals+1));
          if(args[i].getSize()==1)
          {
            il.append(InstructionConstants.SWAP);
          }
          else
          {
            il.append(InstructionConstants.DUP_X2);
            il.append(InstructionConstants.POP);
          }
          il.append(ifc.createInvoke(oname, Constants.CONSTRUCTOR_NAME,
                Type.VOID, new Type[] { args[i] }, Constants.INVOKESPECIAL));
        }
        else if(args[i] instanceof ObjectType)
        {
          String clname = ((ObjectType)args[i]).getClassName();
          if(rewriter.isEntity(clname))
          {
            il.append(ifc.createInvoke(
                  Controller.class.getName(),
                  Controller.method_getEntityReference.getName(),
                  new ObjectType(EntityRef.class.getName()),
                  new Type[] { Type.OBJECT },
                  Constants.INVOKESTATIC));
          }
          il.append(ifc.createStore(Type.OBJECT, maxlocals+1));
        }
        else if(args[i] instanceof ArrayType)
        {
          il.append(ifc.createStore(Type.OBJECT, maxlocals+1));
        }
        else
        {
          throw new VerifyError("unexpected parameter type");
        }
        // insert into array
        il.append(ifc.createLoad(new ArrayType(Type.OBJECT, 1), maxlocals));
        il.append(new PUSH(cpg, i));
        il.append(ifc.createLoad(Type.OBJECT, maxlocals+1));
        il.append(ifc.createArrayStore(Type.OBJECT));
      }
      il.append(ifc.createLoad(new ArrayType(Type.OBJECT, 1), maxlocals));
    }
    // on stack we now have: object (entityref), argument array
  }

  /** {@inheritDoc} */
  public void doInstruction(ClassGen cg, MethodGen mg, InstructionHandle ih, Instruction inst) throws ClassNotFoundException
  {
    // if invocation instruction
    if(inst instanceof INVOKEVIRTUAL)
    {
      INVOKEVIRTUAL iv = (INVOKEVIRTUAL)inst;
      Type retType = iv.getReturnType(cpg);
      boolean isBlocking = rewriter.isBlocking(iv.getClassName(cpg), iv.getMethodName(cpg),
              retType, iv.getArgumentTypes(cpg));
      // if entity invocation
      if(rewriter.isEntity(iv.getClassName(cpg)) 
          && (iv.getReturnType(cpg).equals(Type.VOID) || isBlocking))
      {
        if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("modifying entity invocation ("+iv.toString(cpg.getConstantPool())+") in "+cg.getClassName()+"."+mg.getName());
        InstructionList il = new InstructionList();
        // pack stack of parameters into array
        if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("pack invocation parameters");
        packParametersIntoArray(il, iv.getArgumentTypes(cpg), mg, ih);
        // put method stub on stack, behind object reference and argument array
        il.append(ifc.createGetStatic(
              iv.getClassName(cpg),
              Rewriter.getMethodStubFieldName(iv.getName(cpg), iv.getSignature(cpg)),
              new ObjectType(java.lang.reflect.Method.class.getName())));
        il.append(InstructionConstants.DUP_X2);
        il.append(InstructionConstants.POP);
        // invoke appropriate static entity invocation helper
        if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("invoke jist controller instead");
        java.lang.reflect.Method m = isBlocking
          ? Controller.method_entityInvocationCont
          : Controller.method_entityInvocation;
        il.append(ifc.createInvoke(
              m.getDeclaringClass().getName(), m.getName(),
              Rewriter.getType(m.getReturnType()),
              Rewriter.getTypes(m.getParameterTypes()),
              Constants.INVOKESTATIC));
        // deal with result
        if(isBlocking)
        {
          if(retType.getType()==Constants.T_VOID)
          {
            il.append(InstructionFactory.POP);
          }
          else if(retType instanceof BasicType)
          {
            il.append(ifc.createCheckCast(
                  new ObjectType(Rewriter.getPrimitiveObjectType((BasicType)retType).getName())));
            il.append(ifc.createInvoke(
                  Rewriter.getPrimitiveObjectType((BasicType)retType).getName(),
                  Rewriter.getPrimitiveObjectConversionMethod((BasicType)retType),
                  retType, new Type[] { }, Constants.INVOKEVIRTUAL));
          }
        }
        // insert the code
        InstructionHandle ihStart = mg.getInstructionList().append(ih, il);
        InstructionHandle ihEnd = ihStart;
        for(int i=0; i<il.getLength(); i++)
        {
          ihEnd = ihEnd.getNext();
        }
        // redirect exception handlers
        CodeExceptionGen[] ex = mg.getExceptionHandlers();
        for(int i=0; i<ex.length; i++)
        {
          if(ih.equals(ex[i].getStartPC()))
          {
            ex[i].setStartPC(ihStart);
          }
          if(ih.equals(ex[i].getEndPC()))
          {
            ex[i].setEndPC(ihEnd);
          }
        }
        // redirect targets
        try
        {
          mg.getInstructionList().delete(ih);
        }
        catch(TargetLostException e)
        {
          InstructionHandle[] targets = e.getTargets();
          for(int i=0; i < targets.length; i++) 
          {
            InstructionTargeter[] targeters = targets[i].getTargeters();
            for(int j=0; j < targeters.length; j++)
            {
              targeters[j].updateTarget(targets[i], ihStart);
            }
          }
        }

      } // if entity invocation
    } // if invocation instructions

  } // function: doInstruction

} // class: RewriterTraversalModifyEntityInvocation


/**
 * Traversal object that adds the a self-reference EntityRef field, as well as
 * the appropriate accessor methods (part of the <code>Entity</code>
 * interface). This class also takes care of initializing the ref field. This
 * is done by injecting code just after the invocation of the superclass
 * constructor (the earliest possible time). In this manner, the ref value is
 * initialized for use by any entity method invocations, including any that
 * might be in the constructor body.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @since JIST1.0
 * @see Entity
 * @see EntityRef
 */

class RewriterTraversalAddSelfEntityRef extends ClassTraversal.Empty
{
  /** whether a superclass initializer has been found. */
  private boolean foundSuperInit;
  /** class instruction factory. */
  private InstructionFactory ifc;
  /** whether to insert code to initialize entity reference. */
  private boolean initializeRef;

  /**
   * Create rewriter object that will add an entity self-reference field.
   *
   * @param initializeRef whether to insert code to initialize entity reference
   */
  public RewriterTraversalAddSelfEntityRef(boolean initializeRef)
  {
    this.initializeRef = initializeRef;
  }

  /** {@inheritDoc} */
  public ClassGen doClass(ClassGen cg)
  {
    ifc = new InstructionFactory(cg.getConstantPool());
    // add entityref field
    if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("adding entity self reference field for "+cg.getClassName());
    FieldGen fg = new FieldGen(Constants.ACC_PUBLIC,
        new ObjectType(EntityRef.class.getName()), Rewriter.JIST_ENTITYREF,
        cg.getConstantPool());
    cg.addField(fg.getField());
    // add entityref set accessor
    if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("adding entity self reference set accessor method for "+cg.getClassName());
    InstructionList il = new InstructionList();
    il.append(ifc.createThis());
    il.append(ifc.createLoad(fg.getType(), 1));
    il.append(ifc.createFieldAccess(cg.getClassName(), fg.getName(), 
          fg.getType(), Constants.PUTFIELD));
    il.append(InstructionConstants.RETURN);
    MethodGen mg = new MethodGen(fg.getAccessFlags() | Constants.ACC_FINAL, Type.VOID,
        new Type[] { fg.getType() }, new String[] { "_x" },
        Rewriter.JIST_METHOD_SET+Rewriter.JIST_REF_SUFFIX,
        cg.getClassName(), il, cg.getConstantPool());
    mg.setMaxStack();
    mg.setMaxLocals();
    cg.addMethod(mg.getMethod());
    // add entityref get accessor
    if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("adding entity self reference get accessor method for "+cg.getClassName());
    il = new InstructionList();
    il.append(ifc.createThis());
    il.append(ifc.createFieldAccess(cg.getClassName(), fg.getName(), 
          fg.getType(), Constants.GETFIELD));
    il.append(ifc.createReturn(fg.getType()));
    mg = new MethodGen(fg.getAccessFlags() | Constants.ACC_FINAL, fg.getType(),
        new Type[] { }, new String[] { },
        Rewriter.JIST_METHOD_GET+Rewriter.JIST_REF_SUFFIX,
        cg.getClassName(), il, cg.getConstantPool());
    mg.setMaxStack();
    mg.setMaxLocals();
    cg.addMethod(mg.getMethod());
    return cg;
  }

  /** {@inheritDoc} */
  public MethodGen doMethod(ClassGen cg, MethodGen mg)
  {
    foundSuperInit = false;
    return mg;
  }

  /** {@inheritDoc} */
  public void doInstruction(ClassGen cg, MethodGen mg, InstructionHandle ih, Instruction inst)
  {
    // look for super class initialization call
    if(initializeRef && !foundSuperInit && mg.getName().equals(Constants.CONSTRUCTOR_NAME)
        && inst instanceof INVOKESPECIAL
        && ((INVOKESPECIAL)inst).getMethodName(cg.getConstantPool()).equals(Constants.CONSTRUCTOR_NAME)
        && ((INVOKESPECIAL)inst).getClassName(cg.getConstantPool()).equals(cg.getSuperclassName()))
    {
      if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("modifying superclass constructor call to initialize self reference in "+cg.getClassName());
      // initialize entityref
      InstructionList il = new InstructionList();
      il.append(ifc.createThis());
      il.append(ifc.createThis());
      il.append(ifc.createInvoke(Controller.class.getName(), 
            Controller.method_newEntityReference.getName(),
            new ObjectType(EntityRef.class.getName()), 
            Rewriter.getTypes(Controller.method_newEntityReference.getParameterTypes()),
            Constants.INVOKESTATIC));
      il.append(ifc.createPutField(cg.getClassName(), 
            Rewriter.JIST_ENTITYREF,
            new ObjectType(EntityRef.class.getName())));
      mg.getInstructionList().append(ih, il);
      foundSuperInit = true;
    }
  }

} // class: RewriterTraversalAddSelfEntityRef


/**
 * Traversal object that tags a class as implementing the <code>Entity</code>
 * interface.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @since JIST1.0
 * @see Entity
 */

class RewriterTraversalImplementEntityInterface extends ClassTraversal.Empty
{
  /** whether to add a static trigger field to class. */
  private boolean addStaticTrigger;

  /**
   * Create a rewriter object that will implement the Entity interface.
   *
   * @param addStaticTrigger whether to add a static verification/initialization trigger field
   */
  public RewriterTraversalImplementEntityInterface(boolean addStaticTrigger)
  {
    this.addStaticTrigger = addStaticTrigger;
  }

  /** {@inheritDoc} */
  public ClassGen doClass(ClassGen cg)
  {
    // add entity interface
    if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("adding entity interface to "+cg.getClassName());
    cg.addInterface(Entity.class.getName());
    // public package access for entities to allow Rewriter access
    if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("making class of "+cg.getClassName()+" entity public");
    cg.isPublic(true);
    // adding static trigger field
    //   (accessing this field will trigger static initializer to run...)
    if(addStaticTrigger)
    {
      if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("adding static trigger field "+cg.getClassName());
      FieldGen fg = new FieldGen(Constants.ACC_PUBLIC | Constants.ACC_STATIC,
          new ObjectType(EntityRef.class.getName()), Rewriter.JIST_STATIC_TRIGGER,
          cg.getConstantPool());
      cg.addField(fg.getField());
    }
    return cg;
  }

} // class: RewriterTraversalImplementEntityInterface


/**
 * Traversal object that tags a class as implementing the <code>Timeless</code>
 * interface.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @since JIST1.0
 * @see Entity
 */

class RewriterTraversalImplementTimelessInterface extends ClassTraversal.Empty
{
  /** {@inheritDoc} */
  public ClassGen doClass(ClassGen cg)
  {
    // add timeless interface
    if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("adding timeless interface to "+cg.getClassName());
    cg.addInterface(Timeless.class.getName());
    return cg;
  }

} // class: RewriterTraversalImplementTimelessInterface 


/**
 * Traversal object that adds fields in an entity object that store method
 * references. These method stubs greatly improve the speed of method
 * invocation, since reflection is not performed each time. This traversal also
 * inserts code within the class initializer (creating it if no such method
 * exists) to call the generic method stub initialization method
 * (<code>Rewriter.initializeMethodStubs</code>). This method uses reflection
 * (at load time only) to initialize the stub fields.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @since JIST1.0
 * @see Rewriter#initializeMethodStubs
 */

class RewriterTraversalAddEntityMethodStubs extends ClassTraversal.Empty
{
  /** class instruction factory. */
  private InstructionFactory ifc;
  /** primary rewriter. */
  private Rewriter rewriter;
  /** class initializer. */
  private MethodGen clinit;

  /**
   * Create rewriter object that add entity method stubs.
   *
   * @param rewriter reference to primary rewriter
   */
  public RewriterTraversalAddEntityMethodStubs(Rewriter rewriter)
  {
    this.rewriter = rewriter;
  }

  /** {@inheritDoc} */
  public ClassGen doClass(ClassGen cg)
  {
    ifc = new InstructionFactory(cg.getConstantPool());
    clinit = null;
    return cg;
  }

  /** {@inheritDoc} */
  public MethodGen doMethod(ClassGen cg, MethodGen mg) throws ClassNotFoundException
  {
    // add entity method stub fields
    FieldGen fg = new FieldGen(
        Constants.ACC_PUBLIC | Constants.ACC_STATIC,
        new ObjectType(java.lang.reflect.Method.class.getName()),
        rewriter.getMethodStubFieldName(mg),
        cg.getConstantPool());
    if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("adding method stub field "+cg.getClassName()+"."+fg.getName());
    cg.addField(fg.getField());
    // find clinit method, if it exists
    if(mg.getName().equals(Constants.STATIC_INITIALIZER_NAME))
    {
      clinit = mg;
    }
    return mg;
  }

  /** {@inheritDoc} */
  public ClassGen doClassPost(ClassGen cg)
  {
    // call generic (reflection-based) runtime initializer to initialize method stubs
    if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("adding call to method stub generic intializer in static initializer of "+cg.getClassName());
    InstructionList il = new InstructionList();
    il.append(new LDC(
          cg.getConstantPool().addString(cg.getClassName())));
    java.lang.reflect.Method m = Rewriter.method_initializeMethodStubs;
    il.append(ifc.createInvoke(m.getDeclaringClass().getName(), m.getName(),
          Rewriter.getType(m.getReturnType()), Rewriter.getTypes(m.getParameterTypes()),
          Constants.INVOKESTATIC));
    // insert instruction; create static initializer, if necessary
    if(clinit!=null)
    {
      clinit.getInstructionList().insert(il);
    }
    else
    {
      if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("new static initializer method created, because it did not exist");
      il.append(ifc.createReturn(Type.VOID));
      clinit = new MethodGen(Constants.ACC_STATIC,
          Type.VOID, Type.NO_ARGS, null,
          Constants.STATIC_INITIALIZER_NAME, cg.getClassName(),
          il, cg.getConstantPool());
      clinit.setMaxStack();
      clinit.setMaxLocals();
      cg.addMethod(clinit.getMethod());
    }
    return cg;
  }

} // class: RewriterTraversalAddEntityMethodStubs


/**
 * Traversal object that translates JistAPI method calls into their appropriate
 * Jist system calls.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @since JIST1.0
 * @see JistAPI
 */

class RewriterTraversalTranslateAPICalls extends ClassTraversal.Empty
{
  /** class instruction factory. */
  private InstructionFactory ifc;

  /** {@inheritDoc} */
  public ClassGen doClass(ClassGen cg)
  {
    ifc = new InstructionFactory(cg.getConstantPool());
    return cg;
  }

  /** {@inheritDoc} */
  public MethodGen doMethod(ClassGen cg, MethodGen mg)
  {
    // mark blocking methods as throwing internal Continuation exception
    if(Rewriter.isBlocking(mg))
    {
      mg.addException(Event.ContinuationFrame.class.getName());
    }
    return mg;
  }

  /** {@inheritDoc} */
  public void doInstruction(ClassGen cg, MethodGen mg, InstructionHandle ih, Instruction inst)
  {
    // modify api function calls
    if(inst instanceof INVOKESTATIC)
    {
      ConstantPoolGen cpg = cg.getConstantPool();
      InvokeInstruction invinst = (InvokeInstruction)inst;
      if(invinst.getClassName(cpg).equals(JistAPI.class.getName()))
      {
        if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("converting "+invinst.getMethodName(cpg)+" API call in "+cg.getClassName()+"."+mg.getName());
        try
        {
          java.lang.reflect.Method m = 
            (java.lang.reflect.Method)JistAPI_Impl.class.getField("method_"+invinst.getMethodName(cpg)).get(null);
          ih.swapInstruction(ifc.createInvoke(
                m.getDeclaringClass().getName(),
                m.getName(),
                Rewriter.getType(m.getReturnType()), 
                Rewriter.getTypes(m.getParameterTypes()),
                Constants.INVOKESTATIC));
        }
        catch(Exception e)
        {
          throw new JistException("rewriting", e);
        }
      }
    }
    // modify api field access
    if(inst instanceof GETSTATIC)
    {
      ConstantPoolGen cpg = cg.getConstantPool();
      GETSTATIC getinst = (GETSTATIC)inst;
      if(getinst.getClassName(cpg).equals(JistAPI.class.getName()))
      {
        if(Rewriter.log.isDebugEnabled()) Rewriter.log.debug("converting "+getinst.getFieldName(cpg)+" API field in "+cg.getClassName()+"."+mg.getName());
        try
        {
          java.lang.reflect.Field f = JistAPI.class.getDeclaredField("THIS");
          if(getinst.getFieldName(cpg).equals(f.getName()))
          {
            java.lang.reflect.Method m = Controller.method_getTHIS; 
            ih.swapInstruction(ifc.createInvoke(
                  m.getDeclaringClass().getName(), m.getName(),
                  Rewriter.getType(m.getReturnType()),
                  Rewriter.getTypes(m.getParameterTypes()),
                  Constants.INVOKESTATIC));
          }
        }
        catch(NoSuchFieldException e)
        {
          throw new JistException("rewriting", e);
        }
      }
    }
  } // function: doInstruction

} // class: RewriterTraversalTranslateAPICalls 


/**
 * Traversal object that modifies continuable methods. Continuable needs to be
 * able to pause and resume their execution at various points: at any blocking
 * entity call, or continuable regular method call. Pausing execution means
 * saving both the frame and the program location into a custom state object.
 * Resuming execution means reversing the process.
 *
 * <pre>
 * Static analysis:
 *   - data flow: to determine what the frame (stack and locals) looks like
 *       at each blocking or continuable execution point
 *   - state class: create custom state class (subclass of Event.ContinuationFrame) 
 *     with fields for locals and stack slots at each execution point
 *
 * Pausing execution:
 *  - save frame (locals and stack) to state object before execution and store
 *    in additional local variable
 *  - execute method invocation
 *  - check if we are in save mode
 *    - if yes, then push frame information using Controller and return 
 *      immediately (with some dummy return value, if necessary)
 *
 * Restoring execution:
 *   - (at method start) check if we are invoking this method in restore mode
 *   - if yes, pop frame information from Controller
 *   - switch on program counter
 *   - restore frame (locals and stack) slots
 *   - jump to pause point
 * </pre>
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @since JIST1.0
 * @see JistAPI
 */

class RewriterTraversalContinuableMethods extends ClassTraversal.Empty
{

  /** primary rewriter. */
  private Rewriter rewriter;
  /** data flow analysis object. */
  private RewriterFlow flow;
  /** data flow analysis result. */
  private RewriterFlow.FlowInfoMap flowinfo;
  /** class constant pool. */
  private ConstantPoolGen cpg;
  /** class instruction factory. */
  private InstructionFactory ifc;
  /** program location counter. */
  private int pc;
  /** save/restore frames. */
  private Vector frames;
  /** save/restore instruction handles. */
  private Vector ihs;

  /**
   * Create rewriter object that will transform continuable methods to CPS.
   *
   * @param rewriter reference to primary rewriter
   */
  public RewriterTraversalContinuableMethods(Rewriter rewriter)
  {
    this.rewriter = rewriter;
    this.flow = new RewriterFlow();
  }

  /** {@inheritDoc} */
  public ClassGen doClass(ClassGen cg)
  {
    this.cpg = cg.getConstantPool();
    this.ifc = new InstructionFactory(cg);
    return cg;
  }

  /** {@inheritDoc} */
  public MethodGen doMethod(ClassGen cg, MethodGen mg)
  {
    // if method is continuable
    if(rewriter.isContinuable(mg) && !mg.isAbstract())
    {
      if(Rewriter.log.isDebugEnabled())
      {
        Rewriter.log.debug("Processing continuable method: "+mg.getName());
      }
      // do flow analysis of method
      flowinfo = flow.doFlow(cg, mg);
      // valid pc values >= 1
      frames = new Vector();
      frames.add(null);
      ihs = new Vector();
      ihs.add(null);
      pc = 0;
    }
    return mg;
  }

  /** {@inheritDoc} */
  public MethodGen doMethodPost(ClassGen cg, MethodGen mg)
  {
    if(flowinfo!=null)
    {
      // add restore code
      restoreFrame(mg, frames, ihs, ifc);
      frames = null;
      ihs = null;
      flowinfo = null;
    }
    return mg;
  }

  /** {@inheritDoc} */
  public void doInstruction(ClassGen cg, MethodGen mg, InstructionHandle ih, Instruction inst) throws ClassNotFoundException
  {
    // if invocation instruction is a continuable method
    if(flowinfo!=null && inst instanceof InvokeInstruction)
    {
      InvokeInstruction inv = (InvokeInstruction)inst;
      // if invocation is to continuable or blocking method
      if(rewriter.isBlocking(inv.getClassName(cpg), inv.getMethodName(cpg), inv.getReturnType(cpg), inv.getArgumentTypes(cpg)) ||
          rewriter.isContinuable(inv, cpg))
      {
        // increment program location counter
        pc++;
        // retrieve frame at this location
        Frame f = fixFrame(flowinfo.getFrame(ih));
        // define custom state type for this execution point
        rewriter.define(createStateClass(mg, pc, f.getClone()));
        // save frame into custom state object
        saveFrame(mg, ih, pc, f.getClone(), ifc);
        frames.add(f);
        ihs.add(ih);
      }
    }
  }

  /**
   * Return BCEL frame with fewer object types (only relevant ones)
   * in order to simplify frame save/restore code.
   *
   * @param f input frame from data flow
   * @return frame with fewer object types
   */
  private static Frame fixFrame(Frame f)
  {
    // fix locals
    LocalVariables lv = f.getLocals().getClone();
    for(int i=0; i<lv.maxLocals(); i++)
    {
      Type t = lv.get(i);
      lv.set(i, fixType(t));
    }
    // fix stack
    OperandStack os = f.getStack().getClone();
    Stack tmp = new Stack();
    while(!os.isEmpty()) tmp.push(fixType(os.pop()));
    while(!tmp.isEmpty()) os.push((Type)tmp.pop());
    return new Frame(lv, os);
  }

  /**
   * Helper method for fixFrame to process individual types
   * within Frame object.
   *
   * @param t input type from data flow frame
   * @return simplified type
   */
  private static Type fixType(Type t)
  {
    switch(t.getType())
    {
      case Constants.T_OBJECT:
        // we don't care about reference types
        if(ReferenceType.class.equals(t.getClass())) return Type.OBJECT;
        return t;
      case Constants.T_BOOLEAN:
      case Constants.T_BYTE:
      case Constants.T_CHAR:
      case Constants.T_SHORT:
      case Constants.T_INT:
      case Constants.T_LONG:
      case Constants.T_FLOAT:
      case Constants.T_DOUBLE:
        return t;
      case Constants.T_ARRAY:
        // rimnote: recurse?
        return t;
      case Constants.T_VOID:
        throw new VerifyError("unexpected type: VOID");
      case Constants.T_ADDRESS:
        throw new VerifyError("cannot store return address in continuation: do you perhaps have a blocking call inside a finally clause?");
      case Constants.T_UNKNOWN:
        if(t instanceof UninitializedObjectType)
        {
          throw new VerifyError("Unable to preserve continuation due to "+t.getSignature()+" on stack. Is your blocking call within a constructor call?");
        }
        return null;
      default:
        throw new VerifyError("unexpected type: "+t.getType());
    }
  }

  /**
   * Returns whether Type is Uninitialized.
   *
   * @param t BCEL type
   * @return whether type is uninitialized
   */
  public static boolean isNotInit(Type t)
  {
    return t.getType()==Constants.T_UNKNOWN && t instanceof UninitializedObjectType;
  }

  /**
   * Generate state class for a specific execution point within a method. The
   * generated classes subclass Event.ContinuationFrame, and have properly
   * types variables called 'local_i' and 'stack_j' for each local variable and
   * stack slot in the given frame. The class is unique names by its method and
   * execution point.
   *
   * @param mg method whose frame is being saved
   * @param pc program location identifier
   * @param f frame to save
   * @return BCEL class object
   */
  private static JavaClass createStateClass(MethodGen mg, int pc, Frame f)
  {
    // create class
    String name = Rewriter.getContinutationClassName(mg, pc);
    ClassGen cg = new ClassGen(name, Event.ContinuationFrame.class.getName(), null, 
        Constants.ACC_PRIVATE | Constants.ACC_FINAL, null);
    ConstantPoolGen cpg = cg.getConstantPool();
    InstructionFactory ifc = new InstructionFactory(cpg);
    // add constructor
    InstructionList il = new InstructionList();
    il.append(ifc.createThis());
    il.append(ifc.createInvoke(Event.ContinuationFrame.class.getName(), 
          Constants.CONSTRUCTOR_NAME, Type.VOID, new Type[] { }, 
          Constants.INVOKESPECIAL));
    il.append(ifc.createReturn(Type.VOID));
    MethodGen mginit = new MethodGen(Constants.ACC_PUBLIC, Type.VOID,
        new Type[] { }, new String[] { },
        Constants.CONSTRUCTOR_NAME,
        name, il, cpg);
    mginit.setMaxStack();
    mginit.setMaxLocals();
    cg.addMethod(mginit.getMethod());
    // add fields for locals
    LocalVariables lv = f.getLocals();
    for(int i=0; i<lv.maxLocals(); i++)
    {
      Type t = lv.get(i);
      if(t==null || t==Type.NULL || isNotInit(t)) continue;
      FieldGen fg = new FieldGen(Constants.ACC_PUBLIC, t, "local_"+i, cpg);
      cg.addField(fg.getField());
    }
    // add fields for stack
    OperandStack os=f.getStack();
    for(int i=0; !os.isEmpty(); i++)
    {
      Type t = os.pop();
      if(t==null || t==Type.NULL || isNotInit(t)) continue;
      FieldGen fg = new FieldGen(Constants.ACC_PUBLIC, t, "stack_"+i, cpg);
      cg.addField(fg.getField());
    }
    return cg.getJavaClass();
  }

  /**
   * Insert instructions to save an execution frame to state object.
   * @param mg method to be saved
   * @param ih instruction handle of save point
   * @param pc program location identifier
   * @param f stack and local types
   * @param ifc class instruction factory
   */
  private static void saveFrame(MethodGen mg, InstructionHandle ih,
      int pc, Frame f, InstructionFactory ifc)
  {
    String stateName = Rewriter.getContinutationClassName(mg, pc);
    Type stateType = new ObjectType(stateName);
    InstructionList pre = new InstructionList();
    int locals = mg.getMaxLocals();

    // ** create and initialize state object
    pre.append(ifc.createNew(stateName));
    // stack: (stuff) state_object
    pre.append(InstructionConstants.DUP);
    // stack: (stuff) state_object state_object
    pre.append(ifc.createInvoke(stateName, Constants.CONSTRUCTOR_NAME, 
          Type.VOID, new Type[] { }, Constants.INVOKESPECIAL));
    // stack: (stuff) state_object
    pre.append(ifc.createStore(stateType, locals));
    // stack: (stuff)

    // ** save pc into state object
    pre.append(ifc.createLoad(stateType, locals));
    // stack: (stuff) state_object
    pre.append(InstructionFactory.DUP);
    // stack: (stuff) state_object state_object
    pre.append(new PUSH(mg.getConstantPool(), pc));
    // stack: (stuff) state_object state_object pc
    pre.append(ifc.createPutField(Event.ContinuationFrame.class.getName(), 
          Event.ContinuationFrame.field_pc.getName(), Type.INT));
    // stack: (stuff) state_object

    // ** store locals into state object
    LocalVariables lv = f.getLocals();
    for(int i=0; i<lv.maxLocals(); i++)
    {
      Type t = lv.get(i);
      if(t==null) continue;
      if(t!=Type.NULL && !isNotInit(t))
      {
        pre.append(InstructionFactory.DUP);
        // stack: (stuff) state_object state_object
        pre.append(ifc.createLoad(t, i));
        // stack: (stuff) state_object state_object local_i
        pre.append(ifc.createPutField(stateName, "local_"+i, t));
        // stack: (stuff) state_object
      }
    }
    // stack: (stuff) state_object

    // ** store stack into state object (and leave untouched)
    OperandStack os = f.getStack().getClone();
    InstructionList loadlist = new InstructionList();
    for(int i=0; !os.isEmpty(); i++)
    {
      Type t = os.pop();
      if(t==null) continue;
      if(t==Type.NULL)
      {
        pre.append(InstructionFactory.SWAP);
        // stack: (stuff-1) state_object null
        pre.append(InstructionFactory.POP);
        // stack: (stuff-1) state_object
        loadlist.insert(InstructionFactory.ACONST_NULL);
      }
      else if(isNotInit(t))
      {
        pre.append(InstructionFactory.SWAP);
        // stack: (stuff-1) state_object uninit
        pre.append(InstructionFactory.POP);
        // stack: (stuff-1) state_object
        loadlist.insert(ifc.createNew(((UninitializedObjectType)t).getInitialized()));
        // rimnote: Technicality... hopefully we don't ever have two
        //   uninitialized stack references to the same object. Should not ever
        //   occur if this is Java code that is compiled, because it's not
        //   possible to refer to the result of an expression within itself,
        //   and locals must be initialized.
      }
      else
      {
        pre.append(InstructionFactory.DUP);
        // stack: (stuff-1) (stuff) state_obj state_obj
        if(t.getSize()==1)
        {
          pre.append(InstructionFactory.DUP2_X1);
        }
        else
        {
          pre.append(InstructionFactory.DUP2_X2);
        }
        // stack: (stuff-1) state_obj state_obj (stuff) state_obj state_obj
        pre.append(InstructionFactory.POP);
        pre.append(InstructionFactory.POP);
        // stack: (stuff-1) state_obj state_obj (stuff)
        pre.append(ifc.createPutField(stateName, "stack_"+i, t));
        // stack: (stuff-1) state_obj

        InstructionHandle ihload = 
          loadlist.insert(ifc.createLoad(stateType, locals));
        loadlist.append(ihload, ifc.createGetField(stateName, "stack_"+i, t));
      }
    }
    pre.append(InstructionFactory.POP);
    // stack: (empty)
    pre.append(loadlist);
    // stack: (stuff)

    InstructionList post = new InstructionList();

    // ** if(Controller.isCallSave())
    post.append(ifc.createInvoke(
          Controller.method_isModeSave.getDeclaringClass().getName(), 
          Controller.method_isModeSave.getName(),
          Rewriter.getType(Controller.method_isModeSave.getReturnType()),
          Rewriter.getTypes(Controller.method_isModeSave.getParameterTypes()),
          Constants.INVOKESTATIC));
    // stack: (stuff) true
    post.append(ifc.createBranchInstruction(Constants.IFEQ, ih.getNext()));
    // stack: (stuff)

    // ** pushStateFrame(state_obj)
    post.append(ifc.createLoad(Type.OBJECT, locals));
    // stack: (post-call stuff) state_obj
    post.append(ifc.createInvoke(
          Controller.method_pushStateOutFrame.getDeclaringClass().getName(), 
          Controller.method_pushStateOutFrame.getName(),
          Rewriter.getType(Controller.method_pushStateOutFrame.getReturnType()),
          Rewriter.getTypes(Controller.method_pushStateOutFrame.getParameterTypes()),
          Constants.INVOKESTATIC));
    // stack: (post-call stuff)

    // ** return
    Type retType = mg.getReturnType();
    ConstantPoolGen cpg = mg.getConstantPool();
    switch(retType.getType())
    {
      case Constants.T_VOID:
        post.append(InstructionFactory.RETURN);
        break;
      case Constants.T_BOOLEAN:
        post.append(new PUSH(cpg, true));
        post.append(ifc.createReturn(retType));
        break;
      case Constants.T_BYTE:
      case Constants.T_CHAR:
      case Constants.T_SHORT:
      case Constants.T_INT:
        post.append(new PUSH(cpg, 0));
        post.append(ifc.createReturn(retType));
        break;
      case Constants.T_LONG:
        post.append(new PUSH(cpg, (long)0));
        post.append(ifc.createReturn(retType));
        break;
      case Constants.T_FLOAT:
        post.append(new PUSH(cpg, (float)0));
        post.append(ifc.createReturn(retType));
        break;
      case Constants.T_DOUBLE:
        post.append(new PUSH(cpg, (double)0));
        post.append(ifc.createReturn(retType));
        break;
      case Constants.T_OBJECT:
      case Constants.T_ARRAY:
        post.append(InstructionFactory.ACONST_NULL);
        post.append(ifc.createReturn(retType));
        break;
      default:
        throw new RuntimeException("unexpected return type");
    }

    // modify instruction list
    InstructionHandle newTop = mg.getInstructionList().insert(ih, pre);
    mg.getInstructionList().append(ih, post);
    if(ih.hasTargeters())
    {
      InstructionTargeter[] targeters = ih.getTargeters();
      for(int i=0; i<targeters.length; i++)
      {
        targeters[i].updateTarget(ih, newTop);
      }
    }

    // todo: adjust exception handling regions
  }

  /**
   * Insert instructions to restore an execution frame from a state object.
   * @param mg method to be restored onto stack
   * @param frames frames of restoration points
   * @param ihs instruction handles of restoration points
   * @param ifc class instruction factory
   */
  private static void restoreFrame(MethodGen mg, Vector frames, Vector ihs, InstructionFactory ifc)
  {
    InstructionList il = mg.getInstructionList();
    InstructionHandle start = il.getStart();
    int locals = mg.getMaxLocals();

    // ** if(Controller.isCallbackRestore())
    il.insert(start, ifc.createInvoke(
          Controller.method_isModeRestore.getDeclaringClass().getName(), 
          Controller.method_isModeRestore.getName(),
          Rewriter.getType(Controller.method_isModeRestore.getReturnType()),
          Rewriter.getTypes(Controller.method_isModeRestore.getParameterTypes()),
          Constants.INVOKESTATIC));
    // stack: boolean
    il.insert(start, ifc.createBranchInstruction(Constants.IFEQ, start));
    // stack: (empty)

    // ** pop stack frame
    il.insert(start, ifc.createInvoke(
          Controller.method_popStateInFrame.getDeclaringClass().getName(), 
          Controller.method_popStateInFrame.getName(),
          Rewriter.getType(Controller.method_popStateInFrame.getReturnType()),
          Rewriter.getTypes(Controller.method_popStateInFrame.getParameterTypes()),
          Constants.INVOKESTATIC));
    // stack: popStackFrame()
    il.insert(start, ifc.createStore(
          Rewriter.getType(Controller.method_popStateInFrame.getReturnType()), locals));
    // stack: (empty)

    // ** switch(popStackFrame()pc)
    il.insert(start, ifc.createLoad(
          Rewriter.getType(Controller.method_popStateInFrame.getReturnType()), locals));
    // stack: popStackFrame()
    il.insert(start, InstructionFactory.DUP);
    // stack: popStackFrame() popStackFrame()
    il.insert(start, ifc.createGetField(
          Event.ContinuationFrame.class.getName(), 
          Event.ContinuationFrame.field_pc.getName(),
          Rewriter.getType(Event.ContinuationFrame.field_pc.getType())));
    // stack: popStackFrame() popStackFrame().pc
    Select switchPc = new TABLESWITCH(
        Util.getRange(frames.size()), new InstructionHandle[frames.size()], null);
    // stack: popStackFrame()
    il.insert(start, switchPc);

    // ** case: bad pc
    InstructionHandle ihBadpc = 
      il.insert(start, ifc.createNew(RuntimeException.class.getName()));
    il.insert(start, InstructionConstants.DUP);
    il.insert(start, new PUSH(mg.getConstantPool(), "invalid pc value"));
    il.insert(start, ifc.createInvoke(
          RuntimeException.class.getName(), Constants.CONSTRUCTOR_NAME,
          Type.VOID, new Type[] { Type.STRING }, Constants.INVOKESPECIAL));
    il.insert(start, new ATHROW());
    switchPc.setTarget(ihBadpc);
    switchPc.setTarget(0, ihBadpc);

    // ** case: good pc
    for(int pc=1; pc<frames.size(); pc++)
    {
      String stateName = Rewriter.getContinutationClassName(mg, pc);
      ObjectType stateType = new ObjectType(stateName);
      InstructionHandle ihGoodpci = 
        il.insert(start, ifc.createCheckCast(stateType));
      // stack: (pc_i)popStackFrame()
      switchPc.setTarget(pc, ihGoodpci);
      Frame f = (Frame)frames.elementAt(pc);

      // ** restore locals
      LocalVariables lv = f.getLocals();
      for(int i=0; i<lv.maxLocals(); i++)
      {
        Type t = lv.get(i);
        if(t==null) continue;
        if(t==Type.NULL)
        {
          il.insert(start, InstructionConstants.ACONST_NULL);
          // stack: (pc_i)popStackFrame(), null
        }
        else if(isNotInit(t))
        {
          il.insert(start, ifc.createNew(((UninitializedObjectType)t).getInitialized()));
          // stack: (pc_i)popStackFrame(), uninit
        }
        else
        {
          il.insert(start, InstructionConstants.DUP);
          // stack: (pc_i)popStackFrame(), (pc_i)popStackFrame()
          il.insert(start, ifc.createGetField(stateName, "local_"+i, t));
          // stack: (pc_i)popStackFrame(), popStackFrame().local_i
        }
        il.insert(start, ifc.createStore(t, i));
        // stack: (pc_i)popStackFrame()
      }

      // ** restore stack
      OperandStack os = f.getStack().getClone();
      Stack os2 = new Stack();
      while(!os.isEmpty()) os2.push(os.pop());
      for(int i=os2.size()-1; !os2.isEmpty(); i--)
      {
        Type t = (Type)os2.pop();
        if(t==null) continue;
        if(t==Type.NULL)
        {
          il.insert(start, InstructionFactory.ACONST_NULL);
          // stack: (pc_i)popStackFrame(), null
          il.insert(start, InstructionFactory.SWAP);
          // stack: null (pc_i)popStackFrame()
        }
        else if(isNotInit(t))
        {
          il.insert(start, ifc.createNew(((UninitializedObjectType)t).getInitialized()));
          // stack: (pc_i)popStackFrame(), uninit
        }
        else
        {
          il.insert(start, InstructionFactory.DUP);
          // stack: (pc_i)popStackFrame(), (pc_i)popStackFrame()
          il.insert(start, ifc.createGetField(stateName, "stack_"+i, t));
          // stack: (pc_i)popStackFrame(), popStackFrame().stack_i
          if(t.getSize()==1)
          {
            il.insert(start, InstructionFactory.SWAP);
          }
          else
          {
            il.insert(start, InstructionFactory.DUP2_X1);
            il.insert(start, InstructionFactory.POP2);
          }
          // stack: popStackFrame().stack_i, (pc_i)popStackFrame()
        }
      }
      // stack: (stuff) (pc_i)popStackFrame()
      il.insert(start, ifc.createStore(stateType, locals));
      // stack: (stuff)

      // ** jump back to continuation location
      il.insert(start, new GOTO((InstructionHandle)ihs.elementAt(pc)));
    }
  }

} // class: RewriterTraversalContinuableMethods


/**
 * Traversal object that removes NOP instructions.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @since JIST1.0
 * @see JistAPI
 */

class RewriterTraversalRemoveNop extends ClassTraversal.Empty
{
  /** {@inheritDoc} */
  public void doInstruction(ClassGen cg, MethodGen mg, InstructionHandle ih, Instruction inst)
  {
    if(inst instanceof NOP)
    {
      try
      {
        mg.getInstructionList().delete(ih);
      }
      catch(TargetLostException e)
      {
        InstructionHandle[] targets = e.getTargets();
        for(int i=0; i < targets.length; i++) 
        {
          InstructionTargeter[] targeters = targets[i].getTargeters();
          for(int j=0; j < targeters.length; j++)
          {
            targeters[j].updateTarget(targets[i], ih.getNext());
          }
        }
      }
    }
  }
}

