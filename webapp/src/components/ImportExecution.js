import React, { Component } from 'react';
import {Row, Col, Button, Alert, Table, Popconfirm} from 'antd';
import axios from 'axios';
import moment from 'moment';
import {errorMsg} from './common';

class ImportExecution extends Component {
  constructor(props) {
    super(props);
    this.state = { 
      executions: [],
      error: null,
      loading: false,
    }
    this.columns = [
      {
        title: 'ID',
        dataIndex: 'id',
        key: 'id',
      },
      {
        title: 'Task',
        dataIndex: 'task',
        key: 'task',
      }, 
      {
        title: 'State',
        dataIndex: 'state',
        key: 'state',
        render: state => {
          let color = null; 
          if (state === "Success") color = "limegreen";
          else if (state === "Error") color = "red";
          else if (state === "Running") color = "orange";
          else if (state === "Cancelled") color = "darkgrey";
          return color ? <span style={{color}}>{state}</span> : state
        }
      }, 
      {
        title: 'Start Time',
        dataIndex: 'startTime',
        key: 'start-time',
        render: column => moment(column).format('YYYY-MM-DD H:mm:ss'),
      }, 
      {
        title: 'End Time',
        dataIndex: 'endTime',
        key: 'end-time',
        render: column => column && moment(column).format('YYYY-MM-DD H:mm:ss'),
      }, 
      {
        title: 'Details',
        dataIndex: 'details',
        key: 'details',
        width: 500,
        render: column => <p>{JSON.stringify(column, null, 2)}</p>,
      }, 
      {
        title: '',
        key: 'action',
        render: execution => (
          <Popconfirm title="Confirm delete?" onConfirm={() => this.delete(execution)}>
            <Button type="primary">Delete</Button>
          </Popconfirm>
        )
      }];
      this.deleteAll = this.deleteAll.bind(this);
  }

  componentDidMount() {
    this.load();
  }

  load() {
    this.setState({error: null, loading: true});
    axios("/api/task/execution")
      .then(response => this.setState({executions: response.data, loading: false}))
      .catch(error => this.setState({error: errorMsg(error), loading: false}));
  }

  delete(execution) {
    axios.delete(`/api/task/execution/${execution.id}`)
      .then(response => {
          const executions = this.state.executions.filter(e => e !== execution);
          this.setState({ loading: false, executions });
       })
      .catch(error => this.setState({ loading: false, error: errorMsg(error) }));
    this.setState({error: null, loading: true});
  }

  deleteAll() {
    axios.delete('/api/task/execution')
      .then(response => this.setState({ loading: false, executions:[] }))
      .catch(error => this.setState({ loading: false, error: errorMsg(error) }));
    this.setState({error: null, loading: true});
  }

  render() {
    const {error, executions, loading} = this.state;
    return  (
      <Row>
        <Col>
          <h2 style={{ marginBottom: "20px" }}>Task Execution Log</h2>
          {
            error && <Alert style={{ marginBottom: "15px" }} message={String(error)} type="error" />
          }
          <Button disabled={!this.state.executions.length} type="primary" style={{ marginBottom: "10px" }} onClick={this.deleteAll}>Clear All</Button>
          <Table columns={this.columns} dataSource={executions} rowKey="id" bordered loading={loading} />
        </Col>
      </Row>
    )
  }
}

export default ImportExecution; 