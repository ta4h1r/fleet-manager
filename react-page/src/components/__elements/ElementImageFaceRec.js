import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { CircularProgress } from '@material-ui/core';

const useStyles = makeStyles((theme) => ({
  image: {
    maxWidth: '100%',
  },
}));

export default function Element(props) {
  const classes = useStyles();

  const [imgUrl, setImgUrl] = React.useState('');

  React.useEffect(() => {

    function bucketQuery(folder, path) {
      var albumBucketName = 'fr-module-faces';
      var foldername = decodeURIComponent(folder) + '/' + decodeURIComponent(path) + '/';

      // console.log(foldername);

      const AWS = require('aws-sdk');

      AWS.config.region = 'us-east-1'; // Region
      AWS.config.credentials = new AWS.CognitoIdentityCredentials({
        IdentityPoolId: 'us-east-1:53230161-20c1-4976-9122-4509250e7c12',
      });

      var s3 = new AWS.S3();

      var params = {
        Bucket: albumBucketName,
        Delimiter: '',
        Prefix: foldername
      }

      s3.listObjects(params, function (err, data) {
        if (err) {
          console.error(err);
        } else if (data.Contents.length == 0) {
          console.log("bucketQuery: Directory does not exist");
        } else {
          // console.log(data);
          var href = this.request.httpRequest.endpoint.href;
          var bucketUrl = href + albumBucketName + '/';
          var photoKey = data.Contents[0].Key;
          var photoUrl = bucketUrl + encodeURIComponent(photoKey);
          // console.log(photoUrl);
          setImgUrl(photoUrl);
        }
      });

    }

    bucketQuery(props.Name, props.id)

  }, []);


  const content = {
    'image': 'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?ixlib=rb-1.2.1&ixid=eyJhcHBfaWQiOjEyMDd9&auto=format&fit=crop&w=1351&q=80',
    ...props.content
  };


  return (
    <img src={imgUrl} alt="" className={classes.image} />
  );
}