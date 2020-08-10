import React, { Component } from 'react';
import {Row, Col, Alert, Form, Input, DatePicker, Select, Radio, Button} from 'antd';
import axios from 'axios';
import moment from 'moment';
import {errorMsg, RepeatOption} from './common';

const Option = Select.Option;

class ScheduleTaskForm extends Component {
  constructor(props) {
    super(props)
    this.state = { 
      error: null,
      loading: false,
      hasEndTime: false,
      allPatients: false,
    }
    this.submit = this.submit.bind(this);
    this.validateStartTime = this.validateStartTime.bind(this);
    this.validateEndTime = this.validateEndTime.bind(this);
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
      for (var k in task.parameters) {
        parameters[k] = values[k].trim();
      }
      form.setFieldsValue(parameters);
      if (task.parameters.hasOwnProperty("patient") && allPatients) parameters["patient"] = "ALL";
      const start = values['startTime'];
      start.millisecond(0);
      start.seconds(0);
      const schedule = {
        task: task.id, 
        parameters,
        start,
        repeat: values['repeat'],
      };
      if (this.state.hasEndTime) {
        const until = values['endTime'];
        until.millisecond(0);
        until.seconds(0);
        schedule['until'] = until;
      }
      axios.post("/api/task/schedule", schedule)
        .then(response => {
          this.setState({ loading: false, error: null });
          onSubmit();
        })
        .catch(error => {console.log(error.response); this.setState({ loading: false, error: errorMsg(error) })});
      this.setState({ loading: true });
    });
  }

  validateStartTime(rule, startTime, callback) {
    if (startTime && startTime.isBefore(moment())) callback('Start time must be in the future');
    else callback();
  }

  validateEndTime(rule, endTime, callback) {
    const { getFieldValue } = this.props.form;
    const startTime = getFieldValue('startTime');
    if (startTime && endTime && !startTime.isBefore(endTime)) callback('End time must be later than start time');
    else callback();
  }

  render() {
    const {task, form, onCancel} = this.props;
    const {getFieldDecorator, getFieldValue} = form;
    const {error, loading, allPatients, hasEndTime} = this.state;
    const repeat = getFieldValue('repeat') !== 'none';
    const radioStyle = {
      display: 'block',
      height: '30px',
      lineHeight: '30px',
    };
    return (
      <Row>
        <Col span={12} offset={6}>
          <h2>Schedule Task</h2>
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
            <Form.Item label="Start time">
              {getFieldDecorator('startTime', {rules: [{required: true, message: 'Please choose start time'}, {validator: this.validateStartTime}]})(
                  <DatePicker showToday={false} showTime={{format: 'HH:mm', minuteStep: 15}} format="YYYY-MM-DD HH:mm" />
              )}
            </Form.Item>
            <Form.Item label="Repeat">
              {getFieldDecorator('repeat', { initialValue: "none" })(
                  <Select style={{ width: 150 }}>
                    {Object.keys(RepeatOption).map(k => <Option key={k} value={k}>{RepeatOption[k]}</Option>)}
                  </Select>
              )}
            </Form.Item>
            {repeat && <Form.Item label="Until">
              <Radio.Group value={this.state.hasEndTime} onChange={(e) => this.setState({hasEndTime: e.target.value})}>
                <Radio style={radioStyle} value={false}>Manually stopped</Radio>
                <Radio style={radioStyle} value={true}>End time</Radio>
              </Radio.Group>
              {hasEndTime && getFieldDecorator('endTime', {rules: [{required: true, message: 'Please choose end time'}, {validator: this.validateEndTime}]})(
                  <DatePicker showToday={false} showTime={{format: 'HH:mm', minuteStep: 15}} format="YYYY-MM-DD HH:mm" />
              )}
            </Form.Item>}
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

const ScheduleTask = Form.create()(ScheduleTaskForm);

export default ScheduleTask;