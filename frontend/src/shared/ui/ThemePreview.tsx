import { Alert, Badge, Button, Card, Spinner, TextAreaField, TextField } from './index'

export function ThemePreview() {
  return (
    <main className="min-h-screen p-4 sm:p-8">
      <div className="mx-auto max-w-md space-y-6">
        <header>
          <h1 className="text-2xl font-semibold">Milaavo</h1>
          <p className="text-sm text-ink-muted">UI components preview</p>
        </header>

        <Card className="space-y-3">
          <h2 className="font-medium">Buttons</h2>
          <Button fullWidth>Report an item</Button>
          <Button fullWidth variant="secondary">Cancel</Button>
          <Button fullWidth variant="danger">Delete report</Button>
          <Button fullWidth loading>Saving</Button>
          <Button variant="ghost">Forgot password?</Button>
        </Card>

        <Card className="space-y-4">
          <h2 className="font-medium">Form fields</h2>
          <TextField label="Mobile number" placeholder="+91 98765 43210" hint="We'll send an OTP to this number" />
          <TextField label="Password" type="password" error="Password must be at least 12 characters" />
          <TextAreaField label="Description" placeholder="Where and when did you lose it?" />
        </Card>

        <Card className="space-y-3">
          <h2 className="font-medium">Badges</h2>
          <div className="flex flex-wrap gap-2">
            <Badge tone="lost">Lost</Badge>
            <Badge tone="found">Found</Badge>
            <Badge tone="brand">Matched</Badge>
            <Badge>Closed</Badge>
          </div>
        </Card>

        <div className="space-y-2">
          <Alert tone="success">Report created</Alert>
          <Alert tone="warning">OTP expires in 1 minute</Alert>
          <Alert tone="danger" title="Invalid OTP">Please check the code and try again.</Alert>
          <Alert tone="info">We'll notify you when a match is found.</Alert>
        </div>

        <div className="flex justify-center text-brand">
          <Spinner />
        </div>
      </div>
    </main>
  )
}