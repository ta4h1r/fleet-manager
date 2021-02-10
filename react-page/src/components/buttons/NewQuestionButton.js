import React from 'react';
import Button from '@material-ui/core/Button';

import NewQnaDialog from '../dialog/NewQnaDialog';

export default function Element(props) {
  const content = {
    '04_button': 'Add new +',
    ...props.content
  };

  const buttonProps = props.buttonProps;
  const dialogProps = props.dialogProps;

  return (
    <div>
      <Button disabled={!dialogProps.tableState.setTableChanges} onClick={buttonProps.onClick} size="small" color="primary" variant="outlined">{content['04_button']}</Button>
      <NewQnaDialog dialogProps={dialogProps}/>
    </div>
    
  );
}