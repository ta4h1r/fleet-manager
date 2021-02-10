import React from 'react';

import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';

import { makeStyles } from '@material-ui/core';

import NewQnaIntent from '../forms/NewQnaIntent'

const useStyles = makeStyles((theme) => ({
  reportIssueButton: {
    marginRight: theme.spacing(2),
  }
}));

export default function FormDialog({ dialogProps }) {

  const classes = useStyles()

  return (
    <div>
      {dialogProps.tableState.setTableChanges ? (
        <Dialog open={dialogProps.showDialog} onClose={dialogProps.handleClose} aria-labelledby="form-dialog-title">
          <DialogTitle id="form-dialog-title">New data</DialogTitle>
          <DialogContent>
            <DialogContentText>
              Enter the question, answer, and intent data below and then click submit.
                </DialogContentText>

            <NewQnaIntent
              tableChanges={dialogProps.tableState.tableChanges}
              setTableChanges={dialogProps.tableState.setTableChanges}
              closeDialogFunction={dialogProps.handleClose}
            />

          </DialogContent>
          <DialogActions>
            {/** ... */}
          </DialogActions>
        </Dialog>
      ) : null}
    </div>
  );
}