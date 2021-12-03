package SocketProgramming;


import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;


public class StudentClass implements Runnable{
    Socket socket;
    String stdId;
    ObjectOutputStream out;
    ObjectInputStream in;
    OutputStream os;
    InputStream is;
    boolean isLoggedin = false;
    int maxChunk;
    int minChunk;
    Queue<String> queue = new LinkedList<>();
    public StudentClass(Socket socket, ObjectOutputStream out, ObjectInputStream in,OutputStream os, InputStream is, int maxChunk, int minChunk){
        this.socket = socket;
        this.out = out;
        this.in = in;
        this.os = os;
        this.is = is;
        this.maxChunk = maxChunk;
        this.minChunk = minChunk;

    }

    @Override
    public void run() {
        try {

            while (true) {
                int c = 0;
                int q = 0;
                try {
                    String id = (String) in.readObject();
                    for (StudentClass sc : Server.tc) {
                        if (id.equalsIgnoreCase(sc.stdId)) {
                            q++;
                            if(sc.isLoggedin){
                                c++;
                            }
                        }
                    }
                    for(StudentClass sc : Server.tc){
                        if(id.equalsIgnoreCase(sc.stdId) && !sc.isLoggedin){
                            q = 1;
                        }
                    }
                    if(q == 1){
                        for (StudentClass sc : Server.tc) {
                            if (id.equalsIgnoreCase(sc.stdId)) {
                                queue = sc.queue;
                            }
                        }
                    }
                    if (c == 0) {
                        isLoggedin = true;
                        stdId = id;
                        String msg = "Logged In";
                        out.writeObject(msg);
                        break;
                    } else {
                        String msg = "You are logged in from another client.";
                        out.writeObject(msg);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }


            }


            File file = new File(stdId);
            File file1 = new File(stdId + "\\Private");
            File file2 = new File(stdId + "\\Public");
            file.mkdir();
            file1.mkdir();
            file2.mkdir();
            while (isLoggedin) {

                    String receivedMsg = "";
                    try {
                        receivedMsg = (String) in.readObject();
                    } catch (IOException | ClassNotFoundException e) {
                        System.out.println(stdId + " got disconnected");
                        isLoggedin = false;
                        break;
                    }
                    if (receivedMsg.equalsIgnoreCase("LookupStudent")) {
                        try {
                            out.write(Server.tc.size());
                            int onlineStd = 0;
                            for (StudentClass sc : Server.tc) {
                                out.writeObject(sc.stdId);
                            }

                            for (StudentClass sc : Server.tc) {
                                if (sc.isLoggedin) onlineStd++;
                            }
                            out.write(onlineStd);
                            for (StudentClass sc : Server.tc) {
                                if (sc.isLoggedin) {
                                    out.writeObject(sc.stdId);
                                }
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    } else if (receivedMsg.equalsIgnoreCase("Upload")) {
                        try {
                            String Name = (String) in.readObject();
                            String Type = (String) in.readObject();
                            String fileType = Type.substring(0,1).toUpperCase() + Type.substring(1);
                            int fileSize = (int) in.readObject();
                            int size = fileSize;
                            if ((Server.MAX_BUFFER_SIZE - fileSize) > 0) {
                                Server.MAX_BUFFER_SIZE -= fileSize;
                                Random r = new Random();
                                int chunk = r.nextInt(maxChunk - minChunk);
                                out.writeObject(chunk);
//                                String fileName = stdId + "\\" + fileType + "\\" + Name;
//                                FileOutputStream fos = new FileOutputStream(fileName);
                                byte[] byteArray = new byte[fileSize];
                                int current = 0;
                                int bytesRead;
                                int receivedBytes = 0;
                                String replyFromclient;
                                do {
                                    int sz = Math.min(fileSize, chunk);
                                    bytesRead = is.read(byteArray, current, sz);
//                                    fos.write(byteArray, current, bytesRead);
                                    current += sz;
                                    fileSize -= sz;
                                    receivedBytes += bytesRead;
                                    String ackmsg = "Yes";
                                    out.writeObject(ackmsg);
                                    replyFromclient = (String) in.readObject();
                                } while (replyFromclient.equalsIgnoreCase("Send"));
                                Server.MAX_BUFFER_SIZE += size;
                                String completionMsg = (String) in.readObject();
                                if (completionMsg.equalsIgnoreCase("Done")) {
                                    if (size == receivedBytes) {
                                        String fileName = stdId + "\\" + fileType + "\\" + Name;
                                        FileOutputStream fos = new FileOutputStream(fileName);
                                        fos.write(byteArray, 0, receivedBytes);
                                        System.out.println("Incoming File: " + fileName);
                                        System.out.println("Size: " + size + "Byte");
                                        String verification = "File is verified";
                                        out.writeObject(verification);
                                        System.out.println(verification);
                                        fos.close();
                                    } else {
                                        String verification = "File is not verified";
                                        out.writeObject(verification);
                                        System.out.println("File is corrupted. File received " + receivedBytes + " Bytes");
//                                        fos.close();
//                                        File f = new File(fileName);
//                                        f.delete();
                                    }
                                }
                            } else {
                                int n = -1;
                                out.writeObject(n);
                            }


                        } catch (IOException | ClassNotFoundException e) {
                            System.out.println("Disconnected during upload");;
                        }
                    } else if (receivedMsg.equalsIgnoreCase("LookupFile")) {
                        try {
                            File directory = new File(stdId + "\\Public");
                            File[] contents = directory.listFiles();
                            out.writeObject(contents.length);
                            for (File object : contents) {
                                out.writeObject(object.getName());
                            }
                            File directory2 = new File(stdId + "\\Private");
                            File[] contents2 = directory2.listFiles();
                            out.writeObject(contents2.length);
                            for (File object : contents2) {
                                out.writeObject(object.getName());
                            }
                            String str = null;
                            str = (String) in.readObject();
                            if (str.equalsIgnoreCase("yes")) { //Downloading client's own file
                                String fileName = (String) in.readObject();
                                String fileType = (String) in.readObject();
                                File outgoingFile = new File(stdId + "\\" + fileType + "\\" + fileName);
                                int outgoingFileSize = (int) outgoingFile.length();
                                out.writeObject(outgoingFileSize);
                                out.writeObject(maxChunk);
                                FileInputStream fis = new FileInputStream(outgoingFile);
                                BufferedInputStream bis = new BufferedInputStream(fis);
                                byte[] byteArray = new byte[outgoingFileSize];
                                int current = 0;
                                while (outgoingFileSize > 0) {
                                    int sz = Math.min(outgoingFileSize, maxChunk);
                                    int bytesRead = bis.read(byteArray, current, sz);
                                    os.write(byteArray, current, bytesRead);
                                    current += sz;
                                    outgoingFileSize = outgoingFileSize - sz;
                                }
                                out.flush();
                                System.out.println("File Sent");

                            }

                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else if (receivedMsg.equalsIgnoreCase("LookupOthersfile")) {
                        try {
                            String id = (String) in.readObject();
                            System.out.println(id);
                            File directory = new File(id + "\\Public");
                            File[] contents = directory.listFiles();
                            out.writeObject(contents.length);
                            for (File object : contents) {
                                try {
                                    out.writeObject(object.getName());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                            String str = (String) in.readObject();
                            if (str.equalsIgnoreCase("yes")) { //Downloading other's private file
                                String fileName = (String) in.readObject();
                                File outgoingFile = new File(id + "\\Public" + "\\" + fileName);
                                int outgoingFileSize = (int) outgoingFile.length();
                                out.writeObject(outgoingFileSize);
                                out.writeObject(maxChunk);
                                FileInputStream fis = new FileInputStream(outgoingFile);
                                BufferedInputStream bis = new BufferedInputStream(fis);
                                byte[] byteArray = new byte[outgoingFileSize];
                                int current = 0;
                                while (outgoingFileSize > 0) {
                                    int sz = Math.min(outgoingFileSize, maxChunk);
                                    int bytesRead = bis.read(byteArray, current, sz);
                                    os.write(byteArray, current, bytesRead);
                                    current += sz;
                                    outgoingFileSize = outgoingFileSize - sz;
                                }
                                out.flush();
                                System.out.println("File Sent");

                            }
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }


                    } else if (receivedMsg.equalsIgnoreCase("Request")) {
                        String requestMsg = (String) in.readObject();
                        Server.hashMap.put(Server.requestId, stdId);
                        requestMsg += " . Request id : " + Server.requestId;
                        for (StudentClass sc : Server.tc) {
                            if (!stdId.equalsIgnoreCase(sc.stdId)) {
                                sc.queue.add(requestMsg);
                            }
                        }
                        String reply = "A request has been made with id " + Server.requestId;
                        out.writeObject(reply);
                        Server.requestId++;
                    } else if (receivedMsg.equalsIgnoreCase("View")) {
                        int queueSize = queue.size();
                        out.writeObject(queueSize);
                        for (String str : queue) {
                            out.writeObject(str);
                        }
                        queue.clear();
                    } else if (receivedMsg.equalsIgnoreCase("UploadRequestedFile")) {
                        try {
                            String Name = (String) in.readObject();
                            int fileSize = (int) in.readObject();
                            int size = fileSize;
                            if ((Server.MAX_BUFFER_SIZE - fileSize) > 0) {
                                Server.MAX_BUFFER_SIZE -= fileSize;
                                Random r = new Random();
                                int chunk = r.nextInt(maxChunk - minChunk);
                                out.writeObject(chunk);
//                                String fileName = stdId + "\\" + "Public" + "\\" + Name;
//                                FileOutputStream fos = new FileOutputStream(fileName);
                                byte[] byteArray = new byte[fileSize];
                                int current = 0;
                                int bytesRead;
                                int receivedBytes = 0;
                                do {
                                    int sz = Math.min(fileSize, chunk);
                                    bytesRead = is.read(byteArray, current, sz);
//                                    fos.write(byteArray, current, bytesRead);
                                    current += sz;
                                    fileSize -= sz;
                                    receivedBytes += bytesRead;
                                    String ackmsg = "Yes";
                                    out.writeObject(ackmsg);
                                } while (fileSize > 0);
                                Server.MAX_BUFFER_SIZE += size;
                                String completionMsg = (String) in.readObject();
                                if (completionMsg.equalsIgnoreCase("Done")) {

                                    if (size == receivedBytes) {
                                        String fileName = stdId + "\\" + "Public" + "\\" + Name;
                                        FileOutputStream fos = new FileOutputStream(fileName);
                                        fos.write(byteArray, 0, receivedBytes);
                                        System.out.println("Incoming File: " + fileName);
                                        System.out.println("Size: " + size + "Byte");
                                        String verification = "File is verified";
                                        out.writeObject(true);
                                        int requestId = (int) in.readObject();

                                        System.out.println(verification);

                                        String requester = Server.hashMap.get(requestId);

                                        for (StudentClass sc : Server.tc) {
                                            if (sc.stdId.equalsIgnoreCase(requester)) {
                                                sc.queue.add("Your file with request id " + requestId + " is uploaded by " + stdId);
                                            }
                                        }

                                    } else {
                                        String verification = "File is corrupted";
                                        out.writeObject(verification);
                                        System.out.println("File is corrupted. File Received " + receivedBytes + " Byte");
//                                        fos.close();
//                                        File f = new File(fileName);
//                                        f.delete();
                                    }
                                }
                            } else {
                                int n = -1;
                                out.writeObject(n);
                            }


                        } catch (IOException | ClassNotFoundException e) {
                            System.out.println("Disconnected during upload");
                        }

                    } else if (receivedMsg.equalsIgnoreCase("LogOut")) {
                        isLoggedin = false;

                    }


            }
        }catch (IOException | ClassNotFoundException e){
            System.out.println("Disconnected");
        }
    }


}
