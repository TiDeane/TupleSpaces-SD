package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.getTupleSpacesStateResponse;

import java.util.List;

public class GetTupleSpacesStateObserver implements StreamObserver<getTupleSpacesStateResponse> {

    List<String> tupleSpace;

    public GetTupleSpacesStateObserver() {
    }

    @Override
    synchronized public void onNext(getTupleSpacesStateResponse r) {
        tupleSpace = r.getTupleList();
        notifyAll();
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

    synchronized public void printTupleSpace() throws InterruptedException {
        wait();
        if (tupleSpace != null)
            System.out.println(tupleSpace);
    }
}
