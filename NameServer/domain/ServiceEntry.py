from domain.ServerEntry import ServerEntry

class ServiceEntry:
    def __init__(self, serviceName: str):
        self.serviceName = serviceName
        self.servers = set() # A set of ServerEntrys

    def getServers(self):
        return self.servers

    def addServerEntry(self, serverEntry: ServerEntry):
        if isinstance(serverEntry, ServerEntry):
            self.servers.add(serverEntry)
        else:
            raise TypeError()
    
    def getServerEntry(self, host: str, port: str):
        for serverEntry in self.servers:
            if serverEntry.host == host and serverEntry.port == port:
                return serverEntry
        raise Exception()

    def removeServerEntry(self, serverEntry: ServerEntry):
        self.servers.discard(serverEntry)
