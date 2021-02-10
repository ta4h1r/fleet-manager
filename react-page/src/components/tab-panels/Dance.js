import React from 'react'
import {useEffect, useState} from 'react'

import { Button, TextField } from '@material-ui/core'
import { makeStyles } from '@material-ui/core/styles';
import { Slider, Typography } from '@material-ui/core';

import Switch from '../switches/Switch'

const useStyles = makeStyles((theme) => ({
    root: {
      display: 'flex',
      flexWrap: 'wrap',
      marginLeft: theme.spacing(17),
      '& > *': {
        margin: theme.spacing(1),
        "margin-top": theme.spacing(1),
      },
    },
    form: {
        display: 'flex',
        flexWrap: 'wrap',
        marginLeft: theme.spacing(17),
        '& > *': {
          margin: theme.spacing(1),
          "margin-top": theme.spacing(1),
          
        },
      },
      slider: {
        display: 'flex',
        flexWrap: 'wrap',
        marginLeft: theme.spacing(17),
        marginRight: theme.spacing(17),
        '& > *': {
          margin: theme.spacing(1),
          "margin-top": theme.spacing(1),
        },
      },
  }));

function Dance({ activity, botProps }) {

    const robot = botProps.robot;
    const firebase = botProps.firebase;
    const refPath = botProps.refPath;
    const deviceId = robot.deviceId;

    const classes = useStyles();

    const db = firebase.firestore();
    const refStartSync = db.collection("Sync").doc("PresentationActivity");

    const [buttonDisabled, setButtonDisabled] = useState(false);

    function startSequenceSync(seqNo) {
        switch(seqNo) {
            case 1:
                refStartSync.update("switch", 1);
                break;
            case 2:
                refStartSync.update("switch", 2);
                break;
            case 3:
                refStartSync.update("switch", 3);
                break;
            case "stop":
                refStartSync.update("switch", "stop");
                break;
        }
    }

    useEffect(() => {
        refStartSync.onSnapshot(async (snapshot) => {
            var swtch = snapshot.get("switch")
            try {
                if (swtch == 0) {
                    // startSyncBtn.disabled = false;
                    setButtonDisabled(false);
    
                } else {
                    setButtonDisabled(true);
                }
            } catch (e) {
                console.error(e)
            }
        }, err => {
          console.log(`Encountered error: ${err}`);
        });

    }, [])

    const refHands = db.collection(refPath).doc(deviceId).collection("presentation").doc("hands");
    const refHead = db.collection(refPath).doc(deviceId).collection("presentation").doc("head");

    const [headBtnDisabled, setHeadBtnDisabled] = useState(false);
    const [handsBtnDisabled, setHandsBtnDisabled] = useState(false);

    useEffect(() => {

        refHands.onSnapshot((snapshot) => {
            try {
                if(snapshot.get("command") == 0) {
                    setHandsBtnDisabled(false);
                } else {
                    setHandsBtnDisabled(true)
                }
            } catch(e) {
                console.error("refHands: onSnapshot: exception", e);
            }
        }, err => {console.log(`Encountered error: ${err}`)});
        
        refHead.onSnapshot((snapshot) => {
            try {
                if(snapshot.get("command") == 0) {
                    setHeadBtnDisabled(false);
                } else {
                    setHeadBtnDisabled(true);
                }
            } catch (e) {
                console.error("refHead: onSnapshot: exception", e);
            }
        }, err => {console.log(`Encountered error: ${err}`)});
    
    }, []);

    function doHandMotion(command) {
        switch(command) {
            case "leftWave": 
                try {
                    refHands.update("command", "leftWave")
                } catch (e) {
                    console.error("doHandMotion: exception", e);
                }
                break;
            case "rightWave": 
                try {
                    refHands.update("command", "rightWave")
                } catch (e) {
                    console.error("doHandMotion: exception", e);
                }
                break;
            case "bothMuscles": 
                try {
                    refHands.update("command", "bothMuscles")
                } catch (e) {
                    console.error("doHandMotion: exception", e);
                }
                break;
            default: 
                console.log("Invalid switch case");
                break;
        }
    }

    function doHeadMotion(command) {
        switch(command) {
            case "yes": 
                try {
                    refHead.update("command", "yes")
                } catch (e) {
                    console.error("doHeadMotion: exception", e);
                }
                break;
            case "no":
                try {
                    refHead.update("command", "no")
                } catch (e) {
                    console.error("doHeadMotion: exception", e);
                } 
                break;
            default: 
                console.log("Invalid switch case");
                break;
        }
    }

    const refSay = db.collection(refPath).doc(deviceId).collection("presentation").doc("say");
    
    const [targetSay, setTargetSay] = useState([]);
    function handleSayChange(event) {
        setTargetSay(event.target.value);
    }

    const [targetSay2, setTargetSay2] = useState([]);
    function handleSayChange2(event) {
        setTargetSay2(event.target.value);
    }

    const [targetSay3, setTargetSay3] = useState([]);
    function handleSayChange3(event) {
        setTargetSay3(event.target.value);
    }

    function say(str) {
        console.log("str: ", str)
        if (str.length > 0) {
            refSay.update("time", new Date().getTime() )
            refSay.update("say", str);
        }
    }

    return (
        <>

            <Switch ability={activity} botProps={botProps} />

            <div className={classes.root}>
                <Button variant="contained" onClick={() => startSequenceSync(1)} disabled={buttonDisabled} size="small">Sync 1</Button>
                <Button variant="contained" onClick={() => startSequenceSync(2)} disabled={buttonDisabled} size="small">Sync 2</Button>
                <Button variant="contained" onClick={() => startSequenceSync(3)} disabled={buttonDisabled} size="small">Sync 3</Button>
                <Button variant="contained" onClick={() => startSequenceSync("stop")} disabled={buttonDisabled} size="small">Stop</Button>
            </div>

            <div className={classes.root}>
                <Button variant="contained" onClick={() => doHandMotion("leftWave")} disabled={handsBtnDisabled} size="small">Wave left</Button>
                <Button variant="contained" onClick={() => doHandMotion("rightWave")} disabled={handsBtnDisabled} size="small">Wave right</Button>
                <Button variant="contained" onClick={() => doHandMotion("bothMuscles")} disabled={handsBtnDisabled} size="small">Both muscles</Button>
                <Button variant="contained" onClick={() => doHeadMotion("yes")} disabled={headBtnDisabled} size="small">Nod yes</Button>
                <Button variant="contained" onClick={() => doHeadMotion("no")} disabled={headBtnDisabled} size="small">Nod no</Button>
            </div>

            <br></br>

            <form className={classes.form} noValidate autoComplete="off">
                <TextField
                    style={{marginTop: 10, marginBottom: 10, width:'50ch'}}
                    label="Say 1"
                    onChange={handleSayChange}
                    variant="outlined"
                    >
                </TextField>

                <Button style={{marginTop: 15, marginBottom: 10, height: 40, width:100}} variant="contained" onClick={() => say(targetSay)} disabled={handsBtnDisabled} size="small">Submit 1</Button>
            </form>

            <form className={classes.form} noValidate autoComplete="off">
                <TextField
                    style={{marginTop: 10, marginBottom: 10, width:'50ch'}}
                    label="Say 2"
                    onChange={handleSayChange2}
                    variant="outlined"
                    >
                </TextField>

                <Button style={{marginTop: 15, marginBottom: 10, height: 40, width:100}} variant="contained" onClick={() => say(targetSay2)} disabled={handsBtnDisabled} size="small">Submit 2</Button>
            </form>

            <form className={classes.form} noValidate autoComplete="off">
                <TextField
                    style={{marginTop: 10, marginBottom: 10, width:'50ch'}}
                    label="Say 3"
                    onChange={handleSayChange3}
                    variant="outlined"
                    >
                </TextField>

                <Button style={{marginTop: 15, marginBottom: 10, height: 40, width:100}} variant="contained" onClick={() => say(targetSay3)} disabled={handsBtnDisabled} size="small">Submit 3</Button>
            </form>

            <br></br>

            <RobotControl refPath={refPath} deviceId={deviceId} firebase={firebase}/>

        </>
    )
}

