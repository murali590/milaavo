import { createBrowserRouter } from 'react-router'
import { AppLayout } from './AppLayout'
import { NotFoundPage } from './NotFoundPage'
import { HomeRedirect, RedirectIfAuthenticated, RequireAuth } from '../features/auth/RouteGuards'
import { LoginPage } from '../features/auth/pages/LoginPage'
import { RegisterPage } from '../features/auth/pages/RegisterPage'
import { ReportsPage } from '../features/reports/pages/ReportsPage'
import { VerifyMobilePage } from '../features/auth/pages/VerifyMobilePage'
import { NewReportPage } from '../features/reports/pages/NewReportPage'

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { index: true, element: <HomeRedirect /> },
      {
        element: <RedirectIfAuthenticated />,
        children: [
          { path: 'login', element: <LoginPage /> },
          { path: 'register', element: <RegisterPage /> },
          { path: 'verify-mobile', element: <VerifyMobilePage /> }
        ],
      },
      {
        element: <RequireAuth />,
        children: [{ path: 'reports', element: <ReportsPage /> },
          { path: 'reports/new', element: <NewReportPage /> },
        ],
      },
      { path: '*', element: <NotFoundPage /> }
    ],
  },
])