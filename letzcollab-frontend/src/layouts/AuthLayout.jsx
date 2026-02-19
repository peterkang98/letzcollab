import { Col, Row } from "antd";
import { Outlet } from "react-router-dom";
import Logo from "../components/Logo.jsx";

export default function AuthLayout() {
  return (
    <main>
      <Row justify="center" align="middle" style={{ minHeight: '90vh' }}>
        <Col xs={22} sm={18} md={12} lg={9} xl={7} style={{ maxWidth: "420px", width: "100%" }}>
          <Logo/>
          <Outlet/>
        </Col>
      </Row>
    </main>
  );
};