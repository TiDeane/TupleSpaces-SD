from domain.ServiceEntry import ServiceEntry

class NameServer:
    def __init__(self):
        self.serviceMap = {} # Maps service names to to their ServiceEntry
    
    def checkServiceInMap(self, serviceName):
        return (serviceName in self.serviceMap)

    def getServiceEntry(self, serviceName):
        if self.checkServiceInMap(serviceName): # Is this necessary?
            return self.serviceMap[serviceName]

    def registerServiceEntry(self, serviceEntry):
        if isinstance(serviceEntry, ServiceEntry) and serviceEntry.serviceName not in self.serviceMap:
            self.serviceMap[serviceEntry.serviceName] = serviceEntry
        else:
            raise RuntimeError("Not a ServiceEntry, or Service with the same name already exists.")
