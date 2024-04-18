# TupleSpaces

The goal of this project was to develop the **TupleSpace** system, a service that implements a distributed _tuple space_ using gRPC and Java (along with a Python NameServer). There are **three different implementations**, each in their respective branches, explained further below.

## What is a TupleSpace?

The service allows one or more users (also called _workers_ in the literature) to place tuples in the shared space, read existing tuples, as well as remove tuples from the space. A tuple is an ordered set of fields _<field_1, field_2, ..., field_n>_.
In this project, a tuple must be instantiated as a _string_, for example, `"<vacancy,sd,shift1>"`.

In the TupleSpace, several identical instances can co-exist.
For example, there may be multiple tuples `"<vacancy,sd,turno1>"`, indicating the existence of several vacancies.

It is possible to search, in the space of tuples, for a given tuple to read or remove.
In the simplest variant, one can search for a concrete tuple. For example, `"<vacancy,sd,shift1>"`.
Alternatively, you can use [Java regular expressions](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#sum) to allow pairing with multiple values. For example, `"<vacancy,sd,[^,]+>"` pairs with `"<vacancy,sd,turno1>"` as well as `"<vacancy,sd,turno2>"`.

The operations available to the user are the following [^1]: _put_, _read_, _take_ and _getTupleSpacesState_.

* The *put* operation adds a tuple to the shared space.

* The *read* operation accepts the tuple description (possibly with regular expression) and returns *a* tuple that matches the description, if one exists. This operation blocks the client until a tuple that satisfies the description exists. The tuple is *not* removed from the tuple space.

* The *take* operation accepts the tuple description (possibly with regular expression) and returns *a* tuple that matches the description. This operation blocks the client until a tuple that satisfies the description exists. The tuple *is* removed from the tuple space.

* The *getTupleSpacesState* operation receives as its only argument the qualifier of the server to be queried and returns all tuples present on that server.

Users access the **TupleSpace** service through a client process, which interacts
with one or more servers that offer the service, through calls to remote procedures.

## Implementations:

* **R1** (on branch R1): The service is provided by a single server (i.e. a simple client-server architecture, without server replication), which accepts requests at a fixed address/port.

* **R2** (on branch R2): The service is replicated in three servers (A, B and C), following an adaptation of the [Xu-Liskov algorithm](http://www.ai.mit.edu/projects/aries/papers/programming/linda.pdf). This algorithm performs the _take_ operation in two steps to ensure consistency (explained in more detail in the R2 branch).

* **R3** (on branch R3): This implementation is based on the **State Machine Replication** (SMR) approach, an alternative to the Xu/Liskov algorithm. This approach focuses on ensuring total order for operations between the three replicas.

Note that both Variant 2 and Variant 3 have advantages and disadvantages, and may present better or worse performance depending on the pattern of use of the tuple space.

---------

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

Installation information for each implementation can be found in their respective branches.

Below follows the installation process for branch R3, which is the installation currently present in the master branch.

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
