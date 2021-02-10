import React, { Component } from 'react';
import axios from 'axios';
import { Button, TextField } from '@material-ui/core';

import { makeStyles, withStyles } from '@material-ui/core/styles';

import PropTypes from 'prop-types';

const priorities = ['Low', 'Medium', 'High'];
const statuses = ['Open', 'In Progress', 'Resolved'];
const types = ['Bug/Error', 'Feature Request', 'Security', 'Other'];

const baseUrl = 'https://mlilwyyhgl.execute-api.us-east-1.amazonaws.com/prod'

const useStyles = (theme) => ({
    root: {
        marginTop: theme.spacing(2),
    },
    button: {
        position: 'relative',
        display: 'flex',
        float: 'right',
        marginLeft: theme.spacing(1),
        marginRight: 'auto',
    }
});

class CreateTicket extends Component {
    constructor(props) {
        super(props);

        this.onChangeTitle = this.onChangeTitle.bind(this);
        this.onChangeDescription = this.onChangeDescription.bind(this);
        this.onChangeProjectName = this.onChangeProjectName.bind(this);
        this.onChangeAssignee = this.onChangeAssignee.bind(this);
        this.onChangePriority = this.onChangePriority.bind(this);
        this.onChangeStatus = this.onChangeStatus.bind(this);
        this.onChangeType = this.onChangeType.bind(this);
        this.onSubmit = this.onSubmit.bind(this);

        this.state = {
            title: '',
            description: '',
            projectName: 'Hotel Sky',
            assignee: 'Taahir',
            priority: '',
            status: 'Open',
            email: '',
            type: '',
            handleClose: this.props.closeDialogFunction,
            users: [],
            projects: []
        };
    }

    componentDidMount() {
        // set default values for state properties
        this.setState({
            priority: priorities[0],
            status: statuses[0],
            type: types[0]
        });

    }

    onChangeTitle(e) {
        this.setState({
            title: e.target.value
        })
    }

    onChangeDescription(e) {
        this.setState({
            description: e.target.value
        })
    }

    onChangeProjectName(e) {
        this.setState({
            projectName: e.target.value
        })
    }

    onChangeAssignee(e) {
        this.setState({
            assignee: e.target.value
        })
    }

    onChangePriority(e) {
        this.setState({
            priority: e.target.value
        })
    }

    onChangeStatus(e) {
        this.setState({
            status: e.target.value
        })
    }

    onChangeType(e) {
        this.setState({
            type: e.target.value
        })
    }

    onSubmit(e) {
        e.preventDefault();

        const ticket = {
            title: this.state.title,
            description: this.state.description,
            projectName: this.state.projectName,
            assignee: this.state.assignee,
            priority: this.state.priority,
            status: this.state.status,
            type: this.state.type
        }

        axios.post(baseUrl + '/tickets/create', ticket)
            .then(res => {
                alert('Successfully created ticket. Thank you for your feedback.');
            })
            .catch(res => {
                alert('Failed to create ticket. Please fill out all of the ticket fields.')
            });


        // clear form
        this.setState({
            title: '',
            email: '',
            description: '',
            priority: '',
            type: ''
        });
    }

    render() {

        const { classes } = this.props;

        return (
            <div className={classes.root}>

                <TextField
                    required
                    size='small'
                    onChange={this.onChangeTitle}
                    autoFocus
                    margin="dense"
                    id="title"
                    label="Title"
                    type="title"
                    fullWidth
                />

                <TextField
                    required
                    size='small'
                    value={this.state.description}
                    autoFocus
                    onChange={this.onChangeDescription}
                    margin="dense"
                    id="description"
                    label="Description"
                    multiline
                    fullWidth
                    rows={4}
                />

                <div className={classes.root}>
                    <TextField
                        required
                        size='small'
                        value={this.state.type}
                        select
                        label="Type"
                        onChange={this.onChangeType}
                        variant="outlined"
                        fullWidth
                        SelectProps={{
                            native: true,
                          }}
                    >
                          {types.map((option) => (
                            <option key={option} value={option}>
                                {option}
                            </option>
                        ))}
                    </TextField>
                </div>

                <div className={classes.root}>
                    <TextField
                        required
                        size='small'
                        value={this.state.priority}
                        select
                        label="Priority"
                        onChange={this.onChangePriority}
                        variant="outlined"
                        fullWidth
                        SelectProps={{
                            native: true,
                          }}
                    >
                        {priorities.map((option) => (
                            <option key={option} value={option}>
                                {option}
                            </option>
                        ))}
                    </TextField>
                </div>

                <div className={classes.root}>
                    <Button className={classes.button} onClick={this.state.handleClose} variant="outlined" color="secondary">
                        Close
                    </Button>
                    <Button className={classes.button} onClick={this.onSubmit} variant="outlined" color="primary">
                        Submit
                    </Button>
                </div>

            </div>
        );
    }
}

CreateTicket.propTypes = {
    classes: PropTypes.object.isRequired,
};

export default withStyles(useStyles)(CreateTicket);