import React, { Component } from 'react';
import axios from 'axios';
import ListTask from './ListTask';
import ExecuteTask from './ExecuteTask';
import ScheduleTask from './ScheduleTask';

class ImportTask extends Component {
  constructor(props) {
    super(props);
    this.state = {
      tasks: [],
      error: null,
      loading: false,
      action: 'list',
      task: null
    };
  }

  componentDidMount() {
    this.setState({error: null, loading: true});
    axios("/api/task/import")
      .then(result => this.setState({tasks: result.data, loading: false}))
      .catch(error => this.setState({error, loading: false }));
  }

  render() {
    switch(this.state.action) {
      case 'list': return <ListTask {...this.state}
                            header={'Defined Data Import Task'} 
                            onExecuteClick ={ task => this.setState({ task, action: 'execute' }) }
                            onScheduleClick={ task => this.setState({ task, action: 'schedule' }) } />
      case 'execute':  return <ExecuteTask task={this.state.task} 
                                onSubmit={() => this.props.history.push('/import/execution')}
                                onCancel={() => this.setState({action: 'list' })}
                              />                      
      case 'schedule': return <ScheduleTask task={this.state.task} 
                                onSubmit={() => this.props.history.push('/import/scheduled')}
                                onCancel={() => this.setState({action: 'list'})} 
                              />
      default: return null
    }
  }
}

export default ImportTask;