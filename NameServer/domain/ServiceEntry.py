from domain.ServerEntry import ServerEntry

class ServiceEntry:
    def __init__(self, serviceName):
        self.serviceName = serviceName
        self.servers = set() # A set of ServerEntrys

    def addServerEntry(self, serverEntry):
        if isinstance(serverEntry, ServerEntry):
            self.servers.add(serverEntry)
        else:
            raise TypeError()
    
    def getServerEntry(self, host, port):
        for serverEntry in self.servers:
            if serverEntry.host == host and serverEntry.port == port:
                return serverEntry
        raise Exception()

    def removeServerEntry(self, serverEntry):
        self.servers.discard(serverEntry)
