import React from 'react'
import { useState } from "react";

import Button from '@material-ui/core/Button';
import { makeStyles } from '@material-ui/core/styles';
import { ButtonBase, Input, Slider, Typography } from '@material-ui/core';

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

    slider: {
        display: 'flex',
        flexWrap: 'wrap',
        marginLeft: theme.spacing(17),
        marginRight: theme.spacing(17),
        '& > *': {
            margin: theme.spacing(1),
            marginTop: theme.spacing(1),
        },
    },
    connectButton: {
        margin: theme.spacing(1),
    },
    disconnectButton: {
        margin: theme.spacing(1),
    },
}));

function Control({ activity, botProps }) {

    const classes = useStyles();

    const robot = botProps.robot;
    const firebase = botProps.firebase;
    const refPath = botProps.refPath;
    const deviceId = robot.deviceId;

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
                if (shiftDown) {
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
                if (shiftDown) {
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
                if (shiftDown) {
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
                if (shiftDown) {
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
                if (!shiftDown) {
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

            <Switch ability={activity} botProps={botProps} />

            <Tele refPath={refPath} deviceId={deviceId} firebase={firebase}/>

            <div className={classes.root}>
                <Typography variant="body1" component="h3">  Locomotion </Typography>
                <Input onKeyDown={handleKeyDown} onKeyUp={handleKeyUp} />
            </div>


            <div className={classes.root}>
                <Typography variant="body1" component="h3">  Hardware switch </Typography>
                <Button className="robot-control-btn" variant="contained" color="primary" onClick={submitClickLightOff}>
                    Light Off</Button>

                <Button className="robot-control-btn" variant="contained" color="primary" onClick={submitClickLightOn} >
                    Light On</Button>

            </div>


            {/* <div className={classes.root}>
                <Typography variant="body1" component="h3">  Locomotion </Typography>
                <Button id={`forwardbtn_${deviceId}`} onClick={submitClickF} variant="contained" color="primary" >
                    Forward</Button>
                <Button id="stopbtn" onClick={submitClickS} variant="contained" color="primary" >
                    Stop</Button>
                <Button id="leftbtn" onClick={submitClickL} variant="contained" color="primary" >
                    Left</Button>
                <Button id="rightbtn" onClick={submitClickR} variant="contained" color="primary" >
                    Right</Button>
            </div> */}


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
                    step={10}
                    valueLabelDisplay="auto"
                    min={5}
                    max={360}
                    onChange={angleChange}
                    onChangeCommitted={angleUpload}
                />
            </div>


            <div className={classes.root}>
                <Typography variant="body1" component="h3">  Head movement </Typography>
                {/* <Button onClick={submitClickHeadUp} variant="contained" color="primary" >
                    Up</Button>
                <Button onClick={submitClickHeadDown} variant="contained" color="primary" >
                    Down</Button>
                <Button onClick={submitClickHeadLeft} variant="contained" color="primary" >
                    Left</Button>
                <Button onClick={submitClickHeadRight} variant="contained" color="primary" >
                    Right</Button> */}
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

export default Control

function Tele({refPath, deviceId, firebase}) {

    let [reRender, setRenderState] = useState(false)

    const iceServers = JSON.parse(sessionStorage.getItem("iceServers"))[0];

    const configuration = {
        iceServers: iceServers,
        iceCandidatePoolSize: 10,
    };

    let peerConnection = null;
    let localStream = null;
    let remoteStream = null;
    let roomId = null;

    async function endActivity() {

        if(refPath != null && deviceId != null) {
            // document.getElementById('endBtn').disabled = true;
            // console.log("Ending activity: ", sessionStorage.getItem('activity'));
            const db = firebase.firestore();
            const robotRef = db.collection(refPath).doc(deviceId);
            var robotState = await robotRef.get();
            // console.log('Got robot state: ', robotState.exists);
            var activityValues = robotState.data().activityValues;
            // console.log("activityValues: ", activityValues);
    
            // Finish all activities
            for(const property in activityValues) {
                // console.log(`${property}: ${activityValues[property]}`);
                activityValues[property] = 0;
            }
            var initialState = {
                "activityValues": activityValues,
            };
            await robotRef.update(initialState);
            // location.href = "./" + 'activity' + ".html";  // Navigate to link
        } else {
            console.log("endActivity: The requested robot could not be found.")
        }
        setRenderState(true)
    
    }
    async function joinRoom() {
        // Get roomId from Firebase
        const db = firebase.firestore();
        const robotRef = db.collection(refPath).doc(deviceId);
        const roomIdRef = await robotRef.collection("messages").doc("TelepresenceActivity").get();
        roomId = roomIdRef.data().roomId;
    
        console.log('Join room: ', roomId);
        await joinRoomById(roomId);
    }
    async function joinRoomById(roomId) {
        const db = firebase.firestore();
        const roomRef = await db.collection(refPath).doc(deviceId).collection('rooms').doc(roomId);
        const roomSnapshot = await roomRef.get();
        console.log('Got room:', roomSnapshot.exists);
      
        if (roomSnapshot.exists) {
          console.log('Create PeerConnection with configuration: ', configuration);
          peerConnection = new RTCPeerConnection(configuration);
          registerPeerConnectionListeners();
          localStream.getTracks().forEach(track => {
            peerConnection.addTrack(track, localStream);
          });
      
          // Collecting ICE candidates
          const calleeCandidatesCollection = roomRef.collection('calleeCandidates');
          peerConnection.addEventListener('icecandidate', event => {
            if (!event.candidate) {
              console.log('Got final candidate!');
              return;
            }
            console.log('Got candidate: ', event.candidate);
            calleeCandidatesCollection.add(event.candidate.toJSON());
          });
      
          peerConnection.addEventListener('track', event => {
            console.log('Got remote track:', event.streams[0]);
            event.streams[0].getTracks().forEach(track => {
              console.log('Add a track to the remoteStream:', track);
              remoteStream.addTrack(track);
            });
          });
      
          // Creating SDP answer
          const offer = roomSnapshot.data().offer;
          console.log('Got offer:', offer);
          await peerConnection.setRemoteDescription(new RTCSessionDescription(offer));
          const answer = await peerConnection.createAnswer();
          console.log('Created answer:', answer);
          await peerConnection.setLocalDescription(answer);
      
          const roomWithAnswer = {
            answer: {
              type: answer.type,
              sdp: answer.sdp,
            },
          };
          await roomRef.update(roomWithAnswer);
      
          // Listening for remote ICE candidates
          roomRef.collection('callerCandidates').onSnapshot(snapshot => {
            snapshot.docChanges().forEach(async change => {
              if (change.type === 'added') {
                let data = change.doc.data();
                console.log(`Got new remote ICE candidate: ${JSON.stringify(data)}`);
                await peerConnection.addIceCandidate(new RTCIceCandidate(data));
              }
            });
          });
        }
      }
      function connect() {
        openUserMedia();
    }
    async function openUserMedia() {
    
      // Some of these constraints are not yet supported on all browsers.
      const audioConstraints = {
        echoCancellation: true,
        autoGainControl: true,
        noiseSuppression: true,
        sampleSize: 16,
      };
    
      const stream = await navigator.mediaDevices.getUserMedia({
        video: true,
        audio: audioConstraints,
      });
    
      joinRoom();
    
      document.querySelector(`#localVideo_${deviceId}`).srcObject = stream;
      localStream = stream;
    
      remoteStream = new MediaStream();
      document.querySelector(`#remoteVideo_${deviceId}`).srcObject = remoteStream;
    //   console.log('Stream:', document.querySelector(`#localVideo_${deviceId}`).srcObject);
    
    }
    function registerPeerConnectionListeners() {
    peerConnection.addEventListener('icegatheringstatechange', () => {
        console.log(
            `ICE gathering state changed: ${peerConnection.iceGatheringState}`);
    });
    
    peerConnection.addEventListener('connectionstatechange', () => {
        console.log(`Connection state change: ${peerConnection.connectionState}`);
        if (peerConnection.connectionState == "failed") {
        console.log("HERE");
        }
    });
    
    peerConnection.addEventListener('signalingstatechange', () => {
        console.log(`Signaling state change: ${peerConnection.signalingState}`);
    });
    
    peerConnection.addEventListener('iceconnectionstatechange ', () => {
        console.log(
            `ICE connection state change: ${peerConnection.iceConnectionState}`);
    });
    }


    const classes = useStyles();

    if(reRender) {
        return (
            <div>
    
                <Button className={classes.connectButton} color='primary' variant="outlined" onClick={connect} >Connect</Button>
                <Button className={classes.disconnectButton} color='secondary' variant="outlined" onClick={endActivity}>Disconnect</Button>
    
                  <br></br>
    
                  <div className={classes.root}>
                    <video id={`localVideo_${deviceId}`} muted autoPlay playsInline style={{display: "none"}}></video>
                    <video id={`remoteVideo_${deviceId}`} autoPlay playsInline style={{width: '65%', position: 'relative' , display: 'flex', flexWrap: 'wrap', borderStyle: "solid", borderWidth: "1px", borderColor: "#fffff"}}></video>
                  </div>


            </div>
        )
    } else {
        return (
            <div>
    
                <Button className={classes.connectButton} color='primary' variant="outlined" onClick={connect} >Connect</Button>
                <Button className={classes.disconnectButton} color='secondary' variant="outlined" onClick={endActivity}>Disconnect</Button>
    
                  <br></br>
    
                  <div className={classes.root}>
                    <video id={`localVideo_${deviceId}`} muted autoPlay playsInline style={{display: "none"}}></video>
                    <video id={`remoteVideo_${deviceId}`} autoPlay playsInline style={{width: '65%', position: 'relative' , display: 'flex', flexWrap: 'wrap', borderStyle: "solid", borderWidth: "1px", borderColor: "#fffff"}}></video>
                  </div>
    
            </div>
        )

    }

}