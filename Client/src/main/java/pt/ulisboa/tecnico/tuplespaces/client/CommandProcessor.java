package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.PutRequest;
import pt.ulisboa.tecnico.tuplespaces.*;

import io.grpc.ManagedChannel;

import java.util.Scanner;

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

    private final ClientService clientService;

    public CommandProcessor(ClientService clientService) {
        this.clientService = clientService;
    }

    void parseInput(String target) {

        ManagedChannel channel = clientService.buildChannel(target);
        TupleSpacesGrpc.TupleSpacesBlockingStub stub = clientService.buildStub(channel);

        try (Scanner scanner = new Scanner(System.in)) {
            boolean exit = false;

            while (!exit) {
                System.out.print("> ");
                String line = scanner.nextLine().trim();
                String[] split = line.split(SPACE);
                switch (split[0]) {
                    case PUT:
                        this.put(split, stub);
                        break;

                    case READ:
                        this.read(split, stub);
                        break;

                    case TAKE:
                        this.take(split, stub);
                        break;

                    case GET_TUPLE_SPACES_STATE:
                        this.getTupleSpacesState(split, stub);
                        break;

                    case SLEEP:
                        this.sleep(split, stub);
                        break;

                    case SET_DELAY:
                        this.setdelay(split, stub);
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
    }

    private void put(String[] split, TupleSpacesGrpc.TupleSpacesBlockingStub stub){

        // check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }
        
        // get the tuple
        String tuple = split[1];
        clientService.put(tuple, stub);
        System.out.println("OK");

        // put the tuple
        // operate through clientService
    }

    private void read(String[] split, TupleSpacesGrpc.TupleSpacesBlockingStub stub){
        // check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }
        
        // get the tuple
        String tuple = split[1];

        // read the tuple
        System.out.println("TODO: implement read command");
    }


    private void take(String[] split, TupleSpacesGrpc.TupleSpacesBlockingStub stub){
         // check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }
        
        // get the tuple
        String tuple = split[1];

        // take the tuple
        System.out.println("TODO: implement take command");
    }

    private void getTupleSpacesState(String[] split, TupleSpacesGrpc.TupleSpacesBlockingStub stub){

        if (split.length != 2){
            this.printUsage();
            return;
        }
        String qualifier = split[1];

        // get the tuple spaces state
        System.out.println("TODO: implement getTupleSpacesState command");

    }

    private void sleep(String[] split, TupleSpacesGrpc.TupleSpacesBlockingStub stub) {
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
        Thread.sleep(time*1000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    private void setdelay(String[] split, TupleSpacesGrpc.TupleSpacesBlockingStub stub) {
      if (split.length != 3){
        this.printUsage();
        return;
      }
      String qualifier = split[1];
      Integer time;

      // checks if input String can be parsed as an Integer
      try {
        time = Integer.parseInt(split[2]);
      } catch (NumberFormatException e) {
        this.printUsage();
        return;
      }

      // register delay <time> for when calling server <qualifier>
      System.out.println("TODO: implement setdelay command (only needed in phases 2+3)");
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
