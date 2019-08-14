package com.example.fle;


import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.vault.DataKeyOptions;
import com.mongodb.client.vault.ClientEncryption;
import com.mongodb.client.vault.ClientEncryptions;
import com.mongodb.internal.HexUtils;
import org.bson.BsonDocument;
import org.bson.Document;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.function.Consumer;

import org.json.*;

public class FleApplication {
	static String dbName = "testFLE01";
	static String collName = "coll";

	public static void main(String[] args) {

		var encryptedClient = createSecureClient();
		var secureCollection = encryptedClient.getDatabase(dbName).getCollection(collName);
		secureCollection.drop();
		// secureCollection.insertOne(new Document("encryptedField", "123456789"));
		// secureCollection.insertOne(new Document("encryptedField", "hello world"));
		// secureCollection.insertOne(new Document("encryptedField", "foobar"));
		// secureCollection.insertOne(new Document("encryptedField", "fizzbuzz"));

		Document obj1 = new Document();
		obj1.put("encryptedField", "44-11-1111");
		obj1.put("standardField", "Nintendo Switch");

		secureCollection.insertOne(obj1);
	}
	public static JSONObject makeEncryptedFieldJSON(String keyId){
		JSONObject binaryForKey = new JSONObject();
		binaryForKey.put("base64", keyId);
		binaryForKey.put("subType", "04");
		JSONObject binWrapper = new JSONObject();
		binWrapper.put("$binary", binaryForKey);


		JSONObject encryptedFieldWrap = new JSONObject();

		JSONObject encrypt = new JSONObject();

		encrypt.put("bsonType", "string");
		encrypt.put("algorithm", "AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic");
		encrypt.put("keyId", new JSONArray().put(binWrapper));

		encryptedFieldWrap.put("encrypt", encrypt);

		return encryptedFieldWrap;
	}
	public static String getSchemaJSON(String keyId){
		JSONObject encryptedField = makeEncryptedFieldJSON(keyId);

		JSONObject standardField = new JSONObject();
		
		standardField.put("bsonType", "string");
		standardField.put("description", "must be a string and is required");

		JSONObject properties = new JSONObject();
		properties.put("standardField", standardField);
		properties.put("encryptedField", encryptedField);


		JSONObject patientsSchema = new JSONObject();
		patientsSchema.put("properties", properties);
		patientsSchema.put("bsonType", "object");

		String patientsSchemaAsString = patientsSchema.toString();

		System.out.println(patientsSchemaAsString);

		return patientsSchemaAsString;

		// String jsonString = "{" +
		// "        \"properties\": {" +
		// "            \"standardField\": {" +
		// "                \"bsonType\": \"string\"," +
		// "				 \"description\": \"must be a string and is required\", " +
		// "                }," +
		// "            \"encryptedField\": {" +
		// "                \"encrypt\": {" +
		// "                    \"keyId\": [" +
		// "                        {" +
		// "                            \"$binary\": {" +
		// "                                \"base64\": \"" + keyId + "\"," +
		// "                                \"subType\": \"04\"" +
		// "                            }" +
		// "                        }" +
		// "                    ]," +
		// "                    \"bsonType\": \"string\"," +
		// "                    \"algorithm\": \"AEAD_AES_256_CBC_HMAC_SHA_512-Deterministic\"" +
		// "                }" +
		// "            }" +
		// "        }," +
		// "        \"bsonType\": \"object\"" +
		// "    }";
		// // System.out.println(jsonString);
		// return jsonString;
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
										BsonDocument.parse(getSchemaJSON(base64DataKeyId)))
						)
						.build();

		var clientSettings = MongoClientSettings.builder()
				.autoEncryptionSettings(autoEncryptionSettings)
				.build();

		return  MongoClients.create(clientSettings);

	}
}
