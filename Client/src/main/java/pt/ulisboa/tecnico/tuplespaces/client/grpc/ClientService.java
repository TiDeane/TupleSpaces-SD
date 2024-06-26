package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.util.ArrayList;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc.*;

import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc.*;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass.*;

import pt.ulisboa.tecnico.sequencer.contract.SequencerGrpc;
import pt.ulisboa.tecnico.sequencer.contract.SequencerGrpc.SequencerBlockingStub;
import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass.GetSeqNumberRequest;
import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass.GetSeqNumberResponse;

import pt.ulisboa.tecnico.tuplespaces.client.observers.*;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;


public class ClientService {
  /*  This class contains the methods for building a channel and stub, as well
      as individual methods for each remote operation of this service. */

  private static final String BGN_TUPLE = "<";
  private static final String END_TUPLE = ">";

  private static final int SEQUENCER_PORT = 8080;
  private static final String SEQUENCER_HOST = "localhost";

  private int numServers;
  private OrderedDelayer delayer;

  private boolean debugFlag;

  // This array is instantialized inside CommandProcesssor
  public TupleSpacesReplicaStub[] stubs;

  private void debug(String debugMessage) {
		if (debugFlag)
			System.err.println(debugMessage);
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

  public List<String> getServers(String nameServerTarget, String service, String qualifier, boolean dFlag) {
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

      debugFlag = dFlag;
      
      return addressList;

    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + e.getStatus().getDescription());
      nameServerChannel.shutdownNow();
      
      addressList = new ArrayList<String>(); // Return empty list
      return addressList;
    }
    
  }

  /* Contacts the Sequencer service to acquire the calling operation's sequence number */
  public int getSequenceNumber() {
    int seqNumber;
    String target = SEQUENCER_HOST + ":" + SEQUENCER_PORT;
    
    final ManagedChannel sequencerCh = this.buildChannel(target);
    SequencerBlockingStub sequencerStub;
    sequencerStub = SequencerGrpc.newBlockingStub(sequencerCh);

    try { 
      GetSeqNumberResponse seqNumberResponse;
      seqNumberResponse = sequencerStub.getSeqNumber(GetSeqNumberRequest.getDefaultInstance());

      seqNumber = seqNumberResponse.getSeqNumber();

      sequencerCh.shutdownNow();

      return seqNumber;

    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + e.getStatus().getDescription());
      sequencerCh.shutdownNow();
      
      return -1;
    }
  }

  /* This method allows the command processor to set the request delay assigned to a given server */
  public void setDelay(int id, int delay) {
    delayer.setDelay(id, delay);
  }

  public void put(String tuple) {
    PutRequest putRequest;
    int seqNumber = getSequenceNumber();

    if (seqNumber == -1) {
      System.out.println("An error ocurred while acquiring sequence number, aborting Put command");
      return;
    }

    putRequest = PutRequest.newBuilder().setNewTuple(tuple).setSeqNumber(seqNumber).build();

    PutObserver putObserver = new PutObserver();

    try {
      for (Integer id : delayer) {
        stubs[id].put(putRequest, putObserver);
      }

      putObserver.waitUntilAllReceived(numServers);

      System.out.println("OK");
      System.out.print("\n");

    } catch (StatusRuntimeException e) {
      System.out.println("Caught exception with description: " + 
        e.getStatus().getDescription());
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
  }

  public void read(String tuple) {
    ReadRequest readRequest;
    readRequest = ReadRequest.newBuilder().setSearchPattern(tuple).build();
    String result = "";

    ReadObserver readObserver = new ReadObserver();

    try {
      for (Integer id : delayer) {
        stubs[id].read(readRequest, readObserver);
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
    TakeRequest takeRequest;
    int seqNumber = getSequenceNumber();

    if (seqNumber == -1) {
      System.out.println("An error ocurred while acquiring sequence number, aborting Put command");
      return;
    }

    takeRequest = TakeRequest.newBuilder().setSearchPattern(pattern).setSeqNumber(seqNumber).build();

    TakeObserver takeObserver = new TakeObserver();

    try {
      for (Integer id : delayer) {
        stubs[id].take(takeRequest, takeObserver);
      }

      String result = takeObserver.waitUntilAllReceived(numServers);

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

  public void getTupleSpacesState(int id) {
    getTupleSpacesStateRequest tupleSpacesStateRequest;

    GetTupleSpacesStateObserver observer = new GetTupleSpacesStateObserver();

    try {
      tupleSpacesStateRequest = getTupleSpacesStateRequest.getDefaultInstance();

      System.out.println("OK");

      stubs[id].getTupleSpacesState(tupleSpacesStateRequest, observer);

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
}
