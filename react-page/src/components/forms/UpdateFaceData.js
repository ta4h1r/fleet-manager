import React, { Component } from 'react';
import axios from 'axios';
import { Button, IconButton, Snackbar, Tooltip } from '@material-ui/core';

import { Alert, AlertTitle } from '@material-ui/lab';

import { withStyles } from '@material-ui/core/styles';

import DeleteIcon from '@material-ui/icons/Delete';

import PropTypes from 'prop-types';

import FaceListField from '../list/FaceListField';
import DeleteAlertDialog from '../dialog/DeleteAlertDialog'
import FaceImage from '../__elements/ElementImageFaceRec';

const baseUrl = 'https://73svw35tt1.execute-api.us-east-1.amazonaws.com/prod';

const useStyles = (theme) => ({
    root: {
        marginTop: theme.spacing(2),
        minWidth: 400,
        borderColor: 'white', borderWidth: '0px', borderStyle: 'solid'
    },
    photo: {
        width: '80%',
        margin: 'auto',
        borderColor: 'red', borderWidth: '0px', borderStyle: 'dotted'
    },
    listField: {
        width: '100%',
        borderColor: 'red', borderWidth: '0px', borderStyle: 'dotted'
    },
    icon: {
        width: '100%',
        float: 'right',
        borderColor: 'red', borderWidth: '0px', borderStyle: 'solid',
        '& .MuiIconButton-root': {
            borderColor: 'blue', borderWidth: '0px', borderStyle: 'solid',
            float: 'right',
            marginTop: theme.spacing(1),
            marginRight: theme.spacing(0.5),
        }
    },
    button: {
        position: 'relative',
        display: 'flex',
        flexWrap: 'noWrap',
        float: 'right',
        marginLeft: theme.spacing(1),
        marginRight: 'auto',
        borderColor: 'blue', borderWidth: '0px', borderStyle: 'solid',
        '& .MuiButton-root': { margin: theme.spacing(1) }
    }
});




class FormFields extends Component {
    constructor(props) {
        super(props);

        this.onChangeName = this.onChangeName.bind(this);
        this.onChangeRole = this.onChangeRole.bind(this);

        this.onSubmit = this.onSubmit.bind(this);

        this.handleClickEditName = this.handleClickEditName.bind(this);
        this.handleClickEditRole = this.handleClickEditRole.bind(this);

        this.handleClickDeleteData = this.handleClickDeleteData.bind(this);
        this.closeDeleteAlertDialog = this.closeDeleteAlertDialog.bind(this);

        this.handleClickClearNameField = this.handleClickClearNameField.bind(this);
        this.handleClickClearRoleField = this.handleClickClearRoleField.bind(this);

        this.fetchData = this.fetchData.bind(this);

        this.onDelete = this.onDelete.bind(this);

        this.handleCloseSnackbar = this.handleCloseSnackbar.bind(this);

        this.state = {
            handleClose: this.props.closeDialogFunction,
            nameData: this.props.rowData.Name,
            roleData: this.props.rowData.Role,
            idData: this.props.rowData.id,
            q: '',
            a: '',
            i: '',
            showDeleteAlertDialog: false,
            showNameField: false,
            showIdField: false,
            showRoleField: false,
        };
    }

    componentDidMount() {

        // set default values for state properties
        this.setState({

        });

    }


    onChangeName(e) {
        this.setState({
            q: e.target.value
        })
    }
    onChangeRole(e) {
        this.setState({
            a: e.target.value
        })
    }

    onDelete(ev) {
        ev.preventDefault();  //to stop the form submitting

        const nam = this.state.nameData;
        const rol = this.state.roleData;
        const idm = this.state.idData;

        const requestData = {
            command: "Delete",
            Role: rol,
            Name: nam,
            id: idm
        }

        this.showAlert('updating');
        axios.post(`${baseUrl}`, requestData)
            .then(() => {
                this.fetchData()
                this.showAlert('deleteSuccess');
            })
            .catch((err) => {
                console.error(err);
                this.showAlert('failed')
            });

        this.closeDeleteAlertDialog();
    }
    onSubmit(e) {
        e.preventDefault();

        const nam = this.state.nameData;
        const rol = this.state.roleData;
        const idm = this.state.idData;

        const dataReady = (idm.length > 0) &&
        (rol.length > 0) && (nam.length > 0);

        const requestData = {
            command: "Update",
            Role: rol,
            Name: nam,
            id: idm
        }

        if (dataReady) {
            this.showAlert('updating');
            axios.post(`${baseUrl}`, requestData)
                .then(() => {
                    this.fetchData();
                    this.showAlert('updated');
                })
                .catch((err) => {
                    console.error(err);
                    this.showAlert('failed');
                });
        } else {
            alert("All fields are required to have at least one value.")
        }

    }

