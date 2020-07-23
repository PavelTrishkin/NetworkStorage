package cloud_client;

import cloud_server.SocketThread;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class  Client extends Thread {
    SocketThread socketThread;

    Scanner sc = new Scanner(System.in);

    public Client() {
        try (Socket s = new Socket("127.0.0.1", 8189)) {
            System.out.println("Socket connected");
            DataInputStream in = new DataInputStream(s.getInputStream());
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            System.out.println("Socket ready");
            System.out.println("Press your ID");
            String id = sc.nextLine();
            out.writeUTF(id);
            System.out.println(in.readUTF());
            while (true) {
                out.writeUTF(sc.nextLine());
                String b = in.readUTF();
                System.out.println(b);
                System.out.println(id);
                if(sc.nextLine().equalsIgnoreCase("U")){
                    System.out.println("Введите имя файла");
                    String fileName = "./src/main/java/download/" + sc.nextLine();
                    sendFile(s, new File(fileName));
                }
                System.out.println("byte written");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendFile(Socket socket, File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long size = file.length();
        int count = (int) (size / 8192) / 10, readBuckets = 0;
        // /==========/
        try(DataOutputStream os = new DataOutputStream(socket.getOutputStream())) {
            byte [] buffer = new byte[8192];
            os.writeUTF(file.getName());
            System.out.print("/");
            while (is.available() > 0) {
                int readBytes = is.read(buffer);
                readBuckets++;
                if (readBuckets % count == 0) {
                    System.out.print("=");
                }
                os.write(buffer, 0, readBytes);
            }
            System.out.println("/");
        }
    }

    public static void main(String[] args) {
        new Client();
    }
}
