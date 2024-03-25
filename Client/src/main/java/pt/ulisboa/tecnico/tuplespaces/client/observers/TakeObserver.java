package pt.ulisboa.tecnico.tuplespaces.client.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.TakeResponse;

public class TakeObserver implements StreamObserver<TakeResponse> {
    String response;
    int nResponses;

    public TakeObserver() {
        nResponses = 0;
    }

    @Override
    synchronized public void onNext(TakeResponse r) {
        response = r.getResult(); 
        nResponses++;
        notifyAll();
    }

    @Override
    synchronized public void onError(Throwable throwable) {
    }

    @Override
    synchronized public void onCompleted() {
    }
    
    synchronized public String waitUntilAllReceived(int n) throws InterruptedException {
        while (nResponses < n) 
            wait();
        return response;
    }
}
