import React from 'react';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/core/styles';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';

import FacesTable from '../tables/FacesTable';

function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      style={{ margin: '0px', width: '100%' }}
      role="tabpanel"
      hidden={value !== index}
      id={`scrollable-auto-tabpanel-${index}`}
      aria-labelledby={`scrollable-auto-tab-${index}`}
      {...other}
    >
      {value === index && (
        <div >

          {children}

        </div>
      )}
    </div>
  );
}

TabPanel.propTypes = {
  children: PropTypes.node,
  index: PropTypes.any.isRequired,
  value: PropTypes.any.isRequired,
};

function a11yProps(index) {
  return {
    id: `scrollable-auto-tab-${index}`,
    'aria-controls': `scrollable-auto-tabpanel-${index}`,
  };
}

const useStyles = makeStyles((theme) => ({
  root: {
    display: 'flex',
    flexWrap: 'wrap',
    width: '90%',
    flexGrow: 1,
    position: 'absolute',
    maxWidth: 1200,
    marginLeft: theme.spacing(1),
    // borderWidth: '1px', borderStyle: 'solid', borderColor: 'yellow',
    '& .MuiTab-root': {
      display: 'flex',
      flexGrow: 1,
      position: 'relative',
    },
  },
  newQuestionButton: {
    marginBottom: theme.spacing(2),
    marginLeft: theme.spacing(1),
  },

}));

export default function ScrollableTabsButtonAuto() {
  const classes = useStyles();
  const [value, setValue] = React.useState(0);
  const [tableState, setTableState] = React.useState({});
  const [state, setState] = React.useState({
    showDialog: false,
  }); 

  const handleChange = (event, newValue) => {
    setValue(newValue);
  };

  const handleClose = () => {
    setState({
      showDialog: false,
    })
  }

  const handleClick = () => {
    setState({
      showDialog: true,
    })
  }

  return (
    <>

      <div className={classes.root}>
        <AppBar position="relative" color="default">
          <Tabs
            value={value}
            onChange={handleChange}
            indicatorColor="primary"
            textColor="primary"
            variant="scrollable"
            scrollButtons="auto"
            aria-label="scrollable auto tabs example"
          >
            <Tab label="Memory" {...a11yProps(0)} />

          </Tabs>

        </AppBar>
        <TabPanel value={value} index={0}>
          <FacesTable liftTableState={setTableState}/>
        </TabPanel>
      </div>

    </>

  );
}