package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.ReadResponse;

public class ReadObserver implements StreamObserver<ReadResponse> {
    String response;

    public ReadObserver() {
    }

    @Override
    synchronized public void onNext(ReadResponse r) {
        response = r.getResult(); 
        notifyAll();
    }

    @Override
    synchronized public void onError(Throwable throwable) {
        //System.out.println("Received error: " + throwable);
    }

    @Override
    synchronized public void onCompleted() {
        //System.out.println("Request completed");
    }
    
    synchronized public String waitUntilReceivesResponse() throws InterruptedException {
        wait();
        return response;
    }
}
