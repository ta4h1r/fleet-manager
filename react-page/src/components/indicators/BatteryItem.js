import React from 'react'
import { makeStyles } from '@material-ui/core/styles';
const useStyles = makeStyles((theme) => ({
    root: {
        width: '100%',
        marginTop: 'auto',
        marginBottom: 'auto',
        float: 'right',
        '& span': {
            fontSize: theme.typography.pxToRem(15),
            marginRight: theme.spacing(1),
            marginTop: theme.spacing(0.35),
            float: 'left',
        },
        '& svg': {
            marginRight: 'auto',
            float: 'right',
        }
    },
}));
function BatteryItem({ batteryLevel }) {
    const classes = useStyles();

    let jsx = [];
    if (typeof batteryLevel == "number") {

        if (batteryLevel > 70) {
            jsx.push(
                <g key={1} id="Stockholm-icons-/-Devices-/-Battery-full" stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
                    <rect id="bound" x="0" y="0" width="24" height="24"></rect>
                    <rect id="Combined-Shape" fill="#E6E6E6" x="2" y="7" width="17" height="10" rx="2"></rect>
                    <path d="M20,10 L21,10 C21.5522847,10 22,10.4477153 22,11 L22,13 C22,13.5522847 21.5522847,14 21,14 L20,14 L20,10 Z" id="Rectangle" fill="#E6E6E6" opacity="0.3"></path>
                </g>
            )
        } else if (batteryLevel > 20) {
            jsx.push(
                <g key={2} id="Stockholm-icons-/-Devices-/-Battery-half" stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
                    <rect id="bound" x="0" y="0" width="24" height="24"></rect>
                    <path d="M11,9 L11,15 L17,15 L17,9 L11,9 Z M4,7 L17,7 C18.1045695,7 19,7.8954305 19,9 L19,15 C19,16.1045695 18.1045695,17 17,17 L4,17 C2.8954305,17 2,16.1045695 2,15 L2,9 C2,7.8954305 2.8954305,7 4,7 Z" id="Combined-Shape" fill="#E6E6E6" fillRule="nonzero"></path>
                    <path d="M20,10 L21,10 C21.5522847,10 22,10.4477153 22,11 L22,13 C22,13.5522847 21.5522847,14 21,14 L20,14 L20,10 Z" id="Rectangle" fill="#E6E6E6" opacity="0.3"></path>
                </g>
            );
        } else if (batteryLevel < 20) {
            jsx.push(
                <g key={3} id="Stockholm-icons-/-Devices-/-Battery-empty" stroke="none" strokeWidth="1" fill="none" fillRule="evenodd">
                    <rect id="bound" x="0" y="0" width="24" height="24"></rect>
                    <path d="M4,9 L4,15 L17,15 L17,9 L4,9 Z M4,7 L17,7 C18.1045695,7 19,7.8954305 19,9 L19,15 C19,16.1045695 18.1045695,17 17,17 L4,17 C2.8954305,17 2,16.1045695 2,15 L2,9 C2,7.8954305 2.8954305,7 4,7 Z" id="Combined-Shape" fill="#F34213" fillRule="nonzero"></path>
                    <path d="M20,10 L21,10 C21.5522847,10 22,10.4477153 22,11 L22,13 C22,13.5522847 21.5522847,14 21,14 L20,14 L20,10 Z" id="Rectangle" fill="#F34213" opacity="0.3"></path>
                </g>
            );
        }

    }


    return (
        <div className={classes.root}>

            <span>
                {batteryLevel}%
            </span>
            <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="24"
                    height="24"
                    viewBox="0 0 24 24"
                >
                    {jsx}

                </svg>

        </div>
    )
}

export default BatteryItem
