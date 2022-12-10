// start local cluster with : ./client start-single-node --insecure
// cockroach build tested and working CCL v22.2.0 @ 2022/12/05 16:37:36 (go1.19.1)

// This app : JAva 11 and driver postgresql-42.5.1.jar

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.postgresql.ds.PGSimpleDataSource;

public class App {

  public static void executeStmt(Connection conn, String stmt) {
    try {
      System.out.println(stmt);

      // works whether autocommit=true is present or not as true
      // fails autocommit-false because thie leads to a multi statememtn tran and a refresh isn't allowed in that.
      // I think the postgres/coachroach code has recently changed to have an error about multip stmt trans whereas previously it gave the warning below ..
      //  error : cannot refresh view in an explicit transaction

      // conn.setAutoCommit(true); 

      Statement st = conn.createStatement();
      boolean ret = st.execute(stmt);

      if (ret) {
        ResultSet rs = st.getResultSet();
        while (rs.next())
        {
          System.out.println(" " + rs.getString(1));
        }
        rs.close();
        st.close();
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  private static String[] statements = {
    "DROP MATERIALIZED VIEW IF EXISTS vw",
    "DROP TABLE IF EXISTS tab",
    "CREATE TABLE IF NOT EXISTS tab (a STRING)",
    "INSERT INTO tab(a) VALUES('before-create-view')",
    "CREATE MATERIALIZED VIEW IF NOT EXISTS VW AS SELECT a FROM tab",
    "INSERT INTO tab(a) VALUES('after-create-view')", // only visible in view if refreshed
    "REFRESH MATERIALIZED VIEW VW ",
    "SELECT * FROM vw"
  };

  public static void main(String[] args) {
    try {
      PGSimpleDataSource ds = new PGSimpleDataSource();
      ds.setApplicationName("docs_quickstart_java");
      ds.setUrl("jdbc:postgresql://Zoom:26257/defaultdb?sslmode=disable&user=root");
      ds.setUser("root");
      ds.setPassword("");

      Connection connection = ds.getConnection();
      for (int n=0;n<statements.length;n++){
        executeStmt(connection,statements[n]);
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }
}
