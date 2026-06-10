// Remember which project the Owner was last looking at so "/" can jump straight back to its board.
const KEY = 'supportflow.lastProjectId'

export function getLastProjectId(): string | null {
  return localStorage.getItem(KEY)
}

export function setLastProjectId(id: string): void {
  localStorage.setItem(KEY, id)
}
