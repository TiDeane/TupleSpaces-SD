class ServerEntry():

    def __init__(self, host, port, qualifier):
        self.host = host
        self.port = port
        self.checkQualifier(qualifier)
        self.qualifier = qualifier

    def checkQualifier(self, qualifier):
        if qualifier not in ['A', 'B', 'C']:
            raise ValueError("Invalid qualifier")
    
    def __str__(self):
        return f"{self.host}:{self.port} ({self.qualifier})"        
