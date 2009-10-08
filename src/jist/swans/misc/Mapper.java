//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Mapper.java Tue 2004/04/06 11:46:30 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.misc;

/**
 * Encodes a one-to-one mapping. Used, for example, to dynamically assign port
 * numbers consistently, but in a smaller index range, in order to reduce
 * array sizes.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Mapper.java,v 1.8 2004-04-06 16:07:48 barr Exp $
 * @since SWANS1.0
 */

public final class Mapper
{

  /**
   * Invalid mapping constant.
   */
  public static final int MAP_INVALID = -1;

  /**
   * Forward mapping table.
   */
  private int[] mapTo;
  
  /**
   * Reverse mapping table.
   */
  private int[] mapFrom;

  /**
   * Mapped (range) limit.
   */
  private int limit;

  /**
   * Whether this mapping is sealed.
   */
  private boolean sealed;

  /**
   * Create a mapping with a given index cap.
   *
   * @param max map indices should be non-negative and less than max.
   */
  public Mapper(int max)
  {
    init(max);
  }

  /**
   * Create a sealed mapping of the provided values.
   *
   * @param values array of domain values to map.
   */
  public Mapper(int[] values)
  {
    int max = values[0];
    for(int i=1; i<values.length; i++)
    {
      max = Math.max(max, values[i]);
    }
    init(max);
    for(int i=0; i<values.length; i++)
    {
      mapToNext(values[i]);
    }
    seal();
  }

  /**
   * Private constructor helper method.
   *
   * @param max map indices should be non-negative and less than max.
   */
  private void init(int max)
  {
    mapTo = new int[max+1];
    mapFrom = new int[max+1];
    for(int i=0; i<mapTo.length; i++)
    {
      mapTo[i] = MAP_INVALID;
      mapFrom[i] = MAP_INVALID;
    }
    limit = 0;
    sealed = false;
  }

  /**
   * Seal the current mapping; prevent further changes.
   */
  public void seal()
  {
    sealed = true;
  }

  /**
   * Define a mapping. Map i onto j.
   *
   * @param i integer in domain of mapping
   * @param j integer in range of mapping
   * @return j
   */
  private int map(int i, int j)
  {
    if(sealed) throw new RuntimeException("mapping sealed");
    mapTo[i] = j;
    mapFrom[j] = i;
    limit = Math.max(limit, j);
    limit++;
    return j;
  }

  /**
   * Define a mapping. Map i onto one more than the highest value in the range
   * of the mapping.
   *
   * @param i integer in the domain of mapping
   * @return selected value in the range of the mapping
   */
  public int mapToNext(int i)
  {
    int j = limit;
    map(i, j);
    return j;
  }

  /**
   * Define a mapping if one does not already exist. Map i onto one more than
   * the highest value in the range of the mapping, unless it is already
   * mapped.
   *
   * @param i integer in the domain of mapping
   * @return new or corresponding integer in the range of the mapping
   */
  public int testMapToNext(int i)
  {
    int j = mapTo[i];
    return j==MAP_INVALID ? mapToNext(i) : j;
  }

  /**
   * Return element in the range of the mapping.
   * 
   * @param i integer in the domain of the mapping
   * @return corresponding integer in the range of the mapping
   */
  public int getMap(int i)
  {
    return mapTo[i];
  }

  /**
   * Return element in the domain of the mapping.
   *
   * @param j integer in the range of the mapping
   * @return corresponding integer in the domain of the mapping
   */
  public int getMapR(int j)
  {
    return mapFrom[j];
  }

  /**
   * Return limit of the range of this mapping.
   *
   * @return range limit of mapping
   */
  public int getLimit()
  {
    return limit;
  }

} // class: Mapper

