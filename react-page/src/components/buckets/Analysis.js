import React from 'react'
import ContainedButtons from '../buttons/ContainedButtons';
import CircularIndeterminate from '../progress-icon/CircularProgress';

import { useState, useEffect } from "react";
import { useHistory } from "react-router-dom";

function Analysis() {

    const history = useHistory();

    const [chartData, setChartData] = useState([]);
    const [aggregateData, setAggregateData] = useState([]);
    const [prevAggregateData, setPrevAggregateData] = useState([]);
    const [isLoading, setLoading] = useState(true);

    // Get scores from MetricsDB 
    async function dynamoQuery(sector) {
        const AWS = require('aws-sdk');
        AWS.config = new AWS.Config();

        const awsConfig = sessionStorage.getItem("aws_config");
        const aArray = JSON.parse(awsConfig);
        AWS.config.update(aArray[0]);
        AWS.config.update({
            region: 'us-east-2'
        });
        var dynamoDB = new AWS.DynamoDB.DocumentClient({ apiVersion: '2012-08-10' });

        //query DynamoDB
        var params = {
            ExpressionAttributeValues: {
                ':s': sector
            },
            KeyConditionExpression: 'sector = :s',
            TableName: 'metricsDB'
        };

        return dynamoDB.query(params).promise();
    };
    async function getScores() {

        console.log("Getting scores...");
        const sectors = ['Rooms', 'Service', 'Cleanliness', 'Facilities', 'Food+Beverage', "Connectivity"];
        var data;
        let aggregateScores = [];
        let prevAggregateScores = [];
        let runningScores = [];
        let i = 0;
        const fn = new Promise(resolve => {
            sectors.forEach(async (sector, index, array) => {

                data = await dynamoQuery(sector);
                // console.log(data.Items[0]);
                try {
                    const aggregateObj = {
                        sector: sector,
                        positiveAggregate: data.Items[0].currentSentiment.score.Positive,
                        negativeAggregate: data.Items[0].currentSentiment.score.Negative,
                        mixedAggregate: data.Items[0].currentSentiment.score.Mixed,
                        neutralAggregate: data.Items[0].currentSentiment.score.Neutral,
                    };
                    const prevAggregateObj = {
                        sector: sector,
                        positiveAggregate: data.Items[0].prevSentiment.score.Positive,
                        negativeAggregate: data.Items[0].prevSentiment.score.Negative,
                        mixedAggregate: data.Items[0].prevSentiment.score.Mixed,
                        neutralAggregate: data.Items[0].prevSentiment.score.Neutral,
                    };
                    const runningObj = {
                        sector: sector,
                        positiveRunning: data.Items[0].charts.sentiment.Positive,
                        negativeRunning: data.Items[0].charts.sentiment.Negative,
                        neutralRunning: data.Items[0].charts.sentiment.Neutral,
                        timeStamp: data.Items[0].charts.sentiment.unixTimeStamp
                    };

                    aggregateScores.push(aggregateObj);
                    prevAggregateScores.push(prevAggregateObj);
                    runningScores.push(runningObj);

                } catch (err) {
                    console.log(err);

                    // const aggregateObj = {
                    //   sector: sector,
                    //   positiveAggregate: null,
                    //   negativeAggregate: null,
                    //   mixedAggregate: null,
                    //   neutralAggregate: null,
                    // };
                    // const runningObj = {
                    //   sector: sector, 
                    //   positiveRunning: null,
                    //   negativeRunning: null,
                    //   neutralRunning: null,
                    //   timeStamp: null
                    // };

                }

                if (i === array.length - 1) resolve();            // Only if we've looped through the entire array, then we can resolve the promise (avoid using index as it is not incremented in any particular order)
                i++;

            });
        });

        await fn;         // Since there are async operations inside the forEach loop, we need to wait for the arrays to populate

        setAggregateData(aggregateScores);
        setPrevAggregateData(prevAggregateScores);
        return populateChartData(runningScores);
    }
    function populateChartData(runningScores) {
        console.log("Populating chart data...");
        runningScores.forEach(score => {
            score.timeStamp.forEach((unixTime, index) => {

                var days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
                var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
                
                var dayNum = new Date(unixTime * 1000).getDay();
                var monthNum = new Date(unixTime * 1000).getMonth();
                var dateNum = new Date(unixTime * 1000).getDate();
                
                var day = days[dayNum];
                var month = months[monthNum];
                var date = dateNum;

                var dateString = `${date} ${month}`;

                var obj = {
                    unixTime: unixTime,
                    time: dateString,
                };
                var service = {};
                var rooms = {};
                var cleanliness = {};
                var facilities = {};
                var food_bev = {};
                var connectivity = {};

                const currTime = Math.floor(new Date().getTime() / 1000);
                const diff = 365 * 24 * 3600;                            // x days * 24 h * 3600 s

                switch (score.sector) {   // Indicate the score that has been updated
                    case "Service":
                        service.Positive = score.positiveRunning[index]
                        service.Negative = score.negativeRunning[index]
                        service.Neutral = score.neutralRunning[index]
                        break;

                    case "Rooms":
                        rooms.Positive = score.positiveRunning[index]
                        rooms.Negative = score.negativeRunning[index]
                        rooms.Neutral = score.neutralRunning[index]
                        break;

                    case "Cleanliness":
                        cleanliness.Positive = score.positiveRunning[index]
                        cleanliness.Negative = score.negativeRunning[index]
                        cleanliness.Neutral = score.neutralRunning[index]
                        break;

                    case "Facilities":
                        facilities.Positive = score.positiveRunning[index]
                        facilities.Negative = score.negativeRunning[index]
                        facilities.Neutral = score.neutralRunning[index]
                        break;

                    case "Food+Beverage":
                        food_bev.Positive = score.positiveRunning[index]
                        food_bev.Negative = score.negativeRunning[index]
                        food_bev.Neutral = score.neutralRunning[index]
                        break;

                    case "Connectivity":
                        connectivity.Positive = score.positiveRunning[index]
                        connectivity.Negative = score.negativeRunning[index]
                        connectivity.Neutral = score.neutralRunning[index]
                        break;

                    default:
                        break;
                }
                obj.service = service;
                obj.rooms = rooms;
                obj.cleanliness = cleanliness;
                obj.facilities = facilities;
                obj.food_bev = food_bev;
                obj.connectivity = connectivity;

                // Trim data points older than diff time
                if ((currTime - obj.unixTime) < (diff)) {
                    chartData.push(obj);
                }

            });

        });

        // Sort ascending order
        chartData.sort((a, b) => (a.unixTime > b.unixTime) ? 1 : ((b.unixTime > a.unixTime) ? -1 : 0));

        return chartData;
    }

    useEffect(() => {
        getScores().then(data => {
            setChartData(data);
            setLoading(false);
        });
        fetch("https://hbgvg306lj.execute-api.us-east-1.amazonaws.com/prod", {  // Update the wordclouds
            method: "get",
            mode: "no-cors",
            headers: { "Content-Type": "application/json" },
        })
    }, []);

    // Render Dash on access granted, else push to login page
    if (sessionStorage.getItem("skyKey") == "granted" && !isLoading) {
        return (
            <>

                <ContainedButtons chartData={chartData} aggregateData={aggregateData} prevAggregateData={prevAggregateData} />

            </>
        );
    } else if (!sessionStorage.getItem("skyKey")) {
        return <>{history.push("/login")}; </>;
    } else {
        return (
            <>

                <CircularIndeterminate />

            </>
        )
    }
}

export default Analysis
