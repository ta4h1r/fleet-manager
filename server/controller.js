Table = require('./inputModelRobots');
Table_c = require('./inputModelClients');

const AWS = require('aws-sdk');

// Firebase App (the core Firebase SDK) is always required and
// must be listed before other Firebase SDKs
var firebase = require("firebase/app");

// Add the Firebase products that you want to use
require("firebase/firestore");

// Your web app's Firebase configuration
var firebaseConfig = {
    apiKey: "***",
    authDomain: "***",
    databaseURL: "***",
    projectId: "***",
    storageBucket: "***",
    messagingSenderId: "***",
    appId: "***",
    measurementId: "***"
  };
// Initialize Firebase
firebase.initializeApp(firebaseConfig);

const awsConfig = {
	accessKeyId: "***",
    secretAccessKey: "***"
};
const iceServers = [
	{
		urls: [
			'stun:stun.l.google.com:19302',
			'stun:stun.services.mozilla.com',
			'stun:numb.viagenie.ca',
		]
	},
	{
		urls: 'turn:numb.viagenie.ca',
		username: '***',
		credential: '***'
	},
];

const suspendedRobots = [];

// VIEW ALL
exports.index = (req, res) => {
	console.log('Getting all table data');
	Table_c.get((err, documents) => {
		if (err) {
			res.json({
				status: "error",
				message: err
			});
		}
		res.json({
			status: 200,
			message: "Documents retrieved successfully",
			// data: documents
		});
			console.log("Done");
	});
};


exports.handler = async function (req, res) {

	console.log(req.body);  // {  command: 'getRobotInfo',  deviceId: '35fb3e8e5a35586acfd1e06d2624113f'}


	switch (req.body.command) {

		case 'getRobotInfo':          // Used by android

			// var allClients = await Table.find();
			// console.log(allClients);

  		var deviceId = req.body.deviceId;
			var filter = {
				deviceId: deviceId,
			};
			var deviceIdExists  = await Table.exists({"robots.deviceId":  deviceId});

			if(deviceIdExists) {
				filter = {
					"robots.deviceId": deviceId,
				};
				// get the document
				var doc = await Table.find(filter);
				var robots = doc[0].robots;
				var currentRobot = null;

				robots.forEach((robot, index) => {
					if(robot.deviceId == deviceId) {
						currentRobot = robot;
					}
				});
				console.log(currentRobot);

        if(suspendedRobots.includes(deviceId)) {
          currentRobot.robotAlias = 'All services for this robot have been suspended by admin.'
        }

        res.json({
            status: 200,
            message: "Found robot: " + deviceId,
            data: {
              robotDetails: currentRobot,
              refPath: doc[0].refPath,
            },
        });



			} else {
				// TODO: Add the device ID to a new document with other fields empty

				// // Adding data to appropriate collection
				// var input = new Table();
				// input.id = req.body.workingId;
				// input.Name = req.body.Name;
				// input.FaceID = [];
				//
				// // Save the doc and check for errors
				// input.save((err) => {
				// 	if (err) {
				// 		res.json(err);
				// 	}
				// 	res.json({
				// 		message: 'Added successfully',
				// 		data: input
				// 	});
				// });
				// console.log('Done');

				res.json({
						status: 201,
						message: "Device not found.",
						data: [],
				});
				console.log("deviceId not found");
			}
			break;

		case 'getDeviceId':        // gives refPath also

			var robotAlias = req.body.data.robotAlias;
			var refPath = req.body.data.refPath;
			var robotAliasExists  = await Table.exists({
				"robots.robotAlias":  robotAlias,
				"refPath": refPath,
			});

			console.log("robotAliasExists: ", robotAliasExists);

			if(robotAliasExists) {

				filter = {
					"robots.robotAlias": robotAlias,
					"refPath": refPath,
				};
				// get the document
				var doc = await Table.find(filter);
				// console.log(doc[0]);
				var robotIndex;
				doc[0].robots.forEach((robot, index) => {
					if (robot.robotAlias == robotAlias) {
						robotIndex = index;
					}
				});
				res.json({
						status: 200,
						message: "Found deviceId for " + robotAlias,
						data: {
							deviceId: doc[0].robots[robotIndex].deviceId
						},
				});

			} else {
				res.json({
						status: 201,
						message: "Alias not found.",
						data: [],
				});
				console.log("robotAlias not found");
			}

			break;

		case 'getClientInfo':

			filter = {
				"clientAlias": req.body.data.clientAlias,
				"password": req.body.data.password,
			};

			var doc = await Table_c.find(filter);

			if (doc.length != 0) {
				console.log("Found clientId: " + doc[0].clientId);
				res.json({
						status: 200,
						message: "Found clientId",
						data: {
							clientId: doc[0].clientId,
							config: [firebaseConfig],
							awsConfig: [awsConfig],
							iceServers: [iceServers],
						},
				});
			} else {
				res.json({
						status: 201,
						message: "clientAlias/password not found.",
						data: [],
				});
				console.log("clientAlias/password not found");

			}
			break;

		case 'getFleet':
			filter = {
				"clientId": req.body.data.clientId
			};
		  var doc_c = await Table_c.find(filter);
			var doc = await Table.find(filter);
			var refPath;

			if (doc_c.length != 0) {
				var clientId = doc_c[0].clientId;
				var clientAlias = doc_c[0].clientAlias;

				refPath = clientAlias + "_" + clientId;

			} else {
				res.json({
						status: 201,
						message: "clientId not found.",
						data: [],
				});
				console.log("clientId not found");
			}

			console.log("refPath: " + refPath);

			if (doc.length != 0) {
				console.log("Found fleet: " + doc[0].robots);

				var updateRes = await Table.updateOne({ clientId: doc[0].clientId }, { refPath: refPath });
				console.log("matched: " + updateRes.n);
				console.log("updated: " + updateRes.nModified);
				if (updateRes.nModified != 0) {
					doc = await Table.find(filter);
				}

				res.json({
						status: 200,
						message: "Found fleet",
						data: {
							fleetDetails: doc[0].robots,
							refPath: doc[0].refPath
						},
				});
			} else {
				res.json({
						status: 201,
						message: "Fleet not found.",
						data: [],
				});
				console.log("Fleet not found");

			}

			break;

		case 'getAbilities':

			// var allClients = await Table.find();
			// console.log(allClients);

			var deviceId = req.body.data.deviceId;
			var filter = {
				deviceId: deviceId,
			};
			var deviceIdExists  = await Table.exists({"robots.deviceId":  deviceId});

			if(deviceIdExists) {
				filter = {
					"robots.deviceId": deviceId,
				};
				// get the document
				var doc = await Table.find(filter);
				var robots = doc[0].robots;
				var currentRobot = null;

				robots.forEach((robot, index) => {
					if(robot.deviceId == deviceId) {
						currentRobot = robot;
					}
				});
				console.log(currentRobot);

				res.json({
						status: 200,
						message: "Found robot: " + deviceId,
						data: {
							abilities: currentRobot.abilities,
						},
				});

			} else {
				res.json({
						status: 201,
						message: "Device not found.",
						data: [],
				});
				console.log("deviceId not found");
			}
			break;

		case 'updateMap':

			var refPath = req.body.data.refPath;
			var deviceId = req.body.data.deviceId;
			const folder = refPath + "/" + deviceId;
			var data = await bucketQuery(folder);
      console.log(data);
			if (data != null) {
				var url = data.url;
				var key = data.key;
				updateFirebase(url, refPath, deviceId);
				res.json({
						status: 200,
						message: `Got photo url for ${refPath}/${deviceId}`,
						data: {
							url: url,
						},
				});
			} else {
				res.json({
						status: 201,
						message: "photoUrl not found",
				});
			}

			break;

		default:
			console.log("Invalid switch case");
			break;
	}


}

