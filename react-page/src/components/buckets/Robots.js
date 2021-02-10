import React, { useEffect } from "react";
import { useState } from "react";

import { useHistory } from "react-router-dom";

import { makeStyles } from '@material-ui/core/styles';

import ColoredAccordion from '../accordion/ColoredAccordion';
import CircularIndeterminate from "../progress-icon/CircularProgress";

var firebase = require('firebase');

const useStyles = makeStyles((theme) => ({
    toolbar: {
        flexWrap: 'wrap'
    },
    menuButton: {
        marginRight: theme.spacing(2),
    },
    linkBrand: {
        [theme.breakpoints.down('xs')]: {
            display: 'none',
        }
    },
    linkBrandSmall: {
        display: 'none',
        [theme.breakpoints.down('xs')]: {
            display: 'inline-block',
        }
    },
    tabs: {
        [theme.breakpoints.up('lg')]: {
            position: 'absolute',
            left: '50%',
            top: '50%',
            transform: 'translate(-50%, -50%)'
        },
        [theme.breakpoints.down('md')]: {
            order: 100,
            width: '100%',
        },
    },
    tab: {
        height: 64,
    },
    drawerContainer: {
        width: 256,
    }
}));

function Robots() {
    const classes = useStyles();

    const history = useHistory();

    const [robotsList, setRobotsList] = useState([]);
    const [isLoading, setLoading] = useState(true);

    const postData = {
        command: "getFleet",
        data: {
            clientId: sessionStorage.getItem("clientId"),
        }
    };

    // Gets the list of available robots 
    useEffect(() => {
        function fetchData() {
            fetch("https://1t6ooufoi9.execute-api.us-east-1.amazonaws.com/prod", {
                method: "post",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(postData),
            })
                .then((response) => response.json())
                .then((returnData) => {

                    let fleetDetails = returnData.data.fleetDetails;
                    sessionStorage.setItem("REF_PATH", returnData.data.refPath);

                    let fetchedRobots = [];
                    if (fleetDetails) {
                        fleetDetails.forEach(robot => {
                            fetchedRobots.push(robot);
                        });
                    }

                    setRobotsList(fetchedRobots);
                    setLoading(false);

                });
        }

        fetchData();

        const firebaseConfig = sessionStorage.getItem("config");
        const fArray = JSON.parse(firebaseConfig);
        if (fArray) {
            if (!firebase.apps.length) {
                firebase.initializeApp(fArray[0]);
            }
        }

    }, []);

    // Render Dash on access granted, else push to login page
    if (sessionStorage.getItem("skyKey") == "granted" && !isLoading) {

        return (
            <>

                <br />

                <div className="nav-render-holder">

                    {robotsList.map((robot, index) => (
                        <div className={classes.root} key={`robots-div-${index}`}>

                            <ColoredAccordion robot={robot} firebase={firebase} key={`robots-accordion-${index}`}/>

                        </div>
                    ))}

                </div>

            </>
        );
    } else if (!sessionStorage.getItem("skyKey")) {
        return <> {history.push("./login")}; </>;
    } else {
        return (
            <>

                <CircularIndeterminate />

            </>
        )
    }
}

export default Robots
