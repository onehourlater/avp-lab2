package com.multichat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    public static void main(String[] args) throws IOException, ClassNotFoundException {

        ServerSocket ss = new ServerSocket(4445);

        List<Socket> sockets = new ArrayList<Socket>();
        Socket socket = null;

        List<ObjectOutputStream> objectOutputStreams = new ArrayList<ObjectOutputStream>();
        ObjectOutputStream objectOutputStream = null;

        List<ServerThread> serverThreads = new ArrayList<ServerThread>();
        ServerThread st = null;

        List<Message> messages = new ArrayList<>();

        while(true){
            try{
                System.out.println("Server loop");

                socket = ss.accept();
                sockets.add(socket);

                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectOutputStreams.add(objectOutputStream);

                st=new ServerThread(socket, sockets, objectOutputStream, objectOutputStreams, messages);
                st.start();
                serverThreads.add(st);
            }

            catch(Exception e){
                e.printStackTrace();
                System.out.println("Connection Error");
                break;
            }
        }

        System.out.println("Closing sockets.");
        // ss.close();
        // socket.close();
    }
}

class ServerThread extends Thread{

    private static List<Socket> sockets;
    private Socket socket=null;

    private static List<ObjectOutputStream> objectOutputStreams;
    private ObjectOutputStream objectOutputStream;

    private static List<Message> messages;

    public ServerThread(Socket socket, List<Socket> sockets, ObjectOutputStream objectOutputStream, List<ObjectOutputStream> objectOutputStreams, List<Message> serverMessages){
        this.sockets = sockets;
        this.socket = socket;

        this.objectOutputStreams = objectOutputStreams;
        this.objectOutputStream = objectOutputStream;

        this.messages = serverMessages;
    }

    public void run() {
        System.out.println("Current thread name: "
                + Thread.currentThread().getName());

        System.out.println("run() method called");

        try {

            InputStream inputStream = socket.getInputStream();
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

            Message message = null;

            while (true) {
                try {

                    message = (Message) objectInputStream.readObject();

                    synchronized(messages) {
                        messages.add(message);
                        messages.forEach((msg) -> System.out.println(msg.getClientName() + ": " + msg.getText()));
                    }

                    // send back to all active clients

                    synchronized(objectOutputStreams) {
                        for (int i = 0; i < sockets.size(); i++) {
                            // System.out.println("objectOutputStreams = " + objectOutputStreams.get(i));

                            objectOutputStreams.get(i).reset();
                            objectOutputStreams.get(i).writeObject(messages);
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Connection Error");
                    closeConnection();
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("IO error in server thread");
            closeConnection();
        }

    }

    private void closeConnection(){
        try {
            if (!socket.isClosed()) {
                socket.close();
                objectOutputStream.close();

                sockets.remove(socket);
                objectOutputStreams.remove(objectOutputStream);
            }
        } catch (IOException ignored) {
        }
    }

}
