import { useEffect, useId, useRef, useState } from 'react'

import type { MemberCard } from '@/types/member'
import {
  applyMention,
  useMentionAutocomplete,
} from '@/hooks/use-mention-autocomplete'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Textarea } from '@/components/ui/textarea'
import { cn } from '@/lib/utils'

interface MentionTextareaProps extends Omit<
  React.ComponentProps<'textarea'>,
  'value' | 'onChange'
> {
  /** Current text value (controlled). */
  value: string
  /** Called with the new plain-text value on every change or mention insertion. */
  onChange: (value: string) => void
}

/**
 * A controlled textarea that suggests `@username` mentions as the user types.
 *
 * It wraps the shared {@link Textarea}, keeps the value as plain text (the
 * backend parses `@pseudo` on save), and shows a suggestion popover anchored
 * below the field. Selecting a suggestion inserts `@pseudo ` at the caret.
 */
export function MentionTextarea({
  value,
  onChange,
  onKeyDown,
  onBlur,
  className,
  ...props
}: MentionTextareaProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const pendingCaret = useRef<number | null>(null)
  const [caret, setCaret] = useState(0)
  const [highlighted, setHighlighted] = useState(0)
  const [dismissed, setDismissed] = useState(false)
  const listboxId = useId()

  const { mention, suggestions } = useMentionAutocomplete(value, caret)
  const open = !dismissed && mention !== null && suggestions.length > 0
  const activeQuery = mention?.query ?? null

  const getTextarea = (): HTMLTextAreaElement | null =>
    containerRef.current?.querySelector('textarea') ?? null

  // Reset the highlighted row whenever the suggestion set changes.
  useEffect(() => {
    setHighlighted(0)
  }, [activeQuery, suggestions.length])

  // Restore the caret after a controlled value update from a selection.
  useEffect(() => {
    if (pendingCaret.current !== null) {
      const pos = pendingCaret.current
      pendingCaret.current = null
      const textarea = getTextarea()
      if (textarea) {
        textarea.setSelectionRange(pos, pos)
        setCaret(pos)
      }
    }
  }, [value])

  const selectSuggestion = (member: MemberCard) => {
    if (!mention) {
      return
    }
    const result = applyMention(value, mention, member.pseudo)
    pendingCaret.current = result.caret
    onChange(result.value)
    setDismissed(false)
    getTextarea()?.focus()
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (open) {
      if (e.key === 'ArrowDown') {
        e.preventDefault()
        setHighlighted((i) => (i + 1) % suggestions.length)
        return
      }
      if (e.key === 'ArrowUp') {
        e.preventDefault()
        setHighlighted((i) => (i - 1 + suggestions.length) % suggestions.length)
        return
      }
      if (e.key === 'Enter' || e.key === 'Tab') {
        e.preventDefault()
        const index = Math.min(highlighted, suggestions.length - 1)
        selectSuggestion(suggestions[index])
        return
      }
      if (e.key === 'Escape') {
        e.preventDefault()
        setDismissed(true)
        return
      }
    }
    onKeyDown?.(e)
  }

  return (
    <div ref={containerRef} className="relative">
      <Textarea
        {...props}
        className={className}
        value={value}
        role="combobox"
        aria-expanded={open}
        aria-controls={open ? listboxId : undefined}
        aria-activedescendant={open ? `${listboxId}-${highlighted}` : undefined}
        onChange={(e) => {
          setDismissed(false)
          setCaret(e.target.selectionStart)
          onChange(e.target.value)
        }}
        onKeyUp={(e) => setCaret(e.currentTarget.selectionStart)}
        onClick={(e) => setCaret(e.currentTarget.selectionStart)}
        onKeyDown={handleKeyDown}
        onBlur={(e) => {
          setDismissed(true)
          onBlur?.(e)
        }}
      />
      {open && (
        <ul
          id={listboxId}
          role="listbox"
          className="absolute inset-x-0 top-full z-50 mt-1 max-h-56 overflow-auto rounded-md border bg-popover p-1 text-popover-foreground shadow-md"
        >
          {suggestions.map((member, index) => (
            <li
              key={member.id}
              id={`${listboxId}-${index}`}
              role="option"
              aria-selected={index === highlighted}
              className={cn(
                'flex cursor-pointer items-center gap-2 rounded-sm px-2 py-1.5 text-sm',
                index === highlighted && 'bg-accent text-accent-foreground',
              )}
              onMouseDown={(e) => {
                e.preventDefault()
                selectSuggestion(member)
              }}
              onMouseEnter={() => setHighlighted(index)}
            >
              <Avatar className="size-6">
                <AvatarImage
                  src={member.picture ?? undefined}
                  alt={member.pseudo}
                />
                <AvatarFallback className="text-[10px]">
                  {member.pseudo.substring(0, 2).toUpperCase()}
                </AvatarFallback>
              </Avatar>
              <span className="truncate">@{member.pseudo}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
