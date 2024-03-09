package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.PutResponse;

public class PutObserver implements StreamObserver<PutResponse> {

   int nResponses;

    public PutObserver() {
        nResponses = 0;
    }

    @Override
    synchronized public void onNext(PutResponse r) {
        incrementCount();
        System.out.println("Received response: " + r);
    }

    @Override
    synchronized public void onError(Throwable throwable) {
        System.out.println("Received error: " + throwable);
    }

    @Override
    synchronized public void onCompleted() {
        System.out.println("Request completed");
    }

    synchronized public void incrementCount() {
        nResponses += 1;
        notifyAll();
    }

    /*synchronized public void addString(String s) {
        strings.add(s);
        notifyAll();
    }*/

    /*synchronized public String getStrings() {
        String res = new String();
        for (String s : strings) {
            res = res.concat(s);
        }
        return res;
    }*/

    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        while (nResponses < n) 
            wait();
    }
}
