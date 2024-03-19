package pt.ulisboa.tecnico.tuplespaces.client.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.getTupleSpacesStateResponse;


import java.util.List;

public class GetTupleSpacesStateObserver implements StreamObserver<getTupleSpacesStateResponse> {

    List<String> tupleSpace;

    public GetTupleSpacesStateObserver() {
    }

    @Override
    synchronized public void onNext(getTupleSpacesStateResponse r) {
        tupleSpace = r.getTupleList();
        notifyAll();
    }

    @Override
    synchronized public void onError(Throwable throwable) {
    }

    @Override
    synchronized public void onCompleted() {
    }

    synchronized public void printTupleSpace() throws InterruptedException {
        wait();
        if (tupleSpace != null)
            System.out.println(tupleSpace);
    }
}
