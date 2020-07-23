package cloud_client;

import cloud_server.SocketThread;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
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
                System.out.println(id);
                out.writeUTF(sc.nextLine());
                System.out.println("byte written");
                String b = in.readUTF();
                System.out.println(b);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        new Client();
    }
}
