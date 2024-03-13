package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc;

import io.grpc.ManagedChannel;
import java.util.Scanner;

import java.util.List;

public class CommandProcessor {

    private static final String SPACE = " ";
    private static final String BGN_TUPLE = "<";
    private static final String END_TUPLE = ">";
    private static final String PUT = "put";
    private static final String READ = "read";
    private static final String TAKE = "take";
    private static final String SLEEP = "sleep";
    private static final String SET_DELAY = "setdelay";
    private static final String EXIT = "exit";
    private static final String GET_TUPLE_SPACES_STATE = "getTupleSpacesState";

    private boolean debugFlag;

    private final ClientService clientService;

    private int numServers;

    // An array of channels
    private ManagedChannel[] channels;

    // An array of stubs
    private TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[] stubs;

    public CommandProcessor(ClientService clientService) {
        this.clientService = clientService;
    }

    /** Helper method to print debug messages. */
	private void debug(String debugMessage) {
		if (debugFlag)
			System.err.println(debugMessage);
	}

    void parseInput(String nameServerTarget, String service, String qualifier, boolean dFlag) {

        debugFlag = dFlag;

        List<String> servers;
        servers = clientService.getServers(nameServerTarget, service, qualifier, dFlag);
        numServers = servers.size();

        if (servers.size() < 3) {
            System.out.println("There are not enough servers available with the given service name, " + 
                                "or with the given service name and qualifier");
            return;
        }

        stubs = new TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[numServers];
        channels = new ManagedChannel[numServers];

        for (int i = 0; i < numServers; i += 1) {
            channels[i] = clientService.buildChannel(servers.get(i));
            stubs[i] = clientService.buildStub(channels[i]);
            debug("Successfully connected to: " + servers.get(i));
        }

        clientService.setStubs(stubs);
    
        try (Scanner scanner = new Scanner(System.in)) {
            boolean exit = false;

            while (!exit) {
                System.out.print("> ");
                String line = scanner.nextLine().trim();
                String[] split = line.split(SPACE);
                switch (split[0]) {
                    case PUT:
                        this.put(split);
                        break;

                    case READ:
                        this.read(split);
                        break;

                    case TAKE:
                        this.take(split);
                        break;

                    case GET_TUPLE_SPACES_STATE:
                        this.getTupleSpacesState(split);
                        break;

                    case SLEEP:
                        this.sleep(split);
                        break;

                    case SET_DELAY:
                        this.setdelay(split);
                        break;

                    case EXIT:
                        exit = true;
                        break;

                    default:
                        this.printUsage();
                        break;
                }
            }
        }
        
        for (ManagedChannel ch : channels)
            ch.shutdown();
    }

    private void put(String[] split){
        // check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }
        
        // get the tuple
        String tuple = split[1];
        debug("Sending a Put request to all servers with tuple: " + tuple);
        clientService.put(tuple);
    }

    private void read(String[] split){
        // check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }

        String pattern = split[1];
        debug("Sending a Read request to all servers with pattern: " + pattern);
        clientService.read(pattern);
    }

    private void take(String[] split){
        // check if input is valid
       if (!this.inputIsValid(split)) {
           this.printUsage();
           return;
       }
       
       String pattern = split[1];
       debug("Sending a Take request to all servers with pattern: " + pattern);
       clientService.take(pattern);
   }

   private void getTupleSpacesState(String[] split){
       // check if input is valid
       if (split.length != 2){
           this.printUsage();
           return;
       }

       String qualifier = split[1];
       int qualifierIndex = indexOfServerQualifier(split[1]);

       if (qualifierIndex == -1) {
          System.out.println("Invalid server qualifier");
          return;
       }

       // get the tuple spaces state
       debug("Getting TupleSpaceState for server with qualifier: " + qualifier);
       clientService.getTupleSpacesState(qualifierIndex);
   }

    private void sleep(String[] split) {
      if (split.length != 2){
        this.printUsage();
        return;
      }
      Integer time;

      // checks if input String can be parsed as an Integer
      try {
         time = Integer.parseInt(split[1]);
      } catch (NumberFormatException e) {
        this.printUsage();
        return;
      }

      try {
        debug("Going to sleep.");
        Thread.sleep(time*1000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    private void setdelay(String[] split) {
        if (split.length != 3){
          this.printUsage();
          return;
        }
        int qualifier = indexOfServerQualifier(split[1]);
        if (qualifier == -1)
          System.out.println("Invalid server qualifier");
  
        Integer time;
  
        // checks if input String can be parsed as an Integer
        try {
          time = Integer.parseInt(split[2]);
        } catch (NumberFormatException e) {
          this.printUsage();
          return;
        }
        // register delay <time> for when calling server <qualifier>
        this.clientService.setDelay(qualifier, time);
    }

    private void printUsage() {
        System.out.println("Usage:\n" +
                "- put <element[,more_elements]>\n" +
                "- read <element[,more_elements]>\n" +
                "- take <element[,more_elements]>\n" +
                "- getTupleSpacesState <server>\n" +
                "- sleep <integer>\n" +
                "- setdelay <server> <integer>\n" +
                "- exit\n");
    }

    private int indexOfServerQualifier(String qualifier) {
        switch (qualifier) {
            case "A":
                return 0;
            case "B":
                return 1;
            case "C":
                return 2;
            default:
                return -1;
        }
    }

    private boolean inputIsValid(String[] input){
        if (input.length < 2 
            ||
            !input[1].substring(0,1).equals(BGN_TUPLE) 
            || 
            !input[1].endsWith(END_TUPLE)
            || 
            input.length > 2
            ) {
            this.printUsage();
            return false;
        }
        else {
            return true;
        }
    }

}
