import React from 'react';
import { useState, useEffect } from 'react'
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/core/styles';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import Box from '@material-ui/core/Box';

import Chat from '../tab-panels/Chat';
import Analytics from '../tab-panels/Analytics';
import Dance from '../tab-panels/Dance';
import Control from '../tab-panels/Control';
import Delivery from '../tab-panels/Delivery';
import { Container } from '@material-ui/core';

function TabPanel(props) {
    const { children, value, index, ...other } = props;

    return (
        <div
            role="tabpanel"
            hidden={value !== index}
            id={`scrollable-auto-tabpanel-${index}`}
            aria-labelledby={`scrollable-auto-tab-${index}`}
            {...other}
        >
            {value === index && (
                <Container>
                    <Box>
                        {children}
                    </Box>
                </Container>
            )}
        </div>
    );
}

TabPanel.propTypes = {
    children: PropTypes.node,
    index: PropTypes.any.isRequired,
    value: PropTypes.any.isRequired,
};

function a11yProps(index) {
    return {
        id: `scrollable-auto-tab-${index}`,
        'aria-controls': `scrollable-auto-tabpanel-${index}`,
    };
}

const useStyles = makeStyles((theme) => ({
    root: {
        flexWrap: 'wrap',
        float: 'center',
        width: 0,
        flexGrow: 1,
        backgroundColor: theme.palette.background.paper,
    },
}));

export default function ScrollableTabsButtonAuto({ firebase, refPath, robot }) {


    /** Get the current robot activities state */
    const db = firebase.firestore();
    const refActivityValues = db.collection(refPath).doc(robot.deviceId);

    const [activityValues, setActivityValues] = useState([]);

    useEffect(() => {
        refActivityValues.onSnapshot(snapshot => {
            const actVals = snapshot.get('activityValues');
            var eq = JSON.stringify(actVals) === JSON.stringify(activityValues);
            if (!eq) {
                setActivityValues(actVals);
            }
        });
    }, []);  // make sure that only one listener is created on component mount

    let jsxTabs = [];
    let tabPositions = {};
    let positionIndex = 0;

    /** Set the tabs for active modules pertaining to the client */
    Object.values(robot.abilities).forEach((value, index) => {
        let ability = Object.keys(robot.abilities)[index];
        if (value == 1) {

            jsxTabs.push(
                <Tab key={`tab-${index}`} label={capitalizeFirstLetter(ability)} {...a11yProps(positionIndex)} />
            )

            tabPositions[ability] = positionIndex;
            positionIndex++;

        }
    })

    function capitalizeFirstLetter(string) {
        if (string == "telepresence") {
            string = "control";
        }
        if (string == "presentation") {
            string = "dance";
        }
        return string.charAt(0).toUpperCase() + string.slice(1);
    }

    const classes = useStyles();
    const [value, setValue] = React.useState(0);

    const handleChange = (event, newValue) => {
        setValue(newValue);
    };


    const botProps = {
        activityValues: activityValues,
        robot: robot,
        firebase: firebase,
        refPath: refPath,
    }


    let jsxTabPanels = [];

    Object.keys(tabPositions).forEach((tabPanelName, position) => {
        let jsx = [];

        switch (tabPanelName) {
            case 'chat':
                jsx.push(<Chat activity={tabPanelName} botProps={botProps} key={tabPanelName} />)
                break;
            case 'analytics':
                jsx.push(<Analytics activity={tabPanelName} botProps={botProps} key={tabPanelName} />)
                break;
            case 'delivery':
                jsx.push(<Delivery activity={tabPanelName} botProps={botProps} key={tabPanelName} />)
                break;
            case 'telepresence':
                jsx.push(<Control activity={tabPanelName} botProps={botProps} key={tabPanelName} />)
                break;
            case 'presentation':
                jsx.push(<Dance activity={tabPanelName} botProps={botProps} key={tabPanelName} />)
                break;
        }

        jsxTabPanels.push(
            <TabPanel value={value} index={position} key={`tab-panel-${position}`} >
                {jsx}
            </TabPanel>
        )
    })


    return (
        <div className={classes.root}>
            <AppBar position="relative" color="default">
                <Tabs
                    value={value}
                    onChange={handleChange}
                    indicatorColor="primary"
                    textColor="primary"
                    variant="scrollable"
                    scrollButtons="auto"
                    aria-label="scrollable auto tabs example"
                >
                    {jsxTabs}
                </Tabs>
            </AppBar>

            {jsxTabPanels}
        </div>
    );
}