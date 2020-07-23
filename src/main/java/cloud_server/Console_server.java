package cloud_server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Console_server{

    ServerSocket server;
    SocketThread socketThread;

    public Console_server() throws IOException {
        System.out.println("Server start");
        server = new ServerSocket(8189);
        while (true){
            socketThread = new SocketThread(server.accept());
        }
    }

    public static void main(String[] args) throws IOException {
        new Console_server();
    }

}
