import React from 'react';

export default function Structure(props) {
  const buckets = {
    '1': (Array.isArray(props.bucket1) ? props.bucket1 : [])
  }

  return (
    <div>
      {buckets['1'].map((component, index) => <React.Fragment key={index}>{component}</React.Fragment>)} 
    </div>
  );
}