    fetchData() {
        fetch(baseUrl, {
            method: "get",
            headers: { "Content-Type": "application/json" },
            // body: JSON.stringify(postData),
        })
            .then((response) => response.json())
            .then((returnData) => {

                let tableData = returnData.data;
                this.props.setTableChanges(tableData);
                this.props.setTableChanges(this.props.tableChanges + 1)

            });
    }


    handleClickDeleteData() {
        this.setState({
            deleteMessage: 'All data will be lost. Proceed?',
            showDeleteAlertDialog: true,
        })
    }

    handleClickEditName() {
        this.setState({
            showNameField: !this.state.showNameField,
        })
        if (this.state.q) {
            const newRole = this.state.q;
            this.setState({
                nameData: newRole,
                q: ''
            })
        }
    }
    handleClickEditRole() {
        this.setState({
            showRoleField: !this.state.showRoleField,
        })
        if (this.state.a) {
            const newRole = this.state.a;
            this.setState({
                roleData: newRole,
                a: ''
            })
        }

    }

    handleClickClearNameField() {
        this.setState({
            showNameField: false,
        })
    }
    handleClickClearRoleField() {
        this.setState({
            showRoleField: false,
        })
    }


    closeDeleteAlertDialog() {
        this.setState({
            showDeleteAlertDialog: false,
        })
    }

    showAlert(type) {
        switch (type) {
            case 'failed':
                this.setState({
                    showFailedUpdateAlert: true
                })
                break;
            case 'updating':
                this.setState({
                    showUpdatingAlert: true
                })
                break;
            case 'updated':
                this.setState({
                    showUpdatedAlert: true
                })
                break;
            case 'deleteSuccess':
                this.setState({
                    showDeleteSuccessAlert: true
                })
                break;
        }
    }

    handleCloseSnackbar() {
        this.setState({
            showDeleteSuccessAlert: false,
            showUpdatedAlert: false,
            showUpdatingAlert: false,
            showFailedUpdateAlert: false,
        })
    }


    render() {

        const { classes } = this.props;

        return (
            <div className={classes.root}>


                <div className={classes.photo}>
                    <FaceImage Name={this.state.nameData} id={this.state.idData}/>
                </div>


                <div className={classes.listField}>

                    <FaceListField
                        handleClickClearEditField={this.handleClickClearNameField}
                        showEditField={this.state.showNameField}
                        category={'name'}
                        data={[this.state.nameData]}
                        handleClickDelete={this.handleClickDeleteData}
                        handleClickEdit={this.handleClickEditName}
                        onFieldChange={this.onChangeName}
                        fieldValue={this.state.q}
                    />

                </div>


                <div className={classes.listField}>

                    <FaceListField
                        handleClickClearEditField={this.handleClickClearRoleField}
                        showEditField={this.state.showRoleField}
                        category={'role'}
                        data={[this.state.roleData]}
                        handleClickDelete={this.handleClickDeleteData}
                        handleClickEdit={this.handleClickEditRole}
                        onFieldChange={this.onChangeRole}
                        fieldValue={this.state.a}
                    />

                </div>


                <div className={classes.icon}  >
                    <Tooltip arrow title="Delete face data">
                        <IconButton onClick={() => this.handleClickDeleteData()} color="secondary" edge="end" aria-label="delete">
                            <DeleteIcon />
                        </IconButton>
                    </Tooltip>
                </div>



                <div className={classes.button}>
                    <Button onClick={this.onSubmit} variant="outlined" color="primary">
                        Submit
                    </Button>
                    <Button onClick={this.state.handleClose} variant="outlined" color="secondary">
                        Close
                    </Button>
                </div>


                <DeleteAlertDialog deleteMessage={this.state.deleteMessage} handleDelete={this.onDelete} showDialog={this.state.showDeleteAlertDialog} handleClose={this.closeDeleteAlertDialog} />


                <Snackbar open={this.state.showUpdatingAlert} >
                    <Alert severity="info">
                        <AlertTitle>Info</AlertTitle>
                            Updating data...
                    </Alert>
                </Snackbar>
                <Snackbar open={this.state.showDeleteSuccessAlert} autoHideDuration={2000} onClose={this.handleCloseSnackbar}>
                    <Alert severity="success">
                        <AlertTitle>Success</AlertTitle>
                            Successfully deleted.
                    </Alert>
                </Snackbar>
                <Snackbar open={this.state.showUpdatedAlert} autoHideDuration={2000} onClose={this.handleCloseSnackbar}>
                    <Alert severity="success">
                        <AlertTitle>Success</AlertTitle>
                            Successfully updated.
                    </Alert>
                </Snackbar>
                <Snackbar open={this.state.showFailedUpdateAlert} autoHideDuration={2000} onClose={this.handleCloseSnackbar}>
                    <Alert severity="error">
                        <AlertTitle>Error</AlertTitle>
                            Failed to update.
                    </Alert>
                </Snackbar>

            </div>
        );
    }
}

FormFields.propTypes = {
    classes: PropTypes.object.isRequired,
};

export default withStyles(useStyles)(FormFields);