import { Navigate, Outlet } from "react-router-dom";

export default function PublicRoute() {
  const user = localStorage.getItem('user');

  return user ? <Navigate to="/" replace/> : <Outlet/>;
};