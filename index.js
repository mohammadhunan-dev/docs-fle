const fs = require('fs');
const faker = require('faker');
const Cryptr = require('cryptr');
const env = require('./env.json');
const cryptr = new Cryptr(env.cryptr.key);


const mockEncryptionEngine = () => {
    const min = Math.ceil(111111111);
    const max =Math.floor(999999999);
    const generatedSSN =  Math.floor(Math.random() * (max - min)) + min; 
    const stringifiedSSN = String(generatedSSN)
    return cryptr.encrypt(stringifiedSSN);
}
const data = [];

for(let i = 0; i < 1000; i ++){
    mockEncryptionEngine();
    const patient = {
        fullName: "test", 
        address: {
            street: faker.address.streetName(),
            city: faker.address.city(),
            zipCode: faker.address.zipCode(),
            state: faker.address.state(),
            country: faker.address.country()
        },
        telephone: faker.phone.phoneNumber(),
        ssn: mockEncryptionEngine(),
        lastFourDigitsOfSSN: faker.random.number(10000,99999),
        appointments: { },
        medicalRecords: [
            {
                condition: mockEncryptionEngine(),
                code: faker.random.number(1000,9999)
            },
            {
                condition: mockEncryptionEngine(),
                code: faker.random.number(1000,9999)
            }
        ],
        vitals: {
            pulse: Math.floor(Math.random() * (90 - 60)) + 60 
        },
        heartRate: Math.floor(Math.random() * (80 - 60)) + 60 ,
        bloodPressure: `${Math.floor(Math.random() * (130 - 110)) + 110 }/${Math.floor(Math.random() * (90 - 70)) + 70 }`,
        weight: 180
    }

    data.push(patient);
    fs.writeFileSync("./patientData.json", JSON.stringify(data, null, 4));
}




// Full Name - String
// Address - Object
// Street - String
// City - String
// Zip Code - Number
// State - String
// Country - String
// Telephone - String
// SSN - E Deterministic String
// Last 4 SSN - String
// Appointments - Object
// Medical Records - E (Array of documents) Random
// vitals - Object
// Heart Rate - Number
// Blood Pressure - String
// Weight - Number
// ---doctor notes - String