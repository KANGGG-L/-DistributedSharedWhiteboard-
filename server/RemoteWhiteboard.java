package server;

import java.awt.Color;
import java.awt.Shape;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.javatuples.Triplet;

import remote.IRemoteWhiteboard;
import remote.IRemoteWhiteboardClient;

public class RemoteWhiteboard extends UnicastRemoteObject implements IRemoteWhiteboard {

    private ArrayList<IRemoteWhiteboardClient> users;
    private HashMap<String, Integer> userNames; 
    private ArrayList<String> chatHistory;
    private ArrayList<Triplet<Shape, Color, String>> drawingTuples;

    private ArrayList<IRemoteWhiteboardClient> waitingUsers;
    private HashMap<String, Integer> waitingUserNames; 


    protected RemoteWhiteboard() throws RemoteException {
        users = new ArrayList<>();
        userNames = new HashMap<String, Integer>();

        waitingUsers  = new ArrayList<>();
        waitingUserNames  = new HashMap<String, Integer>();

        chatHistory = new ArrayList<>();
        drawingTuples = new ArrayList<>();
    }

    @Override
    public Boolean isUserNameExist(String userName) {
        if (userNames.get(userName) == null) {
            return false;
        } 
        userNames.put(userName, 1);
        return true;
    }

    @Override
    public synchronized Boolean registerClient(IRemoteWhiteboardClient client) throws RemoteException {
        Boolean isManager = false;
        if (userNames.size() == 0) {
            isManager = true;
            userNames.put(client.getUserName(), 1);
            users.add(client);
        } else {
            waitingUsers.add(client);
            waitingUserNames.put(client.getUserName(), 1);

            users.get(0).remindManager(true);
        }
        
        return isManager;
    }

    @Override
    public ArrayList<String> getWaitingUserNames() throws RemoteException {
        Set<String> keys = waitingUserNames.keySet();
        return new ArrayList<>(keys) ;

    }

    @Override
    public synchronized boolean removeFromWaitingPool(String userName) throws RemoteException {

        
        if (waitingUserNames.remove(userName) == 1) {
            if (waitingUserNames.size() == 0) {
                users.get(0).remindManager(false);
            }
    
            for (IRemoteWhiteboardClient user : waitingUsers) {
                if (user.getUserName().equals(userName)) {
                    waitingUsers.remove(user);
                    break;
                }
            }

            return true;
        } 

        return false;
        
       

    }


    public boolean acceptRequest(String userName) throws RemoteException {
        
        if (waitingUserNames.remove(userName) == 1) {
            for (IRemoteWhiteboardClient user : waitingUsers) {
                if (user.getUserName().equals(userName)) {
                    user.setAccpet();
                    users.add(user);
                    waitingUsers.remove(user);
                    break;
                }
            }
    
            userNames.put(userName, 1);
    
            waitingUserNames.remove(userName);
            if (waitingUserNames.size() == 0) {
                users.get(0).remindManager(false);
            }

            return true;
        }

        return false;
        
    }




    @Override
    public synchronized Boolean isInWating(String userName) throws RemoteException {

        return waitingUserNames.get(userName) == 1;
    }

    @Override
    public ArrayList<String> getChatHistory() throws RemoteException {
        return chatHistory;
    }

    @Override
    public ArrayList<Triplet<Shape, Color, String>> getDrawingTuples() throws RemoteException {
        return drawingTuples;
    }

    @Override
    public synchronized void notifyAllClientsToTerminate() throws RemoteException {
        for (IRemoteWhiteboardClient user : users) {
            try {
                user.kickUser(0);
            } catch (RemoteException e) {

                if (e instanceof UnmarshalException) {
                    System.out.println("Normal User Client Has Terminated");
                } else {
                    System.err.println(e.getMessage());
                }
                
            }
        }

        for (IRemoteWhiteboardClient user : waitingUsers) {

            try {
                // waitingUserNames.remove(user.getUserName());
                // waitingUsers.remove(user);
                user.kickUser(0);
            } catch (RemoteException e) {

                if (e instanceof UnmarshalException) {
                    System.out.println("Normal User Client Has Terminated");
                } else {
                    System.err.println(e.getMessage());
                }
                
            }
        }
    }

    @Override
    public void notifyOneClientToTerminate(String userName) throws RemoteException {
        

        for (IRemoteWhiteboardClient user : users) {

            if (user.getUserName().equals(userName)) {
         
                try {
                    userNames.remove(userName);
                    users.remove(user);
                    user.kickUser(1);
                } catch (RemoteException e) {
    
                    if (e instanceof UnmarshalException) {
                        System.out.println("Normal User Client Has Terminated");
                    } else {
                        System.err.println(e.getMessage());
                    }
                    
                }
                break;
            }
            
        }

        
    }


    @Override
    public synchronized void deregisterClient(IRemoteWhiteboardClient client) throws RemoteException {
        String mangerName = users.get(0).getUserName();
        Boolean flag = false;
        if (client.getUserName().equals(mangerName)) {
            flag = true;
        }

        userNames.remove(client.getUserName());
        users.remove(client);

        if (flag) {
            notifyAllClientsToTerminate();
            drawingTuples.clear();
            chatHistory.clear();
            
        }

        
        
        
    }



    @Override
    public synchronized void broadcastMessage(String message) throws RemoteException {
        chatHistory.add(message);

        for (IRemoteWhiteboardClient user : users) {
            try {
                user.receiveMessage(chatHistory);
            } catch (RemoteException e) {
                System.err.println("RemoteException Is Raised");
                System.exit(1);
            }
        }
    }

    @Override
    public synchronized void broadcastDrawing(Triplet<Shape, Color, String> drawing) throws RemoteException {
        drawingTuples.add(drawing);
        for (IRemoteWhiteboardClient user : users) {
            try {
                user.receiveDrawing(drawingTuples);
            } catch (RemoteException e) {
                System.err.println("RemoteException Is Raised");
                System.exit(1);
            }
        }
    }

    @Override
    public void broadcastMessages(ArrayList<String> messages) throws RemoteException {
        chatHistory = messages;

        for (IRemoteWhiteboardClient user : users) {
            try {
                user.receiveMessage(chatHistory);
            } catch (RemoteException e) {
                System.err.println("RemoteException Is Raised");
                System.exit(1);
            }
        }
    }

    @Override
    public void broadcastDrawings(ArrayList<Triplet<Shape, Color, String>> newDrawingTuples) throws RemoteException {
        drawingTuples = newDrawingTuples;

        for (IRemoteWhiteboardClient user : users) {
            try {
                user.receiveDrawing(drawingTuples);
            } catch (RemoteException e) {
                System.err.println("RemoteException Is Raised");
                System.exit(1);
            }
        }
    }
    

    @Override
    public void broadcastNewFile() throws RemoteException {
        chatHistory = new ArrayList<>();
        drawingTuples = new ArrayList<>();
        for (IRemoteWhiteboardClient user : users) {
            try {
                user.resetWhiteboardState();
                user.receiveMessage(chatHistory);
                user.receiveDrawing(drawingTuples);
            } catch (RemoteException e) {
                System.err.println("RemoteException Is Raised");
                System.exit(1);
            }
        }
    }

    @Override
    public ArrayList<String> getActiveUserNames() throws RemoteException {
        Set<String> keys = userNames.keySet();
        return new ArrayList<>(keys) ;
    }
   
}