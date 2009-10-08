//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <Config.java Tue 2004/04/06 11:30:25 barr pompom.cs.cornell.edu>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans;

import java.util.*;

public final class Config
{
  public static int getInteger(Properties p, String key, int def)
  {
    String val = p.getProperty(key);
    if(val==null)
    {
      return def;
    }
    else
    {
      return Integer.parseInt(val);
    }
  }

  public static int getInteger(Properties p, String key)
  {
    String val = p.getProperty(key);
    if(val==null)
    {
      throw new RuntimeException("missing property: "+key);
    }
    return Integer.parseInt(val);
  }

  public static long getLong(Properties p, String key, long def)
  {
    String val = p.getProperty(key);
    if(val==null)
    {
      return def;
    }
    else
    {
      return Long.parseLong(val);
    }
  }

  public static long getLong(Properties p, String key)
  {
    String val = p.getProperty(key);
    if(val==null)
    {
      throw new RuntimeException("missing property: "+key);
    }
    return Long.parseLong(val);
  }

  public static double getDouble(Properties p, String key, double def)
  {
    String val = p.getProperty(key);
    if(val==null)
    {
      return def;
    }
    else
    {
      return Double.parseDouble(val);
    }
  }

  public static double getDouble(Properties p, String key)
  {
    String val = p.getProperty(key);
    if(val==null)
    {
      throw new RuntimeException("missing property: "+key);
    }
    return Double.parseDouble(val);
  }

  public static boolean getBoolean(Properties p, String key, boolean def)
  {
    String val = p.getProperty(key);
    if(val==null)
    {
      return def;
    }
    else
    {
      if(val.toLowerCase().equals("true"))
      {
        return true;
      }
      if(val.toLowerCase().equals("false"))
      {
        return false;
      }
      throw new RuntimeException("invalid boolean value: "+key+" = "+val);
    }
  }

  public static boolean getBoolean(Properties p, String key)
  {
    String val = p.getProperty(key);
    if(val==null)
    {
      throw new RuntimeException("missing property: "+key);
    }
    if(val.toLowerCase().equals("true"))
    {
      return true;
    }
    if(val.toLowerCase().equals("false"))
    {
      return false;
    }
    throw new RuntimeException("invalid boolean value: "+key+" = "+val);
  }

  public static String getString(Properties p, String key)
  {
    String val = p.getProperty(key);
    if(val==null)
    {
      throw new RuntimeException("missing propery: "+key);
    }
    return val;
  }

  public static String getString(Properties p, String key, String def)
  {
    return p.getProperty(key, def);
  }
 
}

