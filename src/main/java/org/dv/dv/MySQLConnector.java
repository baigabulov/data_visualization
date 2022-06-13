package org.dv.dv;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class MySQLConnector {

    public static Connection conn;

    public MySQLConnector() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        try{
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
            String url = "jdbc:mysql://localhost:3306/dv";
            String username = "root";
            String password = "password";
            Connection conn = DriverManager.getConnection(url, username, password);
            this.conn = conn;
        }catch (Exception e){
            System.out.println(e);
        }
    }

    public void createTable(String query){
        try {
            Statement stmt = this.conn.createStatement();
            stmt.executeUpdate(query);
        }catch (Exception e){
            System.out.println(e);
        }
    }

    public void insertIntoTableData(String tableName, String keys, String values){
        try{
            String stmtString = "INSERT INTO " + tableName + keys + " VALUES" + values+";";
            System.out.println("STMT: " + stmtString);
            Statement statement = this.conn.createStatement();
            statement.executeUpdate(stmtString);
        }catch (Exception e){
            System.out.println(e);
        }
    }

    public void deleteAllTables(){
        try{
            Statement statement = this.conn.createStatement();
            String stmtString = "DROP TABLES IF EXISTS EXAMPLE_1, EXAMPLE_2, EXAMPLE_3, EXAMPLE_4, unescomsko, nbrkcurrencyrates1v16, tpeeduformdata, zhergiliktiatkarushyorgan2v9, tprofedu, darihanav5, hpestteachdata," +
                    "unescomskodata, tprofedudata";
            statement.executeUpdate(stmtString);
        }catch (Exception e){
            System.out.println(e);
        }
    }
}
