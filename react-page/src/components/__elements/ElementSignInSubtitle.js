import React from 'react';
import Typography from '@material-ui/core/Typography';

export default function Element(props) {
  const content = {
    '01_paragraph': 'Sign in to your console',
    ...props.content
  };
 
  return (
    <Typography variant="subtitle1" color="textSecondary" paragraph={true}>{content['01_paragraph']}</Typography>
  );
}