import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
from domain.NameServer import NameServer
from domain.ServiceEntry import ServiceEntry
from domain.ServerEntry import ServerEntry

class NameServerServiceImpl(pb2_grpc.NameServerServicer):

    def __init__(self):
        self.nameServer = NameServer()

    def register(self, request, context):

        print("--------------------")
        print("New register request:\n" + str(request))

        name = request.name
        qualifier = request.qualifier
        address = request.address
        host, port = address.split(':')

        try:
            serverEntry = ServerEntry(host=host, port=port, qualifier=qualifier)

            if (self.nameServer.checkServiceInMap(name)):
                print("Service is already in the NameServer, adding serverEntry")
                serviceEntry = self.nameServer.getServiceEntry(name)
            else:
                print("Registered new Server:\n" + str(serverEntry))
                serviceEntry = ServiceEntry(serviceName=name)
                self.nameServer.registerServiceEntry(serviceEntry)
            
            serviceEntry.addServerEntry(serverEntry)

            return pb2.RegisterResponse()
        except Exception as e:
            print("Failed to register new Server, with exception:\n" + str(e))
            
            context.set_details("Not possible to register the server")
            context.set_code(pb2.StatusCode.INTERNAL)
            return pb2.RegisterResponse()
        
    def lookup(self, request, context):

        print("--------------------")
        print("New lookup request:\n" + str(request))

        name = request.name
        qualifier = request.qualifier
        serverList = []

        try:
            if not self.nameServer.checkServiceInMap(name):
                print("Service not in map, returning empty list\n")
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
            
            print("Returning list of servers")

            return pb2.LookupResponse(servers=serverList)
        except Exception as e:
            print("Failed to find a server, with exception: " + str(e))
            
            context.set_details("Failed when looking for available servers")
            context.set_code(pb2.StatusCode.INTERNAL)
            return pb2.LookupResponse()

    def delete(self, request, context):

        print("--------------------")
        print("New delete request:\n" + str(request))

        name = request.name
        address = request.address
        host, port = address.split(':')

        try:
            if not self.nameServer.checkServiceInMap(name):
                print("Service not in map, throwing exception\n")
                context.set_details("Not possible to delete the server")
                context.set_code(pb2.StatusCode.INTERNAL)
                return pb2.DeleteResponse()
            
            serviceEntry = self.nameServer.getServiceEntry(serviceName=name)
            
            serverEntry = serviceEntry.getServerEntry(host, port)
            serviceEntry.removeServerEntry(serverEntry)

            print("Successfully removed server from NameServer")

            return pb2.DeleteResponse()
        except Exception as e:
            print("Failed to delete the Server, with exception: " + str(e))
            
            context.set_details("Not possible to delete the server")
            context.set_code(pb2.StatusCode.INTERNAL)
            return pb2.DeleteResponse()