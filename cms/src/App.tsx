import './App.css'

function App() {
  return (
    <main className="app-shell">
      <section className="hero">
        <p className="eyebrow">TellPal CMS</p>
        <h1>Workspace bootstrap complete</h1>
        <p className="summary">
          The React, TypeScript, and Vite foundation is in place. Next tasks
          will add routing, app providers, Tailwind, and the shared admin shell.
        </p>
      </section>

      <section className="checklist" aria-label="Current setup">
        <article className="card">
          <h2>Included now</h2>
          <ul>
            <li>React 19 entrypoint</li>
            <li>TypeScript project references</li>
            <li>Vite build and dev scripts</li>
            <li>ESLint scaffold</li>
          </ul>
        </article>

        <article className="card">
          <h2>Next up</h2>
          <ul>
            <li>Tailwind CSS setup</li>
            <li>shadcn/ui primitives</li>
            <li>Route skeleton</li>
            <li>App providers</li>
          </ul>
        </article>
      </section>
    </main>
  )
}

export default App
