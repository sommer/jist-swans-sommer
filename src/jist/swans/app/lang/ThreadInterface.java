//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <ThreadInterface.java Sat 2005/03/12 13:35:46 barr rimbase.rimonbarr.com>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.app.lang;

import jist.runtime.JistAPI;

public interface ThreadInterface
{
  void ThreadRun();  // intentionally non-blocking
  void ThreadJoin() throws JistAPI.Continuation;
}
