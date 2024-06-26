package pt.ulisboa.tecnico.tuplespaces.server;

import java.io.IOException;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.nameserver.contract.NameServerGrpc;
import pt.ulisboa.tecnico.nameserver.contract.NameServerOuterClass;


public class ServerMain {

	/** Set flag to true to print debug messages. 
	 * The flag can be set using the -Ddebug command line option. */
	private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

	private static final int NAME_SERVER_PORT = 5001;

	/** Helper method to print debug messages. */
	private static void debug(String debugMessage) {
		if (DEBUG_FLAG)
			System.err.println(debugMessage);
	}

    public static void main(String[] args) throws IOException, InterruptedException {

		debug(String.format("Received %d arguments", args.length));
		for (int i = 0; i < args.length; i++) {
			debug(String.format("arg[%d] = %s", i, args[i]));
		}

		if (args.length != 2) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: mvn exec:java -Dexec.args=\"<port> <qualifier>\"");
			return;
		}

		final String host = "localhost";
		final int port = Integer.parseInt(args[0]);
		final String qualifier = args[1];
		final int nameServerPort = NAME_SERVER_PORT;
		final BindableService impl = new ServerServiceImpl(DEBUG_FLAG);

		Server server = ServerBuilder.forPort(port).addService(impl).build();

		final String localAddress = host + ":" + port;
		final String target = host + ":" + nameServerPort;

		debug("Connecting to NameServer...");
		final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
		NameServerGrpc.NameServerBlockingStub stub = NameServerGrpc.newBlockingStub(channel);

		// Registers the TupleSpaces server into the NameServer
		try {
			NameServerOuterClass.RegisterRequest registerRequest;

			registerRequest = NameServerOuterClass.RegisterRequest.newBuilder().
				setName("TupleSpace").setQualifier(qualifier).
				setAddress(localAddress).build();

				debug("Sending Register request of service TupleSpace with address " +
						localAddress + " and Qualifier " + qualifier);

      		stub.register(registerRequest);

		} catch (StatusRuntimeException e) {
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());

			server.shutdown();
			System.exit(1);
		}

		// Start the server
		server.start();

		// Server threads are running in the background.
		System.out.println("Server started");

		// Removes server from the NameServer upon shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				NameServerOuterClass.DeleteRequest deleteRequest;
	
				deleteRequest = NameServerOuterClass.DeleteRequest.newBuilder().
					setName("TupleSpace").setAddress(localAddress).build();

				debug("Sending Delete request of service TupleSpace with address " + localAddress);
	
				stub.delete(deleteRequest);
	
			} catch (StatusRuntimeException e) {
				System.out.println("Caught exception with description: " + e.getStatus().getDescription());
			}
            System.out.println("Shutting down server...");
            server.shutdown();
        }));

		// Do not exit the main thread. Wait until server is terminated.
		server.awaitTermination();
    }
}