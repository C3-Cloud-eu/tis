import React, { Component } from 'react';
import {Row, Col, Alert, Form, Input, Radio, Button} from 'antd';
import axios from 'axios';
import {errorMsg} from './common';

class ExecuteTaskForm extends Component {
  constructor(props) {
    super(props)
    this.state = { 
      error: null,
      loading: false,
      allPatients: false,
    }
    this.submit = this.submit.bind(this);
  }

  componentDidMount() {
    const {form, task} = this.props;
    form.setFieldsValue(task.parameters);
  }

  submit() {
    const {form, task, onSubmit} = this.props;
    const {allPatients} = this.state;
    this.props.form.validateFields((err, values) => {
      if (err) { return }
      const parameters = {};
      for (var k in values) {
        parameters[k] = values[k].trim();
      }
      form.setFieldsValue(parameters);
      if (task.parameters.hasOwnProperty("patient") && allPatients) parameters["patient"] = "ALL";
      axios.post("/api/task/execute", { task: task.id, parameters })
        .then(response => {
          this.setState({ loading: false, error: null });
          onSubmit();
        })
        .catch(error => this.setState({ loading: false, error: errorMsg(error) }));
      this.setState({ loading: true });
    });
  }

  render() {
    const {task, form, onCancel} = this.props;
    const {getFieldDecorator} = form;
    const {error, loading, allPatients} = this.state;
    return (
      <Row>
        <Col span={12} offset={6}>
          <h2>Execute Task</h2>
          <h3 style={{ marginBottom: "30px" }}>{task.name} v{task.version}</h3>
          {
            error && <Alert style={{ marginBottom: "15px" }} message={String(error)} type="error" />
          }
          <Form layout='vertical'>
            {Object.keys(task.parameters).map(k => 
              <Form.Item key={k} label={k}>
                {k === 'patient' && <Radio.Group style={{ marginBottom: "10px" }} value={allPatients} onChange={(e) => this.setState({allPatients: e.target.value})}>
                  <Radio value={false}>Input patient ID</Radio>
                  <Radio value={true}>Select all patients</Radio>
                </Radio.Group>}
                {(k !== 'patient' || !allPatients) && getFieldDecorator(k, {rules: [{ required: true, whitespace: true, message: 'Required filed cannot be empty' }]})(
                  <Input/>
                )}
              </Form.Item>
            )}
            <Form.Item>
              <Button type="primary" loading={loading} onClick={this.submit}>Submit</Button>
              <Button style={{ marginLeft: "10px" }} onClick={onCancel}>Cancel</Button>
            </Form.Item>
          </Form>
        </Col>
      </Row>
    )
  }
}

const ExecuteTask = Form.create()(ExecuteTaskForm);

export default ExecuteTask;