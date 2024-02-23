package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.*;

public class ServerServiceImpl extends TupleSpaces.TupleSpacesImplBase {

	private ServerState serverState = new ServerState();

	@Override
	public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
		String tuple = request.getNewTuple();
		serverState.put(tuple);

		TupleSpacesCentralized.PutResponse response = PutResponse.newBuilder().build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}