// S3 bucket Query to get map image
async function bucketQuery(folder){
	var albumBucketName = 'slam-buffer';
	var foldername = decodeURIComponent(folder) +'/';

	console.log("FolderName: " + foldername);

	AWS.config.region = 'us-east-1'; // Region
	AWS.config.credentials = new AWS.CognitoIdentityCredentials({
		IdentityPoolId: 'us-east-1:53230161-20c1-4976-9122-4509250e7c12',
	});

	var s3 = new AWS.S3();

	var params = {
		Bucket: albumBucketName,
		Delimiter: '',
		Prefix: foldername
	}

	var mData = null;
	var url = null;
	try {
		await s3.listObjects(params, function (err, data) {
			if(err) {
				// console.log("ERR: ", err);
			} else if (data.Contents.length == 0) {
				console.log("bucketQuery: Directory does not exist");
			} else {
				console.log("data: ", data);

				var href = this.request.httpRequest.endpoint.href;
				var bucketUrl = href + albumBucketName + '/';
				var photoKey = data.Contents[0].Key;
				var photoUrl = bucketUrl + encodeURIComponent(photoKey);

        // var photoUrlSigned = s3.getSignedUrl('getObject', {Key: photoKey, Bucket: albumBucketName});   // Signed s3 url

        mData = {
					url: photoUrl,
					key: photoKey
				};

			}
		}).promise();
		return mData;
	} catch (err) {
		console.log("bucketQuery: error: ", err);
		return null;
	}

}
async function updateFirebase(photoUrl, refPath, deviceId) {
	const db = firebase.firestore();
	console.log(refPath);
	console.log(deviceId);
	const messageRef = db.collection(refPath).doc(deviceId).collection("messages").doc("SlamController");
	const data = {
		url: decodeURIComponent(photoUrl),
		time: Math.floor(new Date() / 1000)   // Unix timestamp
	}
	await messageRef.set(data);
	console.log("Set data");
}
