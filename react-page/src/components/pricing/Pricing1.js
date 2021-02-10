import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Container from '@material-ui/core/Container';
import Grid from '@material-ui/core/Grid';
import Box from '@material-ui/core/Box';
import Typography from '@material-ui/core/Typography';
import Button from '@material-ui/core/Button';

import Card from '@material-ui/core/Card';
import CardActions from '@material-ui/core/CardActions';
import CardContent from '@material-ui/core/CardContent';
import CardHeader from '@material-ui/core/CardHeader';

const useStyles = makeStyles((theme) => ({
  cardHeader: {
    backgroundColor: theme.palette.action.selected,
  },
}));

export default function Pricing(props) {
  const classes = useStyles();
  
  const content = {
    'header': 'Our plans',
    'description': 'Choose one of our tailored solutions. For you and your team.',
    '01_title': 'Small Piper',
    '01_price': '$9',
    '01_suffix': ' / mo',
    '01_description': 'Join our network, but build and manage everything yourself.',
    '01_primary-action': 'Contact sales',
    '02_title': 'Medium Piper',
    '02_price': '$49',
    '02_suffix': ' / mo',
    '02_description': 'We build what you need, but you still need to manage your data.',
    '02_primary-action': 'Contact sales', 
    ...props.content
  };

  return (
    <section>
      <Container maxWidth="md">
        <Box pt={8} pb={10} textAlign="center">
          <Box mb={6}>
            <Typography variant="h4" component="h2" gutterBottom={true}>{content['header']}</Typography>
            <Typography variant="subtitle1" color="textSecondary">{content['description']}</Typography>
          </Box>
          <Grid container spacing={6}>
            <Grid item xs={12} md={6}>
              <Card>
                <CardHeader title={content['01_title']} className={classes.cardHeader}></CardHeader>
                <CardContent>
                  <Box pt={2} pb={1} px={1}>
                    <Typography variant="h3" component="h2" gutterBottom={true}>
                      {content['01_price']}
                      <Typography variant="h6" color="textSecondary" component="span">{content['01_suffix']}</Typography>
                    </Typography>
                    <Typography variant="body1" component="p">
                      {content['01_description']}
                    </Typography>
                  </Box>
                </CardContent>
                <CardActions>
                  <Button variant="outlined" fullWidth color="primary" className={classes.primaryAction}>{content['01_primary-action']}</Button>
                </CardActions>
              </Card>
            </Grid>
            <Grid item xs={12} md={6}>
              <Card>
                <CardHeader title={content['02_title']} className={classes.cardHeader}></CardHeader>
                <CardContent>
                  <Box pt={2} pb={1} px={1}>
                    <Typography variant="h3" component="h2" gutterBottom={true}>
                      {content['02_price']}
                      <Typography variant="h6" color="textSecondary" component="span">{content['02_suffix']}</Typography>
                    </Typography>
                    <Typography variant="body1" component="p">
                      {content['02_description']}
                    </Typography>
                  </Box>
                </CardContent>
                <CardActions>
                  <Button variant="outlined" fullWidth color="primary">{content['02_primary-action']}</Button>
                </CardActions>
              </Card>
            </Grid>
          </Grid>
        </Box>
      </Container>
    </section>
  );
}