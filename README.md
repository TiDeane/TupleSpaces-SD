# TupleSpaces

Distributed Systems Project 2024

**Group A02**

**Difficulty level: I am Death incarnate!**


### Code Identification

In all source files (namely in the *groupId*s of the POMs), replace __GXX__ with your group identifier. The group
identifier consists of either A or T followed by the group number - always two digits. This change is important for 
code dependency management, to ensure your code runs using the correct components and not someone else's.

### Team Members

| Number | Name              | User                             | Email                                 |
|--------|-------------------|----------------------------------|---------------------------------------|
| 103811 | Tiago Deane       | <https://github.com/TiDeane>     | <mailto:tiagodeane@tecnico.ulisboa.pt>|
| 104145 | Artur Krystopchuk | <https://github.com/ArturKrys>   | <mailto:bob@tecnico.ulisboa.pt>       |
| 93718  | Guilherme Barata  | <https://github.com/GuiBarata216>| <mailto:guilherme.barata@tecnico.ulisboa.pt>|

## Getting Started

The overall system is made up of several modules. The different types of servers are located in _ServerX_ (where X denotes stage 1, 2 or 3). 
The clients is in _Client_.
The definition of messages and services is in _Contract_. The naming server
is in _NameServer_.

See the [Project Statement](https://github.com/tecnico-distsys/TupleSpaces) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

## Installation

### Server and Client

First, go the ``ServerR1\`` folder and execute the following command:

```s
mvn install
```

Afterwards, go to the ``Client\`` folder and execute the same command.

### Contract

For the Contract folder, start a virtual environment by running the following commands:

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

## Running Instructions

### NameServer

While inside your virtual environment, go to the ``NameServer\`` folder and execute the following command:

```s
python3 server.py
```

For additional debug information, append ``-debug`` to the end of the command.

You do not need to use the virtual environment for any further steps.

### TupleSpace Servers

Afterwards, you can run any number of TupleSpace servers by going into the ``ServerR1\`` folder and running the following command:

```s
mvn exec:java -D exec.args="<port> <qualifier>"
``` 

Where **\<port\>** is the server's port and **\<qualifier\>** is 'A', 'B' or 'C'. The server's IP is always _localhost_. For additional debug information, append ``-Ddebug`` to the end of the command.

Each server will automatically connect to the NameServer to register itself, and again upon termination to remove/unregister itself.

### Clients

You can connect clients to the servers by going into the ``Client\`` folder and running:

```s
mvn exec:java
```

Clients will automatically connect to the NameServer to look for available servers with the name "**TupleSpace**" and qualifier "**A**", choosing the first one from the list returned by the NameServer or shutting down if no servers are available. For additional debug information, append ``-Ddebug`` to the end of the command.

The NameServer returns all servers with the given service name and qualifier, or all servers with the given service name if no qualifier is specified.

## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.
