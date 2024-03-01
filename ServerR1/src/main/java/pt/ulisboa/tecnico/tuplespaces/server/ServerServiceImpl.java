package pt.ulisboa.tecnico.tuplespaces.server;

import java.util.List;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc.TupleSpacesImplBase;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

import static io.grpc.Status.INVALID_ARGUMENT;

public class ServerServiceImpl extends TupleSpacesImplBase {
    private static final String BGN_TUPLE = "<";
    private static final String END_TUPLE = ">";

	private ServerState serverState = new ServerState();

	@Override
	public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
		String tuple = request.getNewTuple();

		if (!isTupleValid(tuple)) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple must have the format <element[,more_elements]>").asRuntimeException());
			return;
		}

		synchronized (serverState) {
			serverState.put(tuple);
			serverState.notifyAll();
		}

		TupleSpacesCentralized.PutResponse response = PutResponse.newBuilder().build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
		String pattern = request.getSearchPattern();
		String tuple = serverState.take(pattern);

		if (!isTupleValid(tuple)) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple must have the format <element[,more_elements]>").asRuntimeException());
			return;
		}

		synchronized (serverState) {
			while (tuple == null) {
				try {
					serverState.wait();
					tuple = serverState.take(pattern);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					System.err.println("Interrupted while waiting: " + e.getMessage());		  
				}
			}
		}

		TupleSpacesCentralized.TakeResponse response = TakeResponse.newBuilder()
			.setResult(tuple).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
		String pattern = request.getSearchPattern();
		String tuple = serverState.read(pattern);

		if (!isTupleValid(tuple)) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple must have the format <element[,more_elements]>").asRuntimeException());
			return;
		}

		synchronized (serverState) {
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

		TupleSpacesCentralized.ReadResponse response = ReadResponse.newBuilder()
			.setResult(tuple).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void getTupleSpacesState(GetTupleSpacesStateRequest request, StreamObserver<GetTupleSpacesStateResponse> responseObserver) {
		List<String> tuples = serverState.getTupleSpacesState();

		TupleSpacesCentralized.GetTupleSpacesStateResponse response = TupleSpacesCentralized.GetTupleSpacesStateResponse.newBuilder()
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
