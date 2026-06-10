// Mirrors the backend DTOs and enums (com.supportflow.*). Kept in sync by hand — the backend is the
// source of truth for these shapes.

export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'PENDING' | 'RESOLVED' | 'CLOSED'
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
export type TicketType = 'QUESTION' | 'BUG' | 'FEATURE_REQUEST'
export type AuthProvider = 'GOOGLE' | 'GITHUB' | 'DEV'

export const TICKET_STATUSES: TicketStatus[] = [
  'OPEN',
  'IN_PROGRESS',
  'PENDING',
  'RESOLVED',
  'CLOSED',
]
export const TICKET_PRIORITIES: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
export const TICKET_TYPES: TicketType[] = ['QUESTION', 'BUG', 'FEATURE_REQUEST']

export interface Me {
  id: string
  email: string
  name: string | null
  avatarUrl: string | null
  provider: AuthProvider
}

export interface Project {
  id: string
  name: string
  clientName: string | null
  description: string | null
  jiraProjectKey: string | null
  qaseProjectCode: string | null
  confluenceSpaceKey: string | null
  archived: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateProjectRequest {
  name: string
  clientName?: string | null
  description?: string | null
  jiraProjectKey?: string | null
  qaseProjectCode?: string | null
  confluenceSpaceKey?: string | null
}

export type UpdateProjectRequest = Partial<CreateProjectRequest>

export interface Ticket {
  id: string
  projectId: string
  subject: string
  description: string | null
  status: TicketStatus
  priority: TicketPriority
  type: TicketType
  category: string | null
  escalated: boolean
  jiraIssueKey: string | null
  qaseCaseId: string | null
  confluencePageId: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateTicketRequest {
  subject: string
  description?: string | null
  priority?: TicketPriority | null
  type?: TicketType | null
  category?: string | null
}

export interface UpdateTicketRequest {
  subject?: string
  description?: string | null
  status?: TicketStatus
  priority?: TicketPriority
  type?: TicketType
  category?: string | null
}

// The Advisor's suggested fields arrive as free strings from Gemini, not strict enums.
export interface AiSuggestion {
  suggestedType: string | null
  suggestedCategory: string | null
  suggestedPriority: string | null
  draftNote: string | null
  generatedAt: string
}

export interface AiSuggestionEnvelope {
  status: 'READY' | 'FAILED'
  suggestion?: AiSuggestion
  error?: string // "QUOTA" | "GENERATION_FAILED"
}

export type ToolOutcomeKind = 'CREATED' | 'ALREADY_LINKED' | 'FAILED'

export interface ToolOutcome {
  tool: string // "Jira" | "Qase" | "Confluence"
  outcome: ToolOutcomeKind
  key?: string
  url?: string
  error?: string
}

export type EscalationStatus = 'ESCALATED' | 'PARTIAL' | 'FAILED'

export interface EscalationResult {
  status: EscalationStatus
  targets: ToolOutcome[]
  ticket: Ticket
}

// Spring Data Page<T> — the envelope returned by list endpoints.
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
  numberOfElements: number
}
