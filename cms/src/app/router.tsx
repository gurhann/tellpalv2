import { Navigate, Outlet, createBrowserRouter, useLocation } from "react-router-dom"

import { AppShell } from "@/components/layout/app-shell"
import { hasScaffoldSession } from "@/app/scaffold-session"
import { CategoriesIndexRoute } from "@/app/routes/categories-index"
import { CategoryDetailRoute } from "@/app/routes/category-detail"
import { ContentDetailRoute } from "@/app/routes/content-detail"
import { ContentsIndexRoute } from "@/app/routes/contents-index"
import { ContributorsRoute } from "@/app/routes/contributors"
import { FreeAccessRoute } from "@/app/routes/free-access"
import { LoginRoute } from "@/app/routes/login"
import { MediaProcessingRoute } from "@/app/routes/media-processing"
import { MediaRoute } from "@/app/routes/media"
import { NotFoundRoute } from "@/app/routes/not-found"
import { StoryPagesRoute } from "@/app/routes/story-pages"

function RootRedirect() {
  return <Navigate replace to={hasScaffoldSession() ? "/contents" : "/login"} />
}

function RequireScaffoldSession() {
  const location = useLocation()

  if (!hasScaffoldSession()) {
    return (
      <Navigate
        replace
        to="/login"
        state={{ from: `${location.pathname}${location.search}${location.hash}` }}
      />
    )
  }

  return <AppShell />
}

function LoginRedirect() {
  if (hasScaffoldSession()) {
    return <Navigate replace to="/contents" />
  }

  return <LoginRoute />
}

function ProtectedOutlet() {
  return <Outlet />
}

export const router = createBrowserRouter([
  {
    path: "/",
    element: <RootRedirect />,
  },
  {
    path: "/login",
    element: <LoginRedirect />,
  },
  {
    element: <RequireScaffoldSession />,
    children: [
      {
        element: <ProtectedOutlet />,
        children: [
          { path: "/contents", element: <ContentsIndexRoute /> },
          { path: "/contents/:contentId", element: <ContentDetailRoute /> },
          {
            path: "/contents/:contentId/story-pages",
            element: <StoryPagesRoute />,
          },
          { path: "/categories", element: <CategoriesIndexRoute /> },
          { path: "/categories/:categoryId", element: <CategoryDetailRoute /> },
          { path: "/media", element: <MediaRoute /> },
          { path: "/media-processing", element: <MediaProcessingRoute /> },
          { path: "/contributors", element: <ContributorsRoute /> },
          { path: "/free-access", element: <FreeAccessRoute /> },
        ],
      },
    ],
  },
  {
    path: "*",
    element: <NotFoundRoute />,
  },
])
