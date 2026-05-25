import { REGEXP_ONLY_DIGITS } from 'input-otp'
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from '@/components/ui/input-otp'

interface TotpInputProps {
  /** Id forwarded to the underlying input, so a `<FieldLabel htmlFor>` can target it. */
  id?: string
  value: string
  onChange: (value: string) => void
  /** Fired when all 6 digits have been entered. */
  onComplete?: (value: string) => void
  onBlur?: () => void
  disabled?: boolean
  autoFocus?: boolean
  invalid?: boolean
}

/**
 * Shared 6-digit TOTP entry rendered as six separate segmented cells.
 * Used everywhere a 2FA code is requested: login challenge, 2FA setup, and 2FA disable.
 */
export function TotpInput({
  id,
  value,
  onChange,
  onComplete,
  onBlur,
  disabled,
  autoFocus,
  invalid,
}: TotpInputProps) {
  return (
    <InputOTP
      id={id}
      maxLength={6}
      value={value}
      onChange={onChange}
      onComplete={onComplete}
      onBlur={onBlur}
      disabled={disabled}
      autoFocus={autoFocus}
      pattern={REGEXP_ONLY_DIGITS}
      inputMode="numeric"
      autoComplete="one-time-code"
      aria-invalid={invalid}
      containerClassName="w-full"
    >
      <InputOTPGroup className="w-full">
        {Array.from({ length: 6 }).map((_, i) => (
          <InputOTPSlot key={i} index={i} aria-invalid={invalid} />
        ))}
      </InputOTPGroup>
    </InputOTP>
  )
}
