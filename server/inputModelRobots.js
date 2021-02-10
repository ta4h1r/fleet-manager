var mongoose = require('mongoose');

const collectionName = 'ctrl_robots';

// Setup schema
var inputSchema = mongoose.Schema({
  clientName: {
      type: String,
      required: true
  },
  clientId: {
      type: String,
      required: true
  },
  refPath: {
      type: String,
      required: true
	},
  robots: {
      type: Array,
      required: true
  }
});

// Export Input model
var Input = module.exports = mongoose.model(collectionName, inputSchema);

module.exports.get = function (callback, limit) {
    Input.find(callback).limit(limit);
}
