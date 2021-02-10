import React from 'react';
import { makeStyles } from '@material-ui/core/styles';

const useStyles = makeStyles((theme) => ({
  image: {
    maxWidth: '100%',
  },
}));

export default function Element(props) {
  const classes = useStyles();

  const content = {
    'image': this.props.src,
    ...props.content
  };
 
  return (
    <img src={content['image']} alt="" className={classes.image} />
  );
}