package network;

public interface ConnectionListener {
    void onConnectionReady(Connection connection);
    void onReceiveString(Connection connection, String msg);
    void onConnectionStopped(Connection connection);
    void onException(Connection connection, Exception e);
}
