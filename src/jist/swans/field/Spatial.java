//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Spatial.java Tue 2004/04/06 11:31:14 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.field;

import jist.swans.radio.RadioInfo;
import jist.swans.radio.RadioInterface;
import jist.swans.misc.Location;
import jist.swans.misc.Message;
import jist.swans.misc.Util;
import jist.swans.Main;

/** 
 * Root of all spatial data structures.
 *
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: Spatial.java,v 1.25 2004-04-06 16:07:47 barr Exp $
 * @since SWANS1.0
 */

public abstract class Spatial
{

  //////////////////////////////////////////////////
  // visitor interface
  //

  /** 
   * Transmission visitor object.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */

  public static interface SpatialTransmitVisitor
  {
    /**
     * Compute signal strength between source radio and given point.
     *
     * @param srcInfo source radio information
     * @param srcLoc source radio location
     * @param dst destination radio location
     * @return signal strength
     */
    double computeSignal(RadioInfo srcInfo, Location srcLoc, Location dst);

    /**
     * Transmit packet to given destination.
     *
     * @param srcInfo source radio information
     * @param srcLoc source radio location
     * @param dstInfo destination radio information
     * @param dstEntity destination radio entity
     * @param dstLoc destination radio location
     * @param msg message to transmit
     * @param durationObj transmit duration
     */
    void visitTransmit(RadioInfo srcInfo, Location srcLoc, 
        RadioInfo dstInfo, RadioInterface dstEntity, Location dstLoc, 
        Message msg, Long durationObj);
  }

  /**
   * General radio visitor object.
   *
   * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
   * @since SWANS1.0
   */
  public static interface SpatialVisitor
  {
    /**
     * Visit node.
     *
     * @param dst node to visit
     */
    void visit(Field.RadioData dst);
  }

  //////////////////////////////////////////////////
  // locals
  //

  /** Spatial data structure endpoints. */
  protected Location.Location2D bl, br, tl, tr;
  /** Number of nodes in data structure. */
  protected int size;

  //////////////////////////////////////////////////
  // initialize
  //

  /**
   * Create new (abstract) bin.
   *
   * @param tr top-right corner location
   */
  public Spatial(Location tr)
  {
    this(new Location.Location2D(0, 0), tr);
  }

  /**
   * Create new (abstract) bin.
   *
   * @param bl bottom-left corner location
   * @param tr top-right corner location
   */
  public Spatial(Location bl, Location tr)
  {
    this(bl, 
        new Location.Location2D(tr.getX(), bl.getY()),
        new Location.Location2D(bl.getX(), tr.getY()),
        tr);
  }

  /**
   * Create new (abstract) bin.
   *
   * @param bl bottom-left corner location
   * @param br bottom-right corner location
   * @param tl top-left corner location
   * @param tr top-right corner location
   */
  public Spatial(Location bl, Location br, Location tl, Location tr)
  {
    this.bl = (Location.Location2D)bl;
    this.br = (Location.Location2D)br;
    this.tl = (Location.Location2D)tl;
    this.tr = (Location.Location2D)tr;
  }

  /**
   * Visit radios in bin with transmission visitor.
   *
   * @param visitor transmission visitor object
   * @param srcInfo transmission source radio
   * @param srcLoc transmission source location
   * @param msg message to transmit
   * @param durationObj transmission duration
   * @param limit propagation limit
   * @return number of receiving radios
   */
  public abstract int visitTransmit(SpatialTransmitVisitor visitor, 
      RadioInfo srcInfo, Location srcLoc,
      Message msg, Long durationObj, double limit);

  /**
   * Visit radios in bin.
   *
   * @param visitor visitor object
   * @return number of radios visited
   */
  public abstract int visit(SpatialVisitor visitor);

  /**
   * Add a radio to bin.
   *
   * @param radioData radio information (location inside bin limits)
   */
  public abstract void add(Field.RadioData radioData);

  /**
   * Delete a radio from bin.
   *
   * @param radioData radio information (location inside bin limits)
   */
  public abstract void del(Field.RadioData radioData);

