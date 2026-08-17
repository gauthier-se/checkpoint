import { useForm } from '@tanstack/react-form'
import { useQuery } from '@tanstack/react-query'
import { Loader2 } from 'lucide-react'
import { z } from 'zod'
import type { AdminGameFormValues } from '@/lib/admin-games'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { MultiSelectFilter } from '@/components/games/multi-select-filter'
import { genresQueryOptions, platformsQueryOptions } from '@/queries/catalog'
import { companiesQueryOptions } from '@/queries/admin/games'

const gameSchema = z.object({
  title: z
    .string()
    .min(1, 'Title is required')
    .max(255, 'Title must be at most 255 characters'),
  description: z.string(),
  coverUrl: z.string().max(1024, 'Cover URL must be at most 1024 characters'),
  artworkUrl: z
    .string()
    .max(1024, 'Artwork URL must be at most 1024 characters'),
  trailerYoutubeId: z
    .string()
    .max(64, 'YouTube id must be at most 64 characters'),
  releaseDate: z.string(),
  // Kept as strings so an empty input stays empty rather than becoming 0.
  timeToBeatHastily: z.string(),
  timeToBeatNormally: z.string(),
  timeToBeatCompletely: z.string(),
  genreIds: z.array(z.string()),
  platformIds: z.array(z.string()),
  companyIds: z.array(z.string()),
})

function FieldErrors({ errors }: { errors: Array<unknown> }) {
  if (errors.length === 0) return null
  return (
    <p className="text-sm text-destructive">
      {errors
        .map((error) =>
          typeof error === 'string'
            ? error
            : ((error as { message?: string }).message ?? ''),
        )
        .filter(Boolean)
        .join(', ')}
    </p>
  )
}

interface AdminGameFormProps {
  defaultValues: AdminGameFormValues
  submitLabel: string
  onSubmit: (values: AdminGameFormValues) => Promise<void>
}

export function AdminGameForm({
  defaultValues,
  submitLabel,
  onSubmit,
}: AdminGameFormProps) {
  const { data: genres = [] } = useQuery(genresQueryOptions())
  const { data: platforms = [] } = useQuery(platformsQueryOptions())
  const { data: companies = [] } = useQuery(companiesQueryOptions())

  const form = useForm({
    defaultValues,
    validators: { onSubmit: gameSchema },
    onSubmit: async ({ value }) => {
      await onSubmit(value)
    },
  })

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        void form.handleSubmit()
      }}
      className="flex flex-col gap-6"
    >
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Game details</CardTitle>
          <CardDescription>
            Only the title is required. Leave a field empty to clear it.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <FieldGroup>
            <form.Field
              name="title"
              children={(field) => (
                <Field>
                  <FieldLabel htmlFor="title">Title</FieldLabel>
                  <Input
                    id="title"
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(event) => field.handleChange(event.target.value)}
                    required
                  />
                  <FieldErrors errors={field.state.meta.errors} />
                </Field>
              )}
            />

            <form.Field
              name="description"
              children={(field) => (
                <Field>
                  <FieldLabel htmlFor="description">Description</FieldLabel>
                  <Textarea
                    id="description"
                    rows={5}
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(event) => field.handleChange(event.target.value)}
                  />
                  <FieldErrors errors={field.state.meta.errors} />
                </Field>
              )}
            />

            <form.Field
              name="releaseDate"
              children={(field) => (
                <Field>
                  <FieldLabel htmlFor="releaseDate">Release date</FieldLabel>
                  <Input
                    id="releaseDate"
                    type="date"
                    value={field.state.value}
                    onBlur={field.handleBlur}
                    onChange={(event) => field.handleChange(event.target.value)}
                  />
                  <FieldErrors errors={field.state.meta.errors} />
                </Field>
              )}
            />
          </FieldGroup>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Media</CardTitle>
          <CardDescription>
            Images are referenced by URL — the API stores links, not uploads.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <FieldGroup>
            {(
              [
                ['coverUrl', 'Cover URL'],
                ['artworkUrl', 'Artwork URL'],
                ['trailerYoutubeId', 'Trailer YouTube id'],
              ] as const
            ).map(([name, label]) => (
              <form.Field
                key={name}
                name={name}
                children={(field) => (
                  <Field>
                    <FieldLabel htmlFor={name}>{label}</FieldLabel>
                    <Input
                      id={name}
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(event) =>
                        field.handleChange(event.target.value)
                      }
                    />
                    <FieldErrors errors={field.state.meta.errors} />
                  </Field>
                )}
              />
            ))}
          </FieldGroup>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Time to beat</CardTitle>
          <CardDescription>In hours. Leave empty when unknown.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 sm:grid-cols-3">
            {(
              [
                ['timeToBeatHastily', 'Hastily'],
                ['timeToBeatNormally', 'Normally'],
                ['timeToBeatCompletely', 'Completely'],
              ] as const
            ).map(([name, label]) => (
              <form.Field
                key={name}
                name={name}
                children={(field) => (
                  <Field>
                    <FieldLabel htmlFor={name}>{label}</FieldLabel>
                    <Input
                      id={name}
                      type="number"
                      min={0}
                      value={field.state.value}
                      onBlur={field.handleBlur}
                      onChange={(event) =>
                        field.handleChange(event.target.value)
                      }
                    />
                    <FieldErrors errors={field.state.meta.errors} />
                  </Field>
                )}
              />
            ))}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Classification</CardTitle>
          <CardDescription>
            Leaving a group untouched keeps whatever the game already has.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-2">
          <form.Field
            name="genreIds"
            children={(field) => (
              <MultiSelectFilter
                label="Genres"
                options={genres.map((genre) => ({
                  value: genre.id,
                  label: genre.name,
                }))}
                selected={field.state.value}
                onChange={field.handleChange}
              />
            )}
          />
          <form.Field
            name="platformIds"
            children={(field) => (
              <MultiSelectFilter
                label="Platforms"
                options={platforms.map((platform) => ({
                  value: platform.id,
                  label: platform.name,
                }))}
                selected={field.state.value}
                onChange={field.handleChange}
              />
            )}
          />
          <form.Field
            name="companyIds"
            children={(field) => (
              <MultiSelectFilter
                label="Companies"
                options={companies.map((company) => ({
                  value: company.id,
                  label: company.name,
                }))}
                selected={field.state.value}
                onChange={field.handleChange}
              />
            )}
          />
        </CardContent>
      </Card>

      <form.Subscribe
        selector={(state) => state.isSubmitting}
        children={(isSubmitting) => (
          <div className="flex justify-end">
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting && <Loader2 className="size-4 animate-spin" />}
              {submitLabel}
            </Button>
          </div>
        )}
      />
    </form>
  )
}
