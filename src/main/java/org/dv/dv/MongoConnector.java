package org.dv.dv;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.mongodb.client.*;
import org.bson.Document;
import org.json.*;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;

import static org.bson.codecs.configuration.CodecRegistries.fromCodecs;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoConnector {

    private static MongoClient client;
    private static MongoDatabase database;
    private static MySQLConnector sqlConnector;


    public MongoConnector(){
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
        rootLogger.setLevel(Level.OFF);
        this.client = MongoClients.create("mongodb://root:password@localhost:27017");
        this.database = this.client.getDatabase("dv");
        try{
            this.sqlConnector = new MySQLConnector();
        }catch (Exception e){
            System.out.println(e);
        }
    }


    public MongoClient getClient(){
        return this.client;
    }


    public MongoDatabase getDatabase(){
        return this.database;
    }

    public ArrayList<String> getAllCollections(){
        ArrayList<String> list = new ArrayList<>();
        for(String name: this.database.listCollectionNames()){
            list.add(name);
        }
        return list;
    }

    public ArrayList<Document> getAllDocumentsByCollection(String collectionName){
        ArrayList<Document> list = new ArrayList<>();
        MongoCollection<Document> collection = this.database.getCollection(collectionName);
        FindIterable<Document> iterDoc = collection.find();
        Iterator<Document> iterator = iterDoc.iterator();
        while(iterator.hasNext()){
            Document d = iterator.next();
            list.add(d);
        }
        return list;
    }

    private String getColumnDatatypeMatchingSQL(Document document, String key){
        switch (document.get(key).getClass().toString()){
            case "class java.lang.Integer": return "INTEGER";
            case "class java.lang.Double": return "DOUBLE";
            case "class java.lang.Float": return "FLOAT";
            case "class java.lang.Boolean": return "BOOLEAN";
            case "class java.lang.String": return "TEXT";
            case "class java.lang.Date": return "DATE";
            case "class java.lang.Datetime": return "DATETIME";
            default: return "TEXT";
        }
    }

    public LinkedHashMap<String, String> getAllColumnNamesForCollection(ArrayList<Document> documents){
        if(documents.size() == 0)
            return null;

        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        for(Document d: documents){
            Set<String> keys = d.keySet();
            for(String k: keys){
                columns.put(k, this.getColumnDatatypeMatchingSQL(d, k));
            }
        }
        return columns;
    }

    public void createSQLTablesReferencingCollection(String collectionName, LinkedHashMap<String, String> columns){
        if(columns == null)
            return;
        collectionName = String.join("_", collectionName.split(" "));
        String sqlStatement = "CREATE TABLE " + collectionName + " (";
        for(int i = 0; i < columns.size(); i++){
            if(i == columns.size()-1){
                sqlStatement += columns.keySet().toArray()[i].toString() + " " + columns.values().toArray()[i] + "\n";
            }else{
                sqlStatement += columns.keySet().toArray()[i].toString() + " " + columns.values().toArray()[i] + ",\n";
            }
        }
        sqlStatement += ");";
        System.out.println("CREATING TABLE\n" + sqlStatement);
        this.sqlConnector.createTable(sqlStatement);
    }

    public void insertIntoTable(String collectionName, ArrayList<Document> documents){
        if(documents.size() == 0)
            return;

        System.out.println("INSERTING COLLECTION " + collectionName);
        collectionName = String.join("_", collectionName.split(" "));

        for(Document d: documents){
            String key = "(";
            String values = "(";
            for(int i = 0; i < d.keySet().size(); i++){
                if(i == d.keySet().size()-1) {
                    key += d.keySet().toArray()[i];
                    values += "\'"+d.get(d.keySet().toArray()[i])+"\'";
                }
                else{
                    key += d.keySet().toArray()[i] + ", ";
                    values += "\'"+d.get(d.keySet().toArray()[i]) + "\', ";
                }
            }
            key += ")";
            values += ")";
            this.sqlConnector.insertIntoTableData(collectionName, key, values);
        }

    }

    public void parseAll(){
        // Get all collections' names
        ArrayList<String> allCollections = this.getAllCollections();

        // Get all documents for each collection
        ArrayList<ArrayList<Document>> allDocuments = new ArrayList<>();
        for(String collectionName: allCollections){
            allDocuments.add(this.getAllDocumentsByCollection(collectionName));
        }

        // Figure out SQL tables conversion
        // return type LinkedHashMap<String, String>, key = field name, value = field data type
        ArrayList<LinkedHashMap<String, String>> allColumns = new ArrayList<>();
        for(ArrayList<Document> collectionDocuments: allDocuments){
            LinkedHashMap<String, String> columns = this.getAllColumnNamesForCollection(collectionDocuments);
            allColumns.add(columns);
        }

        // Create table by this collection name and cols
        for(int i = 0; i < allColumns.size(); i++){
            createSQLTablesReferencingCollection(allCollections.get(i), allColumns.get(i));
        }

        // Insert data into tables
        for(int i = 0; i < allCollections.size(); i++){
            insertIntoTable(allCollections.get(i), allDocuments.get(i));
        }
    }

    public void saveFromFileToDatabase(){

        try {
            List<String> allFiles = Files.walk(Paths.get("datafiles")).map(Path::getFileName)
                    .map(Path::toString).collect(Collectors.toList());
            for(String file: allFiles){
                System.out.println(file);
            }
            System.out.println("GETTING ALL LINES OF DATA IN FILES");
            for(String file: allFiles){
                if(file.compareTo("datafiles") != 0 && file.compareTo(".DS_Store") != 0){
                    System.out.println("FILE " + file);
                    System.out.println(Files.readAllLines(Paths.get("datafiles", file)));
                    String finalFile = file;
                    Optional<String> ext = Optional.ofNullable(file)
                            .filter(f -> f.contains("."))
                            .map(f -> f.substring(finalFile.lastIndexOf(".") + 1));
                    if (ext.isPresent()) {
                        JSONArray jsonArray = null;
                        if (ext.get().compareTo("xml") == 0) {
                            String fileStr = String.valueOf(Files.readAllLines(Paths.get("datafiles", file), StandardCharsets.UTF_8));
                            file = FilenameUtils.removeExtension(file);
                            JSONObject json = XML.toJSONObject(fileStr);
                            file = file.replaceAll("-data", "");
                            jsonArray = (JSONArray) json.getJSONObject(file).get("data");
                            file = file.replaceAll("[^A-Za-z0-9]", "");
                            MongoCollection<Document> nCollection = this.getDatabase().getCollection(file);
                            for (Object j : jsonArray) {
                                JSONObject jObj = (JSONObject) j;
                                JSONObject nJSONObject = new JSONObject();
                                for (String k : jObj.keySet()) {
                                    String nKey = k.replaceAll("[^A-Za-z0-9]", "");
                                    nJSONObject.put(nKey, jObj.get(k));
                                }
                                Document doc = Document.parse(nJSONObject.toString());
                                nCollection.insertOne(doc);
                            }
                        } else if (ext.get().compareTo("json") == 0) {
                            String fileStr = String.valueOf(Files.readAllLines(Paths.get("datafiles", file), StandardCharsets.UTF_8));
                            fileStr = fileStr.substring(1, fileStr.length() - 1);
                            file = FilenameUtils.removeExtension(file);
                            file = file.replaceAll("[^A-Za-z0-9]", "");
                            JSONArray json = new JSONArray(fileStr);
                            MongoCollection<Document> nCollection = this.getDatabase().getCollection(file);
                            for (Object j : json) {
                                JSONObject jObj = (JSONObject) j;
                                JSONObject nJSONObject = new JSONObject();
                                for (String k : jObj.keySet()) {
                                    String nKey = k.replaceAll("[^A-Za-z0-9]", "");
                                    nJSONObject.put(nKey, jObj.get(k));
                                }
                                Document doc = Document.parse(nJSONObject.toString());
                                nCollection.insertOne(doc);
                            }
                        } else if (ext.get().compareTo("csv") == 0) {
                            String csv = "";
                            BufferedReader reader;
                            try {
                                reader = new BufferedReader(new FileReader(
                                        Paths.get("datafiles", file).toString()));
                                String line = reader.readLine();
                                while (line != null || line == "") {
                                    System.out.println(line);
                                    if(line != null){
                                        csv += line + "\n";
                                        line = reader.readLine();
                                    }
                                }
                                reader.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            JSONArray json = CDL.toJSONArray(csv);
                            file = FilenameUtils.removeExtension(file);
                            file = file.replaceAll("[^A-Za-z0-9]", "");
                            MongoCollection<Document> nCollection = this.getDatabase().getCollection(file);
                            for (Object j : json) {
                                JSONObject jObj = (JSONObject) j;
                                JSONObject nJSONObject = new JSONObject();
                                for (String k : jObj.keySet()) {
                                    String nKey = k.replaceAll("[^A-Za-z0-9]", "");
                                    nJSONObject.put(nKey, jObj.get(k));
                                }
                                Document doc = Document.parse(nJSONObject.toString());
                                nCollection.insertOne(doc);
                            }
                        }
                    }
                }
            }
        }catch(Exception e){
            System.out.println("EXCEPTION READING FILES " + e);
        }
    }

    public void addSomeTestData(){
        this.database.createCollection("EXAMPLE 5");
        MongoCollection<Document> collection = this.database.getCollection("EXAMPLE 3");
        collection.insertOne(
                new Document("title1", 1)
        );

        this.database.createCollection("EXAMPLE 4");
        MongoCollection<Document> collection2 = this.database.getCollection("EXAMPLE 4");
        collection2.insertOne(
                new Document("title3", 2).append("title4", 4.5)
        );
        collection2.insertOne(new Document("title3", 3).append("title4", 5));
    }

    public void showAllCollectionsAndDocs(){
        // Get all collections' names
        ArrayList<String> allCollections = this.getAllCollections();

        // Get all documents for each collection
        ArrayList<ArrayList<Document>> allDocuments = new ArrayList<>();
        for(String collectionName: allCollections){
            allDocuments.add(this.getAllDocumentsByCollection(collectionName));
        }

        for(ArrayList<Document> docs: allDocuments){
            for(Document d: docs){
                System.out.println(d);
            }
        }
    }

    public void deleteAllCollections(){
        this.database.drop();
        this.sqlConnector.deleteAllTables();
    }

}
