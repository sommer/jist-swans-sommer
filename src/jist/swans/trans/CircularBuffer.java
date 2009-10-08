//////////////////////////////////////////////////
// JIST (Java In Simulation Time) Project
// Timestamp: <CircularBuffer.java Sun 2006/05/14 13:47:17 barr jist>
//

// Copyright (C) 2004 by Cornell University
// All rights reserved.
// Refer to LICENSE for terms and conditions of use.

package jist.swans.trans;

/**
 * A class that implements circular byte array.
 *
 * @author Kelwin Tamtoro &lt;kt222@cs.cornell.edu&gt;
 * @author Rimon Barr &lt;barr+jist@cs.cornell.edu&gt;
 * @version $Id: CircularBuffer.java,v 1.6 2006-05-14 17:50:49 barr Exp $
 * @since SWANS1.0
 */
public class CircularBuffer
{

  /** Array to hold the bytes. */
  private byte[] buffer;
  
  /** current size of the buffer. */
  private int curSize;

  /** points to first unread byte. */
  private int head;

  /** points to the byte after the last unread byte. */
  private int tail;
  
  /** Indicator that buffer is full. */
  private boolean isBufferFull;
  
  /** Indicator that buffer is empty. */
  private boolean isBufferEmpty;

  /**
   * Constructor.
   *
   * @param size size of the buffer
   */
  public CircularBuffer (int size)
  {
    buffer = new byte [size];
    curSize = size;
    head = 0;
    tail = 0;
    isBufferFull = false;
    isBufferEmpty = true;
  }
  
  /**
   * Store the byte array into the buffer.
   * 
   * @param data byte array
   * @return number of bytes actually stored
   */
  public int storeBytes (byte[] data)
  {
    return storeBytes (data, 0, data.length);
  }
  
  /**
   * Store the byte array from specific offset and length into the buffer.
   * 
   * @param data byte array
   * @param offset offset in data to start reading
   * @param length number of bytes to store
   * @return number of bytes actually stored
   */
  public int storeBytes (byte[] data, int offset, int length)
  {
    // check the length of data
    // if length is larger than total bytes in data,
    // then set length to size of data
    if ((data.length - offset) < length)
    {
      length = data.length - offset;
    }
    
    // check if buffer is big enough
    // if buffer is not big enough, we increase the buffer size
    // we double the buffer size until the data can fit into the buffer
    while (length > getFreeBufferSize ())
    {
      if (TcpSocket.PRINTOUT >= TcpSocket.FULL_DEBUG)
      {
        System.out.println ("storeBytes: calling buffer resize: length = " + length + "\tcurSize = " + curSize);
      }
      resizeBuffer (curSize*2);
    }

    // if length == 0, do nothing
    if (length > 0)
    {   
      // initial case (buffer is empty)
      if (isBufferEmpty)
      {
        if (length > (buffer.length-tail))
        {
          int tempLength = buffer.length - tail;
          System.arraycopy (data, offset, buffer, tail, tempLength);
          tail = 0;
          System.arraycopy (data, (tempLength+offset), buffer, tail, length-tempLength);
          tail = length - tempLength;
          if (tail == head)
          {
            isBufferFull = true;
          }
        }
        else
        {
          System.arraycopy (data, offset, buffer, tail, length);
          tail += length;
          if (tail == buffer.length)
          {
            tail = 0;
          }
          if (tail == head)
          {
            isBufferFull = true;
          }
        }
      }
      // usual case
      // head at left, tail at right, and the free space after 
      // tail can still take the message
      else if ((buffer.length - tail >= length) && (head < tail))
      {
        System.arraycopy (data, offset, buffer, tail, length);
        tail += length;
        if (tail == buffer.length)
        {
          tail = 0;
        }
        if (tail == head)
        {
          isBufferFull = true;
        }
      }
      // tail at left, head at right, and the free space between
      // them can store the message
      else if ((tail < head) && (head - tail >= length))
      {
        System.arraycopy (data, offset, buffer, tail, length);
        tail += length;
        if (tail == head)
        {
          isBufferFull = true;
        }
      }
      // head at left, tail at right, and the space after tail
      // and before head can store the message
      else if ((head < tail) && (((buffer.length-tail)+head) >= length))
      {
        int tempLength = buffer.length - tail;
        System.arraycopy (data, offset, buffer, tail, tempLength);
        tail = 0;
        System.arraycopy (data, (offset+tempLength), buffer, tail, length-tempLength);
        tail = length - tempLength;
        if (tail == head)
        {
          isBufferFull = true;
        }
        if (tail == buffer.length)
        {
          tail = 0;
        }
      }
      isBufferEmpty = false;   
    }
    
    return length;
  
  }

