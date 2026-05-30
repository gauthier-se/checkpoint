/**
 * Builds the SEO `<meta>` entries for a route's `head()` function.
 *
 * Returns an array meant to be spread into `head().meta`. TanStack Router
 * dedupes meta by `title`/`name`, so a child route calling `seo()` with a
 * different title overrides the root's instead of appending a second one.
 *
 * @example
 * head: () => ({ meta: seo({ title: 'Games — Checkpoint' }) })
 */
export function seo({
  title,
  description,
  image,
  keywords,
}: {
  title: string
  description?: string
  image?: string
  keywords?: string
}) {
  const tags: Array<Record<string, string>> = [
    { title },
    { name: 'twitter:title', content: title },
    { name: 'og:title', content: title },
    { name: 'og:type', content: 'website' },
  ]

  if (description) {
    tags.push(
      { name: 'description', content: description },
      { name: 'twitter:description', content: description },
      { name: 'og:description', content: description },
    )
  }

  if (keywords) {
    tags.push({ name: 'keywords', content: keywords })
  }

  if (image) {
    tags.push(
      { name: 'twitter:image', content: image },
      { name: 'twitter:card', content: 'summary_large_image' },
      { name: 'og:image', content: image },
    )
  }

  return tags
}
