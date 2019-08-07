const fs = require('fs');

const data = [];

for(let i = 0; i < 1000; i ++){
    const patient = {
        fullName: 'tim', 
        address: {
            street: 'oswego road',
            city: 'new york city',
            zipCode: 10302,
            state: 'new york',
            country: 'usa'
        },
        telephone: "555-5555",
        ssn: "AS#@R%$!@",
        lastFourDigitsOfSSN: 4444,
        appointments: { },
        medicalRecords: [
            {
                condition: "#EWFf!@#$",
                code: 123
            },
            {
                condition: "214r5wt2!",
                code: 456
            }
        ],
        vitals: {
            pulse: "70/min"
        },
        heartRate: 72,
        bloodPressure: "120/80",
        Weight: 180
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