package de.muffincrafter.jabcischool;

import org.apache.derby.iapi.reference.SQLState;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseTools {
    public static Connection getConnection(String username, String password, String dbPath) throws ClassNotFoundException, SQLException {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");

        Properties props = new Properties();
        props.put("user", username);
        props.put("password", password);

        String db = "jdbc:derby:" + dbPath;

        return DriverManager.getConnection(db, props);
    }

    public static Connection createDatabase(String username, String password, String dbPath, String... sqlStrings) throws SQLException, ClassNotFoundException {
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");

        Properties props = new Properties();
        props.put("user", username);
        props.put("password", password);

        String db = "jdbc:derby:" + dbPath + ";create=true";
        Connection conn = DriverManager.getConnection(db, props);

        Statement st = conn.createStatement();
        for (String sql : sqlStrings) {
            st.execute(sql);
        }

        return conn;
    }

    public static void insertIntoDatabase(Statement st, String table, String key, String value) {
        String sql = "INSERT INTO " + table + " VALUES ('" + key + "', '" + value + "')";
        try {
            st.execute(sql);
        } catch (SQLException e) {
            if (e.getSQLState().equals(SQLState.LANG_DUPLICATE_KEY_CONSTRAINT)) {
                sql = "UPDATE " + table + " SET mvalue='" + value + "' WHERE mkey='" + key + "'";
                try {
                    st.execute(sql);
                } catch (SQLException ex) {
                    e.printStackTrace();
                }
            }
        }
    }
}
