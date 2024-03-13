package pt.ulisboa.tecnico.tuplespaces.client.observers;

import io.grpc.stub.StreamObserver;

import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1ReleaseResponse;

import java.util.List;

public class TakePhase1ReleaseObserver implements StreamObserver<TakePhase1ReleaseResponse> {

    int nResponses;
    List<List<String>> receivedTupleLists;

    public TakePhase1ReleaseObserver() {
        nResponses = 0;
    }

    @Override
    synchronized public void onNext(TakePhase1ReleaseResponse r) {
        incrementCount(r);
    }

    @Override
    synchronized public void onError(Throwable throwable) {
    }

    @Override
    synchronized public void onCompleted() {
    }

    synchronized public void incrementCount(TakePhase1ReleaseResponse r) {
        nResponses += 1;
        notifyAll();
    }

    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        while (nResponses < n) 
            wait();
    }
}
