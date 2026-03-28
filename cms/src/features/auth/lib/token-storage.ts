const REFRESH_TOKEN_KEY = "tellpal.cms.refresh-token";

function canUseLocalStorage() {
  return (
    typeof window !== "undefined" && typeof window.localStorage !== "undefined"
  );
}

export const tokenStorage = {
  getRefreshToken() {
    if (!canUseLocalStorage()) {
      return null;
    }

    return window.localStorage.getItem(REFRESH_TOKEN_KEY);
  },
  setRefreshToken(refreshToken: string) {
    if (!canUseLocalStorage()) {
      return;
    }

    window.localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },
  clearRefreshToken() {
    if (!canUseLocalStorage()) {
      return;
    }

    window.localStorage.removeItem(REFRESH_TOKEN_KEY);
  },
};
