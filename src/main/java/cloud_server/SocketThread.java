package cloud_server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketThread extends Thread {
    private final Socket socket;
    private DataOutputStream out;
    private int clientId;
    private boolean isAuth = false;

    public SocketThread(Socket socket) {
        this.socket = socket;
        start();
    }

    @Override
    public void run() {
        try {
            System.out.println("Connected");
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            System.out.println("Socket Ready");
//            String id = in.readUTF();

            mainMenu();
            while (!isInterrupted()) {
                String msg = in.readUTF();
                auth(msg);
                if(isAuth) {
                    System.out.println(clientId);
                    messageProcessing(msg);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
            System.out.println("Server stopped");
        }
    }


    private synchronized void auth(String id){
        clientId = Integer.parseInt(id);
        isAuth = true;
    }

    public synchronized void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            close();
        }
    }

    public synchronized void close() {
        interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void mainMenu() throws IOException {
        sendMessage(
                "Press 0 for Back \n" +
                        "Press 1 for Create New folder \n" +
                        "Press 2 for Upload \n" +
                        "Press 3 for Download \n" +
                        "Press 4 for Info");
    }

    public synchronized void messageProcessing(String msg) throws IOException {
            String msg1 = msg;
            if(msg1.equalsIgnoreCase("b")){
                sendMessage("Back");
                mainMenu();
            }else if(msg1.equalsIgnoreCase("u")){
                if(msg1.equalsIgnoreCase("b")){
                    mainMenu();
                }
                sendMessage("Upload");
            }else if(msg1.equalsIgnoreCase("d")){
                sendMessage("DownLoad");
            }else if(msg1.equalsIgnoreCase("c")){
                createNewFolder(msg);
            }
            else
                mainMenu();

//        int value = Integer.parseInt(msg);
//
//        switch (value) {
//            case 0:
//                sendMessage("Back");
//                break;
//            case 1:
//                sendMessage("Create New folder");
//                createNewFolder(msg);
//                break;
//            case 2:
//                sendMessage("Upload");
//                break;
//            case 3:
//                sendMessage("Download");
//                break;
//            case 4:
//                sendMessage("Info" + msg);
//                break;
//            default:
//                mainMenu();
//        }
    }

    private void createNewFolder(String msg){
        sendMessage("Insert name of folder");
        String name = msg;
        System.out.println(name);
    }
}
