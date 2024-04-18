# TupleSpaces

The service is replicated in three servers (A, B and C), following an adaptation of the [Xu-Liskov algorithm](http://www.ai.mit.edu/projects/aries/papers/programming/linda.pdf). Like the previous implementation, this variant discovers the servers' addresses dynamically through the NameServer. The client must also use non-blocking gRPC stubs.

For _put_ and _read_ operations, the client starts by sending the request to all servers and then waits for responses (from one server, in the case of read, or from all servers, in the case of put).

Before explaining the _take_ operation, which is executed in two steps, we must learn about the changes to the TupleSpace made to support this:

Each tuple maintained on the server, in addition to its _string_, has the following additional fields: a _flag_ that indicates whether the tuple is locked by a client (which executed the first step of the _take_ operation but has not yet completed the second step), and an identifier for that client. The client identifier is a unique number that the customer passes along with _take_ requests. (Note: this aspect partially differs from the original article.)

The _take_ operation in this implementation works as follows:

- **TakePhase1**: The client sends a request to all servers with the tuple he wants to remove (can include regular expressions) along with his _clientId_. Upon receiving this request, the servers "lock" all tuples in the TupleSpace that match the pattern and store the client's ID within each tuple. The servers then reply with a list of all the locked tuples.

- **TakePhase1Release**: This is a partial step; if the intersection of the lists of tuples returned by the 3 servers is empty, the client aborts the _take_ operation by sending a requests containing his _clientId_. The servers then unlock all the tuples locked by the client (using his _clientId_).

- **TakePhase2**: If TakePhase1 was successful, the client sends a request to all servers with his desired tuple and his _clientId_. The servers then remove that tuple from the TupleSpace and unlock all tuples locked by the client (using his _clientId_).


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

In another terminal, outside the virtual environment, enter the ``ServerR2\`` folder and run the following commands:

```s
mvn clean

mvn compile
```

### Client

Finally, in a terminal that is outside the virtual environment, enter the ``Client\`` folder and run the same commands:

```s
mvn clean

mvn compile
```

## Running Instructions

### NameServer

While inside your virtual environment, go to the ``NameServer\`` folder and execute the following command:

```s
python3 server.py
```

For additional debug information, append ``-debug`` to the end of the command.

You do not need to use the virtual environment for any further steps.

### TupleSpace Servers

Afterwards, run 3 different TupleSpace servers by going into the ``ServerR2\`` folder and running the following command:

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
