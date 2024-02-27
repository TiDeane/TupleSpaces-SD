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

    def removeServerEntry(self, serverEntry):
        self.servers.discard(serverEntry)
