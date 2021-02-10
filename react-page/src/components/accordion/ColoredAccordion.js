import { Accordion, AccordionSummary, Container } from '@material-ui/core'
import React, { useEffect, useState } from 'react'

import AccordionDetails from '@material-ui/core/AccordionDetails';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import { makeStyles } from '@material-ui/core/styles';

import ActiveAbilityIndicator from "../indicators/ActiveAbilityIndicator";
import BatteryItem from "../indicators/BatteryItem";
import StatusIndicator from "../indicators/StatusIndicator";

import Tabs from "../tabs/AbilityTabs";

const useStyles = makeStyles((theme) => ({
    root: {
        width: '100%',
        marginTop: theme.spacing(1),
        marginBottom: theme.spacing(1),
        borderRadius: 10,
        borderStyle: 'solid',
        borderWidth: '0px',
        borderColor: '#fffff',
    },
    abilityIndicator: {
        marginLeft: theme.spacing(3),
        marginRight: 'auto',
        marginTop: theme.spacing(1),
        float: 'left',
        borderStyle: 'solid',
        borderWidth: '0px',
        borderColor: '#000000'
    },
    statusIndicator: {
        marginLeft: 'auto',
        marginRight: 'auto',
        marginTop: theme.spacing(1),
        float: 'center',
        borderStyle: 'solid',
        borderWidth: '0px',
        borderColor: '#000000'
    },
    nameIndicator: {
        marginLeft: theme.spacing(3),
        marginRight: theme.spacing(3),
        marginTop: theme.spacing(1),
        float: 'left',
        borderStyle: 'solid',
        borderWidth: '0px',
        borderColor: '#000000',
        '& span': {
            fontSize: theme.typography.pxToRem(15),
            fontWeight: theme.typography.fontWeightMedium,
        }
    },
    batteryIndicator: {
        marginLeft: 'auto',
        marginRight: theme.spacing(1),
        marginTop: theme.spacing(1),
        float: 'right',
        borderStyle: 'solid',
        borderWidth: '0px',
        borderColor: '#000000'
    },
    accordionDetails: {
        width: '95%',
        marginLeft: 'auto',
        marginRight: 'auto',
        float: 'center',
        display: 'flex',
        borderStyle: 'solid',
        borderWidth: '1px',
        borderColor: '#fffff',
    },
}));


function ColoredAccordion({ robot, firebase }) {
    const classes = useStyles();

    const db = firebase.firestore();
    const refPath = sessionStorage.getItem("REF_PATH");

    const messageRef = db.collection(refPath).doc(robot.deviceId).collection('messages').doc('Web');
    const batteryRef = db.collection(refPath).doc(robot.deviceId).collection('sensorValues').doc('battery');

    const [accordionColor, setAccordionColor] = useState([]);
    const [accordionMsg, setAccordionMsg] = useState([]);

    const colors = {
        loading: '#d3dbff',
        normal: '#bbbbbb',
        charge: '#ff847c',
        ready: '#ade498',
        stopped: '#ffce89',
    }

    const [batteryLevel, setBatteryLevel] = useState('');

    useEffect(() => {

        messageRef.onSnapshot(snapshot => {
            if (!snapshot.exists) {
                console.error('No such document!');
            } else {
                const aMsg = snapshot.get("accordionSummaryMsg");
                setAccordionMsg(aMsg);
            }
        }, err => {
            console.error(`Encountered error: ${err}`);
        });

        batteryRef.onSnapshot(snapshot => {
            if (!snapshot.exists) {
                console.error('No such document!');
            } else {
                const batteryValue = snapshot.get("value");
                setBatteryLevel(batteryValue);
            }
        }, err => {
            console.error(`Encountered error: ${err}`);
        });

    }, []); // make sure that only one listener is created on component mount

    useEffect(() => {

        function handleAccordionColor(accordionMsg, batteryLevel) {
            // console.log("robot: " + robot.robotAlias, "msg: " + accordionMsg);

            if (batteryLevel < 20) {
                setAccordionColor(colors.charge);
            } else {
                switch (accordionMsg) {
                    case 'Loading...':
                        setAccordionColor(colors.loading);
                        break;
                    case 'Ready':
                    case 'Caibrated':
                    case 'Enroute':
                        setAccordionColor(colors.ready);
                        break;
                    case 'Stopped':
                    case 'Waiting...':
                        setAccordionColor(colors.stopped);
                        break;
                    default:
                        setAccordionColor(colors.normal);
                        break;
                }
            }

        }

        handleAccordionColor(accordionMsg, batteryLevel);

    });


    return (
        <div className={classes.root}>
            <Accordion style={{
                borderWidth: '2px',
                borderStyle: 'solid',
                borderColor: accordionColor,
                background: '#000000',
                flexGrow: 1,
                marginLeft: 'auto',
                marginRight: 'auto',
                float: 'center',
            }}>
                <AccordionSummary
                    expandIcon={<ExpandMoreIcon />}
                    aria-controls="panel1a-content"
                    id="panel1a-header"
                >
                    {/* Robot name */}
                    <div className={classes.nameIndicator}>
                        <span className="robot-name-text">{robot.robotAlias.charAt(0).toUpperCase() + robot.robotAlias.slice(1)}</span>
                    </div>

                    <div className={classes.abilityIndicator} >
                        <ActiveAbilityIndicator refPath={sessionStorage.getItem("REF_PATH")} robot={robot} firebase={firebase} />
                    </div>

                    <div className={classes.statusIndicator}>
                        <StatusIndicator refPath={sessionStorage.getItem("REF_PATH")} robot={robot} firebase={firebase} />
                    </div>

                    {/* Battery item */}
                    <div className={classes.batteryIndicator}>
                        <BatteryItem batteryLevel={batteryLevel} refPath={sessionStorage.getItem("REF_PATH")} robot={robot} firebase={firebase} />
                    </div>

                </AccordionSummary>

                <AccordionDetails style={{flexGrow: 1}} >

                    <Tabs firebase={firebase} refPath={sessionStorage.getItem("REF_PATH")} robot={robot}/>

                </AccordionDetails>


            </Accordion>


        </div>
    )
}

export default ColoredAccordion
