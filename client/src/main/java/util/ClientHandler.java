package util;

import controllers.MainController;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    public enum State {
        IDLE, NAME_LENGTH, NAME, FILE_LENGTH, FILE
    }

    private FinishCallback finishCallBack;
    private UpdateCallBack updateCallBack;
    private AuthOkCallBack authOkCallBack;

    public void setAuthOkCallBack(AuthOkCallBack authOkCallBack) {
        this.authOkCallBack = authOkCallBack;
    }

    public void setUpdateCallBack(UpdateCallBack updateCallBack) {
        this.updateCallBack = updateCallBack;
    }

    public void setFinishCallBack(FinishCallback finishCallBack){
        this.finishCallBack = finishCallBack;
    }

    private static final byte AUTH_BYTE = 10;
    private static final byte AUTH_BYTE_OK = 20;
    private static final byte UPLOAD_FILE = 15;
    private static final byte DOWNLOAD_FILE = 25;
    private static final byte UPDATE_FILE_LIST = 30;


    private State currentState = State.IDLE;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;
    public static String fileList;

    public BufferedOutputStream getOut() {
        return out;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);
        while (buf.readableBytes() > 0) {
            if (currentState == State.IDLE) {
                int commandByte = buf.readByte();
                if (commandByte == AUTH_BYTE_OK) {
                    MainController.authOk = true;
                    nextLength = buf.readInt();
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    fileList = new String(fileName, StandardCharsets.UTF_8);
                    System.out.println("Получен список файлов");
                    System.out.println(Arrays.toString(fileList.split("SEPARATOR")));
                    try {
                        updateCallBack.updateCallBack();
                    }catch (NullPointerException e){
                        System.out.println("Пришел пусто список файлов " + e);
                    }

                }
                if(commandByte == UPDATE_FILE_LIST){
                    nextLength = buf.readInt();
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    fileList = new String(fileName, StandardCharsets.UTF_8);
                    System.out.println("Получен список файлов");
                    System.out.println(Arrays.toString(fileList.split("SEPARATOR")));
                    try {
                        updateCallBack.updateCallBack();
                    }catch (NullPointerException e){
                        System.out.println("Пришел пусто список файлов " + e);
                    }
                }
                if (commandByte == DOWNLOAD_FILE) {
                    System.out.println("Загружается файл");
                    currentState = State.NAME_LENGTH;
                    receivedFileLength = 0L;
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
                    out = new BufferedOutputStream(new FileOutputStream(MainController.getClientPanel().getCurrentPath() + "/" + new String(fileName, StandardCharsets.UTF_8)));
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
                        updateCallBack.updateCallBack();
                        finishCallBack.finishCallback();
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
  Не получается закинуть скачинвание в отдельный метод, почему то не заходит в IF условие на 138 строке.
 */
//    private void downLoadFile(ByteBuf buf) throws Exception {
//        if (currentState == State.NAME_LENGTH) {
//            if (buf.readableBytes() >= 4) {
//                System.out.println("Получение длины имени файла");
//                nextLength = buf.readInt();
//                currentState = State.NAME;
//            }
//        }
//        if (currentState == State.NAME) {
//            if (buf.readableBytes() >= nextLength) {
//                byte[] fileName = new byte[nextLength];
//                buf.readBytes(fileName);
//                System.out.println(Arrays.toString(fileName));
//                System.out.println("Получение имени файла: " + new String(fileName, StandardCharsets.UTF_8));
//                File file = new File(MainController.getActualClientPath() + new String(fileName));
//                out = new BufferedOutputStream(new FileOutputStream(file));
//                currentState = State.FILE_LENGTH;
//            }
//        }
//
//        if (currentState == State.FILE_LENGTH) {
//            if (buf.readableBytes() >= 8) {
//                fileLength = buf.readLong();
//                System.out.println("Получение длины файла: " + fileLength);
//                currentState = State.FILE;
//            }
//        }
//
//        if (currentState == State.FILE) {
//
//            out.write(buf.readByte());
//            receivedFileLength++;
//            if (fileLength == receivedFileLength) {
//                currentState = State.IDLE;
//                System.out.println("Файл успешно принят.");
//                out.close();
//            }
//        }
//
//        if (buf.readableBytes() == 0) {
//            buf.release();
//        }
//    }

//    public static void sendFile(Path path, Channel channel, ChannelFutureListener finishListener) throws IOException {
//        FileRegion region = new DefaultFileRegion(path.toFile(), 0, Files.size(path));
//
//        buf = ByteBufAllocator.DEFAULT.directBuffer(1);
//        buf.writeByte((byte) 25);
//        channel.writeAndFlush(buf);
//
//        buf = ByteBufAllocator.DEFAULT.directBuffer(4);
//        buf.writeInt(path.getFileName().toString().length());
//        channel.writeAndFlush(buf);
//
//        byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
//        System.out.println(path.getFileName().toString());
//        buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
//        buf.writeBytes(filenameBytes);
//        channel.writeAndFlush(buf);
//
//        buf = ByteBufAllocator.DEFAULT.directBuffer(8);
//        buf.writeLong(Files.size(path));
//        channel.writeAndFlush(buf);
//
//        ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
//        if (finishListener != null) {
//            transferOperationFuture.addListener(finishListener);
//        }
//    }

    public static void authorization(String login, String pass, Channel channel) {
        String lp = login + "DELIMETER" + pass;
        byte[] loginPassByte = lp.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + loginPassByte.length);
        System.out.println("Записали синальный байт на авторизацию");
        buf.writeByte(AUTH_BYTE);
        System.out.println("Записали длину логина и пароля");
        buf.writeInt(lp.length());
        System.out.println("Записали логин и пароль");
        buf.writeBytes(loginPassByte);
        System.out.println("Отправили данные буфера");
        channel.writeAndFlush(buf);
        System.out.println(new String(loginPassByte));
    }

}
