import React from 'react';

import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';

import NewUnansweredIntent from '../forms/NewUnansweredIntent'

export default function FormDialog({ 
  setTableData,
  tableChanges,
  setTableChanges,
  open,
  handleClose,
  rowData 
 }) {

  return (
    <div>

        <Dialog open={open} onClose={handleClose} aria-labelledby="form-dialog-title">
          <DialogTitle id="form-dialog-title">New data</DialogTitle>
          <DialogContent>
            <DialogContentText>
              Enter the question, answer, and intent data below and then click submit.
                </DialogContentText>

            <NewUnansweredIntent
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