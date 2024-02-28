package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass;


public class ClientService {

  /*  This class contains the methods for building a channel and stub, as well
      as individual methods for each remote operation of this service. */

  public ManagedChannel buildChannel(String target) {
    return ManagedChannelBuilder.forTarget(target).usePlaintext().build();
  }

  public TupleSpacesGrpc.TupleSpacesBlockingStub buildStub(ManagedChannel channel) {
    return TupleSpacesGrpc.newBlockingStub(channel);
  }

  public String getServer(String nameServerTarget, String service, String qualifier) {
    System.out.println("building channel with target: " + nameServerTarget);
    
    final ManagedChannel nameServerChannel = this.buildChannel(nameServerTarget);
    NameServerGrpc.NameServerBlockingStub nameServerStub;
    nameServerStub = NameServerGrpc.newBlockingStub(nameServerChannel);

    try {
      NameServerOuterClass.LookupRequest lookupRequest;
      lookupRequest = NameServerOuterClass.LookupRequest.newBuilder().
                      setName(service).setQualifier(qualifier).build();

      System.out.println("built LookupRequest:\nserviceName: " + service + "\nqualifier: " + qualifier + "\n");
                      
      NameServerOuterClass.LookupResponse lookupResponse;
      lookupResponse = nameServerStub.lookup(lookupRequest);

      List<String> addressList = lookupResponse.getServersList();

      nameServerChannel.shutdownNow();

      if (addressList.isEmpty())
        return "";
      
      System.out.println(addressList.get(0));
      
      return addressList.get(0);
    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + e.getStatus().getDescription());
      nameServerChannel.shutdownNow();
      
      return "";
    }
    
  }

  public void put(String tuple, TupleSpacesGrpc.TupleSpacesBlockingStub stub) {
    TupleSpacesCentralized.PutRequest putRequest;

    try {
      putRequest = TupleSpacesCentralized.PutRequest.newBuilder().setNewTuple(tuple).build();
      stub.put(putRequest);

      System.out.println("OK");
    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + 
        e.getStatus().getDescription());
    }
  }

  public void take(String pattern, TupleSpacesGrpc.TupleSpacesBlockingStub stub) {
    TupleSpacesCentralized.TakeRequest takeRequest;
    TupleSpacesCentralized.TakeResponse takeResponse;
    String result;

    try {
      takeRequest = TupleSpacesCentralized.TakeRequest.newBuilder().setSearchPattern(pattern).build();
      takeResponse = stub.take(takeRequest);
      result = takeResponse.getResult();
      
      System.out.println("OK");
      System.out.println(result);
    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + 
        e.getStatus().getDescription());
    }
  }

  public void read(String pattern, TupleSpacesGrpc.TupleSpacesBlockingStub stub) {
    TupleSpacesCentralized.ReadRequest readRequest;
    TupleSpacesCentralized.ReadResponse readResponse;
    String result;

    try {
      readRequest = TupleSpacesCentralized.ReadRequest.newBuilder().setSearchPattern(pattern).build();
      readResponse = stub.read(readRequest);
      result = readResponse.getResult();

      System.out.println("OK");
      System.out.println(result);
    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + 
        e.getStatus().getDescription());
    }
  }

  public void getTupleSpacesState(String qualifier, TupleSpacesGrpc.TupleSpacesBlockingStub stub) {
    /* NOTE: não é usado o qualifier logo vai sempre buscar o mesmo server */
    
    TupleSpacesCentralized.GetTupleSpacesStateRequest getTupleSpacesStateRequest;
    TupleSpacesCentralized.GetTupleSpacesStateResponse getTupleSpacesStateResponse;
    List<String> TupleSpace;

    try {
      getTupleSpacesStateRequest = TupleSpacesCentralized.GetTupleSpacesStateRequest.getDefaultInstance();
      getTupleSpacesStateResponse = stub.getTupleSpacesState(getTupleSpacesStateRequest);
      TupleSpace = getTupleSpacesStateResponse.getTupleList();

      System.out.println("OK");
      for (String tuple : TupleSpace) {
        System.out.println(tuple);
      }
    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + 
        e.getStatus().getDescription());
    }
  }
}
