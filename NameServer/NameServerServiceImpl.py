import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc

class NameServerServiceImpl(pb2_grpc.NameServerServicer):

    def __init__(self, *args, **kwargs):
        pass

    def register(self, request, context):
        # print the received request
        print(request)

        name = request.name
        qualifier = request.qualifier
        address = request.address

        # Create exception if not successful

        return pb2.RegisterResponse()
