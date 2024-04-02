package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.PriorityQueue;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ServerState {
  private final ReadWriteLock lock;

  private List<String> tuples;

  /*
   * For each blocked Take command, maps the given pattern to a Queue of TakeObj.
   * where each TakeObj stores the command's Thread and sequence number, ordered
   * by the sequence number
   */
  private Map<String, PriorityQueue<TakeObj>> takeMap;
  private Map<Thread, String> readMap;

  private int nextOp;

  public ServerState() {
    this.tuples = new ArrayList<String>();
    this.takeMap = new HashMap<>();
    this.readMap = new HashMap<>();
    this.lock = new ReentrantReadWriteLock();
    nextOp = 1;
  }

  public int getNextOp() {
    return nextOp;
  }

  public synchronized void incrementNextOp() {
    nextOp++;
  }

  public synchronized void put(String tuple) {
    lock.writeLock().lock();
    try {
      tuples.add(tuple);
    } finally {
      lock.writeLock().unlock();
    }
  }

  private String getMatchingTuple(String pattern) {
    lock.readLock().lock();
    try {
      for (String tuple : this.tuples) {
        if (tuple.matches(pattern)) {
          return tuple;
        }
      }
      return null;
    } finally {
      lock.readLock().unlock();
    }
  }

  public synchronized String read(String pattern) {
    return getMatchingTuple(pattern);
  }

  public synchronized String take(String pattern) {
    lock.writeLock().lock();
    try {
      String tuple = getMatchingTuple(pattern);
      if (tuple != null) {
        this.tuples.remove(tuple);
      }
      return tuple;
    } finally {
      lock.writeLock().unlock();
    }
  }

  public synchronized List<String> getTupleSpacesState() {
    return this.tuples;
  }

  public synchronized void addReadThread(Thread thread, String pattern) {
    readMap.put(thread, pattern);
  }

  public synchronized void removeReadThread(Thread thread) {
    readMap.remove(thread);
  }

  public synchronized void awakeReadThreads(String readPattern) {
    for (Map.Entry<Thread, String> entry : readMap.entrySet()) {
      String pattern = entry.getValue();

      if (readPattern.matches(pattern)) {
        Thread thread = entry.getKey();
        synchronized (thread) {
          thread.notify();
        }
      }
    }
  }

  public synchronized void addTakeObj(String pattern, TakeObj takeObj) {
    PriorityQueue<TakeObj> queue = takeMap.get(pattern);
    
    if (queue == null) {
      queue = new PriorityQueue<TakeObj>();
    }

    queue.add(takeObj);
    takeMap.put(pattern, queue);
  }

  /*
   * Awakes the Take thread with the lowest sequence number with a pattern that
   * matches the one given as an argument
   */
  public synchronized void awakeTakeThread(String takePattern) {
    PriorityQueue<TakeObj> minQueue = null;
    PriorityQueue<TakeObj> tempQueue = null;
    int minSeqNumber = -1;
    int tempSeqNumber = -1;
    String lastPattern = null;
    
    /*
     * Saves the queue (and corresponding pattern) that contains the TakeObj with
     * the lowest sequence number
     */
    for (String mapPattern : takeMap.keySet()) {
      if (takePattern.matches(mapPattern)) {
        tempQueue = takeMap.get(mapPattern);
        tempSeqNumber = tempQueue.peek().getSeqNumber();

        if (tempSeqNumber < minSeqNumber || minSeqNumber == -1) {
          minSeqNumber = tempSeqNumber;
          minQueue = tempQueue;
          lastPattern = mapPattern;
        }
      }
    }
    
    if (minSeqNumber == -1) {
      return;
    }

    // Removes the TakeObj from the Queue and notifies the corresponding Thread
    Thread thread = minQueue.poll().getThread();

    if (minQueue.isEmpty()) {
      takeMap.remove(lastPattern);
    }

    synchronized(thread) {
      thread.notify();
    }
  }
}
