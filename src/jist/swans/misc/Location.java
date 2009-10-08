//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Location.java Tue 2004/04/06 11:46:26 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.misc;

import jist.swans.Constants;
import jist.swans.Main;

import jist.runtime.JistAPI;

/** 
 * Location (of a node).
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Location.java,v 1.12 2004-04-06 16:07:48 barr Exp $
 * @since SWANS1.0
 */

public abstract class Location implements JistAPI.Timeless, Cloneable
{
  /**
   * Return X-coordinate of location.
   *
   * @return x-coordinate of location
   */
  public abstract float getX();

  /**
   * Return Y-coordinate of location.
   *
   * @return y-coordinate of location
   */
  public abstract float getY();

  /**
   * Return height of location.
   *
   * @return height of location
   */
  public abstract float getHeight();

  /**
   * Compute distance between two locations.
   *
   * @param l second location
   * @return distance between current and second location
   */
  public abstract float distance(Location l);

  /**
   * Compute distance squared between two locations.
   *
   * @param l second location
   * @return distance squared between current and second location
   */
  public abstract float distanceSqr(Location l);

  /**
   * Divide (scale) vector from current to second location into a number of
   * equal step (displacement) vectors.
   *
   * @param l destination location 
   * @param steps number of steps to destination
   * @return step displacement vector
   */
  public abstract Location step(Location l, int steps);

  /**
   * Determine whether point is inside bounds.
   *
   * @param bounds bounds to test again
   * @return whether point within bounds
   */
  public abstract boolean inside(Location bounds);

  /**
   * Determine whether points is inside bounds.
   *
   * @param min lower left bound
   * @param max upper right bound
   * @return whether point within bounds
   */
  public abstract boolean inside(Location min, Location max);

  /**
   * Vector addition of locations... Be careful! This method mutates
   * the current object.
   *
   * @param l second location / displacement
   */
  public abstract void add(Location l);

  /**
   * Return clone of location object.
   *
   * @return clone of location object
   */
  public Location getClone()
  {
    try
    {
      return (Location)this.clone();
    }
    catch(CloneNotSupportedException e) 
    { 
      throw new RuntimeException(e);
    }
  }

  /**
   * Parse string into 2d or 3d Location object. 
   *
   * @param s string to be parsed: format = x,y[,h]
   * @return string parsed into Location object
   */
  public static Location parse(String s)
  {
    String[] data = s.split("x|,");
    if(data.length==2)
    {
      return new Location.Location2D(Float.parseFloat(data[0]), Float.parseFloat(data[1]));
    }
    else if(data.length==3)
    {
      return new Location.Location3D(Float.parseFloat(data[0]), Float.parseFloat(data[1]), Float.parseFloat(data[2]));
    }
    else throw new IllegalArgumentException("invalid format, expected x,y[,h]");
  }

  //////////////////////////////////////////////////
  // 2d
  //

  /** 
   * A planar location implementation.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */

  public static final class Location2D extends Location
  {
    /** co-ordinates. */
    private float x, y, height;

    /**
     * Create two-dimensional coordinate at default height.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     */
    public Location2D(float x, float y)
    {
      this(x, y, (float)Constants.HEIGHT_DEFAULT);
    }

    /**
     * Create two-dimensional coordinate.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @param height z-coordinate
     */
    public Location2D(float x, float y, float height)
    {
      this.x = x;
      this.y = y;
      this.height = height;
    }

    /** {@inheritDoc} */
    public float distanceSqr(Location l)
    {
      Location2D l2d = (Location2D)l;
      float dx = x - l2d.x, dy = y - l2d.y;
      return dx*dx + dy*dy;
    }

    /** {@inheritDoc} */
    public float distance(Location l)
    {
      return (float)Math.sqrt(distanceSqr(l));
    }

    /** {@inheritDoc} */
    public Location step(Location l, int steps)
    {
      Location2D l2d = (Location2D)l;
      return new Location.Location2D((l2d.x-x)/steps, (l2d.y-y)/steps);
    }

    /** {@inheritDoc} */
    public float getX()
    {
      return x;
    }

    /** {@inheritDoc} */
    public float getY()
    {
      return y;
    }

    /** {@inheritDoc} */
    public float getHeight()
    {
      return height;
    }

    /** {@inheritDoc} */
    public boolean inside(Location bounds)
    {
      Location2D l2d = (Location2D)bounds;
      return x<=l2d.x && y<=l2d.y && x>=0 && y>=0;
    }

    /** {@inheritDoc} */
    public boolean inside(Location min, Location max)
    {
      Location2D min2d = (Location2D)min, max2d = (Location2D)max;
      if(Main.ASSERT) Util.assertion(min2d.x<=max2d.x && min2d.y<=max2d.y);
      return x<=max2d.x && y<=max2d.y && x>=min2d.x && y>=min2d.y;
    }

    /** {@inheritDoc} */
    public void add(Location l)
    {
      Location2D l2d = (Location2D)l;
      x+=l2d.x; y+=l2d.y;
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return "("+x+","+y+")";
    }

  } // class: Location2D

  //////////////////////////////////////////////////
  // 3d
  //

  /** 
   * A three-dimensional location implementation.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */

  public static final class Location3D extends Location
  {
    /** co-ordinates. */
    private float x, y, z;

    /**
     * Create three-dimensional coordinate.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @param z z-coordinate
     */
    public Location3D(float x, float y, float z)
    {
      this.x = x;
      this.y = y;
      this.z = z;
    }

    /** {@inheritDoc} */
    public float distanceSqr(Location l)
    {
      Location3D l3d = (Location3D)l;
      float dx = x - l3d.x, dy = y - l3d.y, dz = z - l3d.z;
      return dx*dx + dy*dy + dz*dz;
    }

    /** {@inheritDoc} */
    public float distance(Location l)
    {
      return (float)Math.sqrt(distanceSqr(l));
    }

    /** {@inheritDoc} */
    public Location step(Location l, int steps)
    {
      Location3D l3d = (Location3D)l;
      return new Location.Location3D((l3d.x-x)/steps, (l3d.y-y)/steps, (l3d.z-z)/steps);
    }

    /** {@inheritDoc} */
    public float getX()
    {
      return x;
    }

    /** {@inheritDoc} */
    public float getY()
    {
      return y;
    }

    /** {@inheritDoc} */
    public float getHeight()
    {
      return z;
    }

    /** {@inheritDoc} */
    public boolean inside(Location bounds)
    {
      Location3D l3d = (Location3D)bounds;
      return x<=l3d.x && y<=l3d.y && z<=l3d.z && x>=0 && y>=0 && z>=0;
    }

    /** {@inheritDoc} */
    public boolean inside(Location min, Location max)
    {
      Location3D min3d = (Location3D)min, max3d = (Location3D)max;
      if(Main.ASSERT) Util.assertion(min3d.x<=max3d.x && min3d.y<=max3d.y && min3d.z<=max3d.z);
      return x<=max3d.x && y<=max3d.y && z<=max3d.z
        && x>=min3d.x && y>=min3d.y && z>=min3d.z;
    }

    /** {@inheritDoc} */
    public void add(Location l)
    {
      Location3D l3d = (Location3D)l;
      x+=l3d.x; y+=l3d.y; z+=l3d.z;
    }

    /** {@inheritDoc} */
    public String toString()
    {
      return "("+x+","+y+","+z+")";
    }

  } // class Location3D

} // interface Location

