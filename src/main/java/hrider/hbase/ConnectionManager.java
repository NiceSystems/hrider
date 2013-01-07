package hrider.hbase;

import hrider.config.ConnectionDetails;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConnectionManager {

    private static Map<ConnectionDetails, Connection> connections;

    private ConnectionManager() {
    }

    static {
        connections = new HashMap<ConnectionDetails, Connection>();
    }

    public static Connection create(ConnectionDetails details) throws IOException {
        Connection connection = connections.get(details);
        if (connection == null) {
            connection = new Connection(details);
            connections.put(details, connection);
        }
        return connection;
    }

    public static void release(ConnectionDetails details) {
        connections.remove(details);
    }
}