  /**
   * Update a radio location.
   *
   * @param radioData radio information
   * @param newLoc destination of move
   * @return radio information (unupdated) if new location outside of bin; null if new location inside bin
   */
  public abstract Field.RadioData move(Field.RadioData radioData, Location newLoc);

  /**
   * Update a radio location, but new location is still within same bin.
   *
   * @param rd radio information
   * @param newLoc destination of move (must be within bin)
   */
  public void moveInside(Field.RadioData rd, Location newLoc)
  {
    rd = move(rd, newLoc);
    if(Main.ASSERT) Util.assertion(rd==null);
  }

  /**
   * Get nearest corner to location.
   *
   * @param src location <b>outside</b> bin
   * @return location of nearest corner to given location
   */
  public Location getNearest(Location src)
  {
    if(Main.ASSERT) Util.assertion(!src.inside(bl, tr));
    // cases: 
    //   3  6  9 
    //   2  x  8
    //   1  4  7
    if(src.getX()<=bl.getX())
    {
      if(src.getY()<=bl.getY())
      {
        return bl; // case 1
      }
      else if(src.getY()>=tr.getY())
      {
        return tl; // case 3
      }
      else
      {
        return new Location.Location2D(bl.getX(), src.getY()); // case 2
      }
    }
    else if(src.getX()>=tr.getX())
    {
      if(src.getY()<=bl.getY())
      {
        return br; // case 7
      }
      else if(src.getY()>=tr.getY())
      {
        return tr; // case 9
      }
      else
      {
        return new Location.Location2D(tr.getX(), src.getY()); // case 8
      }
    }
    else
    {
      if(src.getY()<=bl.getY())
      {
        return new Location.Location2D(src.getX(), bl.getY()); // case 4
      }
      else if(src.getY()>=tr.getY())
      {
        return new Location.Location2D(src.getX(), tr.getY()); // case 6
      }
      else
      {
        throw new RuntimeException("get nearest undefined for internal point");
      }
    }
  }

  /**
   * Return number of radios in bin.
   *
   * @return number of radios in bin
   */
  public int getSize()
  {
    return size;
  }

  /**
   * Compute area of bin.
   *
   * @return bin area
   */
  public double area()
  {
    float dx = tr.getX() - bl.getX();
    float dy = tr.getY() - bl.getY();
    return dx*dy;
  }

  /**
   * Return top-right coordinate.
   *
   * @return top-right coordinate
   */
  public Location getTopRight()
  {
    return tr;
  }

  /**
   * Return bottom-left coordinate.
   *
   * @return bottom-left coordinate
   */
  public Location getBottomLeft()
  {
    return bl;
  }


  //////////////////////////////////////////////////
  // linear search implementation
  //

  /**
   * Linear-lookup (no binning).
   */
  public static class LinearList extends Spatial
  {
    /** whether to check for cycles. (debug) */
    private static final boolean CHECK_CYCLE = false;

    /** list of radios in bin. */
    private Field.RadioData radioList;

    /**
     * Create a new linear-lookup bin.
     *
     * @param tr top-right corner location
     */
    public LinearList(Location tr)
    {
      super(tr);
    }

    /**
     * Create a new linear-lookup bin.
     *
     * @param bl bottom-left corner location
     * @param tr top-right corner location
     */
    public LinearList(Location bl, Location tr)
    {
      super(bl, tr);
    }

    /**
     * Create a new linear-lookup bin.
     *
     * @param bl bottom-left corner location
     * @param br bottom-right corner location
     * @param tl top-left corner location
     * @param tr top-right corner location
     */
    public LinearList(Location bl, Location br, Location tl, Location tr)
    {
      super(bl, br, tl, tr);
    }

    /**
     * Determine whether bin radio list contains a cycle.
     *
     * @return whether bin radio list contains a cycle
     */
    private boolean hasCycle()
    {
      boolean passed = false;
      for(Field.RadioData dst=radioList; dst!=null; dst=dst.next)
      {
        if(dst==radioList && passed) return true;
        passed = true;
      }
      return false;
    }

