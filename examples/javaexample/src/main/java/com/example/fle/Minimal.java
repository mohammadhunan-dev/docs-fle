package com.example.fle;


import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.vault.ClientEncryptions;

import org.bson.BsonDocument;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;

import org.json.*;

public class Minimal {
    // this example is just for json schema doc
    static String dbName = "fieldLevelEncryptionDemo002";
    static String collectionName = "patients";
    public static void main(String[] args) {
        MongoCollection secureCollection = createSecureClient().getDatabase(dbName).getCollection(collectionName);
        insertDocument(secureCollection);
    }
    public static void insertDocument(MongoCollection collection){
        Document patientA = new Document();
        patientA.put("name", "John Doe");
        patientA.put("ssn",111111111);
        patientA.put("bloodType", "a-");
        Document medicalRecordOne = new Document();
        medicalRecordOne.put("heartRate", 100);
        ArrayList<Document> medicalRecords = new ArrayList<>();
        medicalRecords.add(medicalRecordOne);
        patientA.put("medicalRecords", medicalRecords);
        Document insuranceInfo = new Document();
        insuranceInfo.put("provider", "Octo Care");
        insuranceInfo.put("policyNumber", 1313);
        patientA.put("insurance",insuranceInfo);
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
        properties.put("name", getStandardField("string"));
        properties.put("ssn", getEncryptedField(keyId, "int", true)); // determin
        properties.put("bloodType", getEncryptedField(keyId, "string", false)); //random

        JSONObject insurance = new JSONObject();
        insurance.put("bsonType", "object");
        JSONObject insuranceProperties = new JSONObject();
            insuranceProperties.put("policyNumber", getEncryptedField(keyId, "int", true));
            insuranceProperties.put("provider", getStandardField("string"));
        insurance.put("properties", insuranceProperties);
        properties.put("insurance", insurance);

        JSONObject medicalRecords = getEncryptedField(keyId, "array", false);
        properties.put("medicalRecords",medicalRecords);

		JSONObject patientsSchema = new JSONObject();
		patientsSchema.put("properties", properties);
		patientsSchema.put("bsonType", "object");
        String patientsSchemaAsString = patientsSchema.toString();
        System.out.println(patientsSchemaAsString);
		return patientsSchemaAsString;
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