  /**
   * Retrieves the specific number of bytes.
   * 
   * @param length number of bytes
   * @return array of bytes retrieved from the buffer
   */
  public byte[] retrieveBytes (int length)
  {
    
    // first, check if there is any byte stored in buffer
    // if no bytes at all, return null
    if (getTotalBytesInBuffer() == 0)
    {
      return null;
    }
    
    int retLength = length;

    // if requested bytes are more than what's in buffer,
    // just return all the bytes in the buffer
    if (length > getTotalBytesInBuffer())
    {
      retLength = getTotalBytesInBuffer();
    }
    
    byte[] data = new byte [retLength];    
    if ((buffer.length -  head) < retLength)
    {
      int tempLength = buffer.length - head;
      System.arraycopy (buffer, head, data, 0, tempLength);
      System.arraycopy (buffer, head, data, tempLength, (retLength-tempLength));
      head = retLength - tempLength;        
    }
    else if ((buffer.length -  head) == retLength)
    {
      System.arraycopy (buffer, head, data, 0, retLength);
      head = 0;
    }
    else
    {
      System.arraycopy (buffer, head, data, 0, retLength);
      head += retLength;
    }     
    
    // if at the end, head = buffer, then it means the buffer is empty
    if (head == tail)
    {
      isBufferEmpty = true;
    }
    
    // if any data is retrieved, set full flag to false
    if (data.length > 0)
    {
      isBufferFull = false;
    }
    
    return data;
    
  }
  
  /**
   * Retrieves all of the bytes stored in the buffer.
   * 
   * @return array of bytes retrieved from the buffer
   */
  public byte[] retrieveAllBytes ()
  {
    return retrieveBytes (getTotalBytesInBuffer ());
  }
  
  
  /**
   * Peek the next byte in buffer.
   * 
   * @return array of one byte
   */
  public byte[] peekOneByte ()
  {   
    // check if buffer is empty
    if (isBufferEmpty)
    {
      return null;
    }   
    byte[] ret = new byte [1];
    ret[0] = buffer[head];   
    return ret;  
  }
  

  /** 
   * Returns the free space in buffer.
   * 
   * @return free space size
   */
  public int getFreeBufferSize ()
  {
    int temp = tail - head;
    if (temp > 0)
    {
      return (buffer.length - temp);
    }
    else if (temp == 0)
    {
      if (isBufferFull)
      {
        return 0;
      }
      else
      {
        return buffer.length;
      }
    }
    else
    {
      return (-temp);
    }
  }
  
  
  /** 
   * Returns the number of bytes stored in buffer.
   * 
   * @return number of bytes in buffer
   */
  public int getTotalBytesInBuffer ()
  {
    return (buffer.length - getFreeBufferSize ());
  }
  
  
  /**
   * Returns the buffer size.
   * 
   * @return buffer size
   */
  public int getCurrentBufferSize ()
  {
    return buffer.length;
  }
  
  
  /** 
   * Resize the buffer to the specified value.
   * Returns false if the operation fails.
   * 
   * @param newSize new size of the buffer
   * @return operation result
   */
  public boolean resizeBuffer (int newSize)
  {

    if (newSize < (buffer.length - getFreeBufferSize()))
    {
      return false;
    }

    byte[] newBuffer = new byte [newSize];
    
    // if buffer is currently empty, just use the new one directly
    if (isBufferEmpty)
    {
      buffer = newBuffer;
      curSize = buffer.length;
      return true;
    }
    
    // Copying data to new buffer
    // head at left, tail at right
    if (head < tail)
    { 
      int curLength = tail - head;
      System.arraycopy (buffer, head, newBuffer, 0, curLength);
      if (newSize == curLength)
      {
        head = 0;
        tail = 0;
        isBufferFull = true;
      } 
      else
      {
        head = 0;
        tail = curLength;
      }
    }
    // tail at left, head at right
    else if (tail < head)
    {    
      int temp = buffer.length - head;
      int curLength = temp + tail;
      System.arraycopy (buffer, head, newBuffer, 0, temp);
      System.arraycopy (buffer, 0, newBuffer, temp, tail);
      if (newSize == curLength)
      {
        head = 0;
        tail = 0;
        isBufferFull = true;
      } 
      else
      {
        head = 0;
        tail = curLength;
      }
    }
    // head == tail (buffer is currently full)
    else
    {
      
      // start from beginning of buffer
      if (head == 0)
      {
        System.arraycopy (buffer, head, newBuffer, 0, buffer.length);
      }
      else
      {
        System.arraycopy (buffer, head, newBuffer, 0, (buffer.length-head));
        System.arraycopy (buffer, 0, newBuffer, (buffer.length-head), tail);
      }
      
      if (newSize == buffer.length)
      {
        head = 0;
        tail = 0;
        isBufferFull = true;
      } 
      else
      {
        head = 0;
        tail = buffer.length;
      }
     
    }      
  
    // use the new buffer
    buffer = newBuffer; 
    // set the new size
    curSize = buffer.length;
  
    return true;
    
  }
  
  /**
   * Check if buffer is full.
   *
   * @return true if buffer is full
   */
  public boolean isBufferFull ()
  {
    return isBufferFull;
  }
  
  /**
   * Check if buffer is empty.
   * 
   * @return true if buffer is empty
   */
  public boolean isBufferEmpty ()
  {
    return isBufferEmpty;
  }  


}// class CircularBuffer