    /** {@inheritDoc} */
    public void add(Field.RadioData data)
    {
      if(Main.ASSERT) Util.assertion(data.loc.inside(bl, tr));
      if(Main.ASSERT) Util.assertion(data.next==null);
      if(Main.ASSERT) Util.assertion(data.prev==null);
      data.next = radioList;
      if(radioList!=null) radioList.prev = data;
      radioList = data;
      size++;
      if(CHECK_CYCLE)
      {
        if(hasCycle())
        {
          boolean passed = false;
          for(Field.RadioData dst=radioList; dst!=null; dst=dst.next)
          {
            System.out.println("radio: id="+dst.info.getUnique().getID());
            if(dst==radioList && passed) break;
            passed = true;
          }
          throw new RuntimeException("cycle detected");
        }
      }
    }

    /** {@inheritDoc} */
    public void del(Field.RadioData data)
    {
      if(Main.ASSERT) Util.assertion(data.loc.inside(bl, tr));
      if(data.prev!=null) data.prev.next = data.next;
      if(data.next!=null) data.next.prev = data.prev;
      if(radioList==data) radioList = radioList.next;
      data.next = null;
      data.prev = null;
      size--;
      if(CHECK_CYCLE)
      {
        if(hasCycle())
        {
          throw new RuntimeException("cycle detected");
        }
      }
    }

    /** {@inheritDoc} */
    public Field.RadioData move(Field.RadioData rd, Location l2)
    {
      if(l2.inside(bl, tr))
      {
        rd.loc = l2;
        return null;
      }
      else
      {
        del(rd);
        return rd;
      }
    }

    /** {@inheritDoc} */
    public int visitTransmit(SpatialTransmitVisitor visitor, 
        RadioInfo srcInfo, Location srcLoc,
        Message msg, Long durationObj, double limit)
    {
      int visited=0;
      for(Field.RadioData dst=radioList; dst!=null && visited<size; dst=dst.next, visited++)
      {
        visitor.visitTransmit(srcInfo, srcLoc, dst.info, dst.entity, dst.loc, msg, durationObj);
      }
      return visited;
    }

    /** {@inheritDoc} */
    public int visit(SpatialVisitor visitor)
    {
      int visited=0;
      for(Field.RadioData dst=radioList; dst!=null && visited<size; dst=dst.next, visited++)
      {
        visitor.visit(dst);
      }
      return visited;
    }

  } // class: LinearList


  //////////////////////////////////////////////////
  // flat grid implementation
  //

  /**
   * Grid-based binning.
   */
  public static class Grid extends Spatial
  {
    /** grid of sub-bins. */
    private LinearList[][] bins;
    /** bin dimensions. */
    private float di, dj;

    /**
     * Create a new grid bin.
     *
     * @param tr top-right corner location
     * @param divisions grid divisions
     */
    public Grid(Location tr, int divisions)
    {
      this(new Location.Location2D(0, 0), tr, divisions);
    }

    /**
     * Create a new grid bin.
     *
     * @param bl bottom-left corner location
     * @param tr top-right corner location
     * @param divisions grid divisions
     */
    public Grid(Location bl, Location tr, int divisions)
    {
      this(bl, 
          new Location.Location2D(tr.getX(), bl.getY()),
          new Location.Location2D(bl.getX(), tr.getY()),
          tr, divisions);
    }

    /**
     * Create a new grid bin.
     *
     * @param bl bottom-left corner location
     * @param br bottom-right corner location
     * @param tl top-left corner location
     * @param tr top-right corner location
     * @param divisions grid divisions
     */
    public Grid(Location bl, Location br, Location tl, Location tr, int divisions)
    {
      super(bl, br, tl, tr);
      bins = new LinearList[divisions][divisions];
      float dx=tr.getX()-bl.getX(), dy=tr.getY()-bl.getY();
      this.di = dx/(float)divisions;
      this.dj = dy/(float)divisions;
      for(int j=0; j<divisions; j++)
      {
        for(int i=0; i<divisions; i++)
        {
          Location pbl, pbr, ptl, ptr;
          // pbl
          try
          {
            pbl = bins[i-1][j].br;
          }
          catch(ArrayIndexOutOfBoundsException e)
          {
            try
            {
              pbl = bins[i][j-1].tl;
            }
            catch(ArrayIndexOutOfBoundsException e2)
            {
              pbl = new Location.Location2D(bl.getX()+i*di, bl.getY()+j*dj);
            }
          }
          // pbr
          try
          {
            pbr = bins[i][j-1].tr;
          }
          catch(ArrayIndexOutOfBoundsException e)
          {
            pbr = new Location.Location2D(bl.getX()+(i+1)*di, bl.getY()+j*dj);
          }
          // ptl
          try
          {
            ptl = bins[i-1][j].tr;
          }
          catch(ArrayIndexOutOfBoundsException e)
          {
            ptl = new Location.Location2D(bl.getX()+i*di, bl.getY()+(j+1)*dj);
          }
          // ptr
          ptr = new Location.Location2D(bl.getX()+(i+1)*di, bl.getY()+(j+1)*dj);
          bins[i][j] = new LinearList(pbl, pbr, ptl, ptr);
        }
      }
    }
    
