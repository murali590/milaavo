import { useEffect, useState } from 'react'

// Counts down once per second until 0. Call restart(seconds) from an event handler to start again.
export function useCountdown(initialSeconds: number) {
  const [secondsLeft, setSecondsLeft] = useState(initialSeconds)

  useEffect(() => {
    if (secondsLeft <= 0) {
      return
    }
    const timer = window.setTimeout(() => setSecondsLeft((seconds) => seconds - 1), 1000)
    return () => window.clearTimeout(timer)
  }, [secondsLeft])

  return { secondsLeft, restart: setSecondsLeft }
}