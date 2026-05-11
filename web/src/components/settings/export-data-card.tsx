import { useMutation } from '@tanstack/react-query'
import { Download, Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { exportData } from '@/queries/profile'

export function ExportDataCard() {
  const exportMutation = useMutation({
    mutationFn: exportData,
    onSuccess: (blob) => {
      const url = URL.createObjectURL(blob)
      const today = new Date().toISOString().slice(0, 10)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = `checkpoint-export-${today}.json`
      anchor.click()
      URL.revokeObjectURL(url)
      toast.success('Your data export is ready')
    },
    onError: () => {
      toast.error('Failed to export your data. Please try again.')
    },
  })

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Export my data</CardTitle>
        <CardDescription>
          Download a JSON file containing every piece of personal data we hold
          about you (GDPR right to data portability). This may take a few
          seconds for users with a long play history.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <Button
          type="button"
          onClick={() => exportMutation.mutate()}
          disabled={exportMutation.isPending}
        >
          {exportMutation.isPending ? (
            <Loader2 className="size-4 animate-spin" />
          ) : (
            <Download className="size-4" />
          )}
          Export my data (JSON)
        </Button>
      </CardContent>
    </Card>
  )
}