    /**
     * Return grid bin x-coordinate.
     *
     * @param l spatial location desired
     * @return grid bin x-coordinate
     */
    private int getBinI(Location l)
    {
      return (int)((l.getX()-bl.getX())/di);
    }

    /**
     * Return grid bin y-coordinate.
     *
     * @param l spatial location desired
     * @return grid bin y-coordinate
     */
    private int getBinJ(Location l)
    {
      return (int)((l.getY()-bl.getY())/dj);
    }

    /**
     * Return grid sub-bin.
     *
     * @param l spatial location desired
     * @return sub-bin containing coordinate
     */
    private LinearList getBin(Location l)
    {
      return bins[getBinI(l)][getBinJ(l)];
    }

    /** {@inheritDoc} */
    public void add(Field.RadioData radioData)
    {
      if(Main.ASSERT) Util.assertion(radioData.loc.inside(bl, tr));
      getBin(radioData.loc).add(radioData);
      size++;
    }

    /** {@inheritDoc} */
    public void del(Field.RadioData radioData)
    {
      if(Main.ASSERT) Util.assertion(radioData.loc.inside(bl, tr));
      getBin(radioData.loc).del(radioData);
      size--;
    }

    /** {@inheritDoc} */
    public Field.RadioData move(Field.RadioData radioData, Location newLoc)
    {
      if(Main.ASSERT) Util.assertion(radioData.loc.inside(bl, tr));
      Field.RadioData rd = getBin(radioData.loc).move(radioData, newLoc);
      if(rd!=null && newLoc.inside(bl, tr))
      {
        radioData.loc = newLoc;
        getBin(newLoc).add(radioData);
        return null;
      }
      else
      {
        if(rd!=null) size--;
        return rd;
      }
    }

    /** {@inheritDoc} */
    public int visitTransmit(SpatialTransmitVisitor visitor, 
        RadioInfo srcInfo, Location srcLoc,
        Message msg, Long durationObj, double limit)
    {
      int visited=0;
      int si = getBinI(srcLoc), sj = getBinJ(srcLoc), r;
      boolean inRange = true;
      for(r=0; inRange; r++)
      {
        inRange = false;
        // top
        for(int di=si-r, dj=sj-r; di<=si+r; di++)
        {
          int i = visitBin(visitor, srcInfo, srcLoc, msg, durationObj, limit, di, dj);
          if(i!=-1)
          {
            inRange = true;
            visited+=i;
          }
        }
        // right (-1)
        for(int di=si+r, dj=sj-r+1; dj<=sj+r; dj++)
        {
          int i = visitBin(visitor, srcInfo, srcLoc, msg, durationObj, limit, di, dj);
          if(i!=-1)
          {
            inRange = true;
            visited+=i;
          }
        }
        // bottom (-1)
        for(int di=si+r-1, dj=sj+r; di>=si-r; di--)
        {
          int i = visitBin(visitor, srcInfo, srcLoc, msg, durationObj, limit, di, dj);
          if(i!=-1)
          {
            inRange = true;
            visited+=i;
          }
        }
        // left (-2)
        for(int di=si-r, dj=sj+r-1; dj>=sj-r+1; dj--)
        {
          int i = visitBin(visitor, srcInfo, srcLoc, msg, durationObj, limit, di, dj);
          if(i!=-1)
          {
            inRange = true;
            visited+=i;
          }
        }
      }
      return visited;
    }

