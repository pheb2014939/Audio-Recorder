package com.example.audiorecorder
//
//import android.util.Log
//import java.sql.Connection
//import java.sql.DriverManager
//import java.sql.SQLException
//class ConnectionClass {
//
//    companion object {
//        private const val db = "quanlymaynuoc"
//        private const val domain = "baokhoagold.ddns.net"
//        private const val port = "3306"
//        private const val username = "user1"
//        private const val password = "12345"
//    }
//
//    fun CNN(): Connection? {
//        var conn: Connection? = null
//        try {
//            // Load the MySQL JDBC driver
//            Class.forName("com.mysql.jdbc.Driver")
//
//            // Configure connection URL
//            val connectionString = "jdbc:mysql://$domain:$port/$db?useSSL=false&serverTimezone=UTC"
//
//            // Establish connection
//            conn = DriverManager.getConnection(connectionString, username, password)
//
//            Log.i("Connection", "Connected to databaseee")
//
//        } catch (e: ClassNotFoundException) {
//            Log.e("ERROR", "MySQL JDBC Driver not found: ${e.message}")
//        } catch (e: SQLException) {
//            Log.e("ERROR", "Failed to connect: ${e.message}")
//        }
//        return conn
//    }
//}

//package com.example.audiorecorder

import android.util.Log
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class ConnectionClass {
    companion object {
        private const val db = "quanlymaynuoc"
        private const val domain = "baokhoagold.ddns.net"
        private const val port = "3306"
        private const val username = "user1"
        private const val password = "12345"
    }

    fun CNN(): Connection? {
        var conn: Connection? = null
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.jdbc.Driver")

            // Configure connection URL
//            val connectionString = "jdbc:mysql://$domain:$port/$db?useSSL=false&serverTimezone=UTC"


            // Establish connection
            conn = DriverManager.getConnection("jdbc:mysql://baokhoagold.ddns.net:3306/quanlymaynuoc?user=user1&password=12345&serverTimezone=UTC")
            Log.i("Connection", "Connected to database")

        } catch (e: ClassNotFoundException) {
            Log.e("ERROR", "MySQL JDBC Driver not found: ${e.message}")
        } catch (e: SQLException) {
            Log.e("ERROR", "Failed to connect: ${e.message}")
        }
        return conn
    }
}
