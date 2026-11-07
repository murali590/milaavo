import { Link, type LinkProps } from 'react-router'
import { buttonClasses, type ButtonVariant } from './buttonStyles'

type LinkButtonProps = LinkProps & {
  variant?: ButtonVariant
  fullWidth?: boolean
}

// A real link (opens in a new tab, read as "link") that looks like a Button.
export function LinkButton({ variant = 'primary', fullWidth = false, className, ...rest }: LinkButtonProps) {
  return <Link className={buttonClasses(variant, fullWidth, className)} {...rest} />
}