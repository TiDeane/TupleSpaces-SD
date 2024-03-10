package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.util.ArrayList;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass;
import pt.ulisboa.tecnico.tuplespaces.client.PutObserver;
import pt.ulisboa.tecnico.tuplespaces.client.ReadObserver;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;


public class ClientService {
  /*  This class contains the methods for building a channel and stub, as well
      as individual methods for each remote operation of this service. */

  private static final String BGN_TUPLE = "<";
  private static final String END_TUPLE = ">";

  private int numServers;
  private OrderedDelayer delayer;

  // This array is instantialized inside CommandProcesssor
  public TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[] stubs;

  public void setStubs(TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[] stubs) {
    this.stubs = stubs;
  }

  public void createDelayer(int numServers) {
    /* The delayer can be used to inject delays to the sending of requests to the 
      different servers, according to the per-server delays that have been set  */
    delayer = new OrderedDelayer(numServers);
  }

  public ManagedChannel buildChannel(String target) {
    return ManagedChannelBuilder.forTarget(target).usePlaintext().build();
  }

  public TupleSpacesReplicaGrpc.TupleSpacesReplicaStub buildStub(ManagedChannel channel) {
    return TupleSpacesReplicaGrpc.newStub(channel);
  }

  public List<String> getServers(String nameServerTarget, String service, String qualifier) {
    List<String> addressList;
    
    final ManagedChannel nameServerChannel = this.buildChannel(nameServerTarget);
    NameServerGrpc.NameServerBlockingStub nameServerStub;
    nameServerStub = NameServerGrpc.newBlockingStub(nameServerChannel);

    try {
      NameServerOuterClass.LookupRequest lookupRequest;
      lookupRequest = NameServerOuterClass.LookupRequest.newBuilder().
                      setName(service).setQualifier(qualifier).build();
                      
      NameServerOuterClass.LookupResponse lookupResponse;
      lookupResponse = nameServerStub.lookup(lookupRequest);

      addressList = lookupResponse.getServersList();

      nameServerChannel.shutdownNow();

      // Now that the Lookup operation is complete, creates the delayer object
      numServers = addressList.size();
      createDelayer(numServers);
      
      return addressList;

    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + e.getStatus().getDescription());
      nameServerChannel.shutdownNow();
      
      addressList = new ArrayList<String>(); // Return empty list
      return addressList;
    }
    
  }

  /* This method allows the command processor to set the request delay assigned to a given server */
  public void setDelay(int id, int delay) {
    delayer.setDelay(id, delay);
  }

  public void put(String tuple) {
    TupleSpacesReplicaXuLiskov.PutRequest putRequest;
    putRequest = TupleSpacesReplicaXuLiskov.PutRequest.newBuilder().setNewTuple(tuple).build();

    PutObserver putObserver = new PutObserver();

    try {
      for (int i = 0; i < numServers; i++) {
        stubs[i].put(putRequest, putObserver);
      }

      try {
        putObserver.waitUntilAllReceived(numServers);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      System.out.println("OK");
      System.out.print("\n");

    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + 
        e.getStatus().getDescription());
    }
  }

  public void read(String tuple) {
    TupleSpacesReplicaXuLiskov.ReadRequest readRequest;
    readRequest = TupleSpacesReplicaXuLiskov.ReadRequest.newBuilder().setSearchPattern(tuple).build();
    String result = "";

    ReadObserver readObserver = new ReadObserver();

    try {
      for (int i = 0; i < numServers; i++) {
        stubs[i].read(readRequest, readObserver);
      }

      try {
        result = readObserver.waitUntilReceivesResponse();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

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

  /*public void take(String pattern, TupleSpacesGrpc.TupleSpacesStub stub) {
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

  public void getTupleSpacesState(String qualifier, TupleSpacesGrpc.TupleSpacesStub stub) {
    TupleSpacesCentralized.GetTupleSpacesStateRequest getTupleSpacesStateRequest;
    TupleSpacesCentralized.GetTupleSpacesStateResponse getTupleSpacesStateResponse;
    List<String> tupleSpace;

    try {
      getTupleSpacesStateRequest = TupleSpacesCentralized.GetTupleSpacesStateRequest.getDefaultInstance();
      getTupleSpacesStateResponse = stub.getTupleSpacesState(getTupleSpacesStateRequest);
      tupleSpace = getTupleSpacesStateResponse.getTupleList();

      // verify arguments given by server 
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
  }*/

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
