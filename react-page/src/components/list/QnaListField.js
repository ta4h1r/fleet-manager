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

import NewField from '../fields/NewField';

const useStyles = makeStyles((theme) => ({
  root: {
    flexGrow: 1,
    maxWidth: 752,
  },
  addNewTextField: {
    display: 'flex',
    flewGrow: 1,
    flexWrap: 'nowrap',
    width: '100%',
    borderStyle: 'solid', borderWidth: '0px', borderColor: 'white',
    '& .MuiIconButton-root': {
      margin: theme.spacing(1),
    }
  },
  demo: {
    backgroundColor: theme.palette.background.paper,
    borderStyle: 'solid', borderWidth: '0px', borderColor: 'red',
  },
  title: {
    margin: theme.spacing(4, 0, 2),
  },
}));

function capitalizeFirstLetterAndAddS(string) {
  return string.charAt(0).toUpperCase() + string.slice(1) + 's';
}


export default function InteractiveList({ 
  category,
  data,
  handleClickDelete,
  handleClickAddNew,
  onFieldChange,
  fieldValue }) {

  const classes = useStyles();
  const [state, setState] = React.useState({
    listJsx: [],
  });

  React.useEffect(() => {
    let dataList = data.map((item, index) => {

      return (
        <ListItem key={`${category}-no-${index}`}>
          <ListItemText
            primary={item}
          />
          <ListItemSecondaryAction>
            <Tooltip arrow title="Delete is not yet supported">
              <span>
              <IconButton disabled={false} onClick={() => handleClickDelete(item)} edge="end" aria-label="delete">
                <DeleteIcon />
              </IconButton>
              </span>
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
          {capitalizeFirstLetterAndAddS(category)}
          </Typography>
        <div className={classes.demo}>
          <List dense>

            {state.listJsx}

          </List>

          <div className={classes.addNewTextField}>

            <NewField
              category={category} 
              onChange={onFieldChange}
              handleClickAddNew={handleClickAddNew}
              fieldValue={fieldValue}
            />

          </div>

        </div>
      </Grid>


    </div>
  );
}