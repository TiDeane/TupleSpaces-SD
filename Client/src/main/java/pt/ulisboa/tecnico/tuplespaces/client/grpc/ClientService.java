package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
    stub.put(TupleSpacesCentralized.PutRequest.newBuilder().setNewTuple(tuple).build());
    // TODO:Check if no errors ocurred and then print OK
  }

  public void take(String pattern, TupleSpacesGrpc.TupleSpacesBlockingStub stub) {
    TupleSpacesCentralized.TakeResponse takeResponse = stub.take(TupleSpacesCentralized.TakeRequest.newBuilder().setSearchPattern(pattern).build());
    // TODO:print OK and then print the response out
  }
}
