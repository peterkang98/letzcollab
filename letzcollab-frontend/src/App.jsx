import "./App.css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import PrivateRoute from "./routes/PrivateRoute.jsx";
import MainLayout from "./layouts/MainLayout.jsx";
import Login from "./pages/Login";
import AuthLayout from "./layouts/AuthLayout.jsx";
import PublicRoute from "./routes/PublicRoute.jsx";
import Signup from "./pages/Signup.jsx";
import VerifyEmail from "./pages/VerifyEmail.jsx";
import RequestPasswordReset from "./pages/RequestPasswordReset.jsx";
import ResetPassword from "./pages/ResetPassword.jsx";
import Dashboard from "./pages/Dashboard.jsx";
import TaskDetailPage from "./pages/TaskDetailPage.jsx";
import WorkspaceSettingsPage from "./pages/WorkspaceSettingsPage.jsx";
import AcceptInvitationPage from "./pages/AcceptInvitationPage.jsx";
import MyPage from "./pages/MyPage.jsx";
import ProjectDetailPage from "./pages/ProjectDetailPage.jsx";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 30,        // 30초로 낮춤: 협업툴 특성상 타인이 데이터를 바꿀 수 있음
      gcTime: 1000 * 60 * 10,      // 언마운트 후 10분 뒤 메모리에서 제거
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

function App() {
  return (
    <>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <Routes>
            {/* 로그인 여부와 무관하게 접근 가능 */}
            <Route path="/workspaces/verify-invitation-email" element={<AcceptInvitationPage />} />

            {/* 인증이 필요한 페이지들의 레이아웃과 라우팅 설정 */}
            <Route element={<PrivateRoute/>}>
              <Route element={<MainLayout/>}>
                <Route path="/" element={<Dashboard/>}/>
                <Route path="/me" element={<MyPage />} />
                <Route path="/workspaces/:workspacePublicId/projects/:projectPublicId" element={<ProjectDetailPage />} />
                <Route path="/projects/:projectPublicId/tasks/:taskPublicId" element={<TaskDetailPage />} />
                <Route path="/workspaces/:workspacePublicId/settings" element={<WorkspaceSettingsPage />} />
              </Route>
            </Route>

            {/* 인증이 필요 없는 페이지들의 레이아웃과 라우팅 설정 (로그인한 사람들은 접속 불가) */}
            <Route element={<PublicRoute/>}>
              <Route element={<AuthLayout/>}>
                <Route path="/auth/login" element={<Login/>}/>
                <Route path="/auth/signup" element={<Signup/>}/>
                <Route path="/auth/verify-email" element={<VerifyEmail/>}/>
                <Route path="/auth/password/reset-request" element={<RequestPasswordReset/>}/>
                <Route path="/auth/password/reset" element={<ResetPassword/>}/>
              </Route>
            </Route>

            {/* 잘못된 경로 접속 시 홈으로 리다이렉트 */}
            <Route path="*" element={<Navigate to="/"/>}/>
          </Routes>
        </BrowserRouter>
      </QueryClientProvider>
    </>
  );
}

export default App;
