package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.PriorityQueue;

public class ServerState {

  private List<String> tuples;

  /*
   * For each blocked Take command, maps the given pattern to a Queue of TakeObj.
   * where each TakeObj stores the command's Thread and sequence number, ordered
   * by the sequence number
   */
  private Map<String, PriorityQueue<TakeObj>> takeMap;
  private List<Thread> readList;

  public ServerState() {
    this.tuples = new ArrayList<String>();
    this.takeMap = new HashMap<>();
    this.readList = new ArrayList<>();
  }

  public synchronized void put(String tuple) {
    tuples.add(tuple);
  }

  private String getMatchingTuple(String pattern) {
    for (String tuple : this.tuples) {
      if (tuple.matches(pattern)) {
        return tuple;
      }
    }
    return null;
  }

  public synchronized String read(String pattern) {
    return getMatchingTuple(pattern);
  }

  public synchronized String take(String pattern) {
    String tuple = getMatchingTuple(pattern);
    if (tuple != null) {
      this.tuples.remove(tuple);
    }
    return tuple;
  }

  public synchronized List<String> getTupleSpacesState() {
    return this.tuples;
  }

  public synchronized void addReadThread(Thread thread) {
    readList.add(thread);
  }

  public synchronized void removeReadThread(Thread thread) {
    readList.remove(thread);
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
    synchronized(thread) {
      thread.notify();
    }

    if (minQueue.isEmpty() || lastPattern != null) {
      takeMap.remove(lastPattern);
    }
  }

  public synchronized void awakeReadThreads() {
    for (int i = 0; i < readList.size(); i++) {
      synchronized(readList.get(i)) {
        readList.get(i).notify();
      }
    }
  }
}
