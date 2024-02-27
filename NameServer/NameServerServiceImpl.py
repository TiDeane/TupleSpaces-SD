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

        print(request)

        name = request.name
        qualifier = request.qualifier
        address = request.address
        host, port = address.split(':')

        try:
            serverEntry = ServerEntry(host=host, port=port, qualifier=qualifier)

            if (self.nameServer.checkServiceInMap(name)):
                # Service is already in the NameServer
                serviceEntry = self.nameServer.getServiceEntry(name)
                print("Service is already in the NameServer, getting ServiceEntry")
            else:
                serviceEntry = ServiceEntry(serviceName=name)
                self.nameServer.registerServiceEntry(serviceEntry)
            
            serviceEntry.addServerEntry(serverEntry)

            print("Registered new Server:\n" + str(serverEntry))

            return pb2.RegisterResponse()
        except Exception as e:
            print("Failed to register new Server, with exception:\n" + e)
            
            context.set_details("Not possible to register the server")
            context.set_code(pb2.StatusCode.INTERNAL)
            return pb2.RegisterResponse()

