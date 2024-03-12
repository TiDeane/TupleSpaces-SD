package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase2Response;

public class TakePhase2Observer implements StreamObserver<TakePhase2Response> {
    int nResponses;

    public TakePhase2Observer() {
        nResponses = 0;
    }
    
    @Override
    synchronized public void onNext(TakePhase2Response r) {
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

    synchronized public void incrementCount(TakePhase2Response r) {
        nResponses += 1;
        notifyAll();
    }

    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        while (nResponses < n) 
            wait();
    }
}
