package pt.ulisboa.tecnico.tuplespaces.client.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.TakeResponse;

public class TakeObserver implements StreamObserver<TakeResponse> {
    String response;

    public TakeObserver() {
    }

    @Override
    synchronized public void onNext(TakeResponse r) {
        response = r.getResult(); 
        notifyAll();
    }

    @Override
    synchronized public void onError(Throwable throwable) {
    }

    @Override
    synchronized public void onCompleted() {
    }
    
    synchronized public String waitUntilReceivesResponse() throws InterruptedException {
        wait();
        return response;
    }
}
