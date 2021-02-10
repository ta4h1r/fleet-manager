import React from 'react'
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Switch from '@material-ui/core/Switch';

function mSwitch({ ability, botProps }) {
    
    const {activityValues, robot, firebase, refPath} = botProps;
    const deviceId = robot.deviceId;

    /** Decide the state of the switch */
    let checkedState = false;

    const [state, setState] = React.useState({
        checkedA: checkedState,
    });

    React.useEffect(() => {
        
        try {

            const objVals = Object.values(activityValues)
            const objKeys = Object.keys(activityValues);

            for (var i = 0; i < objVals.length; i++) {
                let activity = objKeys[i];
                let value = objVals[i];

                if (ability == activity && value == 1) {
                    setState({checkedA: true})
                    break;
                } else {
                    setState({checkedA: false})
                }
            }
        } catch (err) {
            console.error("updateBool: Activity value does not exist. ", err)
            return false;
        }

    }, [activityValues])

    /** What happens when the switch is pressed */
    const handleSwitch = (event) => {
        const switchOn = !event.target.checked;
        if(switchOn) {
            endActivity(refPath, deviceId)
        } else {
            startActivity(refPath, deviceId)
        }
    };

    async function startActivity(refPath, deviceId) {

        sendMessage("accordionSummaryMsg", "Loading...");

        try {
            if (refPath != null && deviceId != null) {
                const db = firebase.firestore();
                const robotRef = db.collection(refPath).doc(deviceId);
                var robotState = await robotRef.get();
                var activityVals = robotState.data().activityValues;
    
                // Finish all other activities
                for(const property in activityVals) {
                    if (property != ability) {
                        activityVals[property] = 0;
                    }
                }
                var initialState = {
                    "activityValues": activityVals,
                };
                await robotRef.update(initialState);
                
                if (activityVals[ability] == 0) {
                    activityVals[ability] = 1;
                    var state = {
                        "activityValues": activityVals,
                    };
                    await robotRef.update(state);
                } else {
                    console.log("The selected activity is already running");
                }
            } else {
                console.log("startActivity: The requested robot could not be found.");
            }
        } catch (err) {
            console.error("startActivity: Firebase value does not exist", err)
        }

    }
    async function endActivity(refPath, deviceId) {

        sendMessage("accordionSummaryMsg", "-");

        if(refPath != null && deviceId != null) {
            const db = firebase.firestore();
            const robotRef = db.collection(refPath).doc(deviceId);
            var robotState = await robotRef.get();
            try {
                var activityVals = robotState.data().activityValues;
                for(const property in activityVals) {
                    activityVals[property] = 0;
                }
                var initialState = {
                    "activityValues": activityVals,
                };
                await robotRef.update(initialState);
            } catch(err) {
                console.error(err);
            }
            
        } else {
            console.log("endActivity: The requested robot could not be found.")
        }
    
    }
    function sendMessage(field, msg) {
        const refPath = sessionStorage.getItem('REF_PATH'); 
        const db = firebase.firestore();
        const robotRef = db.collection(refPath).doc(robot.deviceId);
        var obj = {};
        obj[field] = msg;
        robotRef.collection("messages").doc("Web").update(obj);
    }

    return (
        <span>
            <FormControlLabel
                control={<Switch checked={state.checkedA} onChange={handleSwitch} name="checkedA" />}
                label="On/Off"
            />
        </span>
    )
}

export default mSwitch
