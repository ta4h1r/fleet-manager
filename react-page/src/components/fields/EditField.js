import React from 'react'

import { IconButton, TextField } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';

import DoneIcon from '@material-ui/icons/Done';
import ClearIcon from '@material-ui/icons/Clear';

const useStyles = makeStyles(theme => ({
    root: {
        display: 'flex',
        flexGrow: 1,
        flexWrap: 'nowrap',
        width: '100%',
        // borderWidth: '1px', borderStyle: 'solid', borderColor: 'red',
        position: 'relative',
        marginBottom: theme.spacing(2),
        marginLeft: theme.spacing(2),
    }

}));


export default function EditIntentField({
    handleClickClear,
    category,
    onChange,
    handleClickAddNew,
    fieldValue }) {
    const classes = useStyles();
    return (
        <div className={classes.root}>
            <TextField
                required
                size='small'
                onChange={onChange}
                value={fieldValue}
                autoFocus
                margin="dense"
                id={`${category}-field`}
                label={category == 'name' ? `New ${category}` : `New ${category} name`}
                fullWidth
                controlled="true"
            />

            <IconButton
                onClick={handleClickAddNew}
                color='primary'
                size='small'
                aria-label="delete">
                <DoneIcon />
            </IconButton>
            
            <IconButton
                onClick={handleClickClear}
                color='primary'
                size='small'
                aria-label="delete">
                <ClearIcon />
            </IconButton>
        </div>
    )
}
