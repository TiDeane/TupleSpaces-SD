package pt.ulisboa.tecnico.tuplespaces.client;

//import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {
    public static void main(String[] args) {

        System.out.println(ClientMain.class.getSimpleName());

        // receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }

        // check arguments
        if (args.length != 2) { // Codigo base diz != 3
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: mvn exec:java -D exec.args=<host> <port>");
            return;
        }

        // get the host and the port
        final String host = args[0];
        final String port = args[1];
        final String target = host + ":" + port;

        CommandProcessor parser = new CommandProcessor(new ClientService());
        parser.parseInput(target);

        //channel.shutdownNow();
    }
}
