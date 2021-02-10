import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import Paper from '@material-ui/core/Paper';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableContainer from '@material-ui/core/TableContainer';
import TableHead from '@material-ui/core/TableHead';
import TablePagination from '@material-ui/core/TablePagination';
import TableRow from '@material-ui/core/TableRow';


import EditFaceDialog from '../dialog/EditFaceDataDialog';

const columns = [
  {
    id: 'Name',
    label: 'Name',
    minWidth: 100
  },
  {
    id: 'id',
    label: 'ID',
    minWidth: 200
  },
  {
    id: 'Role',
    label: 'Role',
    minWidth: 200,
  },
];


const useStyles = makeStyles(theme => ({
  root: {
    display: 'flex',
    flexGrow: 1,
    flexWrap: 'wrap',
    width: '100%',
    // borderWidth: '1px', borderStyle: 'solid', borderColor: 'red',
    position: 'relative',
    marginBottom: theme.spacing(2),


    rContainer: {
      height: '15%',
      width: '100%',
      position: 'relative',
      display: 'flex',
      flexGrow: 1,
      flexWrap: 'wrap',
      // borderWidth: '1px', borderStyle: 'solid', borderColor: 'white',
    },
    '& .MuiTableContainer-root': {
      width: "100%",
      position: 'relative',
      display: 'flex',
      flexGrow: 1,
      flexWrap: 'wrap',
      // borderWidth: '1px', borderStyle: 'solid', borderColor: 'blue',
    },

  },

}));


const baseUrl = 'https://73svw35tt1.execute-api.us-east-1.amazonaws.com/prod';


export default function StickyHeadTable({liftTableState}) {
  const classes = useStyles();
  const [page, setPage] = React.useState(0);
  const [rowsPerPage, setRowsPerPage] = React.useState(10);

  const handleChangePage = (event, newPage) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event) => {
    setRowsPerPage(+event.target.value);
    setPage(0);
  };

  const [rows, setTableData] = React.useState([]);
  const [tableChanges, setTableChanges] = React.useState(0)
  const [open, setOpen] = React.useState(false);
  const [rowData, setRowData] = React.useState([]);

  const handleClose = () => {
    setOpen(false);
  };

  const handleRowClick = (rowData) => {
    setOpen(true);
    setRowData(rowData);
  }

  // Gets the list of qna data
  React.useEffect(() => {
    function fetchData() {
      fetch(baseUrl, {
        method: "get",
        headers: { "Content-Type": "application/json" },
        // body: JSON.stringify(postData),
      })
        .then((response) => response.json())
        .then((returnData) => {

          let qnaData = returnData.data;
          setTableData(qnaData);

          liftTableState({
            tableChanges: tableChanges, 
            setTableChanges: setTableChanges,
          })

        });
    }

    fetchData();

  }, [tableChanges]);





  return (

    <Paper className={classes.root}>

      <TableContainer >
        <Table stickyHeader aria-label="sticky table">
          <TableHead>
            <TableRow>
              {columns.map((column) => (
                <TableCell
                  key={`tc-${column.id}`}
                  align={column.align}
                  style={{ minWidth: column.minWidth }}
                >
                  {column.label}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage).map((row, index) => {
              return (
                <TableRow key={`tr-${index}`} onClick={() => (handleRowClick(row))} hover role="checkbox" tabIndex={-1}>
                  {columns.map((column) => {
                    const value = row[column.id];
                    return (
                      <TableCell key={column.id} align={column.align} >
                        {column.format && typeof value === 'number' ? column.format(value) : value}
                      </TableCell>
                    );
                  })}
                </TableRow>
              );
            })}
          </TableBody>
        </Table>

        <div className={classes.rContainer}>

          <TablePagination
            rowsPerPageOptions={[10, 25, 100, 500, 1000]}
            component="div"
            count={rows.length}
            rowsPerPage={rowsPerPage}
            page={page}
            onChangePage={handleChangePage}
            onChangeRowsPerPage={handleChangeRowsPerPage}
          />

        </div>

      </TableContainer>

      <EditFaceDialog setTableData={setTableData} tableChanges={tableChanges} setTableChanges={setTableChanges} open={open} handleClose={handleClose} rowData={rowData} />


    </Paper>

  );
}