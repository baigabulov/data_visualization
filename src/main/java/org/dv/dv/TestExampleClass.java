package org.dv.dv;

public class TestExampleClass {

    public static void main(String[] args) {
        MongoConnector mongoConnector = new MongoConnector();
        mongoConnector.deleteAllCollections();
        mongoConnector.saveFromFileToDatabase();
        mongoConnector.parseAll();
    }
}
