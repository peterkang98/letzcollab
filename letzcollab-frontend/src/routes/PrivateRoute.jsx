import { Navigate, Outlet } from "react-router-dom";

export default function PrivateRoute() {
  const user = localStorage.getItem('user');

  return user ? <Outlet/> : <Navigate to="/auth/login" replace/>
};