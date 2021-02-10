import React from 'react';

import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';

import CreateTicket from '../forms/CreateTicket';
import { makeStyles } from '@material-ui/core';

const useStyles = makeStyles((theme) => ({
  reportIssueButton: {
    marginRight: theme.spacing(2),
  }
}));

export default function FormDialog() {

  const classes = useStyles()
  const [open, setOpen] = React.useState(false);

  const handleClickOpen = () => {
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
  };
  
  return (
    <div>
      <Button className={classes.reportIssueButton} color="inherit" onClick={handleClickOpen}>
        Report Issue
      </Button>
      <Dialog open={open} onClose={handleClose} aria-labelledby="form-dialog-title">
        <DialogTitle id="form-dialog-title">Submit a Ticket</DialogTitle>
        <DialogContent>
          <DialogContentText>
            To report an issue, please fill out all the fields below and click submit.
          </DialogContentText>
          
          <CreateTicket closeDialogFunction={handleClose}/>

        </DialogContent>
        <DialogActions>
             {/** ... */}
        </DialogActions>
      </Dialog>
    </div>
  );
}