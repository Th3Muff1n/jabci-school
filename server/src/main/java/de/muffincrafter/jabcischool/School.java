package de.muffincrafter.jabcischool;

import com.github.jtendermint.jabci.api.*;
import com.github.jtendermint.jabci.socket.TSocket;
import com.github.jtendermint.jabci.types.*;
import com.google.protobuf.ByteString;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Objects;

public class School implements IDeliverTx, ICheckTx, ICommit, IQuery, IInfo {
    final private static String STATE_KEY = "stateKey";
    final private static String PREFIX_KEY = "schoolEntry:";

    private final TSocket socket;
    private final Statement st;
    private ByteString app_hash;
    private long size;
    private long height;

    private School() throws ClassNotFoundException, SQLException, InterruptedException {
        System.out.println("starting school");
        socket = new TSocket();

        Connection conn = null;
        try {
            conn = DatabaseTools.getConnection("javauser", "lomu", "school");
        } catch (SQLException e) {
            if (e.getSQLState().equals("08004")) {
                String sql1 = "CREATE TABLE certificates" +
                        "(" +
                        "    mkey VARCHAR(255) PRIMARY KEY NOT NULL," +
                        "    mvalue VARCHAR(255)" +
                        ")";
                String sql2 = "CREATE UNIQUE INDEX certificates_mkey_uindex ON certificates (mkey)";
                conn = DatabaseTools.createDatabase("javauser", "lomu", "school", sql1, sql2);
            }
        }

        Objects.requireNonNull(conn);
        st = conn.createStatement();

        String sql = "SELECT * FROM certificates WHERE mkey='" + STATE_KEY + "'";
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
        t.setName("School Main Thread");
        t.start();

        while (true) {
            Thread.sleep(1000L);
        }
    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException, InterruptedException {
        new School();
    }

    @Override
    public ResponseDeliverTx receivedDeliverTx(RequestDeliverTx req) {
        ByteString tx = req.getTx();
        String txString = new String(tx.toByteArray());
        System.out.printf("got deliver tx, with %s%n", txString);

        if (!JsonTools.checkJSON(txString)) {
            String message = "expected valid json string with required values, got " + txString;
            System.out.printf("returning bad, %s", message);
            return ResponseDeliverTx.newBuilder().setCode(CodeType.BadNonce).setLog(message).build();
        }

        JSONObject obj = new JSONObject(txString);
        JSONObject info = obj.getJSONObject("info");

        String student_name = info.getString("student_name");

        String key, value;
        key = student_name;
        value = txString;

        DatabaseTools.insertIntoDatabase(st, "certificates", PREFIX_KEY + key, value);

        size += 1;
        System.out.printf("Size is now: %d%n", size);

        return ResponseDeliverTx.newBuilder().setCode(CodeType.OK).build();
    }

    @Override
    public ResponseCheckTx requestCheckTx(RequestCheckTx req) {
        ByteString tx = req.getTx();
        String txString = new String(tx.toByteArray());
        System.out.printf("got check tx, with %s%n", txString);

        if (!JsonTools.checkJSON(txString)) {
            String message = "expected valid json string with required entries, got " + txString;
            System.out.printf("returning bad, %s", message);
            return ResponseCheckTx.newBuilder().setCode(CodeType.BadNonce).build();
        }

        return ResponseCheckTx.newBuilder().setCode(CodeType.OK).build();
    }

    @Override
    public ResponseCommit requestCommit(RequestCommit requestCommit) {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.putLong(size);
        buf.rewind();

        app_hash = ByteString.copyFrom(buf);
        height += 1;

        String json = "{\"size\":" + size + ",\"height\":" + height + ",\"app_hash\":\"" + Base64.getEncoder().encodeToString(app_hash.toByteArray()) + "\"}";

        DatabaseTools.insertIntoDatabase(st, "certificates", STATE_KEY, json);

        return ResponseCommit.newBuilder().setData(app_hash).build();
    }

    @Override
    public ResponseQuery requestQuery(RequestQuery req) {
        if (req.getProve()) {
            ByteString mkey = req.getData();
            String sql = "SELECT * FROM certificates WHERE mkey='" + PREFIX_KEY + mkey.toStringUtf8() + "'";

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
            String sql = "SELECT * FROM certificates WHERE mkey='" + PREFIX_KEY + mkey.toStringUtf8() + "'";

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

    @Override
    public ResponseInfo requestInfo(RequestInfo req) {
        if (height != 0 && app_hash != null) {
            return ResponseInfo.newBuilder().setLastBlockHeight(height).setLastBlockAppHash(app_hash).build();
        }
        return ResponseInfo.newBuilder().build();
    }
}