    /** 
     * Helper method to visit a sub-bin within grid.
     *
     * @param visitor visitor object
     * @param srcInfo source radio information
     * @param srcLoc source location
     * @param msg message to transmit
     * @param durationObj duration of message transmission
     * @param limit propagation limit
     * @param di grid x-coordinate
     * @param dj grid y-coordinate
     * @return number of radios visited
     */
    private int visitBin(SpatialTransmitVisitor visitor, RadioInfo srcInfo, Location srcLoc, Message msg, 
        Long durationObj, double limit, int di, int dj)
    {
      if(di<0 || dj<0 || di>=bins.length || dj>=bins[0].length) return -1;
      LinearList bin = bins[di][dj];
      if(!srcLoc.inside(bin.bl, bin.tr) &&
        visitor.computeSignal(srcInfo, srcLoc, bin.getNearest(srcLoc))<limit) return -1;
      int visited = 0;
      if(bin.size>0)
      {
        visited = bin.visitTransmit(visitor, srcInfo, srcLoc, msg, durationObj, limit);
      }
      return visited;
    }

    /** {@inheritDoc} */
    public int visit(SpatialVisitor visitor)
    {
      int visited = 0;
      for(int i=0; i<bins.length; i++)
      {
        for(int j=0; j<bins.length; j++)
        {
          visited += bins[i][j].visit(visitor);
        }
      }
      return visited;
    }

  } // class: Grid


  //////////////////////////////////////////////////
  // hierarchical grid implementation
  //

  /**
   * Hierarchical binning.
   */
  public static class HierGrid extends Spatial
  {
    /** sub-bin constants. */
    public static int BL=0, BR=1, TL=2, TR=3;
    /** array of sub-bins. */
    private final Spatial[] bins;
    /** bin mid-point. */
    private final Location mid;

    /**
     * Create new hierarchical bin.
     *
     * @param tr top-right corner location
     * @param height height in bin tree
     */
    public HierGrid(Location tr, int height)
    {
      this(new Location.Location2D(0,0), tr, height);
    }

    /**
     * Create new hierarchical bin.
     *
     * @param bl bottom-left corner location
     * @param tr top-right corner location
     * @param height height in bin tree
     */
    public HierGrid(Location bl, Location tr, int height)
    {
      this(bl, 
          new Location.Location2D(tr.getX(), bl.getY()),
          new Location.Location2D(bl.getX(), tr.getY()),
          tr, height);
    }

    /**
     * Create new hierarchical bin.
     *
     * @param bl bottom-left corner location
     * @param br bottom-right corner location
     * @param tl top-left corner location
     * @param tr top-right corner location
     * @param height height in bin tree
     */
    public HierGrid(Location bl, Location br, Location tl, Location tr, int height)
    {
      super(bl, br, tl, tr);
      if(Main.ASSERT) Util.assertion(height>0);
      mid = new Location.Location2D((bl.getX()+tr.getX())/2, (bl.getY()+tr.getY())/2);
      Location left = new Location.Location2D(bl.getX(), mid.getY());
      Location right = new Location.Location2D(tr.getX(), mid.getY());
      Location top = new Location.Location2D(mid.getX(), tr.getY());
      Location bottom = new Location.Location2D(mid.getX(), bl.getY());
      height--;
      bins = new Spatial[4];
      if(height>0)
      {
        bins[BL] = new HierGrid(bl, bottom, left, mid, height);
        bins[BR] = new HierGrid(bottom, br, mid, right, height);
        bins[TL] = new HierGrid(left, mid, tl, top, height);
        bins[TR] = new HierGrid(mid, right, top, tr, height);
      }
      else
      {
        bins[BL] = new LinearList(bl, bottom, left, mid);
        bins[BR] = new LinearList(bottom, br, mid, right);
        bins[TL] = new LinearList(left, mid, tl, top);
        bins[TR] = new LinearList(mid, right, top, tr);
      }
    }

    /**
     * Helper method to determine sub-bin for location.
     *
     * @param loc location to descend towards
     * @return sub-bin containing location
     */
    private Spatial getBin(Location loc)
    {
      return loc.getX()<mid.getX() 
        ? (loc.getY()<mid.getY() ? bins[BL] : bins[TL])
        : (loc.getY()<mid.getY() ? bins[BR] : bins[TR]);
    }

