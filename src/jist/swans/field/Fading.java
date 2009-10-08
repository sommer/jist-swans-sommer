//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Fading.java Wed 2004/06/23 09:16:53 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.field;

import jist.swans.Constants;

/** 
 * Interface for performing fading calculations.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Fading.java,v 1.10 2004-06-23 17:15:54 barr Exp $
 * @since SWANS1.0
 */

public interface Fading
{
  //////////////////////////////////////////////////
  // interface
  //

  /**
   * Compute the fading loss.
   * 
   * @return fading loss (units: dB)
   */
  double compute();

  //////////////////////////////////////////////////
  // implementations
  //

  /** 
   * Computes zero fading.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  final class None implements Fading
  {
    // Fading interface
    /** {@inheritDoc} */
    public double compute()
    {
      return 0.0;
    }
  }

  /** 
   * Computes Rayleigh fading. Equivalent to GloMoSim code.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  final class Rayleigh implements Fading
  {
    /** Rayleigh distribution variance constant. */
    private static final double VARIANCE = 0.6366197723676;

    // Fading interface
    /** {@inheritDoc} */
    public double compute()
    {
      // compute fading_dB; positive values are signal gains
      return 5.0 * Math.log(-2.0 * VARIANCE * Math.log(Constants.random.nextDouble())) / Constants.log10;
    }
  }

  /** 
   * Computes Rician fading. Equivalent to GloMoSim code.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  final class Rician implements Fading
  {
    /** distribution parameters. */
    private final double kFactor, stddev;

    /**
     * Create new Rician fading model object.
     *
     * @param kFactor k
     */
    public Rician(double kFactor)
    {
      this.kFactor = kFactor;
      this.stddev = computeStandardDeviation(kFactor);
    }

    /**
     * Compute zero-order Bessel function.
     *
     * @param x input
     * @return output of Bessel
     */
    private static double Besseli0(double x) 
    {
      double ax = Math.abs(x);
      if (ax < 3.75) 
      {
        double y = x/3.75;
        y *= y;
        return 1.0 + y*(3.5156229 + y*(3.0899424 + y*(1.2067492 +
          y*(0.2659732 + y*(0.360768e-1 + y*0.45813e-2)))));
      }
      else
      {
        double y = 3.75/ax;
        return (Math.exp(ax)/Math.sqrt(ax)) * (0.39894228 + y*(0.1328592e-1 + 
          y*(0.225319e-2 + y*(-0.157565e-2 + y*(0.916281e-2 + y*(-0.2057706e-1 + 
          y*(0.2635537e-1 + y*(-0.1647633e-1 + y*0.392377e-2))))))));
      }
    }

    /**
     * Compute first-order Bessel function.
     *
     * @param x input
     * @return output of Bessel
     */
    private static double Besseli1(double x) 
    {
      double ax = Math.abs(x);
      if (ax < 3.75)
      {
        double y = x/3.75;
        y *= y;
        return x * (0.5 + y*(0.87890494 + y*(0.51498869 + y*(0.15084934 + y*(0.2658733e-1 + 
          y*(0.301532e-2 + y*0.32411e-3))))));
      }
      else
      {
        double y = 3.75/ax;
        return Math.abs((Math.exp(ax)/Math.sqrt(ax)) * (0.39894228 + y*(-0.3988024e-1 + 
          y*(-0.362018e-2 + y*(0.163801e-2 + y*(-0.1031555e-1 + y*(0.2282967e-1 + 
          y*(-0.2895312e-1 + y*(0.1787654e-1 - y*0.420059e-2)))))))));
      }
    }

    /** 
     * Computes standard deviation for Rician distribution such that mean is 1.
     *
     * @param kFactor k
     * @return Rician standard deviation
     */
    private static double computeStandardDeviation(double kFactor)
    {
      return 1.0/(Math.sqrt(Math.PI/2.0) * Math.exp(-kFactor/2.0) * 
        ((1+kFactor)*Besseli0(kFactor/2.0) + kFactor*Besseli1(kFactor/2.0)));
    }

    // Fading interface
    /** {@inheritDoc} */
    public double compute()
    {
      // compute fading_dB; positive values are signal gains
      double a = Math.sqrt(2.0 * kFactor * stddev * stddev), r, v1, v2;
      do 
      {
        v1 = -1.0 + 2.0 * Constants.random.nextDouble();
        v2 = -1.0 + 2.0 * Constants.random.nextDouble();
        r = v1 * v1 + v2 * v2;
      }
      while (r > 1.0);
      r = Math.sqrt(-2.0 * Math.log(r) / r);
      v1 = a + stddev * v1 * r;
      v2 = stddev * v2 * r;
      return 5.0 * Math.log(v1*v1 + v2*v2) / Constants.log10;
    }
  }

} // class: Fading

