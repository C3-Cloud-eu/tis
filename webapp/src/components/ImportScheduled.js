import React, { Component } from 'react';
import {Row, Col, Button, Alert, Table, Popconfirm} from 'antd';
import axios from 'axios';
import moment from 'moment';
import {errorMsg, RepeatOption} from './common';

class ImportScheduled extends Component {
  constructor(props) {
    super(props);
    this.state = { 
      scheduled: [],
      error: null,
      loading: false,
    };
    this.columns = [
      {
        title: 'Task',
        dataIndex: 'task',
        key: 'task',
      },
      {
        title: 'Parameters',
        dataIndex: 'parameters',
        key: 'parameters',
        width: 300,
        render: column => <p>{JSON.stringify(column, null, 2)}</p>,
      },  
      {
        title: 'State',
        dataIndex: 'state',
        key: 'state',
        render: state => {
          let color = null; 
          if (state === "Active") color = "limegreen";
          else if (state === "Stopped") color = "red";
          else if (state === "Complete") color = "darkgrey";
          return color ? <span style={{color}}>{state}</span> : state
        }
      }, 
      {
        title: 'Created',
        dataIndex: 'created',
        key: 'created',
        width: 120,
        render: column => moment(column).format('YYYY-MM-DD H:mm'),
      }, 
      {
        title: 'Start',
        dataIndex: 'start',
        key: 'start',
        width: 120,
        render: column => moment(column).format('YYYY-MM-DD H:mm'),
      }, 
      {
        title: 'Repeat',
        dataIndex: 'repeat',
        key: 'repeat',
        render: column => RepeatOption[column],
      }, 
      {
        title: 'Until',
        dataIndex: 'until',
        key: 'until',
        width: 120,
        render: column => column ? moment(column).format('YYYY-MM-DD H:mm') : '',
      }, 
      {
        title: 'Number of Executions',
        dataIndex: 'executionCount',
        key: 'execution-count',
        width: 120,
      }, 
      {
        title: 'Last Execution',
        dataIndex: 'lastExecution',
        key: 'last-execution',
        width: 120,
        render: column => column ? moment(column).format('YYYY-MM-DD H:mm') : '',
      }, 
      {
        title: '',
        key: 'action',
        render: schedule => {
          const active = schedule['state'] === 'Active';
          return (
            <div>
              <Button type="primary" disabled={!active} onClick={() => this.stop(schedule)}>Stop</Button>
              <Popconfirm title="Confirm delete?" onConfirm={() => this.delete(schedule)}>
                <Button type="primary" style={{marginLeft: "5px"}}>Delete</Button>
              </Popconfirm>
            </div>
          )
        }
      }];
  }

  componentDidMount() {
    this.load();
  }

  load() {
    this.setState({error: null, loading: true});
    axios("/api/task/schedule")
      .then(response => this.setState({scheduled: response.data, loading: false}))
      .catch(error => this.setState({error: errorMsg(error), loading: false}));
  }

  stop(schedule) {
    axios.put(`/api/task/schedule/stop/${schedule.id}`)
      .then(response => {
          const index = this.state.scheduled.findIndex(e => e.id === schedule.id);
          const scheduled = this.state.scheduled.filter(e => e !== schedule);
          scheduled.splice(index, 0, response.data);
          this.setState({loading: false, scheduled});
       })
      .catch(error => this.setState({ loading: false, error: errorMsg(error) }));
    this.setState({error: null, loading: true});
  }

  delete(schedule) {
    axios.delete(`/api/task/schedule/${schedule.id}`)
      .then(response => {
          const scheduled = this.state.scheduled.filter(e => e !== schedule);
          this.setState({ loading: false, scheduled });
       })
      .catch(error => this.setState({ loading: false, error: errorMsg(error) }));
    this.setState({error: null, loading: true});
  }

  render() {
    const {error, scheduled, loading} = this.state;
    return  (
      <Row>
        <Col>
          <h2 style={{ marginBottom: "20px" }}>Scheduled Import Task</h2>
          {
            error && <Alert style={{ marginBottom: "15px" }} message={String(error)} type="error" />
          }
          <Table columns={this.columns} dataSource={scheduled} rowKey="id" bordered loading={loading} />
        </Col>
      </Row>
    )
  }
}

export default ImportScheduled; 