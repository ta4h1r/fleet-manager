import React from 'react';
import { makeStyles } from '@material-ui/core/styles';

import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import Link from '@material-ui/core/Link';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';

const useStyles = makeStyles((theme) => ({
  toolbar: {
    flexWrap: 'wrap'
  },
  menuButton: {
    marginRight: theme.spacing(2),
  },
  linkBrand: {
    [theme.breakpoints.down('xs')]: {
      display: 'none',
    }
  },
  linkBrandSmall: {
    display: 'none',
    [theme.breakpoints.down('xs')]: {
      display: 'inline-block',
    }
  },
  tabs: {
    [theme.breakpoints.up('lg')]: {
      position: 'absolute', 
      left: '50%', 
      top: '63%',
      transform: 'translate(-50%, -50%)'
    },
    [theme.breakpoints.down('md')]: {
      order: 100,
      width: '100%',
    },
  },
  tab: {
    height: 32,
  },
}));

export default function Navigation(props) {
  const classes = useStyles();

  const content = {
    'brand': { image: 'mui-assets/img/logo-pied-piper-white.png', width: 120 },
    'brand-small': { image: 'mui-assets/img/logo-pied-piper-white-icon.png', width: 32 },
    ...props.content
  };

  let brand = content['brand'].text || '';
  let brandSmall = content['brand-small'].text || '';

  console.log(content['brand'].image); 
  if (content['brand'].image) {
    brand = <img src={ content['brand'].image } alt="" width={ content['brand'].width } />;
  }

  if (content['brand-small'].image) {
    brandSmall = <img src={ content['brand-small'].image } alt="" width={ content['brand-small'].width } />;
  }

  return (
    <AppBar position="static">

      <Toolbar className={classes.toolbar}>

        <Link href="#" variant="h5" color="inherit" underline="none" className={classes.linkBrand}>
          {brand}
        </Link>
        <Link href="#" variant="h5" color="inherit" underline="none" className={classes.linkBrandSmall}>
          {brandSmall}
        </Link>

        <Tabs value={props.pageValue} className={classes.tabs}>
          <Tab href="/landing/login" component={Link} label="Login" color="inherit" className={classes.tab} />
        </Tabs>

      </Toolbar>

    </AppBar>
  );
}