//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <RadioNoiseAdditive.java Tue 2004/04/13 18:16:53 barr glenlivet.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.radio;

import jist.swans.misc.Message;
import jist.swans.misc.Util;

import jist.swans.Constants;
import jist.swans.Main;

import jist.runtime.JistAPI;

/** 
 * <code>RadioNoiseAdditive</code> implements a radio with an additive noise model.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&rt;
 * @version $Id: RadioNoiseAdditive.java,v 1.26 2004-11-19 15:55:34 barr Exp $
 * @since SWANS1.0
 */

public class RadioNoiseAdditive extends RadioNoise
{
  //////////////////////////////////////////////////
  // constants
  //

  /** signal-to-noise error model constant. */
  public static final byte SNR = 0;

  /** bit-error-rate error model constant. */
  public static final byte BER = 1;

  //////////////////////////////////////////////////
  // locals
  //

  //
  // properties
  //

  /**
   * radio type: SNR or BER.
   */
  protected byte type;

  /**
   * threshold signal-to-noise ratio.
   */
  protected float thresholdSNR;

  /**
   * bit-error-rate table.
   */
  protected BERTable ber;

  //
  // state
  //

  /**
   * total signal power.
   */
  protected double totalPower_mW;

  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Create new radio with additive noise model.
   *
   * @param id radio identifier
   * @param shared shared radio properties
   */
  public RadioNoiseAdditive(int id, RadioInfo.RadioInfoShared shared)
  {
    this(id, shared, (float)Constants.SNR_THRESHOLD_DEFAULT);
  }

  /**
   * Create a new radio with additive noise model.
   *
   * @param id radio identifier
   * @param shared shared radio properties
   * @param snrThreshold_mW threshold signal-to-noise ratio
   */
  public RadioNoiseAdditive(int id, RadioInfo.RadioInfoShared shared, float snrThreshold_mW)
  {
    super(id, shared);
    this.type = SNR;
    this.thresholdSNR = snrThreshold_mW;
    totalPower_mW = radioInfo.shared.background_mW;
    if(totalPower_mW > radioInfo.shared.sensitivity_mW) mode = Constants.RADIO_MODE_SENSING;
  }

  /**
   * Create a new radio with additive noise model.
   *
   * @param id radio identifier
   * @param shared shared radio properties
   * @param ber bit-error-rate table
   */
  public RadioNoiseAdditive(int id, RadioInfo.RadioInfoShared shared, BERTable ber)
  {
    super(id, shared);
    this.type = BER;
    this.ber = ber;
    totalPower_mW = radioInfo.shared.background_mW;
    if(totalPower_mW > radioInfo.shared.sensitivity_mW) mode = Constants.RADIO_MODE_SENSING;
  }

  //////////////////////////////////////////////////
  // accessors
  //

  /**
   * Register a bit-error-rate table.
   *
   * @param ber bit-error-rate table
   */
  public void setBERTable(BERTable ber)
  {
    this.ber = ber;
  }


  //////////////////////////////////////////////////
  // reception
  //

  // RadioInterface interface
  /** {@inheritDoc} */
  public void receive(final Message msg, final Double powerObj_mW, final Long durationObj)
  {
    final double power_mW = powerObj_mW.doubleValue();
    final long duration = durationObj.longValue();
    switch(mode)
    {
      case Constants.RADIO_MODE_IDLE:
        if(power_mW >= radioInfo.shared.threshold_mW
            &&  power_mW >= totalPower_mW*thresholdSNR)
        {
          lockSignal(msg, power_mW, duration);
          setMode(Constants.RADIO_MODE_RECEIVING);
        }
        else if(totalPower_mW+power_mW > radioInfo.shared.sensitivity_mW)
        {
          setMode(Constants.RADIO_MODE_SENSING);
        }
        break;
      case Constants.RADIO_MODE_SENSING:
        if(power_mW >= radioInfo.shared.threshold_mW
            &&  power_mW >= totalPower_mW*thresholdSNR)
        {
          lockSignal(msg, power_mW, duration);
          setMode(Constants.RADIO_MODE_RECEIVING);
        }
        break;
      case Constants.RADIO_MODE_RECEIVING:
        if(power_mW > signalPower_mW  &&  power_mW >= totalPower_mW*thresholdSNR)
        {
          lockSignal(msg, power_mW, duration);
          setMode(Constants.RADIO_MODE_RECEIVING);
        }
        else if(type == SNR  
            &&  signalPower_mW < (totalPower_mW-signalPower_mW+power_mW)*thresholdSNR)
        {
          unlockSignal();
          setMode(Constants.RADIO_MODE_SENSING);
        }
        break;
      case Constants.RADIO_MODE_TRANSMITTING:
        break;
      case Constants.RADIO_MODE_SLEEP:
        break;
      default:
        throw new RuntimeException("unknown radio mode");
    }
    // cumulative signal
    signals++;
    totalPower_mW += power_mW;
    // schedule an endReceive
    JistAPI.sleep(duration); 
    self.endReceive(powerObj_mW);
  } // function: receive

  // RadioInterface interface
  /** {@inheritDoc} */
  public void endReceive(Double powerObj_mW)
  {
    final double power_mW = powerObj_mW.doubleValue();
    // cumulative signal
    signals--;
    if(Main.ASSERT) Util.assertion(signals>=0);
    totalPower_mW = signals==0 
      ? radioInfo.shared.background_mW 
      : totalPower_mW-power_mW;
    switch(mode)
    {
      case Constants.RADIO_MODE_RECEIVING:
        if(JistAPI.getTime()==signalFinish)
        {
          boolean dropped = false;
          dropped |= type == BER  && totalPower_mW>0 &&
              ber.shouldDrop(signalPower_mW/totalPower_mW, 
                  8*signalBuffer.getSize());
          if(!dropped)
          {
            this.macEntity.receive(signalBuffer);
          }
          unlockSignal();
          setMode(totalPower_mW>=radioInfo.shared.sensitivity_mW
              ? Constants.RADIO_MODE_SENSING 
              : Constants.RADIO_MODE_IDLE);
        }
        break;
      case Constants.RADIO_MODE_SENSING:
        if(totalPower_mW<radioInfo.shared.sensitivity_mW) setMode(Constants.RADIO_MODE_IDLE);
        break;
      case Constants.RADIO_MODE_TRANSMITTING:
        break;
      case Constants.RADIO_MODE_IDLE:
        break;
      case Constants.RADIO_MODE_SLEEP:
        break;
      default:
        throw new RuntimeException("unknown radio mode");
    }
  } // function: endReceive

} // class: RadioNoiseAdditive

