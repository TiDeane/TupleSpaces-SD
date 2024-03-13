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
        //System.out.println(receivedTupleLists);

        if (receivedTupleLists.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<String> intersection = new ArrayList<String>(receivedTupleLists.get(0));

        for (int i = 1; i < nResponses; i++) {
            intersection.retainAll(receivedTupleLists.get(i));
        }

        //System.out.println(intersection);

        return intersection;
    }

    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        while (nResponses < n) 
            wait();
    }

    public boolean operationFailed() {
        return operationFailed;
    }
}
