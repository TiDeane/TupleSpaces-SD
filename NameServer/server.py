import sys
sys.path.insert(1, '../Contract/target/generated-sources/protobuf/python')
import grpc
import NameServer_pb2 as pb2
import NameServer_pb2_grpc as pb2_grpc
from NameServerServiceImpl import NameServerServiceImpl
from concurrent import futures

# define the port
PORT = 5001

if __name__ == '__main__':
    try:
        dFlag = False

        if len(sys.argv) == 2 and sys.argv[1] == "-debug":
            dFlag = True

        # create server
        server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
        # add service
        pb2_grpc.add_NameServerServicer_to_server(NameServerServiceImpl(dFlag), server)
        # listen on port
        server.add_insecure_port('[::]:'+str(PORT))
        # start server
        server.start()
        
        print("Server listening on port " + str(PORT))
        print("Press CTRL+C to terminate")
        
        server.wait_for_termination()

    except KeyboardInterrupt:
        print("NameServer stopped")
        exit(0)
