package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.*;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc.*;

import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc.*;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass.*;

import pt.ulisboa.tecnico.tuplespaces.client.*;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;


public class ClientService {
  /*  This class contains the methods for building a channel and stub, as well
      as individual methods for each remote operation of this service. */

  private static final String BGN_TUPLE = "<";
  private static final String END_TUPLE = ">";

  private int numServers;
  private OrderedDelayer delayer;
  private int clientId;

  // This array is instantialized inside CommandProcesssor
  public TupleSpacesReplicaStub[] stubs;

  public ClientService() {
    // Initializes this client's ID with a seed based on the current time
    Random random = new Random(System.currentTimeMillis());
    clientId = random.nextInt(Integer.MAX_VALUE);
  }

  public void setStubs(TupleSpacesReplicaStub[] stubs) {
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
    NameServerBlockingStub nameServerStub;
    nameServerStub = NameServerGrpc.newBlockingStub(nameServerChannel);

    try {
      LookupRequest lookupRequest;
      lookupRequest = NameServerOuterClass.LookupRequest.newBuilder().
                      setName(service).setQualifier(qualifier).build();
                      
      LookupResponse lookupResponse;
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
    PutRequest putRequest;
    putRequest = TupleSpacesReplicaXuLiskov.PutRequest.newBuilder().setNewTuple(tuple).build();

    PutObserver putObserver = new PutObserver();

    try {
      for (Integer id : delayer) {
        stubs[id].put(putRequest, putObserver);
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
      //TODO: The delayer blocks the client until all delays
      //have finished, so it doesn't work for the read command
      for (int i = 0; i < numServers; i++) {
        stubs[i].read(readRequest, readObserver);
      }

      result = readObserver.waitUntilReceivesResponse();

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
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void take(String pattern) {
    TakePhase1Request takePhase1Request;
    TakePhase1ReleaseRequest takePhase1ReleaseRequest;
    TakePhase2Request takePhase2Request;
    String tupleToRemove;

    TakePhase1Observer takePhase1Observer = new TakePhase1Observer();
    TakePhase1ReleaseObserver takePhase1ReleaseObserver = new TakePhase1ReleaseObserver();
    //TakePhase2Observer takePhase2Observer = new TakePhase2Observer();

    try {
      takePhase1Request = TakePhase1Request.newBuilder().setSearchPattern(pattern).
                                            setClientId(this.clientId).build();

      for (Integer id : delayer) {
        stubs[id].takePhase1(takePhase1Request, takePhase1Observer);
      }

      takePhase1Observer.waitUntilAllReceived(numServers);

      tupleToRemove = takePhase1Observer.getRandomTuple();

      // THIS IS JUST FOR DEBUG
      System.out.println("OK: Phase 1 successful");
      System.out.println("Tuple to remove: " + tupleToRemove + "\n");

      // THIS IS INCORRECT. Phase1Release is only used in case something goes wrong
      takePhase1ReleaseRequest = TakePhase1ReleaseRequest.newBuilder().setClientId(this.clientId).build();

      for (Integer id : delayer) {
        stubs[id].takePhase1Release(takePhase1ReleaseRequest, takePhase1ReleaseObserver);
      }

      takePhase1ReleaseObserver.waitUntilAllReceived(numServers);
      
      /*if (isTupleValid(result)) {
        System.out.println("OK");
        System.out.println(result);
        System.out.print("\n");
      }
      else {
        System.out.printf("Tuple must have the format <element[,more_elements]>" + 
          " but received: %s\n", result);
      }*/
    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + 
                         e.getStatus().getDescription());
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void getTupleSpacesState(int id) {
    getTupleSpacesStateRequest getTupleSpacesStateRequest;

    GetTupleSpacesStateObserver observer = new GetTupleSpacesStateObserver();

    try {
      getTupleSpacesStateRequest = TupleSpacesReplicaXuLiskov.getTupleSpacesStateRequest.getDefaultInstance();

      System.out.println("OK");

      stubs[id].getTupleSpacesState(getTupleSpacesStateRequest, observer);

      observer.printTupleSpace();
      System.out.print("\n");

    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + 
        e.getStatus().getDescription());
    } catch (InterruptedException e) {
      e.printStackTrace();
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

  public static List<String> intersectionOfTuples(List<List<String>> tuplesEachServer) {
    // just making sure there is no problem     
    if (tuplesEachServer == null || tuplesEachServer.isEmpty()) {
        return new ArrayList<String>();
    }

    // Create a set to store common tuples
    Set<String> commonTuples = new HashSet<>(tuplesEachServer.get(0));

    // Iterate through the list of tuples of each server
    for (int i = 1; i < tuplesEachServer.size(); i++) {
        Set<String> tplesOneServer = new HashSet<>(tuplesEachServer.get(i));
        commonTuples.retainAll(tplesOneServer); // Retain only common tuples with the current set
    }

    // Convert set to list and return
    return new ArrayList<String>(commonTuples);
  }
}
