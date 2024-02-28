package pt.ulisboa.tecnico.tuplespaces.server;

import java.util.List;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc.TupleSpacesImplBase;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

public class ServerServiceImpl extends TupleSpacesImplBase {
	private ServerState serverState = new ServerState();

	@Override
	public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
		String tuple = request.getNewTuple();

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

	/*@Override
	public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
		List<String> servers = serverState.getTupleSpacesState();

		TupleSpacesCentralized.LookupResponse response = TupleSpacesCentralized.LookupResponse.newBuilder()
			.addAllServers(servers).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();		
	}*/
}