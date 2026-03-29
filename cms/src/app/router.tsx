import {
  Navigate,
  Outlet,
  createBrowserRouter,
  type RouteObject,
  useLocation,
} from "react-router-dom";

import { AppShell } from "@/components/layout/app-shell";
import { CategoriesIndexRoute } from "@/app/routes/categories-index";
import { CategoryDetailRoute } from "@/app/routes/category-detail";
import { ContentDetailRoute } from "@/app/routes/contents/detail";
import { ContentsIndexRoute } from "@/app/routes/contents";
import { ContributorsRoute } from "@/app/routes/contributors";
import { FreeAccessRoute } from "@/app/routes/free-access";
import { LoginRoute } from "@/app/routes/login";
import { MediaProcessingRoute } from "@/app/routes/media-processing";
import { MediaRoute } from "@/app/routes/media";
import { NotFoundRoute } from "@/app/routes/not-found";
import { StoryPagesRoute } from "@/app/routes/story-pages";
import { AuthLoadingScreen } from "@/features/auth/components/auth-loading-screen";
import { useAuth } from "@/features/auth/providers/use-auth";

function RootRedirect() {
  const auth = useAuth();

  if (auth.session) {
    return <Navigate replace to="/contents" />;
  }

  if (!auth.isBootstrapped || auth.status === "bootstrapping") {
    return <AuthLoadingScreen />;
  }

  return <Navigate replace to="/login" />;
}

function RequireAuthenticatedSession() {
  const auth = useAuth();
  const location = useLocation();

  if (auth.session) {
    return <AppShell />;
  }

  if (!auth.isBootstrapped || auth.status === "bootstrapping") {
    return (
      <AuthLoadingScreen description="The CMS is restoring access before protected routes are rendered." />
    );
  }

  return (
    <Navigate
      replace
      to="/login"
      state={{
        from: `${location.pathname}${location.search}${location.hash}`,
      }}
    />
  );
}

function LoginRedirect() {
  const auth = useAuth();

  if (auth.session) {
    return <Navigate replace to="/contents" />;
  }

  if (!auth.isBootstrapped || auth.status === "bootstrapping") {
    return (
      <AuthLoadingScreen description="The CMS is checking whether an existing refresh token can restore your session." />
    );
  }

  return <LoginRoute />;
}

function ProtectedOutlet() {
  return <Outlet />;
}

export const cmsRoutes: RouteObject[] = [
  {
    path: "/",
    element: <RootRedirect />,
  },
  {
    path: "/login",
    element: <LoginRedirect />,
  },
  {
    element: <RequireAuthenticatedSession />,
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
];

export const router = createBrowserRouter(cmsRoutes);
