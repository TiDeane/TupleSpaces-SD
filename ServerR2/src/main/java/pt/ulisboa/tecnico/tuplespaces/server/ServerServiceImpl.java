package pt.ulisboa.tecnico.tuplespaces.server;

import java.util.List;
import java.util.ArrayList;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.*;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

import static io.grpc.Status.INVALID_ARGUMENT;

public class ServerServiceImpl extends TupleSpacesReplicaImplBase {
    private static final String BGN_TUPLE = "<";
    private static final String END_TUPLE = ">";

	private boolean debugFlag = false;

	private ServerState serverState = new ServerState();

	public ServerServiceImpl(boolean dFlag) {
		debugFlag = dFlag;
	}

	/** Helper method to print debug messages. */
	private void debug(String debugMessage) {
		if (debugFlag)
			System.err.println(debugMessage);
	}

	@Override
	public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
		String tuple = request.getNewTuple();

		debug("--------------------");
		debug("Received Put request with tuple: " + tuple);

		if (!isTupleValid(tuple)) {
			debug("Tuple is not valid, sending exception");
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple must have the format <element[,more_elements]>").asRuntimeException());
			return;
		}

		synchronized (serverState) {
			serverState.put(tuple);
			serverState.notifyAll();
		}

		debug("Successfully put tuple, sending response");
		PutResponse response = PutResponse.newBuilder().build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
		String pattern = request.getSearchPattern();
		String tuple;

		debug("--------------------");
		debug("Received Read request with pattern: " + pattern);

		if (!isTupleValid(pattern)) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple must have the format <element[,more_elements]>").asRuntimeException());
			return;
		}

		synchronized (serverState) {
			tuple = serverState.read(pattern);

			if (tuple == null)
				debug("There is no tuple that matches the given pattern, " +
					  "putting the client on hold");
			
			while (tuple == null) {
				try {
					serverState.wait();
					tuple = serverState.read(pattern);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					System.err.println("Interrupted while waiting: " + e.getMessage());		  
				}
			}
		}

		debug("The first tuple read that matches the pattern is " + tuple + ", sending response");
		TupleSpacesReplicaXuLiskov.ReadResponse response = ReadResponse.newBuilder()
			.setResult(tuple).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void takePhase1(TakePhase1Request request, StreamObserver<TakePhase1Response> responseObserver) {
		TakePhase1Response response;
		String pattern = request.getSearchPattern();
		int clientId = request.getClientId();
		List<String> matchingTuples;

		debug("--------------------");
		debug("Received Take request with pattern: " + pattern);

		if (!isTupleValid(pattern)) {
			debug("Pattern is not valid, sending exception");
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple must have the format <element[,more_elements]>").asRuntimeException());
			return;
		}

		synchronized (serverState) {
			// Gets the list of tuples that match the pattern, locking them in the process
			matchingTuples = serverState.getAllMatchingFreeTuples(pattern, clientId);
			serverState.addClientWaitingTake(clientId);

			if (matchingTuples.isEmpty())
				debug("There is no tuple that matches the given pattern, " +
					  "or all the matching tuples are locked," +
					  "putting the client on hold");

			while (matchingTuples.isEmpty() && serverState.isClientWaitingTake(clientId)) {
				try {
					serverState.wait();
					matchingTuples = serverState.getAllMatchingFreeTuples(pattern, clientId);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					System.err.println("Interrupted while waiting: " + e.getMessage());		  
				}
			}
			serverState.removeClientWaitingTake(clientId);
		}

		debug("Sending list of all the tuples that aren't locked and match the pattern to client " + clientId);
		response = TakePhase1Response.newBuilder().addAllReservedTuples(matchingTuples).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void takePhase1Release(TakePhase1ReleaseRequest request, StreamObserver<TakePhase1ReleaseResponse> responseObserver) {
		int clientId = request.getClientId();

		debug("--------------------");
		debug("Received TakePhase1Release request from client " + clientId);

		synchronized (serverState) {
			serverState.unlockClientTuples(clientId);
			serverState.removeClientWaitingTake(clientId);
			serverState.notifyAll();
		}

		debug("Successfully unlocked all tuples locked by client " + clientId);

		responseObserver.onNext(TakePhase1ReleaseResponse.newBuilder().build());
		responseObserver.onCompleted();
	}

	@Override
	public void takePhase2(TakePhase2Request request, StreamObserver<TakePhase2Response> responseObserver) {
		int clientId = request.getClientId();
		String tuple = request.getTuple();

		debug("--------------------");
		debug("Inside TakePhase2 for client: " + clientId);

		synchronized (serverState) {
			serverState.take(tuple);

			debug("Successfully removed the tuple: " + tuple);

			serverState.unlockClientTuples(clientId);
		}

		debug("Successfully unlocked the other tuples locked by the client, sending the removed tuple");

		responseObserver.onNext(TakePhase2Response.newBuilder().build());
		responseObserver.onCompleted();
	}


	@Override
	public void getTupleSpacesState(getTupleSpacesStateRequest request, StreamObserver<getTupleSpacesStateResponse> responseObserver) {
		List<String> tuples = serverState.getTupleSpacesState();

		debug("--------------------");
		debug("Received GetTupleSpacesState request, sending response");

		TupleSpacesReplicaXuLiskov.getTupleSpacesStateResponse response = TupleSpacesReplicaXuLiskov.getTupleSpacesStateResponse.newBuilder()
			.addAllTuple(tuples).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();		
	}

	private boolean isTupleValid(String tuple){
        if (!tuple.substring(0,1).equals(BGN_TUPLE) 
            || 
            !tuple.endsWith(END_TUPLE)) {
            return false;
        }
        else {
            return true;
        }
    }
}
