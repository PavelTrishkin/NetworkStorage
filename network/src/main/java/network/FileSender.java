package network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSender {
    private static final byte SIGNAL_BYTE = 25;

    public static void sendFile(Path srcPath, Channel channel, ChannelFutureListener finishListener) throws IOException {
        System.out.println("Начинаем отправку файла");
        FileRegion region = new DefaultFileRegion(srcPath.toFile(), 0, Files.size(srcPath));
        byte[] filenameBytes = srcPath.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1 + 4 + filenameBytes.length + 8);

        buf.writeByte(SIGNAL_BYTE);
        System.out.println("Записали сигнальный байт" + SIGNAL_BYTE);
        buf.writeInt(filenameBytes.length);
        System.out.println("Записали длину имени файла" + filenameBytes.length);
        buf.writeBytes(filenameBytes);
        System.out.println("Записали имя файла" + filenameBytes);
        buf.writeLong(Files.size(srcPath));
        System.out.println("Записали размер файла" + Files.size(srcPath));
        channel.writeAndFlush(buf);
        System.out.println("Отправили файл");

        ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }

    }
}
