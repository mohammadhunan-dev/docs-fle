package com.example.fle;


import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.vault.ClientEncryption;
import com.mongodb.client.vault.ClientEncryptions;
import com.mongodb.internal.HexUtils;

import org.bson.BsonDocument;
import org.bson.Document;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.function.Consumer;

import org.json.*;

public class FleApplication {
	static String dbName = "testFLE01";
	static String collName = "coll";

	public static void main(String[] args) {

		MongoCollection secureCollection = createSecureClient().getDatabase(dbName).getCollection(collName);
		secureCollection.drop();

		// insert one object:
		Document obj1 = new Document();
		obj1.put("address", "300 golden road");
		obj1.put("fullName", "jane doe");
		obj1.put("last4SSN", 3333);
		obj1.put("ssn", 777553333);
		obj1.put("telephone",718981444);
		Document apptForObj1 = new Document();
		apptForObj1.put("appointmentDescription", "headaches and migraines");
		apptForObj1.put("date", new Date());
		obj1.append("appointments", apptForObj1);

		secureCollection.insertOne(obj1);
	}

	public static JSONObject makeEncryptedFieldJSON(String keyId, String chosenBsonType){
		JSONObject binaryForKey = new JSONObject();
		binaryForKey.put("base64", keyId);
		binaryForKey.put("subType", "04");
		JSONObject binWrapper = new JSONObject();
		binWrapper.put("$binary", binaryForKey);


		JSONObject encryptedFieldWrap = new JSONObject();

		JSONObject encrypt = new JSONObject();

		encrypt.put("bsonType", chosenBsonType);
		encrypt.put("algorithm", "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic");
		encrypt.put("keyId", new JSONArray().put(binWrapper));

		encryptedFieldWrap.put("encrypt", encrypt);

		return encryptedFieldWrap;
	}

	public static JSONObject makeStandardFieldJSON(String chosenBsonType){
		JSONObject standardField = new JSONObject();
		standardField.put("bsonType", chosenBsonType);
		standardField.put("description", "must be a " + chosenBsonType + " and is required");
		return standardField;
	}


	public static String makeJSONSchema(String keyId){
		// create fields
		// make nested appts
		JSONObject appointments = new JSONObject();
			appointments.put("bsonType", "array");
			JSONObject appointmentProperties = new JSONObject();
			appointmentProperties.put("date", makeStandardFieldJSON("date"));
			appointmentProperties.put("appointmentDescription",  makeStandardFieldJSON("string"));

			JSONObject itemsSubObject = new JSONObject();
			itemsSubObject.put("bsonType", "object");
			itemsSubObject.put("properties", appointmentProperties);

			appointments.put("items", itemsSubObject);


		// put fields in properties
		JSONObject properties = new JSONObject();
		properties.put("fullName", makeStandardFieldJSON("string"));
		properties.put("address", makeEncryptedFieldJSON(keyId, "string"));
		properties.put("telephone", makeEncryptedFieldJSON(keyId, "int"));
		properties.put("ssn", makeEncryptedFieldJSON(keyId, "int"));
		properties.put("last4SSN", makeStandardFieldJSON("int"));
		properties.put("appointments", appointments); 

		JSONObject patientsSchema = new JSONObject();
		patientsSchema.put("properties", properties);
		patientsSchema.put("bsonType", "object");
		String patientsSchemaAsString = patientsSchema.toString();
		return patientsSchemaAsString;
	}

	public static MongoClient createSecureClient(){

		//make a static Master Key, ideally you would get a master key from AWS
		var localMasterKey = Base64.getDecoder().decode("MHZsOF/POyDm24Jih9NF+30VcMAXx6YJv/urrVU2VoHtoH7FFXxia/RsEGx1nqc+m9vpoU/ov+AIJbaa9hRiZPQ+T0p8hN0mxBlBgyt74vFhCYyep3eqljh1yIsouBlD");

		var kmsProviders = Map.of("local",
				Map.<String, Object>of("key", localMasterKey));

		var keyVaultNamespace = "admin.datakeys";


		var keyVaultSettings = ClientEncryptionSettings.builder()
				.keyVaultMongoClientSettings(MongoClientSettings.builder()
				// If you're not using a local mongodb for your key vault.
				// .applyConnectionString(new ConnectionString("mongodb://localhost"))
				.build())
				.keyVaultNamespace(keyVaultNamespace)
				.kmsProviders(kmsProviders)
				.build();

		var keyVault = ClientEncryptions.create(keyVaultSettings);
		// Might need data key options, but maybe not for "local"
		var dataKeyId = keyVault.createDataKey("local", new DataKeyOptions());
		var base64DataKeyId = Base64.getEncoder().encodeToString(dataKeyId.getData());

		var autoEncryptionSettings =
				AutoEncryptionSettings.builder()
						.keyVaultNamespace(keyVaultNamespace)
						.kmsProviders(kmsProviders)
						.schemaMap(
								Map.of(dbName + "." + collName,
										// Need a schema that references the new data key
										BsonDocument.parse(makeJSONSchema(base64DataKeyId)))
						)
						.build();

		var clientSettings = MongoClientSettings.builder()
				.autoEncryptionSettings(autoEncryptionSettings)
				.build();

		return  MongoClients.create(clientSettings);

	}
}
