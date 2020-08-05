package IOSolution;

import java.io.*;
import java.net.Socket;

public class FileHandler implements Runnable {

    private String serverFilePath = "./src/main/resources/serverFiles";
    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;
    private boolean isRunning = true;
    private static int cnt = 1;

    public FileHandler(Socket socket) throws IOException {
        this.socket = socket;
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        String userName = "user" + cnt;
        cnt++;
        serverFilePath += "/" + userName;
        File dir = new File(serverFilePath);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                String command = is.readUTF();
                if (command.equals("./download")) {
                    String fileName = is.readUTF();
                    System.out.println("find file with name: " + fileName);
                    File file = new File(serverFilePath + "/" + fileName);
                    if (file.exists()) {
                        os.writeUTF("OK");
                        long len = file.length();
                        os.writeLong(len);
                        FileInputStream fis = new FileInputStream(file);
                        byte[] buffer = new byte[1024];
                        while (fis.available() > 0) {
                            int count = fis.read(buffer);
                            if (count == -1){
                                break;
                            }
                            os.write(buffer, 0, count);
                        }
                        fis.close();
                    } else {
                        os.writeUTF("File not exists");
                    }
                } else if (command.equals("./upload")) {
                    String fileName = is.readUTF();
                    String response = is.readUTF();
                    System.out.println("resp: " + response);
                    if (response.equals("OK")){
                        File file = new File(serverFilePath + "/" + fileName);
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                        long len = is.readLong();
                        byte [] buffer = new byte[1024];
                        try(FileOutputStream fos = new FileOutputStream(file)){
                            if (len < 1024) {
                                int count = is.read(buffer);
                                fos.write(buffer, 0, count);
                            } else {
                                for (long i = 0; i < len / 1024; i++) {
                                    int count = is.read(buffer);
                                    fos.write(buffer, 0, count);
                                }
                            }
                            fos.flush();
                            System.out.println(fos);
                        }
                    }

                    // TODO: 7/23/2020 upload
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}