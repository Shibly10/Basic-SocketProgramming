package SocketProgramming;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;

public class Server {
    static Vector<StudentClass> tc = new Vector<>();
    static HashMap<Integer,String> hashMap = new HashMap<>();
    static int requestId = 0;
    static int MAX_BUFFER_SIZE = 1000000000;
    static int MAX_CHUNK_SIZE = 1000;
    static int MIN_CHUNK_SIZE = 0;

    public static void main(String[] args) throws IOException {
        ServerSocket welcomeSocket = new ServerSocket(6666);

        while(true) {
            System.out.println("Waiting for connection...");
            Socket socket = welcomeSocket.accept();
            System.out.println("Connection established");

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();

            StudentClass sc = new StudentClass(socket,out,in,os,is,MAX_CHUNK_SIZE,MIN_CHUNK_SIZE);
            tc.add(sc);
            Thread thread = new Thread(sc);
            thread.start();

        }

    }
}
