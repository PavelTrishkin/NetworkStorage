package NIO;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Iterator;

public class NIOServer implements Runnable {
    private ServerSocketChannel server;
    private Selector selector;
    private int client = 1;
    private byte[] complete = {-1};

    public NIOServer() throws IOException {
        server = ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(8189));
        server.configureBlocking(false);
        selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
    }

    @Override
    public void run() {
        try {
            System.out.println("server started");
            while (server.isOpen()) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        System.out.println("client accepted");
                        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
                        channel.configureBlocking(false);
                        channel.register(selector, SelectionKey.OP_READ);
                    }
                    if (key.isReadable()) {
                        System.out.println("read key");
                        ByteBuffer buffer = ByteBuffer.allocate(80);
                        ByteBuffer downloadBuffer = ByteBuffer.allocate(8192);
                        ByteBuffer length_file_byteBuf = ByteBuffer.allocate(100);

                        int count = 0;
                        try {
                            count = ((SocketChannel) key.channel()).read(buffer);
                        } catch (IOException e) {
                            System.out.println("Client disconnect");
                            key.channel().close();
                            break;
                        }
                        if (count == -1) {
                            key.channel().close();
                            break;
                        }
                        buffer.flip();
                        String msg = new String(buffer.array(), 0, buffer.limit(), StandardCharsets.UTF_8);
                        System.out.println(msg);
                        String[] splitMsg = msg.split(" ");
                        buffer.clear();
                        switch (splitMsg[0]) {
                            case "./ID":
                                key.channel().register(selector, SelectionKey.OP_READ, Integer.parseInt(splitMsg[1]));
                                ((SocketChannel) key.channel()).write(ByteBuffer.wrap(complete));
                                Path directory_client = Paths.get("data_server/" + key.attachment().toString());
                                if (!Files.exists(directory_client, LinkOption.NOFOLLOW_LINKS)) {
                                    Files.createDirectory(Paths.get("data_server", key.attachment().toString()));
                                }
                                break;
                            case "./download":
                                Path downloadFile = Paths.get("data_server", String.valueOf(key.attachment()), splitMsg[1]);
                                ((SocketChannel) key.channel()).write(ByteBuffer.wrap(complete));
                                if (Files.exists(downloadFile, LinkOption.NOFOLLOW_LINKS)) {
                                    Files.delete(downloadFile);
                                }
                                ((SocketChannel) key.channel()).read(length_file_byteBuf);
                                while (true) {
                                    count = ((SocketChannel) key.channel()).read(length_file_byteBuf);
                                    if (count <= 0) break;
                                }
                                length_file_byteBuf.flip();
                                long lentgh_file = length_file_byteBuf.getLong();
                                System.out.println(lentgh_file);
                                ((SocketChannel) key.channel()).write(ByteBuffer.wrap(complete));
                                RandomAccessFile accessFile =
                                        new RandomAccessFile("data_server/" + key.attachment() + "/" + splitMsg[1], "rw");
                                FileChannel fileChannel = accessFile.getChannel();
                                while (Files.size(downloadFile) < lentgh_file) {
                                    ((SocketChannel) key.channel()).read(downloadBuffer);
                                    downloadBuffer.flip();
                                    fileChannel.write(downloadBuffer);
                                    downloadBuffer.compact();
                                }
                                fileChannel.close();
                                accessFile.close();
                                length_file_byteBuf.rewind();
                                length_file_byteBuf.clear();
                                System.out.println("Client " + key.attachment() + ", download file: " + splitMsg[1]);
                                break;
                            case "./upload":
                                Path uploadFile = Paths.get("data_server", String.valueOf(key.attachment()), splitMsg[1]);
                                ByteBuffer length_upload_file = ByteBuffer.allocate(100).putLong(Files.size(uploadFile));
                                ((SocketChannel) key.channel()).write(length_upload_file);
                                System.out.println(Files.size(uploadFile));
                                ((SocketChannel) key.channel()).write(ByteBuffer.wrap(complete));
                                break;
                            case "./info":
                                String pathUser = "data_server/" + key.attachment();
                                File file = new File(pathUser);
                                String userFiles = "";
                                for (String str :
                                        file.list()) {
                                    if (userFiles.equals("")) {
                                        userFiles += str;
                                    } else {
                                        userFiles += "," + str;
                                    }
                                }
                                ((SocketChannel) key.channel()).write(ByteBuffer.wrap(userFiles.getBytes()));
                                ((SocketChannel) key.channel()).write(ByteBuffer.wrap(complete));
                                break;
                        }
                        System.out.println();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        new Thread(new NIOServer()).start();
    }
}