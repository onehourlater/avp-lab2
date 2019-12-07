package com.multichat;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

class Message implements Serializable{
    private final String text;
    private final String clientName;

    public Message(String text, String clientName) {
        this.text = text;
        this.clientName = clientName;
    }

    public String getText() {
        return text;
    }
    public String getClientName() {
        return clientName;
    }
}

public class Client {

    private static String clientName = "";

    private static Socket socket;
    private static OutputStream outputStream;
    private static InputStream inputStream;

    private static String secretKey = "secret-key-we-don't-tell-anyone";

    public static void main(String[] args) throws IOException {

        Scanner scanner = new Scanner(System.in);

        socket = new Socket("localhost", 4445);

        outputStream = socket.getOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        inputStream = socket.getInputStream();
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

        // Ask user for username

        System.out.println("Enter your username:");

        clientName = scanner.nextLine();

        // thread for input messages

        new Thread(new Runnable() {
            public void run() {

                String clientMessage;

                try {

                    Message message = null;

                    while (true) {

                        System.out.println("Waiting for input");
                        clientMessage = scanner.nextLine();

                        message = new Message(AES.encrypt(clientMessage, secretKey), clientName);

                        objectOutputStream.writeObject(message);

                        if (clientMessage.compareTo("QUIT") == 0){
                            closeConnection();
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Socket read Error");
                    closeConnection();
                }
            }
        }).start();

        // thread for output messages

        new Thread(new Runnable() {
            public void run() {

                List<Message> listOfMessages;

                while(true) {
                    try {

                        listOfMessages = (List<Message>) objectInputStream.readObject();

                        System.out.println("All messages:");
                        listOfMessages.forEach((msg)-> System.out.println(msg.getClientName() + ": " + AES.decrypt(msg.getText(), secretKey)));

                    }catch(Exception e){
                        System.out.println("Got Error = " + e);
                        closeConnection();
                        break;
                    }
                }

            }
        }).start();

    }

    private static void closeConnection(){
        try {
            if (!socket.isClosed()) {
                socket.close();
                outputStream.close();
                inputStream.close();
            }
        } catch (IOException ignored) {
        }
    }
}
