package remote;
import java.rmi.*;
import java.util.ArrayList;
import java.awt.*;
import org.javatuples.Triplet;

public interface IRemoteWhiteboardClient extends Remote {
    public String getUserName() throws RemoteException;
    public void receiveMessage(ArrayList<String> newChatHistory) throws RemoteException;
    public void receiveDrawing(ArrayList<Triplet<Shape, Color, String>> drawingTuples) throws RemoteException;
    public void resetWhiteboardState() throws RemoteException;
    public void kickUser(int mode) throws RemoteException;
    public void remindManager(Boolean flag) throws RemoteException;

    public void setAccpet() throws RemoteException;
}
