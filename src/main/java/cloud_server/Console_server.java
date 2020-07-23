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
//        String fileName = in.readUTF();
//        System.out.println("fileName: " + fileName);
//        File file = new File("./common/server/" + fileName);
//        file.createNewFile();
//        try (FileOutputStream os = new FileOutputStream(file)) {
//            byte[] buffer = new byte[8192];
//            while (true) {
//                int r = in.read(buffer);
//                if (r == -1) break;
//                os.write(buffer, 0, r);
//            }
//        }
//        System.out.println("File uploaded!");
    }

    public static void main(String[] args) throws IOException {
        new Console_server();
    }

}