export default Dance

function RobotControl({refPath, deviceId, firebase}) {

    const db = firebase.firestore();
    const motionVals = db.collection(refPath).doc(deviceId).collection("motionValues");
    
    // Firebase refs
    const refF = motionVals.doc("Forward");
    const refL = motionVals.doc("Left");
    const refR = motionVals.doc("Right");
    const refS = motionVals.doc("Stop");
    
    const refLight = motionVals.doc("light");
    const refPrjctr = motionVals.doc("projector");
    
    const refHeadUp = motionVals.doc("headUp");
    const refHeadDown = motionVals.doc("headDown");
    const refHeadLeft = motionVals.doc("headLeft");
    const refHeadRight = motionVals.doc("headRight");
    const refHeadReset = motionVals.doc("headReset");
    
    // Locomotion
    function submitClickF() {
        refF.update("switch", 1);
        // forwardbtn.disabled = true;
    }
    function submitClickS() {
        refS.update("switch", 1);
        // stopbtn.disabled = true;
    }
    function submitClickL() {
        refL.update("switch", 1);
        // leftbtn.disabled = true;;
    }
    function submitClickR() {
        refR.update("switch", 1);
        // rightbtn.disabled = true;
    }
    
    // Head movement
    function submitClickHeadUp() {
        refHeadUp.update("switch", 1);
        // headUpbtn.disabled = true;
    }
    function submitClickHeadDown() {
        refHeadDown.update("switch", 1);
        // headDownbtn.disabled = true;
    }
    function submitClickHeadLeft() {
        refHeadLeft.update("switch", 1);
        // headLeftbtn.disabled = true;
    }
    function submitClickHeadRight() {
        refHeadRight.update("switch", 1);
        // headRightbtn.disabled = true;
    }
    function submitClickHeadReset() {
        refHeadReset.update("switch", 1);
        // headResetbtn.disabled = true;
    }
    
    // Hardware switch
    function submitClickLightOn() {
        refLight.update("switch", 1);
        // lightOnbtn.disabled = true;
        // lightOffbtn.disabled = false;
    }
    function submitClickLightOff() {
        refLight.update("switch", 0);
        // lightOnbtn.disabled = false;
        // lightOffbtn.disabled = true;
    }
    function submitClickPrjctrOn() {
        refPrjctr.update("switch", 1);
        // prjctrOnbtn.disabled = true;
        // prjctrOffbtn.disabled = false;
    }
    function submitClickPrjctrOff() {
        refPrjctr.update("switch", 0);
        // prjctrOnbtn.disabled = false;
        // prjctrOffbtn.disabled = true;
    }

    const classes = useStyles();
    const [speed, setSpeed] = React.useState(5);
    const [distance, setDistance] = React.useState(500);
    const [angle, setAngle] = React.useState(360);
    const [headAngle, setHeadAngle] = React.useState(45);
    const [headSpeed, setHeadSpeed] = React.useState(5);
    
    const speedChange = (event, newValue) => {
        setSpeed(newValue);
    };
    const speedUpload = () => {
        console.log("speedUpload: ", speed);
        refF.update("speed", speed);
        refL.update("speed", speed);
        refR.update("speed", speed);
    } 

    const distanceChange = (event, newValue) => {
        setDistance(newValue);
    }
    const distanceUpload = () => {
        console.log("distanceUpload: ", distance);
        refF.update("distance", distance);
        refL.update("distance", distance);
        refR.update("distance", distance);
    }

    const angleChange = (event, newValue) => {
        setAngle(newValue);
    }
    const angleUpload = () => {
        console.log("angleUpload: ", distance);
        refL.update("angle", angle);
        refR.update("angle", angle);
    }

    const headAngleChange = (event, newValue) => {
        setHeadAngle(newValue);
    }
    const headAngleUpload = () => {
        console.log("headAngleChange: ", headAngle);
        refHeadUp.update("angle", headAngle);
        refHeadDown.update("angle", headAngle);
        refHeadLeft.update("angle", headAngle);
        refHeadRight.update("angle", headAngle);
    }

    const headSpeedChange = (event, newValue) => {
        setHeadSpeed(newValue);
    }
    const headSpeedUpload = () => {
        console.log("headSpeedChange: ", headSpeed);
        refHeadUp.update("speed", headAngle);
        refHeadDown.update("speed", headAngle);
        refHeadLeft.update("speed", headAngle);
        refHeadRight.update("speed", headAngle);
    }


    
    var shiftDown;
    const handleKeyDown = (event) => {
        if (event.defaultPrevented || event.repeat) {
            event.preventDefault();
        return; // Do nothing if the event was already processed
      }
    
      switch (event.key) {
        case "Down": // IE/Edge specific value
        case "ArrowDown":
          // Do something for "down arrow" key press.
                if(shiftDown) {
                    console.log("SHIFT+DOWN");
                    submitClickHeadDown();
                } else {
                    console.log("DOWN");
                    submitClickS();
                }
          break;
        case "Up": // IE/Edge specific value
        case "ArrowUp":
          // Do something for "up arrow" key press.
                if(shiftDown) {
                    console.log("SHIFT+UP");
                    submitClickHeadUp();
                } else {
                    console.log("UP");
                    submitClickF();
                }
          break;
        case "Left": // IE/Edge specific value
        case "ArrowLeft":
          // Do something for "left arrow" key press
                if(shiftDown) {
                    console.log("SHIFT+LEFT");
                    submitClickHeadLeft();
                } else {
                    console.log("LEFT");
                    submitClickL();
                }
          break;
        case "Right": // IE/Edge specific value
        case "ArrowRight":
          // Do something for "right arrow" key press.
                if(shiftDown) {
                    console.log("SHIFT+RIGHT");
                    submitClickHeadRight();
                } else {
                    console.log("RIGHT");
                    submitClickR();
                }
          break;
            case "Shift":
                shiftDown = true;
                break;
        default:
          return; // Quit when this doesn't handle the key event.
      }
    
      // Cancel the default action to avoid it being handled twice
      event.preventDefault();
    }
    const handleKeyUp = (event) => {
        if (event.defaultPrevented) {
            return; // Do nothing if the event was already processed
          }
        
          switch (event.key) {
            case "Down": // IE/Edge specific value
            case "ArrowDown":
            case "Up": // IE/Edge specific value
            case "ArrowUp":
            case "Left": // IE/Edge specific value
            case "ArrowLeft":
            case "Right": // IE/Edge specific value
            case "ArrowRight":
                    if(!shiftDown) {
                        console.log("KEY_UP");
                  submitClickS();
                    }
              break;
                case "Shift":
                    shiftDown = false;
                    break;
            default:
              return; // Quit when this doesn't handle the key event.
          }
        
          // Cancel the default action to avoid it being handled twice
          event.preventDefault();
    }

    return (
        <>

            {/* <div className={classes.root}>
                <Input disabled onKeyDown={handleKeyDown} onKeyUp={handleKeyUp}/>
            </div> */}


            <div className={classes.root}>
            <Typography variant="body1" component="h3">  Hardware switch </Typography>
            <Button className="robot-control-btn" variant="contained" color="primary" onClick={submitClickLightOff}>
                Light Off</Button>

            <Button className="robot-control-btn" variant="contained" color="primary" onClick={submitClickLightOn} >
                Light On</Button>
                
            </div>


            <div className={classes.root}>
                <Typography variant="body1" component="h3">  Locomotion </Typography>
                <Button id={`forwardbtn_${deviceId}`} onClick={submitClickF} variant="contained" color="primary" >
                    Forward</Button>
                <Button id="stopbtn" onClick={submitClickS} variant="contained" color="primary" >
                    Stop</Button>
                <Button id="leftbtn" onClick={submitClickL} variant="contained" color="primary" >
                    Left</Button>
                <Button id="rightbtn" onClick={submitClickR} variant="contained" color="primary" >
                    Right</Button>
            </div>


            <div className={classes.slider}>
                <Typography id="discrete-slider-custom" gutterBottom>
                    {`Speed: ${speed}`}
                </Typography>
                <Slider
                    defaultValue={5}
                    value={speed}
                    aria-labelledby="discrete-slider-custom"
                    step={1}
                    valueLabelDisplay="auto"
                    min={1}
                    max={10}
                    onChange={speedChange}
                    onChangeCommitted={speedUpload}
                />
            </div>


            <div className={classes.slider}>
                <Typography id="discrete-slider-custom" gutterBottom>
                    {`Distance: ${distance}`}
                </Typography>
                <Slider
                    defaultValue={500}
                    value={distance}
                    aria-labelledby="discrete-slider-custom"
                    step={10}
                    valueLabelDisplay="auto"
                    min={10}
                    max={1000}
                    onChange={distanceChange}
                    onChangeCommitted={distanceUpload}
                />
            </div>


            <div className={classes.slider}>
                <Typography id="discrete-slider-custom" gutterBottom>
                    {`Turn: ${angle}`}
                </Typography>
                <Slider
                    defaultValue={360}
                    value={angle}
                    aria-labelledby="discrete-slider-custom"
                    step={5}
                    valueLabelDisplay="auto"
                    min={5}
                    max={360}
                    onChange={angleChange}
                    onChangeCommitted={angleUpload}
                />
            </div>


            <div className={classes.root}>
                <Typography variant="body1" component="h3">  Head movement </Typography>
                <Button onClick={submitClickHeadUp} variant="contained" color="primary" >
                    Up</Button>
                <Button onClick={submitClickHeadDown} variant="contained" color="primary" >
                    Down</Button>
                <Button onClick={submitClickHeadLeft} variant="contained" color="primary" >
                    Left</Button>
                <Button onClick={submitClickHeadRight} variant="contained" color="primary" >
                    Right</Button>
                <Button onClick={submitClickHeadReset} variant="contained" color="primary" >
                    Reset</Button>
            </div>


            <div className={classes.slider}>
                <Typography id="discrete-slider-custom" gutterBottom>
                    {`Head speed: ${headSpeed}`}
                </Typography>
                <Slider
                    defaultValue={5}
                    value={headSpeed}
                    aria-labelledby="discrete-slider-custom"
                    step={1}
                    valueLabelDisplay="auto"
                    min={1}
                    max={10}
                    onChange={headSpeedChange}
                    onChangeCommitted={headSpeedUpload}
                />
            </div>


            <div className={classes.slider}>
                <Typography id="discrete-slider-custom" gutterBottom>
                    {`Head turn: ${headAngle}`}
                </Typography>
                <Slider
                    defaultValue={45}
                    value={headAngle}
                    aria-labelledby="discrete-slider-custom"
                    step={10}
                    valueLabelDisplay="auto"
                    min={10}
                    max={90}
                    onChange={headAngleChange}
                    onChangeCommitted={headAngleUpload}
                />
            </div>

        </>
    )


}
