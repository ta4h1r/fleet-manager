import React from 'react';

import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';

import { makeStyles } from '@material-ui/core';

import UpdateQnaIntent from '../forms/UpdateQnaIntent'

const useStyles = makeStyles((theme) => ({
  reportIssueButton: {
    marginRight: theme.spacing(2),
  }
}));

export default function FormDialog({
  setTableData,
  tableChanges,
  setTableChanges,
  open,
  handleClose,
  rowData }) {

  const classes = useStyles()

  return (
    <div>
      <Dialog open={open} onClose={handleClose} aria-labelledby="form-dialog-title">
        <DialogTitle id="form-dialog-title">Update data</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Edit your question and answer data below and then click submit.
          </DialogContentText>

          <UpdateQnaIntent
            setTableData={setTableData}
            tableChanges={tableChanges}
            setTableChanges={setTableChanges}
            closeDialogFunction={handleClose}
            rowData={rowData}
          />

        </DialogContent>
        <DialogActions>
          {/** ... */}
        </DialogActions>
      </Dialog>
    </div>
  );
}