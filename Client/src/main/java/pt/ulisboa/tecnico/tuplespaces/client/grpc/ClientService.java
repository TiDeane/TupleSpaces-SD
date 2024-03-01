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

  private static final String BGN_TUPLE = "<";
  private static final String END_TUPLE = ">";

  public ManagedChannel buildChannel(String target) {
    return ManagedChannelBuilder.forTarget(target).usePlaintext().build();
  }

  public TupleSpacesGrpc.TupleSpacesBlockingStub buildStub(ManagedChannel channel) {
    return TupleSpacesGrpc.newBlockingStub(channel);
  }

  public String getServer(String nameServerTarget, String service, String qualifier) {
    
    final ManagedChannel nameServerChannel = this.buildChannel(nameServerTarget);
    NameServerGrpc.NameServerBlockingStub nameServerStub;
    nameServerStub = NameServerGrpc.newBlockingStub(nameServerChannel);

    try {
      NameServerOuterClass.LookupRequest lookupRequest;
      lookupRequest = NameServerOuterClass.LookupRequest.newBuilder().
                      setName(service).setQualifier(qualifier).build();
                      
      NameServerOuterClass.LookupResponse lookupResponse;
      lookupResponse = nameServerStub.lookup(lookupRequest);

      List<String> addressList = lookupResponse.getServersList();

      nameServerChannel.shutdownNow();

      if (addressList.isEmpty())
        return "";
      
      
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
      System.out.print("\n");
      
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
      
      if (isTupleValid(result)) {
        System.out.println("OK");
        System.out.println(result);
        System.out.print("\n");
      }
      else {
        System.out.printf("Tuple must have the format <element[,more_elements]>" + 
          " but received: %s\n", result);
      }
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

      if (isTupleValid(result)) {
        System.out.println("OK");
        System.out.println(result);
        System.out.print("\n");
      }
      else {
        System.out.printf("Tuple must have the format <element[,more_elements]>" + 
          " but received: %s\n", result);
      }
    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + 
        e.getStatus().getDescription());
    }
  }

  public void getTupleSpacesState(String qualifier, TupleSpacesGrpc.TupleSpacesBlockingStub stub) {
    TupleSpacesCentralized.GetTupleSpacesStateRequest getTupleSpacesStateRequest;
    TupleSpacesCentralized.GetTupleSpacesStateResponse getTupleSpacesStateResponse;
    List<String> tupleSpace;

    try {
      getTupleSpacesStateRequest = TupleSpacesCentralized.GetTupleSpacesStateRequest.getDefaultInstance();
      getTupleSpacesStateResponse = stub.getTupleSpacesState(getTupleSpacesStateRequest);
      tupleSpace = getTupleSpacesStateResponse.getTupleList();

      /* verify arguments given by server */
      for (String tuple : tupleSpace) {
        if (isTupleValid(tuple)) {
          continue;
        }
        else {
          System.out.printf("Tuple must have the format <element[,more_elements]>" + 
            " but one of the tuples are: %s\n", tuple);
          return;
        }
      }

      System.out.println("OK");
      System.out.println(tupleSpace);
      System.out.print("\n");

    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + 
        e.getStatus().getDescription());
    }
  }

  	private boolean isTupleValid(String tuple){
        if (tuple.length() < 2 
            ||
            !tuple.substring(0,1).equals(BGN_TUPLE) 
            || 
            !tuple.endsWith(END_TUPLE)) {
            return false;
        }
        else {
            return true;
        }
    }
}
