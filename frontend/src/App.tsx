export default function App() {
  return (
    <main className="min-h-screen p-4 sm:p-8">
      <div className="mx-auto max-w-md space-y-6">
        <header>
          <h1 className="text-2xl font-semibold">Milaavo</h1>
          <p className="text-sm text-ink-muted">Theme preview</p>
        </header>

        <section className="rounded-card border border-line bg-surface p-4 shadow-sm space-y-3">
          <h2 className="font-medium">Buttons</h2>
          <button className="h-11 w-full rounded-lg bg-brand px-4 font-medium text-on-brand hover:bg-brand-hover">
            Report an item
          </button>
          <button className="h-11 w-full rounded-lg border border-line bg-surface px-4 font-medium text-ink hover:bg-surface-muted">
            Cancel
          </button>
          <a href="#" className="block text-sm font-medium text-brand hover:underline">
            Forgot password?
          </a>
        </section>

        <section className="rounded-card border border-line bg-surface p-4 shadow-sm space-y-3">
          <h2 className="font-medium">Input</h2>
          <label className="block text-sm font-medium" htmlFor="mobile">Mobile number</label>
          <input
            id="mobile"
            className="h-11 w-full rounded-lg border border-line bg-surface px-3 text-ink placeholder:text-ink-muted"
            placeholder="+91 98765 43210"
          />
          <p className="text-sm text-danger">Mobile number is required</p>
        </section>

        <section className="rounded-card border border-line bg-surface p-4 shadow-sm space-y-3">
          <h2 className="font-medium">Report types</h2>
          <div className="flex gap-2">
            <span className="rounded-full bg-lost-soft px-3 py-1 text-sm font-medium text-lost">Lost</span>
            <span className="rounded-full bg-found-soft px-3 py-1 text-sm font-medium text-found">Found</span>
          </div>
        </section>

        <section className="space-y-2">
          <p className="rounded-lg bg-success-soft p-3 text-sm text-success">✅ Report created</p>
          <p className="rounded-lg bg-warning-soft p-3 text-sm text-warning">⚠️ OTP expires in 1 minute</p>
          <p className="rounded-lg bg-danger-soft p-3 text-sm text-danger">❌ Invalid OTP</p>
        </section>
      </div>
    </main>
  )
}