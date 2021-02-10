import React, { Component } from 'react';
import axios from 'axios';
import { Button, Snackbar } from '@material-ui/core';

import { Alert, AlertTitle } from '@material-ui/lab';

import { withStyles } from '@material-ui/core/styles';

import PropTypes from 'prop-types';

import QnaListField from '../list/QnaListField'
import IntentListField from '../list/IntentListField'
import DeleteAlertDialog from '../dialog/DeleteAlertDialog'

const baseUrl = 'https://bgxan3yqs5.execute-api.us-east-1.amazonaws.com/prod'

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



class FormFields extends Component {
    constructor(props) {
        super(props);

        this.onChangeQuestion = this.onChangeQuestion.bind(this);
        this.onChangeAnswer = this.onChangeAnswer.bind(this);
        this.onChangeIntent = this.onChangeIntent.bind(this);
        this.onSubmit = this.onSubmit.bind(this);

        this.handleClickDeleteQuestionFromList = this.handleClickDeleteQuestionFromList.bind(this);
        this.handleClickAddNewQuestionToList = this.handleClickAddNewQuestionToList.bind(this);

        this.handleClickDeleteAnswerFromList = this.handleClickDeleteAnswerFromList.bind(this);
        this.handleClickAddNewAnswerToList = this.handleClickAddNewAnswerToList.bind(this);

        this.handleClickEditIntent = this.handleClickEditIntent.bind(this);
        this.handleClickDeleteIntent = this.handleClickDeleteIntent.bind(this);

        this.closeDeleteAlertDialog = this.closeDeleteAlertDialog.bind(this);
        this.handleClickClearEditField = this.handleClickClearEditField.bind(this);
        this.fetchData = this.fetchData.bind(this);

        this.onDelete = this.onDelete.bind(this);

        this.handleCloseSnackbar = this.handleCloseSnackbar.bind(this);

        this.state = {
            handleClose: this.props.closeDialogFunction,
            questionData: [this.props.rowData.question],
            answerData: [],
            intentData: [],
            q: '',
            a: '',
            i: '',
            showDeleteAlertDialog: false,
            showEditField: false,
        };
    }

    componentDidMount() {

        // set default values for state properties
        this.setState({

        });

    }


    onChangeQuestion(e) {
        this.setState({
            q: e.target.value
        })
    }
    onChangeAnswer(e) {
        this.setState({
            a: e.target.value
        })
    }
    onChangeIntent(e) {
        this.setState({
            i: e.target.value,
        })
    }
    onSubmit(e) {
        e.preventDefault();

        const qs = this.state.questionData;
        const as = this.state.answerData;
        const intent = this.state.intentData;

        //Removing empty strings 
        var filtered_qs = qs.filter(function (el) {
            return el != "";
        });
        var filtered_as = as.filter(function (el) {
            return el != "";
        });

        const dataReady = (qs.length > 0) &&
            (as.length > 0) && (intent.length > 0);

        const requestData = {
            questions: filtered_qs,
            answers: filtered_as,
            intent: intent
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
                    this.showAlert('failed')
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

                let qnaData = returnData.data;
                this.props.setTableChanges(qnaData);
                this.props.setTableChanges(this.props.tableChanges + 1)

            });
    }


    handleClickDeleteQuestionFromList(listItem) {
        var newQuestionData = this.state.questionData.filter(q => q != listItem)
        this.setState({
            questionData: newQuestionData,
        });
    }
    handleClickAddNewQuestionToList() {
        if (this.state.q) {
            const questionToAdd = this.state.q
            var newQuestionData = [];
            this.state.questionData.forEach(item => {
                newQuestionData.push(item);
            })
            newQuestionData.push(questionToAdd);
            this.setState({
                questionData: newQuestionData,
                q: '',
            })
        }

    }
    handleClickDeleteAnswerFromList(listItem) {
        var newAnswerData = this.state.answerData.filter(a => a != listItem)
        this.setState({
            answerData: newAnswerData,
        });
    }
    handleClickAddNewAnswerToList() {
        if (this.state.a) {
            const answerToAdd = this.state.a
            var newAnswerData = [];
            this.state.answerData.forEach(item => {
                newAnswerData.push(item);
            })
            newAnswerData.push(answerToAdd);
            this.setState({
                answerData: newAnswerData,
                a: '',
            })
        }

    }


    handleClickDeleteIntent() {
        this.setState({
            showDeleteAlertDialog: true,
        })
    }
    handleClickEditIntent() {
        this.setState({
            showEditField: !this.state.showEditField,
        })
        if (this.state.i) {
            const newIntent = this.state.i;
            this.setState({
                intentData: newIntent,
                i: ''
            })
        }

    }
    handleClickClearEditField() {
        this.setState({
            showEditField: false,
        })
    }


    closeDeleteAlertDialog() {
        this.setState({
            showDeleteAlertDialog: false,
        })
    }

    onDelete(ev) {
        ev.preventDefault();  //to stop the form submitting
        this.showAlert('updating');

        const qs = this.state.questionData;

        //Removing empty strings 
        var filtered_qs = qs.filter(function (el) {
            return el != "";
        });

        const requestData = {
            questions: filtered_qs,
        }

        console.log(requestData);

        axios.delete(`${baseUrl}`, {
            data: requestData,
        })
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

    showAlert(type) {
        this.handleCloseSnackbar();
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


                <div className={classes.root}>

                    <QnaListField
                        category={'question'}
                        data={this.state.questionData}
                        handleClickDelete={this.handleClickDeleteQuestionFromList}
                        handleClickAddNew={this.handleClickAddNewQuestionToList}
                        onFieldChange={this.onChangeQuestion}
                        fieldValue={this.state.q}
                    />

                </div>


                <div className={classes.root}>

                    <QnaListField
                        category={'answer'}
                        data={this.state.answerData}
                        handleClickDelete={this.handleClickDeleteAnswerFromList}
                        handleClickAddNew={this.handleClickAddNewAnswerToList}
                        onFieldChange={this.onChangeAnswer}
                        fieldValue={this.state.a}
                    />

                </div>


                <div className={classes.root}>

                    <IntentListField
                        handleClickClearEditField={this.handleClickClearEditField}
                        showEditField={this.state.showEditField}
                        category={'intent'}
                        data={[this.state.intentData]}
                        handleClickDelete={this.handleClickDeleteIntent}
                        handleClickEdit={this.handleClickEditIntent}
                        onFieldChange={this.onChangeIntent}
                        fieldValue={this.state.i}
                    />

                </div>


                <div className={classes.root}>
                    <Button className={classes.button} onClick={this.state.handleClose} variant="outlined" color="secondary">
                        Close
                    </Button>
                    <Button className={classes.button} onClick={this.onSubmit} variant="outlined" color="primary">
                        Submit
                    </Button>
                </div>


                <DeleteAlertDialog handleDelete={this.onDelete} showDialog={this.state.showDeleteAlertDialog} handleClose={this.closeDeleteAlertDialog} />


                <Snackbar open={this.state.showUpdatingAlert} onClose={this.handleCloseSnackbar}>
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