import { Form, Input, Typography } from 'antd';
const { Text } = Typography;

export default function AuthFormInput({ name, label, icon: Icon, isPassword = false, rules = [], placeholder, ...rest }) {

  const InputComponent = isPassword ? Input.Password : Input;

  return (
    <Form.Item name={name} label={<Text strong>{label}</Text>} rules={rules} {...rest}>
      <InputComponent
        prefix={<Icon style={{ color: '#bfbfbf', marginRight: 4 }}/>}
        placeholder={placeholder}
        style={{ backgroundColor: '#f5f5f5' }}
      />
    </Form.Item>
  );
};