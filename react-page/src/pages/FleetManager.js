import React from 'react';

import VerticalNav3 from '../components/navs/VerticalNav3';
import StructureDiv from '../components/__structures/StructureDiv';

import Robots from '../components/buckets/Robots'
import Analysis from '../components/buckets/Analysis';
import ChatData from '../components/buckets/ChatbotData';
import FaceData from '../components/buckets/FaceData';

export default function Fleet() {
  return (
    <React.Fragment>
      <VerticalNav3
        content={{
          brand: {
            text: 'CTRL Robotics ',
            image: './images/ctrl.svg',
            width: '56',
          },
          'brand-small': {
            text: '',
            image: './images/ctrl.svg',
            width: '32',
          },
          link1: 'Robots',
          link2: 'Analytics',
          link3: 'Chatbot Data',
          link4: 'Face Data',
          'secondary-action': 'Report Issue',
          'primary-action': 'Logout',
        }}
        bucketMain={[<StructureDiv bucket1={[<Robots/>]} />]}
        bucketAnalytics={[<StructureDiv bucket1={[<Analysis/>]} />]}
        bucketChatMem={[<StructureDiv bucket1={[<ChatData/>]} />]}
        bucketFaceMem={[<StructureDiv bucket1={[<FaceData/>]} />]}
      />
    </React.Fragment>
  ); 
}

