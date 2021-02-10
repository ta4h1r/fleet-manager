import React from 'react'

import Switch from '../switches/Switch'

function Chat({activity, botProps}) {

    return (
        <Switch ability={activity} botProps={botProps}/>
    )
}

export default Chat
