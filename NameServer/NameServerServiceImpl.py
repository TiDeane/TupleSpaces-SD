import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
from domain.NameServer import NameServer
from domain.ServiceEntry import ServiceEntry
from domain.ServerEntry import ServerEntry

class NameServerServiceImpl(pb2_grpc.NameServerServicer):

    def __init__(self, dFlag):
        self.nameServer = NameServer()
        self.debugFlag = dFlag
    
    # Helper method to print debug messages.
    def debug(self, debugMessage):
        if self.debugFlag:
            print(debugMessage)

    def register(self, request, context):

        self.debug("--------------------")
        self.debug("New register request:\n" + str(request))

        name = request.name
        qualifier = request.qualifier
        address = request.address
        host, port = address.split(':')

        try:
            serverEntry = ServerEntry(host=host, port=port, qualifier=qualifier)

            if (self.nameServer.checkServiceInMap(name)):
                self.debug("Service is already in the NameServer, adding serverEntry")
                serviceEntry = self.nameServer.getServiceEntry(name)

                for server in serviceEntry.getServers():
                    if server.qualifier == qualifier:
                        self.debug("Operation failed: there is already a server with given name and qualifier")

                        context.set_details("Not possible to register the server: there is already a server with given name and qualifier")
                        context.set_code(pb2.StatusCode.INTERNAL)
                        return pb2.RegisterResponse()
            else:
                self.debug("Registered new Server:\n" + str(serverEntry))
                serviceEntry = ServiceEntry(serviceName=name)
                self.nameServer.registerServiceEntry(serviceEntry)
            
            serviceEntry.addServerEntry(serverEntry)

            return pb2.RegisterResponse()
        except Exception as e:
            self.debug("Failed to register new Server, with exception:\n" + str(e))
            
            message = "Not possible to register the server, " + str(e)
            context.set_details(message)
            context.set_code(pb2.StatusCode.INTERNAL)
            return pb2.RegisterResponse()
        
    def lookup(self, request, context):

        self.debug("--------------------")
        self.debug("New lookup request:\n" + str(request))

        name = request.name
        qualifier = request.qualifier
        serverList = []

        try:
            if not self.nameServer.checkServiceInMap(name):
                self.debug("Service not in map, returning empty list\n")
                return pb2.LookupResponse(servers=serverList)
            
            serviceEntry = self.nameServer.getServiceEntry(serviceName=name)

            if qualifier == "":
                # Adds all servers if qualifier is not specified
                for serverEntry in serviceEntry.servers:
                    address = serverEntry.host + ":" + serverEntry.port
                    serverList.append(address)
            else:
                # Adds all servers with the given qualifier
                for serverEntry in serviceEntry.servers:
                    if serverEntry.qualifier == qualifier:
                        address = serverEntry.host + ":" + serverEntry.port
                        serverList.append(address)
            
            self.debug("Returning list of servers")

            return pb2.LookupResponse(servers=serverList)
        except Exception as e:
            self.debug("Failed to find a server, with exception: " + str(e))
            
            context.set_details("Failed when looking for available servers")
            context.set_code(pb2.StatusCode.INTERNAL)
            return pb2.LookupResponse()

    def delete(self, request, context):

        self.debug("--------------------")
        self.debug("New delete request:\n" + str(request))

        name = request.name
        address = request.address
        host, port = address.split(':')

        try:
            if not self.nameServer.checkServiceInMap(name):
                self.debug("Service not in map, throwing exception\n")
                context.set_details("Not possible to delete the server")
                context.set_code(pb2.StatusCode.INTERNAL)
                return pb2.DeleteResponse()
            
            serviceEntry = self.nameServer.getServiceEntry(serviceName=name)
            
            serverEntry = serviceEntry.getServerEntry(host, port)
            serviceEntry.removeServerEntry(serverEntry)

            self.debug("Successfully removed server from NameServer")

            return pb2.DeleteResponse()
        except Exception as e:
            self.debug("Failed to delete the Server, with exception: " + str(e))
            
            context.set_details("Not possible to delete the server")
            context.set_code(pb2.StatusCode.INTERNAL)
            return pb2.DeleteResponse()