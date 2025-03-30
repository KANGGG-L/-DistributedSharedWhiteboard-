package remote;

import java.awt.*;
import java.rmi.*;
import java.util.ArrayList;

import org.javatuples.Triplet;

public interface IRemoteWhiteboard extends Remote {
    public Boolean isUserNameExist(String userName) throws RemoteException;
    public Boolean registerClient(IRemoteWhiteboardClient client) throws RemoteException;
    public void deregisterClient(IRemoteWhiteboardClient client) throws RemoteException;
    public void broadcastMessage(String message) throws RemoteException;
    public void broadcastDrawing(Triplet<Shape, Color, String> drawing) throws RemoteException;
    public ArrayList<String> getChatHistory() throws RemoteException;
    public ArrayList<Triplet<Shape, Color, String>> getDrawingTuples() throws RemoteException;

    public void broadcastNewFile() throws RemoteException;

    public void notifyAllClientsToTerminate() throws RemoteException;
    public void notifyOneClientToTerminate(String userName) throws RemoteException;

    public ArrayList<String> getActiveUserNames() throws RemoteException;

    public ArrayList<String> getWaitingUserNames() throws RemoteException;

    // For open a new file
    public void broadcastMessages(ArrayList<String> messages) throws RemoteException;
    public void broadcastDrawings(ArrayList<Triplet<Shape, Color, String>> newDrawingTuples) throws RemoteException;

    public Boolean isInWating(String name) throws RemoteException;

    public boolean removeFromWaitingPool(String userName) throws RemoteException;
    public boolean acceptRequest(String userName) throws RemoteException;
}
