package SocketProgramming;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class Student {
    public static String stdId;
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Socket socket = new Socket("localhost", 6666);
        System.out.println("Connection established");
        System.out.println("Remote port: " + socket.getPort());
        System.out.println("Local port: " + socket.getLocalPort());
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        OutputStream os = socket.getOutputStream();
        InputStream is = socket.getInputStream();
        Scanner scanner = new Scanner(System.in);

        while(true) {
            System.out.println("Enter Student Id");
            stdId = scanner.nextLine();
            out.writeObject(stdId);
            String confirmation = (String) in.readObject();
            System.out.println(confirmation);
            if(confirmation.equalsIgnoreCase("Logged In")) break;

        }
        while(true){

            System.out.println("What do you want?");
            String choice = scanner.nextLine();
            if(choice.equalsIgnoreCase("LookupStudent")){
                out.writeObject(choice);
                try {
                    int allStd =  in.read();
                    System.out.println("All student list:");
                    for(int i = 0 ; i < allStd ; i++){
                        System.out.println((String) in.readObject());
                    }
                    int onlineStd =  in.read();
                    System.out.println("Online student list:");
                    for(int i = 0 ; i < onlineStd ; i++){
                        System.out.println((String) in.readObject());
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            else if(choice.equalsIgnoreCase("Upload")){
                out.writeObject(choice);
                System.out.println("Give file name or complete path");
                String fileName = scanner.nextLine();
                System.out.println("File Type");
                String fileType = scanner.nextLine();
                File myFile = new File(fileName);
                int fileSize = (int) myFile.length();
                try {
                    out.writeObject(fileName);
                    out.writeObject(fileType);
                    out.writeObject(fileSize);
                    int chunk = (int) in.readObject();
                    if(chunk != -1){
                        FileInputStream fis = new FileInputStream(myFile);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        byte[] byteArray = new byte[fileSize];
                        int current = 0;
                        int bytesRead;
                        String ackmsg;
                        do{
                            int sz = Math.min(chunk,fileSize);
                            bytesRead = bis.read(byteArray,current, sz);
                            os.write(byteArray,current,bytesRead);
                            current += sz;
                            fileSize -= sz;
                            try {
                                socket.setSoTimeout(30000);
                                ackmsg = (String) in.readObject();
                                String replyToserver;
                                if(fileSize > 0){
                                    replyToserver = "Send";
                                }
                                else replyToserver = "Completed";
                                out.writeObject(replyToserver);
                            }catch (SocketTimeoutException e){
                                System.out.println("Server Timeout");
                                out.writeObject("Timeout");
                                break;
                            }
                        }while(fileSize > 0 && ackmsg.equalsIgnoreCase("Yes"));
                        String completionMsg = "Done";
                        out.writeObject(completionMsg);
                        String verification = (String) in.readObject();
                        out.flush();
                        System.out.println(verification);
                    }
                    else{
                        System.out.println("Buffer overflowed . Try again later");
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if(choice.equalsIgnoreCase("LookupFile")){
                out.writeObject(choice);
                System.out.println("Public files of yours:");
                int publicLen = (int) in.readObject();
                for(int i = 0 ; i < publicLen ; i++){
                    String name = (String) in.readObject();
                    System.out.println(name);
                }
                System.out.println("Private files of yours:");
                int privateLen = (int) in.readObject();
                for(int i = 0 ; i < privateLen ; i++){
                    String name = (String) in.readObject();
                    System.out.println(name);
                }
                System.out.println("Do you want to download?"); //Download Files
                String str = scanner.nextLine();
                out.writeObject(str);
                if(str.equalsIgnoreCase("yes")){
                    System.out.println("Enter FileName and FileType");
                    String fileName = scanner.nextLine();
                    String fileType = scanner.nextLine();
                    out.writeObject(fileName);
                    out.writeObject(fileType);
                    int downloadedFileSize = (int) in.readObject();
                    int maxChunk = (int) in.readObject();
                    File downloadedFile = new File(fileName);
                    FileOutputStream fos = new FileOutputStream(downloadedFile);
                    byte[] byteArray = new byte[downloadedFileSize];
                    int current = 0;
                    while (downloadedFileSize > 0){
                        int sz = Math.min(downloadedFileSize,maxChunk);
                        int bytesRead = is.read(byteArray,current,sz);
                        fos.write(byteArray,current,bytesRead);
                        current += sz;
                        downloadedFileSize = downloadedFileSize - sz;
                    }

                    System.out.println("Download Complete");


                }

            }
            else if(choice.equalsIgnoreCase("LookupOthersfile")){
                out.writeObject(choice);
                System.out.println("Give student id");
                String id = scanner.nextLine();
                out.writeObject(id);
                System.out.println("Public files of :"+id);
                int publicLen = (int) in.readObject();
                for(int i = 0 ; i < publicLen ; i++){
                    String name = (String) in.readObject();
                    System.out.println(name);
                }
                System.out.println("Do you want to download?");
                String str = scanner.nextLine();
                out.writeObject(str);
                if(str.equalsIgnoreCase("yes")){
                    System.out.println("Enter FileName");
                    String fileName = scanner.nextLine();
                    out.writeObject(fileName);
                    int downloadedFileSize = (int) in.readObject();
                    int maxChunk = (int) in.readObject();
                    File downloadedFile = new File(fileName);
                    FileOutputStream fos = new FileOutputStream(downloadedFile);
                    byte[] byteArray = new byte[downloadedFileSize];
                    int current = 0;
                    while (downloadedFileSize > 0){
                        int sz = Math.min(downloadedFileSize,maxChunk);
                        int bytesRead = is.read(byteArray,current,sz);
                        fos.write(byteArray,current,bytesRead);
                        current += sz;
                        downloadedFileSize = downloadedFileSize - sz;
                    }

                    System.out.println("Download Complete");


                }
            }
            else if(choice.equalsIgnoreCase("Request")){
                out.writeObject(choice);
                System.out.println("Give file name and short description");
                String requestMsg = scanner.nextLine();
                out.writeObject(requestMsg);
                String reply = (String) in.readObject();
                System.out.println(reply);
            }
            else if(choice.equalsIgnoreCase("View")){
                out.writeObject(choice);
                int sz = (int) in.readObject();
                for(int i = 0 ; i < sz ; i++){
                    String msgs = (String) in.readObject();
                    System.out.println(i+1 +". "+msgs);
                }

            }
            else if(choice.equalsIgnoreCase("UploadRequestedFile")){
                out.writeObject(choice);
                System.out.println("Give file name or complete path");
                String fileName = scanner.nextLine();
                File myFile = new File(fileName);
                int fileSize = (int) myFile.length();
                try {
                    out.writeObject(fileName);
                    out.writeObject(fileSize);
                    int chunk = (int) in.readObject();
                    if(chunk != -1){
                        FileInputStream fis = new FileInputStream(myFile);
                        BufferedInputStream bis = new BufferedInputStream(fis);
                        byte[] byteArray = new byte[fileSize];
                        int current = 0;
                        int bytesRead;
                        String ackmsg;
                        do{
                            int sz = Math.min(chunk,fileSize);
                            bytesRead = bis.read(byteArray,current, sz);
                            os.write(byteArray,current,bytesRead);
                            current += sz;
                            fileSize -= sz;
                            ackmsg = (String) in.readObject();
                        }while(fileSize > 0 && ackmsg.equalsIgnoreCase("Yes"));
                        String completionMsg = "Done";
                        out.writeObject(completionMsg);
                        boolean verification = (boolean) in.readObject();
                        if(verification){
                            System.out.println("Provide request ID");
                            int requestId = scanner.nextInt();
                            out.writeObject(requestId);
                        }
                        out.flush();
                    }
                    else{
                        System.out.println("Buffer overflowed . Try again later");
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
            else if(choice.equalsIgnoreCase("LogOut")){
                out.writeObject(choice);
                socket.close();
                break;
            }


        }



    }
}
