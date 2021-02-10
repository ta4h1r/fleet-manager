import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Container from '@material-ui/core/Container';
import Grid from '@material-ui/core/Grid';
import Box from '@material-ui/core/Box';
import Typography from '@material-ui/core/Typography';
import Link from '@material-ui/core/Link';
import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';

import { useForm } from "react-hook-form";

import SignInSubtitle from '../__elements/ElementSignInSubtitle';

const useStyles = makeStyles((theme) => ({
  tertiaryAction: {
    [theme.breakpoints.up('sm')]: {
      textAlign: 'right'
    }
  },
  actions: {
    [theme.breakpoints.down('sm')]: {
      marginTop: theme.spacing(3)
    },
  }
}));

export default function Form(props) {
  const classes = useStyles();
  
  const content = {
    'brand': { image: 'mui-assets/img/logo-pied-piper-icon.png', width: 40 },
    '02_header': 'Sign in',
    '02_primary-action': 'Sign in',
    '02_secondary-action': 'Don\'t have an account?',
    '02_tertiary-action': 'Forgot password?',
    ...props.content
  };

  let brand;

  if (content.brand.image) {
    brand = <img src={ content.brand.image } alt="" width={ content.brand.width } />;
  } else {
    brand = content.brand.text || '';
  }

  const { register, handleSubmit } = useForm();

  const formSubmit = (data) => {

    sessionStorage.setItem("clientAlias", data.username); 
    const postData = {
      command: "getClientInfo",
      data: {
        clientAlias: data.username,
        password:  data.pass,
      },
    };

    fetch("https://1t6ooufoi9.execute-api.us-east-1.amazonaws.com/prod", {     // fleet-manager 
      method: "post",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(postData),
    })
    .then(response => response.json())
    .then(returnData => {
      var permission = null;

      if (returnData.status == 200) {
        permission = "granted"
        sessionStorage.setItem("skyKey", permission);
        sessionStorage.setItem("clientId", returnData.data.clientId);
        sessionStorage.setItem("config", JSON.stringify(returnData.data.config));
        sessionStorage.setItem("aws_config", JSON.stringify(returnData.data.awsConfig));
        sessionStorage.setItem("iceServers", JSON.stringify(returnData.data.iceServers));
      } else {
        permission = "denied"
      }

      if (permission === "granted") {
        window.location.href="/landing/fleet";                       // Render Dashboard
      } else if (permission == "denied") {
        alert("Incorrect password and username combination"); 
      }

      return returnData;
    });
  };

  return (
    <section>
      <Container maxWidth="xs">
        <Box pt={8} pb={10}>
          <Box mb={3} textAlign="center">
            <Link href="#" variant="h4" color="inherit" underline="none">
              {brand}
            </Link>
            <Typography variant="h5" component="h2">{content['02_header']}</Typography>
            <SignInSubtitle/>
          </Box>
          <Box>
            <form noValidate onSubmit={handleSubmit(formSubmit)}>
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <TextField inputRef={register} variant="outlined" required fullWidth name="username" label="Username" type="email" autoComplete="email" />
                </Grid>
                <Grid item xs={12}>
                  <TextField inputRef={register} variant="outlined" required fullWidth name="pass" label="Password" type="password" autoComplete="current-password" />
                </Grid>
              </Grid>
              <Box my={2}>
                <Button type="submit" fullWidth variant="contained" color="primary">
                  {content['02_primary-action']}
                </Button>
              </Box>
              <Grid container spacing={2} className={classes.actions}>
                <Grid item xs={12} sm={6}>
                  <Link href="#" variant="body2">{content['02_secondary-action']}</Link>
                </Grid>
                <Grid item xs={12} sm={6} className={classes.tertiaryAction}>
                  <Link href="#" variant="body2">{content['02_tertiary-action']}</Link>
                </Grid>
              </Grid>
            </form>
          </Box>
        </Box>
      </Container>
    </section>
  );
}