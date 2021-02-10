import { Typography } from "@material-ui/core";
import React, { useEffect } from "react";
import { useState } from "react";


function ActiveAbilityIndicator({refPath, robot, firebase}) {

    const db = firebase.firestore();
    const refDoc = db.collection(refPath).doc(robot.deviceId);

    const [activeAbility, setActiveAbility] = useState([]);

    useEffect(() => {
       
        refDoc.onSnapshot(snapshot => {
            const keys = Object.keys(snapshot.get('activityValues'));
            const vals = Object.values(snapshot.get('activityValues'));
            for(var i=0;i<keys.length;i++) {
                if(vals[i] === 1) {
                    setActiveAbility(capitalizeFirstLetter(keys[i]));
                    break;
                }
                setActiveAbility("-");
            }
            
        });
    }, []);

    // console.log("Active ability: " + activeAbility);

    function capitalizeFirstLetter(string) {
        if (string == "telepresence") {
            string = "control";
        }
        if (string == "presentation") {
            string = "dance";
        }
        return string.charAt(0).toUpperCase() + string.slice(1);
    }

    return (
        <div>
            <Typography> {`Active ability: ${activeAbility}`} </Typography>
        </div>
    )
}

export default ActiveAbilityIndicator
