import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import network.FileSender;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    public enum State {
        IDLE, NAME_LENGTH, NAME, FILE_LENGTH, FILE
    }

    private static final String SERVER_ROOT = "./server-storage-dir/";
    private static final byte AUTH_BYTE = 10;
    private static final byte AUTH_BYTE_OK = 20;
    private static final byte ISLOGIN_BYTE = 45;
    private static final byte UPLOAD_FILE = 25;
    private static final byte DOWNLOAD_FILE = 15;
    private static final byte UPDATE_FILE_LIST = 30;
    private static final byte REGISTRATION_BYTE = 50;
    private static final byte REGISTRATION_OK_BYTE = 55;
    private static final byte REGISTRATION_FAILED_BYTE = 50;

    private State currentState = State.IDLE;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;
    private String login;
    private StringBuffer stringBuffer = new StringBuffer();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Клиент подключился. Addr: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Клиент отключился. Addr: " + ctx.channel().remoteAddress() + " Login: " + login);
        SqlClient.setIsLogin(login, false);
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("Жду команды");
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            if (currentState == State.IDLE) {
                int commandByte = buf.readByte();
                if (commandByte == AUTH_BYTE) {
                    System.out.println("Получена команда на авторизацию пользователя");
                    receivedFileLength = 0L;
                    connection(ctx, buf);
                }
                if (commandByte == REGISTRATION_BYTE){
                    registration(ctx, buf);
                }
                if (commandByte == UPLOAD_FILE) {
                    System.out.println("Получена команда на прием файла от клиента");
                    currentState = State.NAME_LENGTH;
                    receivedFileLength = 0L;
//                    getFileFromClient(buf);
                }
                if (commandByte == DOWNLOAD_FILE) {
                    System.out.println("Получена команда на отправку файла");
                    uploadFile(buf, ctx);
                }
                if (commandByte == UPDATE_FILE_LIST) {
                    System.out.println("Получена команда обновления списка файлов");
                    updateFileList(ctx.channel(), getFileList(SERVER_ROOT + login + "//"));
                }
                if (commandByte == 60){
                    if (buf.readableBytes() >= 4) {
                        System.out.println("Получаем длину названия файла для удаления");
                        nextLength = buf.readInt();
                    }
                    if (buf.readableBytes() >= nextLength) {
                        byte[] fileName = new byte[nextLength];
                        buf.readBytes(fileName);
                        System.out.println("Имя файла для удаления " + new String(fileName, StandardCharsets.UTF_8));
                        Files.delete(Paths.get(Paths.get(SERVER_ROOT + login).toAbsolutePath().normalize().toString(),new String(fileName, StandardCharsets.UTF_8)));
                        System.out.println("Файл " + new String(fileName, StandardCharsets.UTF_8) + " был удален");
                        updateFileList(ctx.channel(), getFileList(SERVER_ROOT + login + "//"));
                    }
                    if (buf.readableBytes() == 0) {
                        buf.release();
                    }
                }
            }
            if (currentState == State.NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    System.out.println("Получаем длину названия");
                    nextLength = buf.readInt();
                    currentState = State.NAME;
                }
            }
            if (currentState == State.NAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    System.out.println("Получен файл: " + new String(fileName, StandardCharsets.UTF_8));
                    out = new BufferedOutputStream(new FileOutputStream(SERVER_ROOT + login + "/" + new String(fileName, StandardCharsets.UTF_8)));
                    currentState = State.FILE_LENGTH;
                }
            }
            if (currentState == State.FILE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    fileLength = buf.readLong();
                    System.out.println("Получена длина файла " + fileLength);
                    currentState = State.FILE;
                }
            }
            if (currentState == State.FILE) {
                if (buf.readableBytes() > 0) {
                    out.write(buf.readByte());
                    receivedFileLength++;
                    if (fileLength == receivedFileLength) {
                        currentState = State.IDLE;
                        System.out.println("Файл получен");
                        out.close();
                        updateFileList(ctx.channel(), getFileList(SERVER_ROOT + login + "//"));
                        break;
                    }
                }
            }
        }
        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    /*
       Не получается закинуть скачинвание в отдельный метод, почему то не заходит в IF условие на 159 строке.
     */
