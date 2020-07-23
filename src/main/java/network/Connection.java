package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Connection {
    private final Socket socket;
    private final Thread socketThread;
    private final ConnectionListener listener;
    private DataInputStream in;
    private DataOutputStream out;

    public Connection(ConnectionListener listener, String ipAdd, int port, String name) throws IOException {
        this(listener, new Socket(ipAdd, port));
    }

    public Connection(ConnectionListener listener, Socket socket) throws IOException {
        this.listener = listener;
        this.socket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        socketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    listener.onConnectionReady(Connection.this);
                    while (!socketThread.isInterrupted()){
                        listener.onReceiveString(Connection.this, in.readUTF());
                    }

                } catch (IOException e) {
                    listener.onException(Connection.this, e);
                }
                finally {
                    listener.onConnectionStopped(Connection.this);
                }
            }
        });
        socketThread.start();
    }

    public synchronized void sendMsg(String msg){
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            listener.onException(Connection.this, e);
            disconnect();
        }
    }

    public synchronized void disconnect(){
        socketThread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            listener.onException(Connection.this, e);
        }
        listener.onConnectionStopped(Connection.this);
    }
}
