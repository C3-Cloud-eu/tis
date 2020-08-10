import React, { Component } from 'react';
import {Row, Col, Button, Alert, Table, Popconfirm, Modal, Form, Icon, Input, Checkbox, Radio} from 'antd';
import axios from 'axios';
import {errorMsg} from './common';

const AddPatientForm = Form.create()(
  class extends Component {
    render() {
      const {visible, confirmLoading, error, onCancel, onSubmit, form} = this.props;
      const {getFieldDecorator} = form;
      return (
        <Modal
          visible={visible}
          confirmLoading={confirmLoading}
          title={<h3 style={{ color: "teal" }}>New Patient</h3>}
          okText="Save"
          onOk={onSubmit}
          onCancel={onCancel}
        >
          {
            error && <Alert style={{ marginBottom: "15px" }} message={String(error)} type="error" />
          }
          <Form>
            <Form.Item>
              {getFieldDecorator('id', {rules: [{ required: true, whitespace: true, message: 'Patient ID cannot be empty' }]})(
                <Input autoComplete="off" prefix={<Icon type="user" style={{ color: 'rgba(0,0,0,.25)' }} />} placeholder="Patient ID" />
              )}
            </Form.Item>
            <Form.Item>
              {getFieldDecorator('c3cloudId', {rules: [{ required: true, whitespace: true, message: 'C3-Cloud ID cannot be empty' }]})(
                <Input prefix={<Icon type="tag" style={{ color: 'rgba(0,0,0,.25)' }} />} placeholder="C3-Cloud ID" />
              )}
            </Form.Item>
            <Form.Item>
              {getFieldDecorator('email', {rules: [{ type: 'email', message: 'The input is not valid E-mail!' }]})(
                <Input prefix={<Icon type="mail" style={{ color: 'rgba(0,0,0,.25)' }} />} placeholder="Email" />
              )}
            </Form.Item>
            <Form.Item>
              {getFieldDecorator('layer3', {initialValue: false})(
                <Radio.Group>
                  <Radio value={false}>Layer 4 Group</Radio>
                  <Radio value={true}>Layer 3 Group</Radio>
                </Radio.Group>
              )}
            </Form.Item>
            <Form.Item>
              {getFieldDecorator('useMedicalDevice')(
                <Checkbox>Use Medical Device</Checkbox>
              )}
            </Form.Item>
          </Form>
        </Modal>);
    }
  }
)
  

class Patient extends Component {
  constructor(props) {
    super(props);
    this.state = { 
      patients: [],
      error: null,
      loading: false,
      addPatientModal: false,
      addPatientLoading: false,
      addPatientError: null
    }
    this.columns = [
      {
        title: 'Patient ID',
        dataIndex: 'id',
        key: 'patient-id',
      }, 
      {
        title: 'C3-Cloud ID',
        dataIndex: 'c3cloudId',
        key: 'c3cloud-id',
      }, 
      {
        title: 'Email',
        dataIndex: 'email',
        key: 'email',
      }, 
      {
        title: 'Evaluation Group',
        dataIndex: 'layer3',
        key: 'evaluation-group',
        render: layer3 => layer3 ? "Layer 3" : "Layer 4",
      }, 
      {
        title: 'Use Medical Device',
        dataIndex: 'useMedicalDevice',
        key: 'medical-device',
        render: useMedicalDevice => useMedicalDevice ? "Yes" : "No",
      }, 
      {
        title: 'Creation Date',
        dataIndex: 'dateCreated',
        key: 'date-created',
      }, 
      {
        title: 'Last Import Time',
        dataIndex: 'lastImport',
        key: 'last-import',
      }, 
      {
        title: 'Action',
        key: 'action',
        render: patient => (
          <Popconfirm title="Confirm delete?" onConfirm={() => this.delete(patient)}>
            <Button type="primary">Delete</Button>
          </Popconfirm>
        )
      }];
    this.addPatient = this.addPatient.bind(this);
  }

  componentDidMount() {
    this.load();
  }

  load() {
    this.setState({error: null, loading: true});
    axios("/api/patient")
      .then(response => this.setState({patients: response.data, loading: false}))
      .catch(error => this.setState({error: errorMsg(error), loading: false }));
  }

  addPatient() {
    const form = this.form.props.form;
    form.validateFields((err, patient) => {
      if (patient.id) {
        patient.id = patient.id.trim();
      }
      if (patient.c3cloudId) {
        patient.c3cloudId = patient.c3cloudId.trim();
      }
      if (patient.email) {
        patient.email = patient.email.trim();
      }
      form.setFieldsValue(patient)
      if (err) { return }
      axios.post("/api/patient", patient)
        .then(response => {
          form.resetFields();
          this.state.patients.unshift(response.data);
          this.setState({ addPatientLoading: false, addPatientModal: false, addPatientError: null });
        })
        .catch(error => this.setState({ addPatientLoading: false, addPatientError: errorMsg(error) }));
      this.setState({addPatientLoading: true});
    });
  }
  
  delete(patient) {
    axios.delete("/api/patient", {
      headers: {'Content-Type': 'application/json'},
      data: patient
    })
      .then(response => {
        const patients = this.state.patients.filter(e => e !== patient);
          this.setState({ loading: false, patients });
       })
      .catch(error => this.setState({ loading: false, error: errorMsg(error) }));
    this.setState({error: null, loading: true});
  }

  render() {
    const {error, patients, loading} = this.state;
    return  (
      <Row>
        <Col span={16} offset={4}>
          <h2 style={{ marginBottom: "20px" }}>Register Patient</h2>
          {
            error && <Alert style={{ marginBottom: "15px" }} message={String(error)} type="error" />
          }
          <Button type="primary" style={{ marginBottom: "10px" }} onClick={() => this.setState({addPatientModal: true})}>Add Patient</Button>
          <AddPatientForm
            wrappedComponentRef={form => this.form = form}
            visible={this.state.addPatientModal}
            confirmLoading={this.state.addPatientLoading}
            error={this.state.addPatientError}
            onCancel={() => {
              this.form.props.form.resetFields(); 
              this.setState({addPatientError: null, addPatientModal: false});
            }}
            onSubmit={this.addPatient}
          />
          <Table columns={this.columns} dataSource={patients} rowKey="id" bordered loading={loading} />
        </Col>
      </Row>
    )
  }
}

export default Patient; 