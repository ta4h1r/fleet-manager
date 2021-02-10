import React from 'react';

import HorizontalNav7 from '../components/navs/HorizontalNav7';
import SignIn2 from '../components/sign-in/SignIn2';

export default function Login() {
  sessionStorage.clear();
  return (
    <React.Fragment>
      <HorizontalNav7 pageValue={3}
        content={{
          brand: {
            text: 'CTRL Robotics',
            image: '',
            width: '80',
          },
          'brand-small': {
            text: 'CTRL Robotics',
            image: '',
            width: '32',
          },
          link1: 'About Us',
          link2: 'Products',
          link3: 'Contact',
          link4: 'Login',
        }}
      />

      <SignIn2 content={null} />
    </React.Fragment>
  );
}

