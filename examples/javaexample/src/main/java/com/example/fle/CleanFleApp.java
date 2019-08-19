package com.example.fle;


import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.vault.ClientEncryptions;

import org.bson.BsonDocument;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import org.json.*;
import java.util.function.Consumer;

public class CleanFleApp {
    static String dbName = "fieldLevelEncryptionDemo001";
    static String collectionName = "patients";
    
    public static void main(String[] args) {
        MongoCollection secureCollection = createSecureClient().getDatabase(dbName).getCollection(collectionName);
        secureCollection.drop();
        // insertDocument(secureCollection);
        // getDocs(secureCollection);
        // getOnePatient(secureCollection);

        Document filterDoc = new Document();
        filterDoc.put("ssn",777331111);
  
        Document updateDoc = new Document();
        updateDoc.append("$set", new Document("address","200 summerville terrace"));
        updateOne(secureCollection, filterDoc, updateDoc);
    }
    public static void getDocs(MongoCollection collection){
        collection.find().forEach((Consumer<Document>) document -> {
			System.out.println("   "+ document);
		});
    }
    public static void getOnePatient(MongoCollection collection) {
        Document query = new Document();
        query.put("ssn", 777331111);
        // query.put("address", "300 golden road");        
        Object ab = collection.find(query).first();
        // System.out.println("--------");
        // System.out.println(ab);
        // System.out.println("--------");

    }

    public static void updateOne(MongoCollection collection, Document filterDoc, Document updateDoc) {
       
        collection.updateOne(filterDoc, updateDoc);

        Object ab = collection.find(filterDoc).first();
        // System.out.println("--------");
        // System.out.println(ab);
        // System.out.println("--------");
    }


    public static void insertDocument(MongoCollection collection){
        Document patientA = new Document();
        patientA.put("address", "300 golden road");
        patientA.put("last4SSN", 1111);
        patientA.put("fullName", "john doe");
        Document patientInfo = new Document();
        patientInfo.put("phone", 718400200);
        patientInfo.put("provider", "NXINSURE");
        patientA.put("patientInfo", patientInfo);
        patientA.put("ssn", 777331111);
    
        Document medicalRecordOne = new Document();
        medicalRecordOne.put("heartRate", 100);

        ArrayList<Document> medicalRecords = new ArrayList<>();
        medicalRecords.add(medicalRecordOne);

        patientA.put("medicalRecords", medicalRecords);
        collection.insertOne(patientA);
    }
    public static JSONObject getEncryptedField(String keyId, String chosenBsonType, Boolean isDeterministic){
        JSONObject binaryForKey = new JSONObject();
		binaryForKey.put("base64", keyId);
		binaryForKey.put("subType", "04");
		JSONObject binWrapper = new JSONObject();
        binWrapper.put("$binary", binaryForKey);
        
		JSONObject encryptedFieldWrap = new JSONObject();
		JSONObject encrypt = new JSONObject();

        encrypt.put("bsonType", chosenBsonType);
        if(isDeterministic){
            encrypt.put("algorithm", "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic");

        }else{
            encrypt.put("algorithm", "AEAD_AES_256_CBC_HMAC_SHA_512-Random");

        }
		encrypt.put("keyId", new JSONArray().put(binWrapper));
		encryptedFieldWrap.put("encrypt", encrypt);
		return encryptedFieldWrap;
    }
    public static JSONObject getStandardField(String chosenBsonType){
        JSONObject standardField = new JSONObject();
		standardField.put("bsonType", chosenBsonType);
		standardField.put("description", "must be a " + chosenBsonType + " and is required");
		return standardField;
    }
    public static String getJSONSchema(String keyId){

        JSONObject properties = new JSONObject();
        properties.put("fullName", getStandardField("string"));
        // System.out.println("original " + keyId);
        // System.out.println("new " + makeKey());
		properties.put("address", getEncryptedField(makeKey(), "string", false));
		properties.put("ssn", getEncryptedField(keyId, "int", true));
        properties.put("last4SSN", getStandardField("int"));
        JSONObject patientInfo = new JSONObject();
        patientInfo.put("bsonType", "object");
        JSONObject patientInfoProperties = new JSONObject();
            patientInfoProperties.put("phone", getEncryptedField(keyId, "int", true));
            patientInfoProperties.put("provider", getStandardField("string"));
        patientInfo.put("properties", patientInfoProperties);
        properties.put("patientInfo", patientInfo);

        JSONObject medicalRecords = getEncryptedField(keyId, "array", false);
        properties.put("medicalRecords",medicalRecords);

        
		JSONObject patientsSchema = new JSONObject();
		patientsSchema.put("properties", properties);
		patientsSchema.put("bsonType", "object");
        String patientsSchemaAsString = patientsSchema.toString();
        System.out.println(patientsSchemaAsString);
		return patientsSchemaAsString;
    }
    public static String makeKey(){
        var localMasterKey = Base64.getDecoder().decode("MHZsOF/POyDm24Jih9NF+30VcMAXx6YJv/urrVU2VoHtoH7FFXxia/RsEGx1nqc+m9vpoU/ov+AIJbaa9hRiZPQ+T0p8hN0mxBlBgyt74vFhCYyep3eqljh1yIsouBlC");

		var kmsProviders = Map.of("local",
				Map.<String, Object>of("key", localMasterKey));

		var keyVaultNamespace = "admin.datakeys";


		var keyVaultSettings = ClientEncryptionSettings.builder()
				.keyVaultMongoClientSettings(MongoClientSettings.builder()
				.build())
				.keyVaultNamespace(keyVaultNamespace)
				.kmsProviders(kmsProviders)
				.build();

		var keyVault = ClientEncryptions.create(keyVaultSettings);
		var dataKeyId = keyVault.createDataKey("local", new DataKeyOptions());
        var base64DataKeyId = Base64.getEncoder().encodeToString(dataKeyId.getData());
        return base64DataKeyId;
    }
    public static MongoClient createSecureClient(){
		var localMasterKey = Base64.getDecoder().decode("MHZsOF/POyDm24Jih9NF+30VcMAXx6YJv/urrVU2VoHtoH7FFXxia/RsEGx1nqc+m9vpoU/ov+AIJbaa9hRiZPQ+T0p8hN0mxBlBgyt74vFhCYyep3eqljh1yIsouBlD");

		var kmsProviders = Map.of("local",
				Map.<String, Object>of("key", localMasterKey));

		var keyVaultNamespace = "admin.datakeys";


		var keyVaultSettings = ClientEncryptionSettings.builder()
				.keyVaultMongoClientSettings(MongoClientSettings.builder()
				.build())
				.keyVaultNamespace(keyVaultNamespace)
				.kmsProviders(kmsProviders)
				.build();

		var keyVault = ClientEncryptions.create(keyVaultSettings);
		var dataKeyId = keyVault.createDataKey("local", new DataKeyOptions());
		var base64DataKeyId = Base64.getEncoder().encodeToString(dataKeyId.getData());
		var autoEncryptionSettings =
				AutoEncryptionSettings.builder()
						.keyVaultNamespace(keyVaultNamespace)
						.kmsProviders(kmsProviders)
						.schemaMap(
								Map.of(dbName + "." + collectionName,
										BsonDocument.parse(getJSONSchema(base64DataKeyId)))
						)
						.build();

		var clientSettings = MongoClientSettings.builder()
				.autoEncryptionSettings(autoEncryptionSettings)
				.build();

		return  MongoClients.create(clientSettings);
    }
}