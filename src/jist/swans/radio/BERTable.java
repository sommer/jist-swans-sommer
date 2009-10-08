//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <BERTable.java Sun 2005/03/13 11:08:33 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.radio;

import jist.swans.misc.Util;
import jist.swans.Constants;
import jist.swans.Main;

import java.io.*;
import java.util.*;

/**
 * Implementation of Bit-Error-Rate calculations (via a loaded table) for
 * a range of Signal-to-Noise-Ratio values.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: BERTable.java,v 1.9 2005-03-13 16:11:55 barr Exp $
 * @since SWANS1.0
 */

public class BERTable
{
  /**
   * Table with Bit-Error-Rate values for given Signal-to-Noise-Ratio values.
   */
  private double[] snr, ber;

  /**
   * Initialize the BER table with data from given file.
   *
   * @param filename file to load
   * @throws IOException unable to read BER file
   */
  public BERTable(String filename) throws IOException
  {
    load(new File(filename));
    check();
  }

  /**
   * Load BER data from a given file. BER data should be formatted as a plain
   * text file with floating point numbers in two columns (SNR BER). The SNR
   * values should be ascending.
   *
   * @param f BER data file
   * @throws IOException unable to read BER file
   */
  private void load(File f) throws IOException
  {
    // parse lines
    String[] lines = jist.swans.misc.Util.readLines(f);
    snr = new double[lines.length];
    ber = new double[lines.length];
    for(int i=0; i<lines.length; i++)
    {
      // read snr, ber pair from each line
      StringTokenizer st = new StringTokenizer(lines[i]);
      snr[i] = Double.parseDouble(st.nextToken());
      ber[i] = Double.parseDouble(st.nextToken());
    }
  }

  /**
   * Verify the input data. Check that SNR values were in ascending order.
   */
  private void check()
  {
    // todo: check for ascending snr values
  }

  /**
   * Compute BER value by interpolating among existing SNR points.
   *
   * @param snrVal input SNR value for BER computation (interpolation)
   * @return Bit-Error-Rate
   */
  public double calc(double snrVal)
  {
    // if snr larger than largest value return zero bit-error rate.
    if(snrVal>snr[snr.length-1]) return 0;
    // snr[i1] < snrVal < snr[i2]
    int i1 = 0;
    int i2 = snr.length-1;
    // binary range-search
    while(i2 - i1 > 1)
    {
      int i3 = (i1+i2)/2;
      if(snrVal > snr[i3])
        i1 = i3;
      else 
        i2 = i3;
    }
    // linear interpolation
    return ber[i1] + (ber[i2]-ber[i1]) * (snrVal-snr[i1]) / (snr[i2]-snr[i1]);
  }

  /**
   * Compute probabilistically whether an error occured for a given number of
   * bits and SNR value.
   *
   * @param snrVal Signal-to-Noise-Ratio value
   * @param bits number of bits
   * @return whether (probabilistically) an error occurred
   */
  public boolean shouldDrop(double snrVal, int bits)
  {
    double ber = calc(snrVal);
    if(Main.ASSERT) Util.assertion(ber>=0.0 && ber<=1.0);
    if(ber<=0) return false;
    double error = 1.0 - Math.pow((1.0-ber), bits);
    if(Main.ASSERT) Util.assertion(error>=0.0 && error<=1.0);
    return error>Constants.random.nextDouble();
  }

} // class: BERTable
