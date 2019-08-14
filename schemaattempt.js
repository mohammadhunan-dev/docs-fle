var Validator = require('jsonschema').Validator;
var v = new Validator();


console.log(v.validate(4, { type: 'Array'}));




