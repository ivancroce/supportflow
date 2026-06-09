import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/Button'
import { Dialog } from '@/components/Dialog'
import { Field, Select, TextArea, TextInput } from '@/components/Field'
import { ApiError } from '@/lib/api'
import { PRIORITY_LABEL, TYPE_LABEL } from '@/lib/format'
import { useCreateTicket } from '@/lib/queries'
import { TICKET_PRIORITIES, TICKET_TYPES } from '@/lib/types'
import type { TicketPriority, TicketType } from '@/lib/types'

/** Modal form to open a new ticket. On success it routes straight to the new ticket's detail page. */
export function NewTicketDialog({
  projectId,
  open,
  onClose,
}: {
  projectId: string
  open: boolean
  onClose: () => void
}) {
  const navigate = useNavigate()
  const createTicket = useCreateTicket(projectId)

  const [subject, setSubject] = useState('')
  const [description, setDescription] = useState('')
  const [priority, setPriority] = useState<TicketPriority>('MEDIUM')
  const [type, setType] = useState<TicketType>('QUESTION')
  const [category, setCategory] = useState('')

  function reset() {
    setSubject('')
    setDescription('')
    setPriority('MEDIUM')
    setType('QUESTION')
    setCategory('')
    createTicket.reset()
  }

  function close() {
    if (createTicket.isPending) return
    reset()
    onClose()
  }

  function submit(e: React.FormEvent) {
    e.preventDefault()
    if (!subject.trim()) return
    createTicket.mutate(
      {
        subject: subject.trim(),
        description: description.trim() || null,
        priority,
        type,
        category: category.trim() || null,
      },
      {
        onSuccess: (ticket) => {
          reset()
          onClose()
          navigate(`/projects/${projectId}/tickets/${ticket.id}`)
        },
      },
    )
  }

  const error =
    createTicket.error instanceof ApiError
      ? createTicket.error.message
      : createTicket.isError
        ? 'Could not create the ticket. Please try again.'
        : null

  return (
    <Dialog
      open={open}
      onClose={close}
      title="New ticket"
      description="Capture a customer issue, question, or request."
      width={520}
      dismissable={!createTicket.isPending}
      hideClose={createTicket.isPending}
    >
      <form onSubmit={submit} className="flex flex-col gap-4 px-6 pb-6 pt-5">
        <Field label="Subject" htmlFor="ticket-subject" required>
          <TextInput
            id="ticket-subject"
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            placeholder="Short summary of the issue"
            autoFocus
            maxLength={200}
          />
        </Field>

        <Field label="Description" htmlFor="ticket-description">
          <TextArea
            id="ticket-description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Add any detail that helps resolve it…"
          />
        </Field>

        <div className="grid grid-cols-2 gap-4">
          <Field label="Priority" htmlFor="ticket-priority">
            <Select
              id="ticket-priority"
              value={priority}
              onChange={(e) => setPriority(e.target.value as TicketPriority)}
            >
              {TICKET_PRIORITIES.map((p) => (
                <option key={p} value={p}>
                  {PRIORITY_LABEL[p]}
                </option>
              ))}
            </Select>
          </Field>

          <Field label="Type" htmlFor="ticket-type">
            <Select
              id="ticket-type"
              value={type}
              onChange={(e) => setType(e.target.value as TicketType)}
            >
              {TICKET_TYPES.map((t) => (
                <option key={t} value={t}>
                  {TYPE_LABEL[t]}
                </option>
              ))}
            </Select>
          </Field>
        </div>

        <Field label="Category" htmlFor="ticket-category" hint="Optional — e.g. Billing, Auth, API.">
          <TextInput
            id="ticket-category"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            placeholder="Uncategorized"
            maxLength={80}
          />
        </Field>

        {error && (
          <div className="rounded-lg border border-danger/30 bg-danger-soft px-3.5 py-2.5 text-[13px] text-danger-ink">
            {error}
          </div>
        )}

        <div className="mt-1 flex justify-end gap-2.5">
          <Button type="button" variant="secondary" onClick={close} disabled={createTicket.isPending}>
            Cancel
          </Button>
          <Button type="submit" loading={createTicket.isPending} disabled={!subject.trim()}>
            Create ticket
          </Button>
        </div>
      </form>
    </Dialog>
  )
}
