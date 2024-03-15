package pt.ulisboa.tecnico.tuplespaces.client.observers;

import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1Response;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class TakePhase1Observer implements StreamObserver<TakePhase1Response> {

    int nResponses;
    boolean operationFailed;
    List<List<String>> receivedTupleLists;

    public TakePhase1Observer() {
        nResponses = 0;
        operationFailed = false;
        receivedTupleLists = new ArrayList<List<String>>();
    }

    @Override
    synchronized public void onNext(TakePhase1Response r) {
        receivedTupleLists.add(r.getReservedTuplesList());
        incrementCount();
    }

    @Override
    synchronized public void onError(Throwable throwable) {
        operationFailed = true;
        notifyAll();
    }

    @Override
    synchronized public void onCompleted() {
    }

    synchronized public void incrementCount() {
        nResponses += 1;
        notifyAll();
    }

    synchronized public String getRandomTuple() {
        Random random = new Random();

        List<String> intersection = getIntersectionOfTupleLists();

        if (intersection.isEmpty()) {
            return "";
        }

        int tupleIndex = random.nextInt(intersection.size());

        return intersection.get(tupleIndex);
    }

    synchronized public List<String> getIntersectionOfTupleLists() {
        if (receivedTupleLists.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<String> intersection = new ArrayList<String>(receivedTupleLists.get(0));

        for (int i = 1; i < nResponses; i++) {
            intersection.retainAll(receivedTupleLists.get(i));
        }

        return intersection;
    }

    /* if it didn't receive the response in 5 seconds it changes the operationFailed flag to true */
    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        int nResponsesBefore;
        boolean waitedFiveSecs = false;
        /* the only way to leave this while loop is by:
         * -> receiving all responses 
         * -> waiting 5 seconds and not receiving the majority of the responses 
         * -> one of the servers gives an error 
         */
        while (nResponses < n && !waitedFiveSecs && !operationFailed) {
            nResponsesBefore = nResponses;
            wait(5000);
            /*
             * verifies if it left the wait because it timed out
             * and if the majority of the servers didn't respond
             * leaves the while loop
             */
            if (nResponsesBefore == nResponses && (float) n / 2 > nResponses) {
                waitedFiveSecs = true;
                operationFailed = true;
            }
        }
    }

    public boolean operationFailed() {
        return operationFailed;
    }
}
