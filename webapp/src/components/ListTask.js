import React from 'react';
import { Table, Button, Alert } from 'antd';

const columns = (onExecuteClick, onScheduleClick) => [
{
  title: 'Name',
  dataIndex: 'name',
  key: 'name',
}, 
{
  title: 'Version',
  dataIndex: 'version',
  key: 'version',
}, 
{
  title: 'Date Modified',
  dataIndex: 'dateModified',
  key: 'dateModified',
}, 
{
  title: 'Description',
  dataIndex: 'description',
  key: 'description',
}, 
{
  title: 'Action',
  key: 'action',
  render: (task) => (
    <div>
      <Button type="primary" onClick={() => onExecuteClick(task)}>Execute</Button>
      <Button type="primary" style={{marginLeft: "5px"}} onClick={() => onScheduleClick(task)}>Schedule</Button>
    </div>
  ),
}];

const ListTask = ({tasks, error, loading, onExecuteClick, onScheduleClick, header}) => 
  <div>
    <h2 style={{ marginBottom: "20px" }}>{header}</h2>
    {
      error && <Alert style={{ marginBottom: "15px" }} message={String(error)} type="error" />
    }
    <Table columns={columns(onExecuteClick, onScheduleClick)} dataSource={tasks} rowKey="id" bordered loading={loading} />
  </div>

export default ListTask;