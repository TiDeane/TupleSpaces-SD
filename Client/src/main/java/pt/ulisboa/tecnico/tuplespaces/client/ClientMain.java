package pt.ulisboa.tecnico.tuplespaces.client;

//import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {

    /** Set flag to true to print debug messages. 
	 * The flag can be set using the -Ddebug command line option. */
	private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

	/** Helper method to print debug messages. */
	private static void debug(String debugMessage) {
		if (DEBUG_FLAG)
			System.err.println(debugMessage);
	}
    public static void main(String[] args) {

        // receive and print arguments
		debug(String.format("Received %d arguments", args.length));
		for (int i = 0; i < args.length; i++) {
			debug(String.format("arg[%d] = %s", i, args[i]));
		}

         // check arguments
         if (args.length != 4) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java");
            return;
        }

        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        final String nameServerTarget = host + ":" + port;
        debug("Target: " + nameServerTarget);

        final String service = args[2];
        String qualifier = args[3];

        if (qualifier.compareTo("A") == 0  || qualifier.compareTo("B") == 0 || qualifier.compareTo("C") == 0) {
            debug("Service and Qualifier: " + service + " " + qualifier);
        } else {
            qualifier = "";
            debug("Service: " + service + "\nQualifier: none");
        }

        CommandProcessor parser = new CommandProcessor(new ClientService());
        parser.parseInput(nameServerTarget, service, qualifier, DEBUG_FLAG);
        
    }
}
