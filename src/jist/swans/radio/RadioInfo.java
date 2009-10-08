//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RadioInfo.java Tue 2004/04/06 11:31:32 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.radio;

import jist.swans.Constants;

import jist.runtime.JistAPI;

/** 
 * Radio properties.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: RadioInfo.java,v 1.18 2004-04-06 16:07:50 barr Exp $
 * @since SWANS1.0
 */

public class RadioInfo implements JistAPI.Timeless
{
  /**
   * Timeless information unique to this Radio instance.
   */
  protected RadioInfoUnique unique;

  /**
   * Timeless information possibly shared among numerous Radio
   * instances (only to save simulation memory).
   */
  protected RadioInfoShared shared;


  /**
   * Timeless information unique to a single Radio instance.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  public static class RadioInfoUnique implements JistAPI.Timeless
  {
    /**
     * Unique radio identifier.
     */
    protected Integer id;

    /**
     * Return radio identifier.
     *
     * @return radio identifier
     */
    public Integer getID()
    {
      return id;
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return "id="+id;
    }

  } // class: RadioInfoUnique


  /**
   * Timeless information possibly shared among numerous Radio instances (only
   * to save simulation memory.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  public static class RadioInfoShared implements JistAPI.Timeless
  {
    /**
     * Wavelength of radio (units: meter).
     */
    protected double wavelength;

    /**
     * Bandwidth (units: bits/second).
     */
    protected int bandwidth;

    /**
     * Transmission power (units: dBm).
     */
    protected double transmit;

    /**
     * Antenna gain (units: dBm).
     */
    protected double gain;

    /**
     * Reception sensitivity (units: mW).
     */
    protected double sensitivity_mW;

    /**
     * Reception threshold (units: mW).
     */
    protected double threshold_mW;

    /**
     * Background noise, including bandwidth factor
     *   (units: mW * bits/second).
     */
    protected double background_mW;

    /**
     * Return radio wavelength.
     *
     * @return wavelength (units: meter)
     */
    public double getWaveLength()
    {
      return wavelength;
    }

    /**
     * Return radio bandwidth.
     *
     * @return bandwidth (units: bits/second)
     */
    public int getBandwidth()
    {
      return bandwidth;
    }

    /**
     * Return radio transmission power.
     *
     * @return transmission power (units: dBm)
     */
    public double getPower()
    {
      return transmit;
    }

    /**
     * Return antenna gain.
     *
     * @return antenna gain (units: dBm)
     */
    public double getGain()
    {
      return gain;
    }

    /**
     * Return reception sensitivity.
     *
     * @return reception sensitivity (units: mW)
     */
    public double getSensitivity_mW()
    {
      return sensitivity_mW;
    }

    /**
     * Return reception threshold.
     *
     * @return reception threshold (units: mW)
     */
    public double getThreshold_mW()
    {
      return threshold_mW;
    }

    /**
     * Return background noise.
     *
     * @return background noise (units: mW)
     */
    public double getBackground_mW()
    {
      return background_mW;
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return null;
    }

  } // class: RadioInfoShared


  /**
   * Create radio information object with shared and unique properties.
   *
   * @param unique unique radio properties
   * @param shared shared radio properties (shared only to save some memory)
   */
  public RadioInfo(RadioInfoUnique unique, RadioInfoShared shared)
  {
    this.unique = unique;
    this.shared = shared;
  }

  /**
   * Return unique radio properties.
   *
   * @return unique radio properties
   */
  public RadioInfoUnique getUnique()
  {
    return unique;
  }

  /**
   * Return shared radio properties.
   *
   * @return shared radio properties
   */
  public RadioInfoShared getShared()
  {
    return shared;
  }

  /** {@inheritDoc} */
  public String toString()
  {
    return unique+" "+shared;
  }

  /**
   * Create shared radio parameters.
   *
   * @param frequency radio frequency (units: Hertz)
   * @param bandwidth bandwidth (units: bits/second)
   *
   * @param transmit transmission power (units: dBm)
   * @param gain antenna gain (units: dB)
   *
   * @param sensitivity_mW receive sensivity (units: mW)
   * @param threshold_mW receive threshold (units: mW)
   *
   * @param temperature field temperature (units: degrees Kelvin)
   * @param thermalFactor thermal noise
   * @param ambientNoise_mW ambient noise (units: mW)
   *
   * @return shared radio information object
   */
  public static RadioInfoShared createShared(double frequency, int bandwidth, 
      double transmit, double gain, double sensitivity_mW, double threshold_mW, 
      double temperature, double thermalFactor, double ambientNoise_mW)
  {
    RadioInfoShared shared = new RadioInfoShared();
    // wavelength
    shared.wavelength = Constants.SPEED_OF_LIGHT / frequency;
    // bandwidth
    shared.bandwidth = bandwidth;
    // transmit
    shared.transmit = transmit;
    shared.gain = gain;
    // receive
    shared.sensitivity_mW = sensitivity_mW;
    shared.threshold_mW = threshold_mW;
    // noise
    double thermalNoise_mW = Constants.BOLTZMANN * temperature * thermalFactor * 1000.0;
    shared.background_mW = (ambientNoise_mW + thermalNoise_mW) * bandwidth;
    return shared;
  }

} // class: RadioInfo

