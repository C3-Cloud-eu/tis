import React from 'react';
import { Layout, Menu, Row, Col, Icon } from 'antd';
import { withRouter, NavLink, Switch, Route } from 'react-router-dom';
import logo from '../c3-cloud.png';
import './App.css';
import Home from './Home';
import ImportTask from './ImportDefined';
import ImportScheduled from './ImportScheduled';
import ImportExecution from './ImportExecution';
import Patient from './Patient';

const App = withRouter(props => {
    const { Header, Content } = Layout;
    return (
      <Layout className="layout" style={{ backgroundColor: "transparent" }}>
        <Header style={{ backgroundColor: "#2185d0" }}>
          <Row type="flex" align="middle">
            <Col span={4} offset={4}>
              <h3 className="header-logo"><img src={logo} alt="logo" style={{ width: 50, height:50, marginRight: "10px"}}/>TIS Control Center </h3>
            </Col>
            <Col span={8}>
              <Menu className="header-menu" defaultSelectedKeys={['/']} selectedKeys={[props.location.pathname]} mode="horizontal">
                <Menu.Item className="header-menu-item" style={{borderBottom: "none"}} key="/">
                  <NavLink to="/">Home</NavLink>
                </Menu.Item>
                <Menu.SubMenu className="header-menu-item" style={{borderBottom: "none"}} title={<span>Import <Icon type="down"/></span>}>
                  <Menu.Item className="header-submenu-item" key="/import/task"><NavLink to="/import/task">Defined Task</NavLink></Menu.Item>
                  <Menu.Item className="header-submenu-item" key="/import/scheduled"><NavLink to="/import/scheduled">Scheduled Task</NavLink></Menu.Item>
                  <Menu.Item className="header-submenu-item" key="/import/execution"><NavLink to="/import/execution">Execution Log</NavLink></Menu.Item>
                </Menu.SubMenu>
                <Menu.Item className="header-menu-item" style = {{borderBottom: "none"}} key="/patient">
                  <NavLink to="/patient">Patient</NavLink>
                </Menu.Item>
              </Menu>
            </Col>
          </Row>
        </Header>
        <Content style={{ marginTop: "40px" }}>
          <Row>
            <Col span={20} offset={2}>
              <Switch>
                <Route exact path="/" component={Home} />
                <Route path="/import/task" component={ImportTask} />
                <Route path="/import/scheduled" component={ImportScheduled} />
                <Route path="/import/execution" component={ImportExecution} />
                <Route path="/patient" component={Patient} />
              </Switch>
            </Col>
          </Row>
        </Content>
      </Layout>
    )
  }
)
export default App;
