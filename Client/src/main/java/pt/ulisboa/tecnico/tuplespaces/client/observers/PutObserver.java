package pt.ulisboa.tecnico.tuplespaces.client.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.PutResponse;

public class PutObserver implements StreamObserver<PutResponse> {

   int nResponses;

    public PutObserver() {
        nResponses = 0;
    }

    @Override
    synchronized public void onNext(PutResponse r) {
        incrementCount();
    }

    @Override
    synchronized public void onError(Throwable throwable) {
    }

    @Override
    synchronized public void onCompleted() {
    }

    synchronized public void incrementCount() {
        nResponses += 1;
        notifyAll();
    }

    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        while (nResponses < n) 
            wait();
    }
}
