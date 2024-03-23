package pt.ulisboa.tecnico.tuplespaces.server;

import java.util.List;
import java.util.ArrayList;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.*;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase;

import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import pt.ulisboa.tecnico.tuplespaces.server.domain.TakeObj;

import static io.grpc.Status.INVALID_ARGUMENT;

public class ServerServiceImpl extends TupleSpacesReplicaImplBase {
    private static final String BGN_TUPLE = "<";
    private static final String END_TUPLE = ">";

	private boolean debugFlag = false;

	private int nextOp;

	private ServerState serverState = new ServerState();

	public ServerServiceImpl(boolean dFlag) {
		debugFlag = dFlag;
		nextOp = 1;
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
		debug("Received Put request with tuple " + tuple + " and sequence number " + seqNumber +
			", and nextOp is currently " + nextOp);

		if (!isTupleValid(tuple)) {
			debug("Tuple is not valid, sending exception");
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple must have the format <element[,more_elements]>").asRuntimeException());
			return;
		}

		synchronized (serverState) {
			try {
				while (seqNumber != nextOp) {
					serverState.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			serverState.put(tuple);
			
			nextOp++;
			
			serverState.awakeReadThreads(tuple);
			serverState.awakeTakeThread(tuple);

			serverState.notifyAll();
		}

		debug("Successfully put tuple (sequence number "+seqNumber+"), sending response");
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
			debug("Pattern has the wrong format, sending exception");
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple must have the format <element[,more_elements]>").asRuntimeException());
			return;
		}

		Thread thread = Thread.currentThread();

		synchronized (thread) {
			tuple = serverState.read(pattern);
			
			if (tuple == null) {
				debug("There is no tuple that matches the given pattern, " +
					  "putting the client on hold");				
					  
				serverState.addReadThread(thread, pattern);
				try {
					// The thread is only woken up when there's a tuple that matches
					// its desired pattern
					thread.wait();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					System.err.println("Interrupted while waiting: " + e.getMessage());		  
				}

				tuple = serverState.read(pattern);
				serverState.removeReadThread(thread);
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
		String pattern = request.getSearchPattern();
		int seqNumber = request.getSeqNumber();
		String tuple;

		debug("--------------------");
		debug("Received Take request with pattern " + pattern + " and sequence number " + seqNumber +
			", and nextOp is currently " + nextOp);

		if (!isTupleValid(pattern)) {
			debug("Pattern has the wrong format, sending exception");
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple must have the format <element[,more_elements]>").asRuntimeException());
			return;
		}

		synchronized (serverState) {
			try {
				while (seqNumber != nextOp) {
					serverState.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			tuple = serverState.take(pattern);
		}

		// Increments nextOp regardless of whether the operation was successful
		nextOp++;

		Thread thread = Thread.currentThread();
		TakeObj takeObj = new TakeObj(thread, seqNumber);

		if (tuple == null) {
			debug("There is no tuple that matches the given pattern, " +
					"putting the client on hold");
		}
		
		synchronized(thread) {
			while (tuple == null) {
				serverState.addTakeObj(pattern, takeObj);
				try {
					thread.wait();
					tuple = serverState.take(pattern);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					System.err.println("Interrupted while waiting: " + e.getMessage());		  
				}
			}
		}

		debug("The first tuple read that matches the pattern is " + tuple + ", sending response (sequence number "+seqNumber+")");
		TakeResponse response = TakeResponse.newBuilder()
			.setResult(tuple).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
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
