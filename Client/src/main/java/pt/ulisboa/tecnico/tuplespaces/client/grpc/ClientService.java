package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.PutRequest;

public class ClientService {

  /*TODO: The gRPC client-side logic should be here.
        This should include a method that builds a channel and stub,
        as well as individual methods for each remote operation of this service. */

  public ManagedChannel buildChannel(String target) {
    return ManagedChannelBuilder.forTarget(target).usePlaintext().build();
  }

  public TupleSpacesGrpc.TupleSpacesBlockingStub buildStub(ManagedChannel channel) {
    return TupleSpacesGrpc.newBlockingStub(channel);
  }

  public void put(String tuple, TupleSpacesGrpc.TupleSpacesBlockingStub stub) {
    try {
      stub.put(TupleSpacesCentralized.PutRequest.newBuilder().setNewTuple(tuple).build());
      System.out.println("OK");
    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + 
        e.getStatus().getDescription());
    }
  }

  public void take(String pattern, TupleSpacesGrpc.TupleSpacesBlockingStub stub) {
    try {
      TupleSpacesCentralized.TakeResponse takeResponse = stub.take(
        TupleSpacesCentralized.TakeRequest.newBuilder().setSearchPattern(pattern).build());
      System.out.println(takeResponse.getResult());
      System.out.println("OK");
    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + 
        e.getStatus().getDescription());
    }
  }

  public void read(String pattern, TupleSpacesGrpc.TupleSpacesBlockingStub stub) {
    try {
      TupleSpacesCentralized.ReadResponse readResponse = stub.read(
        TupleSpacesCentralized.ReadRequest.newBuilder().setSearchPattern(pattern).build());
      System.out.println(readResponse.getResult());
      System.out.println("OK");
    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + 
        e.getStatus().getDescription());
    }
  }

}
