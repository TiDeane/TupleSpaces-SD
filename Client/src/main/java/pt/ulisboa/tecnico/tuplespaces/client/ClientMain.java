package pt.ulisboa.tecnico.tuplespaces.client;

//import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {
    public static void main(String[] args) {

        System.out.println(ClientMain.class.getSimpleName());

        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        final String nameServerTarget = host + ":" + port;

        final String service = args[2];
        final String qualifier = args[3];

        CommandProcessor parser = new CommandProcessor(new ClientService());
        parser.parseInput(nameServerTarget, service, qualifier);
        
    }
}