//    private void getFileFromClient(ByteBuf buf) throws Exception {
//        if (currentState == State.NAME_LENGTH) {
//            if (buf.readableBytes() >= 4) {
//                System.out.println("Получаем длину названия");
//                nextLength = buf.readInt();
//                currentState = State.NAME;
//            }
//        }
//        if (currentState == State.NAME) {
//            if (buf.readableBytes() >= nextLength) {
//                byte[] fileName = new byte[nextLength];
//                buf.readBytes(fileName);
//                System.out.println("Получен файл: " + new String(fileName, StandardCharsets.UTF_8));
//                out = new BufferedOutputStream(new FileOutputStream(SERVER_ROOT + login + "/" + new String(fileName, StandardCharsets.UTF_8)));
//                currentState = State.FILE_LENGTH;
//            }
//        }
//        if (currentState == State.FILE_LENGTH) {
//            if (buf.readableBytes() >= 8) {
//                fileLength = buf.readLong();
//                System.out.println("Получена длина файла " + fileLength);
//                currentState = State.FILE;
//            }
//        }
//        if (currentState == State.FILE) {
//            if (buf.readableBytes() > 0) {
//                out.write(buf.readByte());
//                receivedFileLength++;
//                if (fileLength == receivedFileLength) {
//                    currentState = State.IDLE;
//                    System.out.println("Файл получен");
//                    out.close();
//                }
//            }
//        }
//        if (buf.readableBytes() == 0) {
//            buf.release();
//        }
//    }

    private void connection(ChannelHandlerContext ctx, ByteBuf buf) {
        nextLength = buf.readInt();
        byte[] log = new byte[nextLength];
        buf.readBytes(log);
        String[] lpBefore = new String(log, StandardCharsets.UTF_8).split("DELIMETER");
        login = lpBefore[0];
        String pass = lpBefore[1];
        System.out.println("Логин " + login + " пароль " + pass);
        if(SqlClient.isLogin(login)) {
            buf = ByteBufAllocator.DEFAULT.directBuffer(1);
            buf.writeByte(ISLOGIN_BYTE);
            ctx.channel().writeAndFlush(buf);
        }
        else if(SqlClient.authorise(login, pass)){
            SqlClient.setIsLogin(login, true);
            File f = new File(SERVER_ROOT + login);
            if (!f.exists()) {
                f.mkdir();
            }
            String fileList = getFileList(SERVER_ROOT + login);
            serverFileList(ctx.channel(), fileList);
            System.out.println("Авторизация прошла успешна");
        }
    }

    private void registration(ChannelHandlerContext ctx, ByteBuf buf){
        nextLength = buf.readInt();
        byte[] log = new byte[nextLength];
        buf.readBytes(log);
        String[] lpBefore = new String(log, StandardCharsets.UTF_8).split("DELIMETER");
        login = lpBefore[0];
        String pass = lpBefore[1];
        System.out.println("Логин " + login + " пароль " + pass);
        if (SqlClient.isRegisteredUser(login)){
            buf = ByteBufAllocator.DEFAULT.directBuffer(1);
            buf.writeByte(REGISTRATION_FAILED_BYTE);
            ctx.channel().writeAndFlush(buf);
            System.out.println("Такой пользователь существует");
        }else {
            if(SqlClient.registration(login, pass)){
                buf = ByteBufAllocator.DEFAULT.directBuffer(1);
                buf.writeByte(REGISTRATION_OK_BYTE);
                ctx.channel().writeAndFlush(buf);
            }
        }

    }

    private static void serverFileList(Channel channel, String fileList) {
        byte[] fileListByte = fileList.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + fileListByte.length + 8);

        buf.writeByte(AUTH_BYTE_OK);
        buf.writeInt(fileListByte.length);
        buf.writeBytes(fileListByte);
        channel.writeAndFlush(buf);
        System.out.println("Отправляем список файлов клиенту");
    }

    private void uploadFile(ByteBuf buf, ChannelHandlerContext ctx) throws IOException {
        nextLength = buf.readInt();
        byte[] fileName = new byte[nextLength];
        buf.readBytes(fileName);
        String name = new String(fileName, StandardCharsets.UTF_8);
        System.out.println("Имя файла, который запросил клиент" + name);

        FileSender.sendFile(Paths.get(SERVER_ROOT + login + "/" + new String(fileName, StandardCharsets.UTF_8)), ctx.channel(), channelFuture -> {
            if (!channelFuture.isSuccess()) {
                channelFuture.cause().printStackTrace();
            }
            if (channelFuture.isSuccess()) {
                System.out.println("Файл успешно передан!");
            }
        });
    }

    private void updateFileList(Channel channel, String fileList) {
        byte[] fileListBytes = fileList.getBytes(StandardCharsets.UTF_8);
        ByteBuf buffer = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + fileListBytes.length);
        buffer.writeByte(UPDATE_FILE_LIST);
        buffer.writeInt(fileListBytes.length);
        buffer.writeBytes(fileListBytes);
        channel.writeAndFlush(buffer);
        System.out.println("Записали и отправили сигнальный байт  " + UPDATE_FILE_LIST + "  - успешная проверка + список файлов");
    }

    private String getFileList(String pathToFile) {
        String filePath;
        File filesDir = new File(pathToFile);
        stringBuffer.setLength(0);
        File[] dir = filesDir.listFiles();
        for (int i = 0; i < dir.length; i++) {
            if (dir[i].isFile()) {
                filePath = pathToFile + "//" + dir[i].getName();
                try {
                    stringBuffer.append(dir[i].getName()).append("SEPARATOR").append(Files.size(Paths.get(filePath))).append("NEXT_FILE");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return stringBuffer.toString();
    }

    @Override

    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ;
    }
}
