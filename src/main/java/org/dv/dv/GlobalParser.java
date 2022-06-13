package org.dv.dv;

public class GlobalParser {
    public static void main(String[] args) {
        MongoConnector mongoConnector = new MongoConnector();

        // Parse from file to MongoDB
        mongoConnector.saveFromFileToDatabase();

        // Parse from MongoDB to MySQL
        mongoConnector.parseAll();
    }
}
