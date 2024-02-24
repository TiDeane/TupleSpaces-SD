package pt.ulisboa.tecnico.tuplespaces.server;

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
		serverState.put(tuple);

		TupleSpacesCentralized.PutResponse response = PutResponse.newBuilder().build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
		String pattern = request.getSearchPattern();
		String tuple = serverState.take(pattern);

		TupleSpacesCentralized.TakeResponse response = TakeResponse.newBuilder().setResult(tuple).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
		String pattern = request.getSearchPattern();
		String tuple = serverState.read(pattern);

		TupleSpacesCentralized.ReadResponse response = ReadResponse.newBuilder().setResult(tuple).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
	
}