const SCAFFOLD_SESSION_KEY = "tellpal.cms.scaffold-session"

export function hasScaffoldSession() {
  return window.localStorage.getItem(SCAFFOLD_SESSION_KEY) === "active"
}

export function enableScaffoldSession() {
  window.localStorage.setItem(SCAFFOLD_SESSION_KEY, "active")
}

export function clearScaffoldSession() {
  window.localStorage.removeItem(SCAFFOLD_SESSION_KEY)
}
