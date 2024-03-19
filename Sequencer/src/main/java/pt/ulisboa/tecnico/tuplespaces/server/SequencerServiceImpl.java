package pt.ulisboa.tecnico.tuplespaces.sequencer;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass;
import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass.*;
import pt.ulisboa.tecnico.sequencer.contract.SequencerGrpc.SequencerImplBase;

public class SequencerServiceImpl extends SequencerImplBase {

	private boolean debugFlag = false;

	private int seqNumber = 0;

	public SequencerServiceImpl(boolean dFlag) {
		debugFlag = dFlag;
	}

	/** Helper method to print debug messages. */
	private void debug(String debugMessage) {
		if (debugFlag)
			System.err.println(debugMessage);
	}

	@Override
	public void getSeqNumber(GetSeqNumberRequest request, StreamObserver<GetSeqNumberResponse> responseObserver) {
		GetSeqNumberResponse response;
		int sentSeqNumber;

		synchronized(this) {
			sentSeqNumber = this.seqNumber;
			response = GetSeqNumberResponse.newBuilder().setSeqNumber(sentSeqNumber).build();
			this.seqNumber++;
		}

		debug("--------------------");
		debug("Received GetSequenceNumber request, returning " + sentSeqNumber);

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}
