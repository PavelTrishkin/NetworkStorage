package cloud_server;

import java.io.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Arrays;

public class SocketThread extends Thread {
    private final Socket socket;
    private DataOutputStream out;
    DataInputStream in;
    private int clientId;
    private boolean isAuth = false;

    private String folderPath = "./src/main/java/cloud/ ";
    private String downloadPath = "./src/main/java/download/ ";
    File dir;

    public SocketThread(Socket socket) {
        this.socket = socket;
        start();
    }

    @Override
    public void run() {
        try {
            System.out.println("Connected");
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            System.out.println("Socket Ready");
            String id = in.readUTF();
            auth(id);
            mainMenu();
            if (isAuth) {
                createDir();
                while (!isInterrupted()) {
                    String msg = in.readUTF();
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


    /*
     **Создание директории с именем пользователя
     */
    private synchronized void createDir() {
        dir = new File(folderPath + clientId);
        boolean created = dir.mkdirs();
        if (created) {
            System.out.println("Folder created" + dir.getName());
        }
    }
/*
** Информация о файлах
 */
    private synchronized void folderInfo() throws IOException {
        dir = new File(folderPath + clientId);
        if (dir.listFiles().length == 0) {
            sendMessage("Folder is empty");
            return;
        }
        sendMessage(Arrays.toString(dir.list()));
    }

    /*
     * Авторизация
     */
    private synchronized void auth(String id) {
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
/*
** Главное меню
 */
    public synchronized void mainMenu() throws IOException {
        sendMessage(
                "Press B for Back \n" +
                        "Press U for Upload \n" +
                        "Press D for Download \n" +
                        "Press I for Info");
    }

    /*
    ** Обработчик сообщений
     */
    public synchronized void messageProcessing(String msg) throws IOException {

        if (msg.equalsIgnoreCase("U")) {
//            sendMessage("Upload");
            uploadFile();
        } else if (msg.equalsIgnoreCase("D")) {
            sendMessage("DownLoad");
        } else if (msg.equalsIgnoreCase("I")) {
            folderInfo();
        } else
            mainMenu();
    }

//    public synchronized void downloadFile(String fileName){
//        out.writeUTF();
//    }

    public synchronized void uploadFile() throws IOException {
//        out.writeUTF("Insert FileName");
        String fileName = in.readUTF();
        System.out.println("fileName: " + fileName);
        File file = new File(folderPath + fileName);
        file.createNewFile();
        try (FileOutputStream os = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            while (true) {
                int r = in.read(buffer);
                if (r == -1) break;
                os.write(buffer, 0, r);
            }
        }
        System.out.println("File uploaded!");
    }

}
