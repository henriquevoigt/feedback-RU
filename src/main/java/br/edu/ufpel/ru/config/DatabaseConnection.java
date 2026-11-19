package br.edu.ufpel.ru.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static final String CONFIG_FILE = "db.properties";

    private static String host = "localhost";
    private static String port = "5432";
    private static String dbName = "projeto_ru";
    private static String user = "grupo_bd";
    private static String password = "default";

    static {
        loadConfig();
    }

    public static void loadConfig() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            return;
        }
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
            host = props.getProperty("db.host", host).trim();
            port = props.getProperty("db.port", port).trim();
            dbName = props.getProperty("db.name", dbName).trim();
            user = props.getProperty("db.user", user).trim();
            password = props.getProperty("db.password", password);
        } catch (IOException e) {
            System.err.println("Aviso: Não foi possível ler " + CONFIG_FILE + ": " + e.getMessage());
        }
    }

    public static void saveConfig(String newHost, String newPort, String newDbName, String newUser, String newPassword) throws IOException {
        host = newHost.trim();
        port = newPort.trim();
        dbName = newDbName.trim();
        user = newUser.trim();
        password = newPassword;

        Properties props = new Properties();
        props.setProperty("db.host", host);
        props.setProperty("db.port", port);
        props.setProperty("db.name", dbName);
        props.setProperty("db.user", user);
        props.setProperty("db.password", password);

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "Configuracao de conexao PostgreSQL (Tailscale / Local)");
        }
    }

    public static String getJdbcUrl() {
        return String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);
    }

    public static Connection getConnection() throws SQLException {
        DriverManager.setLoginTimeout(5);
        return DriverManager.getConnection(getJdbcUrl(), user, password);
    }

    public static String getHost() {
        return host;
    }

    public static String getPort() {
        return port;
    }

    public static String getDbName() {
        return dbName;
    }

    public static String getUser() {
        return user;
    }

    public static String getPassword() {
        return password;
    }
}
