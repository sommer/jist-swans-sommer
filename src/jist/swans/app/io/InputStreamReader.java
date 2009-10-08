//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <InputStreamReader.java Tue 2004/04/06 11:45:02 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app.io;

import jist.runtime.JistAPI;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


/**
 * A functionally identical port of java.io.InputStreamReader, primarily
 * brought into jist.swans.app.io package so that it could be dynamically
 * rewritten.
 *
 * @author Sun Microsystems
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: InputStreamReader.java,v 1.4 2004-04-06 16:07:46 barr Exp $
 * @since SWANS1.0
 */

public class InputStreamReader extends java.io.InputStreamReader 
{

  /** unicode to ascii decoder. */
  private final sun.nio.cs.StreamDecoder sd;

  /** @see java.io.InputStreamReader */
  public InputStreamReader(InputStream in) 
  {
    //super(in);
    super(in);
    try 
    {
      sd = sun.nio.cs.StreamDecoder.forInputStreamReader(in, this, (String)null); // ## check lock object
    } 
    catch (UnsupportedEncodingException e) 
    {
      // The default encoding should always be available
      throw new Error(e);
    }
  }

  /** @see java.io.InputStreamReader */
  public InputStreamReader(InputStream in, String charsetName)
    throws UnsupportedEncodingException
  {
    //super(in);
    super(in, charsetName);
    if (charsetName == null)
      throw new NullPointerException("charsetName");
    sd = sun.nio.cs.StreamDecoder.forInputStreamReader(in, this, charsetName);
  }

  /** @see java.io.InputStreamReader */
  public InputStreamReader(InputStream in, Charset cs) 
  {
    //super(in);
    super(in, cs);
    if (cs == null)
      throw new NullPointerException("charset");
    sd = sun.nio.cs.StreamDecoder.forInputStreamReader(in, this, cs);
  }

  /** @see java.io.InputStreamReader */
  public InputStreamReader(InputStream in, CharsetDecoder dec) 
  {
    //super(in);
    super(in, dec);
    if (dec == null)
      throw new NullPointerException("charset decoder");
    sd = sun.nio.cs.StreamDecoder.forInputStreamReader(in, this, dec);
  }

  /** @see java.io.InputStreamReader */
  public String getEncoding() 
  {
    return sd.getEncoding();
  }

  /** @see java.io.InputStreamReader */
  public int read() throws IOException, JistAPI.Continuable 
  {
    return sd.read();
  }

  /** @see java.io.InputStreamReader */
  public int read(char[] cbuf, int offset, int length) throws IOException, JistAPI.Continuable 
  {
    return sd.read(cbuf, offset, length);
  }

  /** @see java.io.InputStreamReader */
  public boolean ready() throws IOException 
  {
    return sd.ready();
  }

  /** @see java.io.InputStreamReader */
  public void close() throws IOException 
  {
    sd.close();
  }

} // class: InputStreamReader