    /** {@inheritDoc} */
    public void add(Field.RadioData rd)
    {
      if(Main.ASSERT) Util.assertion(rd.loc.inside(bl, tr));
      getBin(rd.loc).add(rd);
      size++;
    }

    /** {@inheritDoc} */
    public void del(Field.RadioData rd)
    {
      if(Main.ASSERT) Util.assertion(rd.loc.inside(bl, tr));
      getBin(rd.loc).del(rd);
      size--;
    }

    /** {@inheritDoc} */
    public Field.RadioData move(Field.RadioData rd, Location l2)
    {
      if(Main.ASSERT) Util.assertion(rd.loc.inside(bl, tr));
      rd = getBin(rd.loc).move(rd, l2);
      if(rd==null) return null;
      if(l2.inside(bl, tr))
      {
        rd.loc = l2;
        getBin(l2).add(rd);
        return null;
      }
      else
      {
        size--;
        return rd;
      }
    }

    /** {@inheritDoc} */
    public int visitTransmit(SpatialTransmitVisitor visitor, 
        RadioInfo srcInfo, Location srcLoc, 
        Message msg, Long durationObj, double limit)
    {
      if(!srcLoc.inside(bl, tr) && 
          visitor.computeSignal(srcInfo, srcLoc, getNearest(srcLoc))<limit) return 0;
      int total = 0;
      for(int i=0; i<bins.length; i++)
      {
        if(bins[i].size>0)
        {
          total += bins[i].visitTransmit(visitor, srcInfo, srcLoc, msg, durationObj, limit);
        }
      }
      return total;
    }

    /** {@inheritDoc} */
    public int visit(SpatialVisitor visitor)
    {
      int visited = 0;
      for(int i=0; i<bins.length; i++)
      {
        if(bins[i].size>0)
        {
          visited += bins[i].visit(visitor);
        }
      }
      return visited;
    }

  } // class: HierGrid


  //////////////////////////////////////////////////
  // tiled wraparound implementation
  //

  /**
   * Tile wraparound spatial implementation. This object transmits a signal in
   * nine locations (3x3), each separated by the size of the nested tile.
   */
  public static class TiledWraparound extends Spatial
  {
    /** nested spatial data structure. */
    private final Spatial tile;
    /** transmission offsets corresponding to top-left of each tile. */
    private final Location[] offsets;

    /**
     * Initialize a tiled wraparound spatial data structure.
     *
     * @param tile nested spatial data structure
     */
    public TiledWraparound(Spatial tile)
    {
      super(tile.getTopRight());
      final int PAD = 1;
      int tiles = PAD*2+1;
      this.tile = tile;
      float BLx = -tr.getX(), BLy = -tr.getY();
      offsets = new Location[tiles*tiles];
      for(int i=0; i<tiles; i++)
      {
        for(int j=0; j<tiles; j++)
        {
          offsets[i*tiles+j] = new Location.Location2D(BLx+tr.getX()*i, BLy+tr.getY()*j);
        }
      }
    }

    /** {@inheritDoc} */
    public int visitTransmit(SpatialTransmitVisitor visitor, 
        RadioInfo srcInfo, Location srcLoc, 
        Message msg, Long durationObj, double limit)
    {
      int count = 0;
      for(int i=0; i<offsets.length; i++)
      {
        Location l = srcLoc.getClone();
        l.add(offsets[i]);
        count += tile.visitTransmit(visitor, srcInfo, l, msg, durationObj, limit);
      }
      return count;
    }

    /** {@inheritDoc} */
    public int visit(SpatialVisitor visitor)
    {
      return tile.visit(visitor);
    }

    /** {@inheritDoc} */
    public void add(Field.RadioData radioData)
    {
      tile.add(radioData);
    }

    /** {@inheritDoc} */
    public void del(Field.RadioData radioData)
    {
      tile.del(radioData);
    }

    /** {@inheritDoc} */
    public Field.RadioData move(Field.RadioData radioData, Location newLoc)
    {
      return tile.move(radioData, newLoc);
    }

  } // class: TiledWraparound
}

