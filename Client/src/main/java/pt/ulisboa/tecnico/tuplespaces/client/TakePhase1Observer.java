package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1Response;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class TakePhase1Observer implements StreamObserver<TakePhase1Response> {

    int nResponses;
    List<List<String>> receivedTupleLists;

    public TakePhase1Observer() {
        nResponses = 0;
        receivedTupleLists = new ArrayList<List<String>>();
    }

    @Override
    synchronized public void onNext(TakePhase1Response r) {
        incrementCount(r);
        //System.out.println("Received response: " + r);
    }

    @Override
    synchronized public void onError(Throwable throwable) {
        //System.out.println("Received error: " + throwable);
    }

    @Override
    synchronized public void onCompleted() {
        //System.out.println("Request completed");
    }

    synchronized public void incrementCount(TakePhase1Response r) {
        nResponses += 1;
        receivedTupleLists.add(r.getReservedTuplesList());
        notifyAll();
    }

    /*synchronized public void addString(String s) {
        strings.add(s);
        notifyAll();
    }*/

    synchronized public String getRandomTuple() {
        Random random = new Random();

        // Gets a random index
        int serverIndex = random.nextInt(nResponses);

        // Chooses a random tuple from that index's server
        int tupleIndex = random.nextInt(receivedTupleLists.get(serverIndex).size());

        return receivedTupleLists.get(serverIndex).get(tupleIndex);
    }

    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        /*
         * Maybe make it wait a certain amount of time, and then repeat all requests
         * if not all responses have returned? Do this on "put" aswell?
         */
        while (nResponses < n) 
            wait();
    }
}
