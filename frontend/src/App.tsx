import { Button } from '@/components/ui/button'

function App() {
  return (
    <div className="min-h-svh flex flex-col items-center justify-center gap-6 p-8">
      <div className="text-center space-y-2">
        <h1 className="text-3xl font-semibold tracking-tight">SupportFlow</h1>
        <p className="text-muted-foreground">
          Per-project issue &amp; documentation hub
        </p>
      </div>
      <Button>Get started</Button>
    </div>
  )
}

export default App
