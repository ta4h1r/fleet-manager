import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Button from '@material-ui/core/Button';
import { useState, useEffect } from 'react'
import {
  LineChart,
  Line,
  XAxis, YAxis, CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import Typography from '@material-ui/core/Typography';

const useStyles = makeStyles((theme) => ({
  root: {
    width: '100%',
    float: 'centre',
    marginLeft: 'auto', marginRight: 'auto',
    '& > *': {
      marginTop: theme.spacing(1), marginBottom: theme.spacing(1),
      marginLeft: theme.spacing(1), marginRight: theme.spacing(1),
    },
  },
  buttons: {
    display: 'flex',
    flexWrap: 'nowrap',
    flex: 1,
    '& > *': {
      float: 'centre',
      flex: '1',
      margin: theme.spacing(1),
      width: theme.spacing(12),
      height: theme.spacing(11),
    },
  },
  chart: {
    display: 'flex',
    flexWrap: 'nowrap',
    flex: 1,
  },
  image: {
    display: 'flex',
    flexWrap: 'nowrap',
    flex: 1,
    '& img': {
      display: 'flex',
      flexWrap: 'nowrap',
      flex: 1,
      width: 'auto',
      height: 'auto',
      maxWidth: theme.spacing(60),
      margin: 'auto',
      alt: '',
    },
  },
}));

export default function ContainedButtons({ chartData, aggregateData, prevAggregateData }) {
  const classes = useStyles();

  const [sectorData, setSectorData] = useState([]);
  const [buttonState, setButtonState] = useState({
    rooms: "primary",
    service: "primary",
    cleanliness: "primary",
    facilities: "primary",
    food_bev: "primary",
    connectivity: "primary",
  });
  const [imgSource, setImgSource] = useState(null);

  let newScores = {
    rooms: null,
    service: null,
    cleanliness: null,
    facilities: null,
    food_bev: null,
    connectivity: null,
  }
  let newChanges = {
    rooms: null,
    service: null,
    cleanliness: null,
    facilities: null,
    food_bev: null,
    connectivity: null,
  }
  let newChangeSigns = {
    rooms: null,
    service: null,
    cleanliness: null,
    facilities: null,
    food_bev: null,
    connectivity: null,
  }
  aggregateData.forEach((item, index) => {
    switch (item.sector) {
      case "Cleanliness":
        newScores.cleanliness = (item.positiveAggregate * 100).toFixed(0);
        newChanges.cleanliness = ((item.positiveAggregate - prevAggregateData[index].positiveAggregate) / prevAggregateData[index].positiveAggregate * 100).toFixed(0);
        newChangeSigns.cleanliness = newChanges.cleanliness > 0 ? "+" : "-";
        newChanges.cleanliness = Math.abs(newChanges.cleanliness);
        break;
      case "Service":
        newScores.service = (item.positiveAggregate * 100).toFixed(0);
        newChanges.service = ((item.positiveAggregate - prevAggregateData[index].positiveAggregate) / prevAggregateData[index].positiveAggregate * 100).toFixed(0);
        newChangeSigns.service = newChanges.service > 0 ? "+" : "-";
        newChanges.service = Math.abs(newChanges.service);
        break;
      case "Food+Beverage":
        newScores.food_bev = (item.positiveAggregate * 100).toFixed(0);
        newChanges.food_bev = ((item.positiveAggregate - prevAggregateData[index].positiveAggregate) / prevAggregateData[index].positiveAggregate * 100).toFixed(0);
        newChangeSigns.food_bev = newChanges.food_bev > 0 ? "+" : "-";
        newChanges.food_bev = Math.abs(newChanges.food_bev);
        break;
      case "Facilities":
        newScores.facilities = (item.positiveAggregate * 100).toFixed(0);
        newChanges.facilities = ((item.positiveAggregate - prevAggregateData[index].positiveAggregate) / prevAggregateData[index].positiveAggregate * 100).toFixed(0);
        newChangeSigns.facilities = newChanges.facilities > 0 ? "+" : "-";
        newChanges.facilities = Math.abs(newChanges.facilities);
        break;
      case "Rooms":
        newScores.rooms = (item.positiveAggregate * 100).toFixed(0);
        newChanges.rooms = ((item.positiveAggregate - prevAggregateData[index].positiveAggregate) / prevAggregateData[index].positiveAggregate * 100).toFixed(0);
        newChangeSigns.rooms = newChanges.rooms > 0 ? "+" : "-";
        newChanges.rooms = Math.abs(newChanges.rooms);
        break;
      case "Connectivity":
        newScores.connectivity = (item.positiveAggregate * 100).toFixed(0);
        newChanges.connectivity = ((item.positiveAggregate - prevAggregateData[index].positiveAggregate) / prevAggregateData[index].positiveAggregate * 100).toFixed(0);
        newChangeSigns.connectivity = newChanges.connectivity > 0 ? "+" : "-";
        newChanges.connectivity = Math.abs(newChanges.connectivity);
        break;
      default:
        console.log("Invalid switch case");
        break;
    }
  });

  const [aggregateScore, setAggregateScore] = useState(newScores);
  const [aggregateChange, setAggregateChange] = useState(newChanges);
  const [aggregateChangeSign, setAggregateChangeSign] = useState(newChangeSigns);

  const handleClick = async (sect) => {
    let newButtonState = {
      rooms: "primary",
      service: "primary",
      cleanliness: "primary",
      facilities: "primary",
      food_bev: "primary",
      connectivity: "primary",
    }
    let newSectorData = [];
    switch (sect) {
      case "rooms":
        newButtonState.rooms = "secondary";
        var sectorScores = {};
        chartData.forEach((item, index) => {
          sectorScores = item.rooms;
          if (Object.keys(sectorScores).length != 0) {
            var obj = {
              time: item.time,
              Positive: sectorScores.Positive,
              Negative: sectorScores.Negative,
              Neutral: sectorScores.Neutral,
            };
            newSectorData.push(obj);
          }

        });
        break;
      case "service":
        newButtonState.service = "secondary";
        var sectorScores = {};
        chartData.forEach((item, index) => {
          sectorScores = item.service;
          if (Object.keys(sectorScores).length != 0) {
            var obj = {
              time: item.time,
              Positive: sectorScores.Positive,
              Negative: sectorScores.Negative,
              Neutral: sectorScores.Neutral,
            };
            newSectorData.push(obj);
          }

        });
        break;
      case "cleanliness":
        newButtonState.cleanliness = "secondary";
        var sectorScores = {};
        chartData.forEach((item, index) => {
          sectorScores = item.cleanliness;
          if (Object.keys(sectorScores).length != 0) {
            var obj = {
              time: item.time,
              Positive: sectorScores.Positive,
              Negative: sectorScores.Negative,
              Neutral: sectorScores.Neutral,
            };
            newSectorData.push(obj);
          }

        });
        break;
      case "facilities":
        newButtonState.facilities = "secondary";
        var sectorScores = {};
        chartData.forEach((item, index) => {
          sectorScores = item.facilities;
          if (Object.keys(sectorScores).length != 0) {
            var obj = {
              time: item.time,
              Positive: sectorScores.Positive,
              Negative: sectorScores.Negative,
              Neutral: sectorScores.Neutral,
            };
            newSectorData.push(obj);
          }

        });
        break;
      case "food_bev":
        newButtonState.food_bev = "secondary";
        var sectorScores = {};
        chartData.forEach((item, index) => {
          sectorScores = item.food_bev;
          if (Object.keys(sectorScores).length != 0) {
            var obj = {
              time: item.time,
              Positive: sectorScores.Positive,
              Negative: sectorScores.Negative,
              Neutral: sectorScores.Neutral,
            };
            newSectorData.push(obj);
          }

        });
        break;
      case "connectivity":
        newButtonState.connectivity = "secondary";
        var sectorScores = {};
        chartData.forEach((item, index) => {
          sectorScores = item.connectivity;
          if (Object.keys(sectorScores).length != 0) {
            var obj = {
              time: item.time,
              Positive: sectorScores.Positive,
              Negative: sectorScores.Negative,
              Neutral: sectorScores.Neutral,
            };
            newSectorData.push(obj);
          }

        });
        break;
      default:
        console.log("Moron alert.");
        break;
    }

    setSectorData(newSectorData);
    setButtonState(newButtonState);

    S3Query(sect).then(data => {
      setImgSource(data);
    });

  }

  async function S3Query(sector) {
    try {
      // Load the AWS SDK for Node.js
      const AWS = require('aws-sdk');
      AWS.config = new AWS.Config();

      const awsConfig = sessionStorage.getItem("aws_config");
      const aArray = JSON.parse(awsConfig);
      AWS.config.update(aArray[0]);
      AWS.config.update({
        region: 'us-east-2'
      });

      var s3 = new AWS.S3({ apiVersion: '2006-03-01' });
      var bucketParams = {
        Bucket: 'wordclouds3analytics',
        Key: "wordCloud/" + sector + ".png",
      };

      // Get Object from S3
      return new Promise(resolve => {
        s3.getSignedUrlPromise('getObject', bucketParams)
          .then(url => {
            resolve(url);
          });
      });
    } catch (err) {
      console.error("AWS exception: ", err);
      return null;
    }
  }


  return (

    <div className={classes.root}>



      <div className={classes.buttons}>
        <Button variant="contained" color={buttonState.rooms} onClick={() => handleClick("rooms")}>

          <div>
            <Typography gutterBottom variant="body1" component="h3">
              Rooms
</Typography>
            <div>
              <Typography variant="h5" color="inherit" component="h1">
                {aggregateScore.rooms}
              </Typography>
              <div>
                <Typography variant="body1" color="inherit" component="h2">
                  {aggregateChangeSign.rooms + " " + aggregateChange.rooms}%
    </Typography>
              </div>
            </div>
          </div>

        </Button>
        <Button variant="contained" color={buttonState.service} onClick={() => handleClick("service")}>

          <div>
            <Typography gutterBottom variant="body1" component="h3">
              Service
</Typography>
            <div>
              <Typography variant="h5" color="inherit" component="h1">
                {aggregateScore.service}
              </Typography>
              <div>
                <Typography variant="body1" color="inherit" component="h2">
                  {aggregateChangeSign.service + " " + aggregateChange.service}%
    </Typography>
              </div>
            </div>
          </div>

        </Button>
        <Button variant="contained" color={buttonState.cleanliness} onClick={() => handleClick("cleanliness")}>

          <div>
            <Typography gutterBottom variant="body1" component="h3">
              Cleanliness
</Typography>
            <div>
              <Typography variant="h5" color="inherit" component="h1">
                {aggregateScore.cleanliness}
              </Typography>
              <div>
                <Typography variant="body1" color="inherit" component="h2">
                  {aggregateChangeSign.cleanliness + " " + aggregateChange.cleanliness}%
    </Typography>
              </div>
            </div>
          </div>

        </Button>
        <Button variant="contained" color={buttonState.facilities} onClick={() => handleClick("facilities")}>

          <div>
            <Typography gutterBottom variant="body1" component="h3">
              Facilities
</Typography>
            <div>
              <Typography variant="h5" color="inherit" component="h1">
                {aggregateScore.facilities}
              </Typography>
              <div>
                <Typography variant="body1" color="inherit" component="h2">
                  {aggregateChange.facilities}%
    </Typography>
              </div>
            </div>
          </div>

        </Button>
        <Button variant="contained" color={buttonState.food_bev} onClick={() => handleClick("food_bev")}>

          <div>
            <Typography gutterBottom variant="body1" component="h3">
              Food/Bev
</Typography>
            <div>
              <Typography variant="h5" color="inherit" component="h1">
                {aggregateScore.food_bev}
              </Typography>
              <div>
                <Typography variant="body1" color="inherit" component="h2">
                  {aggregateChangeSign.food_bev + " " + aggregateChange.food_bev}%
    </Typography>
              </div>
            </div>
          </div>

        </Button>
        <Button variant="contained" color={buttonState.connectivity} onClick={() => handleClick("connectivity")}>

          <div>
            <Typography gutterBottom variant="body1" component="h3">
              Connectivity
</Typography>
            <div>
              <Typography variant="h5" color="inherit" component="h1">
                {aggregateScore.connectivity}
              </Typography>
              <div>
                <Typography variant="body1" color="inherit" component="h2">
                  {aggregateChangeSign.cleanliness + " " + aggregateChange.connectivity}%
    </Typography>
              </div>
            </div>
          </div>

        </Button>
      </div>

      <div className={classes.chart}>

        <ResponsiveContainer width="90%"
            height={400}>

          <LineChart
            style={{
              marginLeft: "auto",
              marginRight: "auto",
              borderRadius: ".4em"
            }}
            
            data={sectorData}
            margin={{
              top: 5, right: 30, left: 20, bottom: 5,
            }}
          >
            <CartesianGrid strokeDasharray="6 3" stroke="false" />

            <XAxis name='date' allowDuplicatedCategory={true} dataKey="time" />
            <YAxis />

            {/* <YAxis />*/}
            <Tooltip />
            <Legend />
            <Line type="monotone" dataKey="Positive" stroke="#1BC5BD" strokeWidth="4" activeDot={{ r: 8 }} />
            <Line type="monotone" dataKey="Negative" stroke="#F64E60" strokeWidth="4" />
            <Line type="monotone" dataKey="Neutral" stroke="#8950FC" strokeWidth="4" />
          </LineChart>

        </ResponsiveContainer>

      </div>

      <div className={classes.image} >

        <img src={imgSource} />

      </div>

    </div>



  );
}
