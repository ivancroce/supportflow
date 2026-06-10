import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiFetch } from './api'
import type {
  AiSuggestionEnvelope,
  CreateProjectRequest,
  CreateTicketRequest,
  EscalationResult,
  Page,
  Project,
  Ticket,
  UpdateProjectRequest,
  UpdateTicketRequest,
} from './types'

// A portfolio app has a handful of projects/tickets, so we fetch a large page and filter/search
// client-side (matching the design's behavior) rather than round-tripping query params per keystroke.
const PAGE_SIZE = 100

// ── Projects ───────────────────────────────────────────────────────────────

export function useProjects() {
  return useQuery({
    queryKey: ['projects'],
    queryFn: () => apiFetch<Page<Project>>('/api/projects', { params: { size: PAGE_SIZE } }),
    select: (page) => page.content,
  })
}

export function useProject(projectId: string | undefined) {
  return useQuery({
    queryKey: ['project', projectId],
    queryFn: () => apiFetch<Project>(`/api/projects/${projectId}`),
    enabled: !!projectId,
  })
}

export function useCreateProject() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateProjectRequest) =>
      apiFetch<Project>('/api/projects', { method: 'POST', body }),
    onSuccess: (project) => {
      qc.invalidateQueries({ queryKey: ['projects'] })
      qc.setQueryData(['project', project.id], project)
    },
  })
}

export function useUpdateProject(projectId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: UpdateProjectRequest) =>
      apiFetch<Project>(`/api/projects/${projectId}`, { method: 'PATCH', body }),
    onSuccess: (project) => {
      qc.setQueryData(['project', project.id], project)
      qc.invalidateQueries({ queryKey: ['projects'] })
    },
  })
}

// ── Tickets ────────────────────────────────────────────────────────────────

export function useTickets(projectId: string | undefined) {
  return useQuery({
    queryKey: ['tickets', projectId],
    queryFn: () =>
      apiFetch<Page<Ticket>>(`/api/projects/${projectId}/tickets`, {
        params: { size: PAGE_SIZE },
      }),
    select: (page) => page.content,
    enabled: !!projectId,
  })
}

export function useTicket(ticketId: string | undefined) {
  return useQuery({
    queryKey: ['ticket', ticketId],
    queryFn: () => apiFetch<Ticket>(`/api/tickets/${ticketId}`),
    enabled: !!ticketId,
  })
}

export function useCreateTicket(projectId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateTicketRequest) =>
      apiFetch<Ticket>(`/api/projects/${projectId}/tickets`, { method: 'POST', body }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['tickets', projectId] }),
  })
}

export function useUpdateTicket(ticketId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: UpdateTicketRequest) =>
      apiFetch<Ticket>(`/api/tickets/${ticketId}`, { method: 'PATCH', body }),
    onSuccess: (ticket) => {
      qc.setQueryData(['ticket', ticket.id], ticket)
      qc.invalidateQueries({ queryKey: ['tickets', ticket.projectId] })
    },
  })
}

// ── AI Advisor ───────────────────────────────────────────────────────────────

// GET is synchronous on the backend: a cache miss blocks on a multi-second Gemini call, so this
// query must stand on its own (its own spinner) and never block the ticket view. The result is
// cached server-side, so we never re-fetch on focus.
export function useAiSuggestion(ticketId: string | undefined) {
  return useQuery({
    queryKey: ['ai-suggestion', ticketId],
    queryFn: () => apiFetch<AiSuggestionEnvelope>(`/api/tickets/${ticketId}/ai-suggestion`),
    enabled: !!ticketId,
    retry: false,
    staleTime: Infinity,
    refetchOnWindowFocus: false,
  })
}

export function useRegenerateSuggestion(ticketId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () =>
      apiFetch<AiSuggestionEnvelope>(`/api/tickets/${ticketId}/ai-suggestion/regenerate`, {
        method: 'POST',
      }),
    onSuccess: (envelope) => qc.setQueryData(['ai-suggestion', ticketId], envelope),
  })
}

// ── Escalation ───────────────────────────────────────────────────────────────

export function useEscalate(ticketId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () =>
      apiFetch<EscalationResult>(`/api/tickets/${ticketId}/escalate`, { method: 'POST' }),
    onSuccess: (result) => {
      qc.setQueryData(['ticket', result.ticket.id], result.ticket)
      qc.invalidateQueries({ queryKey: ['tickets', result.ticket.projectId] })
    },
  })
}

// ── Dev login (local profile only) ───────────────────────────────────────────

export function useDevLogin() {
  return useMutation({
    mutationFn: (email: string) =>
      apiFetch<{ token: string }>('/api/dev/login', { method: 'POST', body: { email } }),
  })
}
