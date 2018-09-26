import com.github.jtendermint.jabci.api.*;
import com.github.jtendermint.jabci.socket.TSocket;
import com.github.jtendermint.jabci.types.*;
import com.google.protobuf.ByteString;
import org.apache.derby.iapi.reference.SQLState;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.sql.*;
import java.util.Base64;
import java.util.Objects;
import java.util.Properties;

public class JavaKVStore implements IDeliverTx, ICheckTx, ICommit, IQuery {
    final private static String STATE_KEY = "stateKey";
    final private static String KV_PAIR_PREFIX_KEY = "kvPairKey:";

    final private Statement st;
    final private TSocket socket;

    private long size;
    private ByteString app_hash;
    private long height;

    private static Connection getDatabaseConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");

        Properties props = new Properties();
        props.put("user", "javauser");
        props.put("password", "lomu");

        String db = "jdbc:derby:kvstore";
        Connection conn;
        conn = DriverManager.getConnection(db, props);

        return conn;
    }

    private static void setupDatabase() throws SQLException, ClassNotFoundException {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");

        Properties props = new Properties();
        props.put("user", "javauser");
        props.put("password", "lomu");

        String db = "jdbc:derby:kvstore;create=true";
        Connection conn;
        conn = DriverManager.getConnection(db, props);

        Statement st = conn.createStatement();

        String sql = "CREATE TABLE kvstore" +
                "(" +
                "    mkey VARCHAR(255) PRIMARY KEY NOT NULL," +
                "    mvalue VARCHAR(255)" +
                ")";
        st.execute(sql);

        sql = "CREATE UNIQUE INDEX kvstore_mkey_uindex ON kvstore (mkey)";
        st.execute(sql);
    }

    public JavaKVStore() throws SQLException, ClassNotFoundException, InterruptedException {
        System.out.println("starting kvstore");
        socket = new TSocket();
        Connection conn = getDatabaseConnection();
        st = conn.createStatement();

        String sql = "SELECT * FROM kvstore WHERE mkey='" + STATE_KEY + "'";
        ResultSet result = st.executeQuery(sql);
        if (!result.wasNull()) {
            while (result.next()) {
                String jsonString = result.getString("mvalue");
                JSONObject obj = new JSONObject(jsonString);

                String hashString = obj.getString("app_hash");

                size = obj.getLong("size");
                height = obj.getLong("height");
                app_hash = ByteString.copyFrom(Base64.getDecoder().decode(hashString));
            }
        }

        socket.registerListener(this);

        Thread t = new Thread(() -> socket.start(26658));
        t.setName("KVStore Java Main Thread");
        t.start();

        System.out.println("started");

        while (true) {
            Thread.sleep(1000L);
        }
    }

    public static void main(String args[]) throws SQLException, ClassNotFoundException, InterruptedException {
        new JavaKVStore();
        //setupDatabase();
    }

    public ResponseDeliverTx receivedDeliverTx(RequestDeliverTx req) {
        ByteString tx = req.getTx();
        String txString = new String(tx.toByteArray());
        System.out.printf("got deliver tx, with %s%n", txString);

        String key, value;
        String[] parts = txString.split("=");
        if (parts.length == 2) {
            key = parts[0];
            value = parts[1];
        } else {
            key = txString;
            value = txString;
        }

        insertIntoDatabase(KV_PAIR_PREFIX_KEY + key, value);
        size += 1;
        System.out.printf("Size is now: %d%n", size);

        return ResponseDeliverTx.newBuilder().setCode(CodeType.OK).build();
    }

    private void insertIntoDatabase(String key, String value) {
        String sql = "INSERT INTO kvstore VALUES ('" + key + "', '" + value + "')";
        try {
            st.execute(sql);
        } catch (SQLException e) {
            if (e.getSQLState().equals(SQLState.LANG_DUPLICATE_KEY_CONSTRAINT)) {
                sql = "UPDATE kvstore SET mvalue='" + value + "' WHERE mkey='" + key + "'";
                try {
                    st.execute(sql);
                } catch (SQLException ex) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ResponseCheckTx requestCheckTx(RequestCheckTx req) {
        ByteString tx = req.getTx();
        String txString = new String(tx.toByteArray());
        System.out.printf("got check tx, with %s%n", txString);

        return ResponseCheckTx.newBuilder().setCode(CodeType.OK).build();
    }

    public ResponseCommit requestCommit(RequestCommit requestCommit) {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(size);
        buf.rewind();
        app_hash = ByteString.copyFrom(buf);
        height += 1;

        String json = "{\"size\":" + size + ",\"height\":" + height + ",\"app_hash\":\"" + Base64.getEncoder().encodeToString(app_hash.toByteArray()) + "\"}";

        insertIntoDatabase(STATE_KEY, json);
        return ResponseCommit.newBuilder().setData(app_hash).build();
    }

    public ResponseQuery requestQuery(RequestQuery req) {
        if (req.getProve()) {
            ByteString mkey = req.getData();
            String sql = "SELECT * FROM kvstore WHERE mkey='" + KV_PAIR_PREFIX_KEY + mkey.toStringUtf8() + "'";
            ResultSet result = null;
            try {
                result = st.executeQuery(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            Objects.requireNonNull(result);
            ByteString value = null;
            try {
                while (result.next()) {
                    value = ByteString.copyFrom(result.getString("mvalue"), "UTF-8");
                }
            } catch (SQLException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (value != null) {
                return ResponseQuery.newBuilder().setIndex(-1).setKey(mkey).setValue(value).setLog("exists").build();
            } else {
                return ResponseQuery.newBuilder().setLog("does not exist").build();
            }
        } else {
            ByteString mkey = req.getData();
            String sql = "SELECT * FROM kvstore WHERE mkey='" + KV_PAIR_PREFIX_KEY + mkey.toStringUtf8() + "'";
            ResultSet result = null;
            try {
                result = st.executeQuery(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            Objects.requireNonNull(result);
            ByteString value = null;
            try {
                while (result.next()) {
                    value = ByteString.copyFrom(result.getString("mvalue"), "UTF-8");
                }
            } catch (SQLException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (value != null) {
                return ResponseQuery.newBuilder().setValue(value).setLog("exists").build();
            } else {
                return ResponseQuery.newBuilder().setLog("does not exist").build();
            }
        }
    }
}