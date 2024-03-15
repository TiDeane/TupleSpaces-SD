package pt.ulisboa.tecnico.tuplespaces.server.domain;

import pt.ulisboa.tecnico.tuplespaces.server.domain.TupleSpaceObj;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class ServerState {

  // Each TupleSpaceObj stores a String tuple, a flag for whether the tuple
  // is locked or not and the clientId of the client that locked it 
  private List<TupleSpaceObj> tuples;
  
  // Saves the clientId of clients that are waiting for a Take operation
  private Set<Integer> clientWaitingTake;

  public ServerState() {
    this.tuples = new ArrayList<TupleSpaceObj>();
    this.clientWaitingTake = new HashSet<Integer>();
  }

  public synchronized void put(String tuple) {
    TupleSpaceObj newTuple = new TupleSpaceObj(tuple);
    tuples.add(newTuple);
  }

  private TupleSpaceObj getMatchingTuple(String pattern) {
    for (TupleSpaceObj tuple : this.tuples) {
      if (tuple.getTuple().matches(pattern)) {
        return tuple;
      }
    }
    return null;
  }

  /*  This method returns a list with all the tuples that match the given pattern,
      locking them and setting their clientId in the process */
  public List<String> getAllMatchingFreeTuples(String pattern, int clientId) {
    ArrayList<String> matchingTuples = new ArrayList<String>();

    for (TupleSpaceObj tuple : this.tuples) {
      if (tuple.getFlag() == 0 && tuple.getTuple().matches(pattern)) {
        matchingTuples.add(tuple.getTuple());
        tuple.lockTuple(clientId);
      }
    }

    return matchingTuples;
  }

  public synchronized void unlockClientTuples(int clientId) {
    for (TupleSpaceObj tuple : tuples) {
      if (tuple.getClientId() == clientId) {
        tuple.unlockTuple();
      }
    }
  }

  public synchronized boolean isClientWaitingTake(int clientId) {
    return this.clientWaitingTake.contains(clientId);
  }

  public synchronized void addClientWaitingTake(int clientId) {
    this.clientWaitingTake.add(clientId); 
  }

  public synchronized void removeClientWaitingTake(int clientId) {
    this.clientWaitingTake.remove(Integer.valueOf(clientId));
  }

  public synchronized String read(String pattern) {
    TupleSpaceObj tupleObj = getMatchingTuple(pattern);
    if (tupleObj == null) {
      return null;
    }
    return tupleObj.getTuple();
  }

  public synchronized String take(String pattern) {
    TupleSpaceObj tuple = getMatchingTuple(pattern);
    if (tuple != null) {
      this.tuples.remove(tuple);
      return tuple.getTuple();
    }

    return null;
  }

  public synchronized List<String> getTupleSpacesState() {
    ArrayList<String> stringTuples = new ArrayList<String>();

    for (TupleSpaceObj t : tuples) {
      stringTuples.add(t.getTuple());
    }

    return stringTuples;
  }
}
