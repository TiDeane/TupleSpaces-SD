from domain.ServiceEntry import ServiceEntry

class NameServer:
    def __init__(self):
        self.serviceMap = {} # Maps service names to to their ServiceEntry
    
    def checkServiceInMap(self, serviceName: str):
        return (serviceName in self.serviceMap)

    def getServiceEntry(self, serviceName: str):
        if self.checkServiceInMap(serviceName): # Is this necessary?
            return self.serviceMap[serviceName]

    def registerServiceEntry(self, serviceEntry: ServiceEntry):
        if isinstance(serviceEntry, ServiceEntry) and serviceEntry.serviceName not in self.serviceMap:
            self.serviceMap[serviceEntry.serviceName] = serviceEntry
        else:
            raise RuntimeError("Not a ServiceEntry, or Service with the same name already exists.")
