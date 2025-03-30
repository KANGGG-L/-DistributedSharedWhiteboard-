package server;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import remote.IRemoteWhiteboard;

public class WhiteboardServer {
    
    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Please Input <host> <port>");
            System.exit(1);
        }
  
        int port = 0;
		try {
			port = Integer.parseInt(args[1]);
		 } catch (NumberFormatException e) {
            System.out.println("Please Input valid format for port (Fully Integer)");
		 	System.exit(1);
		 }

        try {
            IRemoteWhiteboard remoteWhiteboard = (IRemoteWhiteboard) new RemoteWhiteboard();

            System.setProperty("java.rmi.server.hostname", args[0]);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind("Whiteboard", remoteWhiteboard);
            
            System.out.println("Whiteboard Server Is Running");
        } catch (Exception e) {
            if (e instanceof RemoteException) {
                System.err.println("Remote Exception is Raised");
            } else if (e instanceof AlreadyBoundException) {
                System.err.println("Registry Has Bound Given Info");
                
            } else {
        		System.err.println(e.getClass().getSimpleName() + " Exception Raised");
        	}
            
            System.exit(1);
        }





		
    }
}
