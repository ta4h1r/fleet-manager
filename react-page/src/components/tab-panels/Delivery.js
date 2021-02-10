import { Button, MenuItem, TextField, Typography } from '@material-ui/core'
import { makeStyles } from '@material-ui/core/styles';
import React from 'react'
import { useEffect, useState } from 'react';

import Switch from '../switches/Switch'
import NavStatusIndicator from '../indicators/NavStatusIndicator';


const useStyles = makeStyles((theme) => ({
    root: {
        display: 'flex',
        flexWrap: 'wrap',
        marginLeft: theme.spacing(17),
        '& .MuiTextField-root': {
            margin: theme.spacing(1),
            width: '25ch',
        },
    },
    button: {
        marginTop: theme.spacing(2.5),
        marginLeft: theme.spacing(2),
        width: '15ch',
        height: '4ch'
    },
    typography: {
        float: "left",
        marginTop: theme.spacing(2.5),
        marginLeft: "auto",
        width: '30ch',
        height: '4ch'
    }
}));



function Delivery({ activity, botProps }) {

    const robot = botProps.robot;
    const firebase = botProps.firebase;
    const refPath = botProps.refPath;
    const deviceId = robot.deviceId;

    // Maps & Tags names 
    const db = firebase.firestore();
    const refTags = db.collection(refPath).doc(deviceId).collection("navigation").doc("tags");
    const refMaps = db.collection(refPath).doc(deviceId).collection("navigation").doc("maps");

    const [tagsData, setTagsData] = useState([]);
    const [mapsData, setMapsData] = useState([]);

    const [startPoint, setStartPoint] = useState([]);
    const [endPoint, setEndPoint] = useState([]);
    const [currentMap, setCurrentMap] = useState([]);

    useEffect(() => {

        refMaps.onSnapshot(snapshot => {
            try {

                var keys = Object.keys(snapshot.data());
                var vals = Object.values(snapshot.data());

                setMapsData(snapshot.data());

                for (var i = 0; i < keys.length; i++) {
                    if (vals[i] == 1) {
                        setCurrentMap(keys[i]);
                        break;
                    }
                }
            } catch (err) {
                console.error("Maps unavailable exception: ", err);
            }
        })

        refTags.onSnapshot(snapshot => {
            try {
                var keys = Object.keys(snapshot.data());
                var vals = Object.values(snapshot.data());

                setTagsData(snapshot.data());

                for (var i = 0; i < keys.length; i++) {
                    if (vals[i] == 1) {
                        setStartPoint(keys[i]);
                        break;
                    }
                }
            } catch (err) {
                console.error("Tags unavailable exception: ", err);
            }
        })

    }, []);

    let maps = [];
    for (const item in mapsData) {
        maps.push({
            value: item,
            label: item
        })
    }
    const tags = []
    for (const item in tagsData) {
        tags.push({
            value: item,
            label: item
        })
    }

    // Map images
    const refMsg = db.collection(refPath).doc(deviceId).collection("messages").doc("SlamController");

    const [imgUrl, setImgUrl] = useState([]);
    // const [msgTime, setMsgTime] = useState([]);

    useEffect(() => {
        refMsg.onSnapshot(snapshot => {
            setImgUrl(snapshot.get('url') + '?' + Math.random());   // use Math.random() to change the url on each render so that the updated image loads
            // setMsgTime(snapshot.get('time'));
        });
    }, []);


    // UI handles
    const classes = useStyles();

    const [targetStartPoint, setTargetStartPoint] = useState([]);
    const [targetEndPoint, setTargetEndPoint] = useState([]);
    const [targetMap, setTargetMap] = useState([]);

    const handleChangeMap = (event) => {
        setTargetMap(event.target.value);
    };
    const handleChangeStart = (event) => {
        setTargetStartPoint(event.target.value);
    }
    const handleChangeEnd = (event) => {
        setTargetEndPoint(event.target.value);
    }

    // Checkbox 
    const [state, setState] = useState({
        checkedA: true,
        checkedB: true,
        checkedF: true,
        checkedG: true,
    });
    const handleChange = (event) => {
        setState({ ...state, [event.target.name]: event.target.checked });
    };


    // Navigation
    async function reqNavPath() {
        console.log(`Planning route to ${targetEndPoint}`);

        const postData = {
            command: "route",
            data: {
                endPoint: targetEndPoint,
                returnToStart: state.checkedB,
                deviceId: deviceId,
                refPath: refPath,
            }
        }

        fetch("https://3i0al4myn6.execute-api.us-east-1.amazonaws.com/prod", {
            method: "post",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(postData),
        })
            .then((response) => response.json())
            .then((returnData) => {

                console.log(returnData);

            });

    }
    async function reqCalibrate() {
        try {
            console.log(`Calibrating to position ${targetStartPoint} in map ${currentMap}`);
            const tagsRef = db.collection(refPath).doc(deviceId).collection("navigation").doc("tags");
            const REQ_CALIBRATE_TAG = 3;
            tagsRef.update(targetStartPoint, REQ_CALIBRATE_TAG);
        } catch (err) {
            console.error("Invalid target start point exception: ", err);
        }
    }
    async function reqSetMap() {
        try {
            console.log(`Setting map ${targetMap}`);
            const mapsRef = db.collection(refPath).doc(deviceId).collection("navigation").doc("maps");
            const REQ_APPLY_MAP = 2;
            mapsRef.update(targetMap, REQ_APPLY_MAP);
        } catch (err) {
            console.error("Invalid target map exception: ", err);
        }
    }

    return (
        <>

            <Switch ability={activity} botProps={botProps} />

            <div className={classes.root}>

                <div>

                    <Typography className={classes.typography} id="discrete-slider-custom" gutterBottom>
                        {`Current map: ${currentMap}`}
                    </Typography>

                    <TextField
                        select
                        label="Load map"
                        value={(targetMap.length > 0) ? targetMap : ''}
                        onChange={handleChangeMap}
                        variant="outlined"
                    >
                        {maps.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>

                    <Button
                        variant="contained"
                        color="primary"
                        className={classes.button}
                        onClick={reqSetMap}
                    >
                        Set
                    </Button>

                </div>


                <div>

                    <Typography className={classes.typography} id="discrete-slider-custom" gutterBottom>
                        {`Start point: ${startPoint}`}
                    </Typography>

                    <TextField
                        select
                        label="Start point"
                        value={(targetStartPoint.length > 0) ? targetStartPoint : ''}
                        onChange={handleChangeStart}
                        variant="outlined"
                    >
                        {tags.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>

                    <Button
                        variant="contained"
                        color="primary"
                        className={classes.button}
                        onClick={reqCalibrate}
                    >
                        Calibrate
                    </Button>

                </div>


                <div>

                    <Typography className={classes.typography} id="discrete-slider-custom" gutterBottom>
                        {`End point: ${endPoint}`}
                    </Typography>

                    <TextField
                        style={{ marginBottom: 10 }}
                        // select ={''}
                        label="End point"
                        value={targetEndPoint}
                        onChange={handleChangeEnd}
                        variant="outlined"
                    >
                        {tags.map((option) => (
                            <MenuItem key={option.value} value={option.value}>
                                {option.label}
                            </MenuItem>
                        ))}
                    </TextField>

                    <Button
                        variant="contained"
                        color="primary"
                        className={classes.button}
                        onClick={reqNavPath}
                    >
                        Navigate
                    </Button>

                </div>

                <br></br>

                <div className={classes.root}>
                    <NavStatusIndicator refPath={sessionStorage.getItem("REF_PATH")} deviceId={deviceId} firebase={firebase} />
                </div>


                {/* <FormControlLabel
                    control={
                        <Checkbox
                        checked={state.checkedB}
                        onChange={handleChange}
                        name="checkedB"
                        color="primary"
                        />
                    }
                    label="Return to start"
                    /> */}

                <div style={{
                    marginTop: "8dp",
                    marginLeft: "auto",
                    marginRight: "auto",
                    width: "640px",
                    height: "480px"
                }}>
                    <img key={Date.now()} src={imgUrl} style={{ maxWidth: "100%", maxHeight: "100%" }} />
                </div>

            </div>

        </>

    )
}

export default Delivery
