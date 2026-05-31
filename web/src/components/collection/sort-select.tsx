import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

interface SortSelectProps<T extends string> {
  value: T
  /** Map of sort key → human label, rendered as the dropdown options. */
  options: Record<T, string>
  onChange: (value: T) => void
}

/**
 * Compact "Sort by" dropdown shared by the collection grids. Rendered inline on
 * the secondary tab-bar row so sorting lines up with the sub-tabs.
 */
export function SortSelect<T extends string>({
  value,
  options,
  onChange,
}: SortSelectProps<T>) {
  return (
    <div className="flex shrink-0 items-center gap-2">
      <span className="text-xs text-muted-foreground">Sort by</span>
      <Select value={value} onValueChange={(v) => onChange(v as T)}>
        <SelectTrigger size="sm" className="h-8 w-[150px] text-xs">
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {(Object.keys(options) as Array<T>).map((key) => (
            <SelectItem key={key} value={key}>
              {options[key]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  )
}
