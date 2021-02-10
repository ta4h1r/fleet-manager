import React from 'react';
import { Typography } from '@material-ui/core';

function NavStatusIndicator({refPath, deviceId, firebase}) {
    const db = firebase.firestore();
    const refDoc = db.collection(refPath).doc(deviceId).collection("messages").doc("SlamActivity");

    const [statusText, setStatusText] = React.useState([]);

    React.useEffect(() => {
       
        refDoc.onSnapshot(snapshot => {
            setStatusText(snapshot.get("navStatus"));
        });
    }, []);

    return (
        <div style={{marginTop: 30}}>
            <Typography> {`Navigation state: ${statusText ? statusText : "-"}`} </Typography>
        </div>
    )
}

export default NavStatusIndicator
