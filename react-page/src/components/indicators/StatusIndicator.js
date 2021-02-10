import { Typography } from '@material-ui/core';
import React, {useEffect, useState} from 'react'

function StatusIndicator({refPath, robot, firebase}) {

    const db = firebase.firestore();

    const refDoc = db.collection(refPath).doc(robot.deviceId).collection("messages").doc("Web");

    const [statusText, setStatusText] = useState([]);

    useEffect(() => {
        refDoc.onSnapshot(snapshot => {
            setStatusText(snapshot.get("accordionSummaryMsg"));
        });
    }, []);

    return (
        <div>
            <Typography> {`Status: ${statusText ? statusText : "-"}`} </Typography>
        </div>
    )
}

export default StatusIndicator




