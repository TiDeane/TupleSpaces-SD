package pt.ulisboa.tecnico.tuplespaces.server;

import java.util.List;
import java.util.ArrayList;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

import static io.grpc.Status.INVALID_ARGUMENT;

public class ServerServiceImpl extends TupleSpacesReplicaImplBase {
    private static final String BGN_TUPLE = "<";
    private static final String END_TUPLE = ">";

	private boolean debugFlag = false;

	private int nextOp;

	private ServerState serverState = new ServerState();

	public ServerServiceImpl(boolean dFlag) {
		debugFlag = dFlag;
		nextOp = 0;
	}

	/** Helper method to print debug messages. */
	private void debug(String debugMessage) {
		if (debugFlag)
			System.err.println(debugMessage);
	}

	@Override
	public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
		String tuple = request.getNewTuple();
		int seqNumber = request.getSeqNumber();

		debug("--------------------");
		debug("Received Put request with tuple " + tuple + " and sequence number " + seqNumber);

		if (!isTupleValid(tuple)) {
			debug("Tuple is not valid, sending exception");
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple must have the format <element[,more_elements]>").asRuntimeException());
			return;
		}

		try {
			while (seqNumber != nextOp) {
				wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		synchronized (serverState) {
			try {
				while (seqNumber != nextOp) {
					wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			serverState.put(tuple);
			nextOp++;
			serverState.notifyAll(); // NOTE: Maybe this should be out of the synchronized block?
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
		ReadResponse response = ReadResponse.newBuilder()
			.setResult(tuple).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
		/*TakePhase1Response response;
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

			// Client is waiting for Take operation (hasn't sent a Release request)
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
		responseObserver.onCompleted();*/
	}

	@Override
	public void getTupleSpacesState(getTupleSpacesStateRequest request, StreamObserver<getTupleSpacesStateResponse> responseObserver) {
		List<String> tuples = serverState.getTupleSpacesState();

		debug("--------------------");
		debug("Received GetTupleSpacesState request, sending response");

		getTupleSpacesStateResponse response = getTupleSpacesStateResponse.newBuilder()
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