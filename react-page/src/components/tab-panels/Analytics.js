import React from 'react'

import Switch from '../switches/Switch'

function Analytics({ activity, botProps }) {

    return (
        <>
            <Switch ability={activity} botProps={botProps} />
        </>
    )
}

export default Analytics
