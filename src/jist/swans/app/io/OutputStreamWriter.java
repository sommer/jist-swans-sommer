//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <OutputStreamWriter.java Tue 2004/04/06 11:45:20 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app.io;

// import java.nio.charset.Charset;
// import java.nio.charset.CharsetEncoder;
// import sun.nio.cs.StreamEncoder;
// import java.io.IOException;
// import java.io.UnsupportedEncodingException;

/**
 * A functionally identical port of java.io.OutputStreamWriter, primarily
 * brought into jist.swans.app.io package so that it could be dynamically
 * rewritten.
 *
 * @author Sun Microsystems
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: OutputStreamWriter.java,v 1.4 2004-04-06 16:07:46 barr Exp $
 * @since SWANS1.0
 */

public class OutputStreamWriter //extends java.io.OutputStreamWriter 
{
/*

  private final StreamEncoder se;

  public OutputStreamWriter(OutputStream out, String charsetName)
    throws UnsupportedEncodingException
  {
    //super(out);
    super(out, charsetName);
    if (charsetName == null)
      throw new NullPointerException("charsetName");
    se = StreamEncoder.forOutputStreamWriter(out, this, charsetName);
  }

  public OutputStreamWriter(OutputStream out) {
    //super(out);
    super(out);
    try {
      se = StreamEncoder.forOutputStreamWriter(out, this, (String)null);
    } catch (UnsupportedEncodingException e) {
      throw new Error(e);
    }
  }

  public OutputStreamWriter(OutputStream out, Charset cs) {
    //super(out);
    super(out, cs);
    if (cs == null)
      throw new NullPointerException("charset");
    se = StreamEncoder.forOutputStreamWriter(out, this, cs);
  }

  public OutputStreamWriter(OutputStream out, CharsetEncoder enc) {
    //super(out);
    super(out, enc);
    if (enc == null)
      throw new NullPointerException("charset encoder");
    se = StreamEncoder.forOutputStreamWriter(out, this, enc);
  }

  public String getEncoding() {
    return se.getEncoding();
  }

  void flushBuffer() throws IOException, JistAPI.Continuable {
    se.flushBuffer();
  }

  public void write(int c) throws IOException, JistAPI.Continuable {
    se.write(c);
  }

  public void write(char cbuf[], int off, int len) throws IOException, JistAPI.Continuable {
    se.write(cbuf, off, len);
  }

  public void write(String str, int off, int len) throws IOException, JistAPI.Continuable {
    se.write(str, off, len);
  }

  public void flush() throws IOException, JistAPI.Continuable {
    se.flush();
  }

  public void close() throws IOException {
    se.close();
  }

*/
}
