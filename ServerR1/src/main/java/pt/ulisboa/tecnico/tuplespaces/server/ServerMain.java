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

    public static void main(String[] args) throws IOException, InterruptedException {

		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// check arguments
		if (args.length != 2) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: mvn exec:java -Dexec.args=\"<port> <qualifier>\"");
			return;
		}

		final String host = "localhost";
		final int port = Integer.parseInt(args[0]);
		final String qualifier = args[1];
		final int nameServerPort = 5001;
		final BindableService impl = new ServerServiceImpl();

		// Create a new server to listen on port
		Server server = ServerBuilder.forPort(port).addService(impl).build();

		final String localAddress = host + ":" + port;
		final String target = host + ":" + nameServerPort;

		// Building channel to NameServer
		final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
		NameServerGrpc.NameServerBlockingStub stub = NameServerGrpc.newBlockingStub(channel);

		// Registers the TupleSpaces server into the NameServer
		try {
			NameServerOuterClass.RegisterRequest registerRequest;

			registerRequest = NameServerOuterClass.RegisterRequest.newBuilder().
				setName("TupleSpace").setQualifier(qualifier).
				setAddress(localAddress).build();

      		stub.register(registerRequest);

		} catch (StatusRuntimeException e) {
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());

			server.shutdown();
			return;
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
	
				stub.delete(deleteRequest);
	
			} catch (StatusRuntimeException e) {
				System.out.println("Caught exception with description: " + e.getStatus().getDescription());
			}
            System.out.println("Shutting down server...");
            server.shutdown();
            System.out.println("Server shut down.");
        }));

		// Do not exit the main thread. Wait until server is terminated.
		server.awaitTermination();
    }
}