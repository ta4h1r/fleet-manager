import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import CircularProgress from '@material-ui/core/CircularProgress';

const useStyles = makeStyles((theme) => ({
  root: {
    display: 'flex',
    float: 'centre', 
    width:"auto", 
    margin:"auto",
    marginTop: theme.spacing(10),
    '& > * + *': {
      marginLeft: 'auto',
      marginRight: 'auto',
      float: 'centre', 
    },
  },
}));

export default function CircularIndeterminate() {
  const classes = useStyles();

  return (
    <div className={classes.root}>
        <br/>
        <br/>
        <br/>
        <CircularProgress color="primary" />
    </div>
  );
}
