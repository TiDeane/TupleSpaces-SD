package pt.ulisboa.tecnico.tuplespaces.server.domain;

import pt.ulisboa.tecnico.tuplespaces.server.domain.TupleSpaceObj;

import java.util.ArrayList;
import java.util.List;

public class ServerState {
  private List<TupleSpaceObj> tuples;

  public ServerState() {
    this.tuples = new ArrayList<TupleSpaceObj>();
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

  public synchronized String read(String pattern) {
    return getMatchingTuple(pattern).getTuple();
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
