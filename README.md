# TupleSpaces

This implementation is based on the **State Machine Replication** (SMR) approach, an alternative to the Xu/Liskov algorithm. This approach focuses on ensuring total order for operations between the three replicas. Like the other implementations, the clients discover the servers' addresses dynamically through the NameServer. The client must also use non-blocking gRPC stubs.

In this alternative, when invoking a _put_ or _take_ operation, the client starts by contacting a remote service (_sequencer_) that provides it with a unique sequence number. Then, the client sends the request to the TupleSpace servers, together with the sequence number.

The servers process _put_/_take_ requests in total order, thus implementing a replicated state machine.
It was up to each group to define the algorithm (executed by the servers) that ensures this objective, taking advantage of the sequence numbers sent with each order.

Note: The implementation of the remote service that provides the sequence numbers was provided by the faculty.

### Team Members

| Number | Name              | User                             | Email                                 |
|--------|-------------------|----------------------------------|---------------------------------------|
| 103811 | Tiago Deane       | <https://github.com/TiDeane>     | <mailto:tiagodeane@tecnico.ulisboa.pt>|
| 104145 | Artur Krystopchuk | <https://github.com/ArturKrys>   | <mailto:arturkrystopchuk@tecnico.ulisboa.pt>|
| 93718  | Guilherme Barata  | <https://github.com/GuiBarata216>| <mailto:guilherme.barata@tecnico.ulisboa.pt>|

## Getting Started

The overall system is made up of several modules. The different types of servers are located in _ServerX_ (where X denotes stage 1, 2 or 3). 
The clients are in _Client_.
The definition of messages and services is in _Contract_. The naming server is in _NameServer_.

Link to the [original Project Statement](https://github.com/tecnico-distsys/TupleSpaces/blob/master/tuplespaces.md).

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

## Installation

### Contract

First, start a virtual environment by running the following commands:

For **Windows:**
```s
python -m venv .venv

.venv\Scripts\activate

python -m pip install grpcio

python -m pip install grpcio-tools
```

Or **Linux:**
```s
python3 -m venv .venv

source .venv/bin/activate

python3 -m pip install grpcio

python3 -m pip install grpcio-tools
```

Then, inside the ``Contract\`` folder, run the following commands:

```s
mvn install

mvn exec:exec
```

The command to exit the virtual environment is ``deactivate``, but do not exit it yet.

### Server

In another terminal, outside the virtual environment, enter the ``ServerR3\`` folder and run the following command:

```s
mvn clean compile
```

### Client

Also outside the virtual environment, enter the ``Client\`` folder and run the same command:

```s
mvn clean compile
```

### Sequencer

Finally, outside the virtual environment, enter the ``Sequencer\`` folder and run the same command:

```s
mvn clean compile
```

## Running Instructions

### NameServer

While inside your virtual environment, go to the ``NameServer\`` folder and execute the following command:

```s
python3 server.py
```

For additional debug information, append ``-debug`` to the end of the command.

You do not need to use the virtual environment for any further steps.

### Sequencer

This implementation uses a Sequencer to establish total order between the three servers. To run the sequencer, go into the ``Sequencer\`` folder and run the following command:

```s
mvn exec:java
``` 

### TupleSpace Servers

Afterwards, run 3 different TupleSpace servers by going into the ``ServerR3\`` folder and running the following command:

```s
mvn exec:java -D exec.args="<port> <qualifier>"
``` 

Where **\<port\>** is the server's port and **\<qualifier\>** is 'A', 'B' or 'C'. There can only be one server for each qualifier, and the server's IP is always _localhost_. For additional debug information, append ``-Ddebug`` to the end of the command.

Each server will automatically connect to the NameServer to register itself, and again upon termination to remove/unregister itself.

### Clients

You can connect clients to the servers by going into the ``Client\`` folder and running:

```s
mvn exec:java
```

Clients will automatically connect to the NameServer to look for available servers with the service name "**TupleSpace**", creating non-blocking stubs to each server or shutting down if less than 3 servers are available (A, B and C). For additional debug information, append ``-Ddebug`` to the end of the command.

## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.
