 import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import ListItemText from '@material-ui/core/ListItemText';
import IconButton from '@material-ui/core/IconButton';
import Grid from '@material-ui/core/Grid';
import Typography from '@material-ui/core/Typography';
import DeleteIcon from '@material-ui/icons/Delete';
import { Tooltip } from '@material-ui/core';

import EditIcon from '@material-ui/icons/Edit';
import EditField from '../fields/EditField';

const useStyles = makeStyles((theme) => ({
  root: {
    flexGrow: 1,
    maxWidth: 752,
  },
  addNewTextField: {
    display: 'flex',
    flewGrow: 1,
    flexWrap: 'nowrap',
    // borderStyle: 'solid', borderWidth: '1px', borderColor: 'white',
    '& .MuiIconButton-root': {
      // borderStyle: 'solid', borderWidth: '1px', borderColor: 'red',
      margin: theme.spacing(1),


    }
  },
  demo: {
    backgroundColor: theme.palette.background.paper,
  },
  title: {
    margin: theme.spacing(4, 0, 2),
  },
}));

function capitalizeFirstLetter(string) {
  return string.charAt(0).toUpperCase() + string.slice(1);
}


export default function InteractiveList({
  handleClickClearEditField,
  showEditField,
  category,
  data,
  handleClickDelete,
  handleClickEdit,
  onFieldChange,
  fieldValue }) {

  const classes = useStyles();
  const [state, setState] = React.useState({
    listJsx: [],
  });


  React.useEffect(() => {
    let dataList = data.map((item, index) => {

      return (
        <ListItem key={`q-no-${index}`}>
          <ListItemText
            primary={item}
          />
          <ListItemSecondaryAction>
            <Tooltip arrow title="Edit intent name">
              <IconButton onClick={() => handleClickEdit(item)} color="primary" edge="end" aria-label="delete">
                <EditIcon />
              </IconButton>
            </Tooltip>
          </ListItemSecondaryAction>
        </ListItem>
      )

    });

    setState({
      listJsx: dataList,
    });

  }, [data]);


  return (
    <div className={classes.root}>

      <Grid item xs={12} md={12}>
        <Typography variant="h6" className={classes.title}>
          {capitalizeFirstLetter(category)}
        </Typography>
        <div className={classes.demo}>
          <List dense>

            {state.listJsx}

            <div className={classes.addNewTextField}>

              {showEditField ? (
                <EditField
                  handleClickClear={handleClickClearEditField}
                  category={category}
                  onChange={onFieldChange}
                  handleClickAddNew={handleClickEdit}
                  fieldValue={fieldValue}
                />
              ) : null}

            </div>

          </List>



        </div>
      </Grid>


    </div>
  );
}