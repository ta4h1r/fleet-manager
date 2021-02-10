import React from 'react';
import { makeStyles } from '@material-ui/core/styles';

import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import IconButton from '@material-ui/core/IconButton';
import Link from '@material-ui/core/Link';
import Button from '@material-ui/core/Button';

import Drawer from '@material-ui/core/Drawer';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';

import MenuIcon from '@material-ui/icons/Menu';
import AppsIcon from '@material-ui/icons/Apps';
import ChatIcon from '@material-ui/icons/Chat';
import { Equalizer } from '@material-ui/icons';
import PersonIcon from '@material-ui/icons/Person';

import { useHistory } from "react-router-dom";

import TicketForm from '../dialog/TicketDialog';


const useStyles = makeStyles((theme) => ({
  root: {
    display: 'flex',
  },
  appBar: {
    zIndex: theme.zIndex.drawer + 1,
  },
  menuButton: {
    display: 'none',
    marginRight: theme.spacing(2),
    [theme.breakpoints.down('md')]: {
      display: 'inline-flex',
    }
  },
  secondaryButton: {
    marginRight: theme.spacing(2),
  },
  linkBrand: {
    flexGrow: 1,
    [theme.breakpoints.down('xs')]: {
      display: 'none',
    }
  },
  linkBrandSmall: {
    display: 'none',
    flexGrow: 1,
    [theme.breakpoints.down('xs')]: {
      display: 'inline-block',
    }
  },
  drawer: {
    width: 256,
    flexShrink: 0,
    [theme.breakpoints.down('md')]: {
      display: 'none',
    }
  },
  drawerContainer: {
    width: 256,
    overflow: 'auto',
  },
  content: {
    flexGrow: 1,
    padding: theme.spacing(3),
  },
}));

export default function Navigation(props) {
  const classes = useStyles();
  const history = useHistory();

  const content = {
    'brand': { image: 'mui-assets/img/logo-pied-piper-white.png', width: 120 },
    'brand-small': { image: 'mui-assets/img/logo-pied-piper-white-icon.png', width: 32 },
    ...props.content
  };

  let brand = content['brand'].text || '';
  let brandSmall = content['brand-small'].text || '';

  if (content['brand'].image) {
    brand = <img src={content['brand'].image} alt="" width={content['brand'].width} />;
  }

  if (content['brand-small'].image) {
    brandSmall = <img src={content['brand-small'].image} alt="" width={content['brand-small'].width} />;
  }

  const [state, setState] = React.useState({ open: false });

  const toggleDrawer = (open) => (event) => {
    if (event.type === 'keydown' && (event.key === 'Tab' || event.key === 'Shift')) {
      return;
    }

    setState({ ...state, open });
  };

  const buckets = {
    'main': (Array.isArray(props.bucketMain) ? props.bucketMain : []),
    'analytics': (Array.isArray(props.bucketAnalytics) ? props.bucketAnalytics : []),
    'chatMem': (Array.isArray(props.bucketChatMem) ? props.bucketChatMem : []),
    'faceMem': (Array.isArray(props.bucketFaceMem) ? props.bucketFaceMem : [])
  }

  const [bucketToLoad, setBucketToLoad] = React.useState('main')

  const handleClickRobots = () => {
    setBucketToLoad('main');
  }

  const handleClickAnalytics = () => {
    setBucketToLoad('analytics');
  }

  const handleClickChatMem = () => {
    setBucketToLoad('chatMem');
  }

  const handleClickFaceMem = () => {
    setBucketToLoad('faceMem');
  }

  const logout = () => {
    sessionStorage.clear();
    history.push('./login');
  }

  const jsxList = [
    <List key={`list-item-${0}`}>
      <ListItem button onClick={handleClickRobots} key={content['link1']}>
        <ListItemIcon>
          <AppsIcon />
        </ListItemIcon>
        <ListItemText primary={content['link1']} />
      </ListItem>
      <ListItem button onClick={handleClickAnalytics} key={content['link2']}>
        <ListItemIcon>
          <Equalizer />
        </ListItemIcon>
        <ListItemText primary={content['link2']} />
      </ListItem>
      <ListItem button onClick={handleClickChatMem} key={content['link3']}>
        <ListItemIcon>
          <ChatIcon />
        </ListItemIcon>
        <ListItemText primary={content['link3']} />
      </ListItem>
      <ListItem button onClick={handleClickFaceMem} key={content['link4']}>
        <ListItemIcon>
          <PersonIcon />
        </ListItemIcon>
        <ListItemText primary={content['link4']} />
      </ListItem>
    </List>
  ]

  return (
    <div className={classes.root}>
      <AppBar position="fixed" className={classes.appBar}>
        <Toolbar>
          <IconButton edge="start" className={classes.menuButton} color="inherit" aria-label="menu" onClick={toggleDrawer(true)}>
            <MenuIcon />
          </IconButton>
          <Link href="#" variant="h5" color="inherit" underline="none" className={classes.linkBrand}>
            {brand}
          </Link>
          <Link href="#" variant="h5" color="inherit" underline="none" className={classes.linkBrandSmall}>
            {brandSmall}
          </Link>
          <TicketForm className={classes.secondaryButton} classes={classes} />
          <Button onClick={logout} variant="contained" color="secondary">{content['primary-action']}</Button>
        </Toolbar>
      </AppBar>


      <Drawer className={classes.drawer} variant="permanent" classes={{ paper: classes.drawerPaper }}>
        <Toolbar />
        <div className={classes.drawerContainer}>
          {jsxList}
        </div>
      </Drawer>


      <Drawer variant='temporary' anchor="left" open={state.open} onClose={toggleDrawer(false)}>
        <div className={classes.drawerContainer}>
          {jsxList}
        </div>
      </Drawer>


      <main className={classes.content}>
        <Toolbar />
        <div>
          {buckets[bucketToLoad].map((component, index) => <React.Fragment key={index}>{component}</React.Fragment>)}
        </div>
      </main>


    </div>
  );
}