package cloud_client;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class Client {
    private Socket socket;
    private DataInputStream is;
    private DataOutputStream os;
    private static final int PORT = 8189;

    ByteBuffer byteBufferLengthFile = ByteBuffer.allocate(Long.BYTES);
    private static Scanner scanner = new Scanner(System.in);

    private static Socket createSocket() throws IOException {
        return new Socket("localhost", PORT);
    }

    private void connect(Socket socket, int id) throws IOException {
        this.socket = socket;
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        os.write(String.format("./ID " + id).getBytes("UTF8"));
        if (is.readByte() == -1) {
            System.out.println("Successful connection");
        }

    }

    public void downloadFile(String fileName) throws IOException {
        File file = new File("./src" + "/" + fileName);
        long size = file.length();
        int count = (int) (size / 8192) / 10, transferBuckets = 0;
        os.write(String.format("./download " + fileName).getBytes("UTF8"));
        if (is.readByte() == -1) {
            byteBufferLengthFile.putLong(size);
            os.write(byteBufferLengthFile.array());
        }
        byteBufferLengthFile.clear();
        if (is.readByte() == -1) {
            System.out.println("Start download file " + fileName);
        }
        try (FileInputStream fis = new FileInputStream(file);) {
            byte[] buffer = new byte[8192];
            System.out.print("[ ");
            while (fis.available() > 0) {
                int readBytes = fis.read(buffer);
                transferBuckets++;
                if (count != 0 && transferBuckets % count == 0) {
                    System.out.print("=");
                }
                os.write(buffer, 0, readBytes);
            }
            System.out.println(" ]");
        }

    }

    public void uploadFile(String fileName) throws IOException {
        os.write(String.format("./upload " + fileName).getBytes("UTF8"));
        ByteBuffer byteBuffer = ByteBuffer.allocate(100);
        byte readByte = 0;
        while (true) {
            readByte = is.readByte();
            if (readByte == -1) break;
            byteBuffer.put(readByte);
        }
        byteBuffer.rewind();
        long length_file = byteBuffer.get(); // всегда 0
        System.out.println(length_file);
    }

    public void info() throws IOException {
        os.write(String.format("./info ").getBytes("UTF8"));
        byte readByte = 0;
        String files = "";
        while (true) {
            readByte = is.readByte();
            if (readByte == -1) break;
            files += (char) readByte;
        }

        String[] filesList = files.split(",");

        System.out.println("*********");
        for (int i = 0; i < filesList.length; i++) {
            System.out.println(filesList[i]);
        }
        System.out.println("*********");
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        System.out.println("Enter your ID");
        int id = scanner.nextInt();
        System.out.println("Your ID : " + id);
        client.connect(createSocket(), id);
        String msg;
        while (true) {
            System.out.println("./download fileName - for download file\n./upload fileName - for upload file" +
                    "\n./info - for get a list of files");
            msg = scanner.nextLine();
            String[] split_msg = msg.split(" ");
            switch (split_msg[0]) {
                case "./download":
                    client.downloadFile(split_msg[1]);
                    break;
                case "./upload":
                    client.uploadFile(split_msg[1]);
                    break;
                case "./info":
                    client.info();
                    break;
            }
            if (msg.equals("exit")) break;
        }

        client.socket.close();
    }
}