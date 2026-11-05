
import './AdminDashboard.css'
import { useCallback, useEffect, useState } from 'react'
import {
  getAdminDashboard,
  getAdminApplications,
  getAdminApplicationDetail,
  updateAdminApplicationStatus,
  rescheduleAdminInterview,
  reviewAdminDocument,
  getAdminDocumentFile,
  getApiErrorMessage,
  type AdminDashboardResponse,
  type AdminApplicationListItem,
  type AdminApplicationDetail,
  type InterviewScheduleInput,
} from './services/api'
import type { Applicant } from './services/api'

type Props = {
  applicant: Applicant
  onLogout: () => void
}

const statuses = [
  'SUBMITTED',
  'UNDER_REVIEW',
  'ACTION_REQUIRED',
  'READY_FOR_INTERVIEW',
  'REJECTED',
]

const nextStatuses: Record<string, string[]> = {
  SUBMITTED: ['UNDER_REVIEW'],
  UNDER_REVIEW: ['ACTION_REQUIRED', 'READY_FOR_INTERVIEW', 'REJECTED'],
}

const styles = {
  page: {
    minHeight: '100vh',
    background: '#f4f7fb',
    color: '#182230',
    fontFamily: 'Arial, sans-serif',
  } as const,
  header: {
    background: '#12315b',
    color: 'white',
    padding: '18px 5%',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 16,
    flexWrap: 'wrap' as const,
  },
  content: {
    width: 'min(1400px, 94%)',
    margin: '24px auto',
  },
  panel: {
    background: 'white',
    padding: 20,
    borderRadius: 12,
    border: '1px solid #e0e6ef',
    marginBottom: 18,
    minWidth: 0,
  },
  button: {
    padding: '10px 14px',
    borderRadius: 7,
    border: '1px solid #cbd5e1',
    background: 'white',
    cursor: 'pointer',
    fontWeight: 600,
  } as const,
  field: {
    width: '100%',
    padding: 10,
    border: '1px solid #cbd5e1',
    borderRadius: 7,
    boxSizing: 'border-box' as const,
    background: 'white',
    color: '#182230',
  },
}

export default function AdminDashboard({ applicant, onLogout }: Props) {
  const token = sessionStorage.getItem('passport_access_token') ?? ''

  const [stats, setStats] = useState<AdminDashboardResponse | null>(null)
  const [applications, setApplications] = useState<AdminApplicationListItem[]>([])
  const [selected, setSelected] = useState<AdminApplicationDetail | null>(null)
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [newStatus, setNewStatus] = useState('')
  const [reason, setReason] = useState('')

  const [interviewDate, setInterviewDate] = useState('')
  const [interviewTime, setInterviewTime] = useState('')
  const [interviewMode, setInterviewMode] = useState<
    'IN_PERSON' | 'ONLINE'
  >('IN_PERSON')
  const [interviewLocation, setInterviewLocation] = useState('')
  const [meetingLink, setMeetingLink] = useState('')
  const [interviewInstructions, setInterviewInstructions] = useState('')

  const [reviewNotes, setReviewNotes] = useState<Record<number, string>>({})
  const [busy, setBusy] = useState(false)
  const [activeView, setActiveView] = useState<'visuals' | 'applications'>(
    'visuals',
  )

  const loadList = useCallback(async () => {
    setLoading(true)
    setError('')

    try {
      const [dashboard, result] = await Promise.all([
        getAdminDashboard(token),
        getAdminApplications(token, {
          search: search.trim() || undefined,
          status: statusFilter || undefined,
          page,
          size: 20,
        }),
      ])

      setStats(dashboard)
      setApplications(result.content)
      setTotalPages(result.totalPages)
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [token, search, statusFilter, page])

  useEffect(() => {
    void loadList()
  }, [loadList])

  const loadInterviewFields = (detail: AdminApplicationDetail) => {
    const schedule = detail.interviewSchedule

    setInterviewDate(schedule?.interviewDate ?? '')
    setInterviewTime(schedule?.interviewTime?.slice(0, 5) ?? '')
    setInterviewMode(schedule?.mode ?? 'IN_PERSON')
    setInterviewLocation(
      schedule?.mode === 'IN_PERSON' ? schedule.location : '',
    )
    setMeetingLink(schedule?.meetingLink ?? '')
    setInterviewInstructions(schedule?.instructions ?? '')
  }

  const openApplication = async (id: number) => {
    setDetailLoading(true)
    setError('')
    setNotice('')

    try {
      const detail = await getAdminApplicationDetail(token, id)
      setSelected(detail)
      setNewStatus('')
      setReason('')
      loadInterviewFields(detail)
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setDetailLoading(false)
    }
  }

  const refreshSelected = async () => {
    if (!selected) return

    const detail = await getAdminApplicationDetail(token, selected.id)
    setSelected(detail)
    loadInterviewFields(detail)
    await loadList()
  }

  const buildInterviewInput = (): InterviewScheduleInput | null => {
    if (!interviewDate || !interviewTime) {
      setError('Please provide the interview date and time.')
      return null
    }

    if (interviewMode === 'IN_PERSON' && !interviewLocation.trim()) {
      setError('Please provide the interview location.')
      return null
    }

    if (interviewMode === 'ONLINE') {
      if (!meetingLink.trim()) {
        setError('Please provide the online meeting link.')
        return null
      }

      try {
        const parsedLink = new URL(meetingLink.trim())

        if (
          parsedLink.protocol !== 'http:' &&
          parsedLink.protocol !== 'https:'
        ) {
          setError('The meeting link must start with http:// or https://.')
          return null
        }
      } catch {
        setError('Please enter a valid online meeting link.')
        return null
      }
    }

    return {
      interviewDate,
      interviewTime,
      mode: interviewMode,
      location:
        interviewMode === 'ONLINE'
          ? 'Online'
          : interviewLocation.trim(),
      meetingLink: meetingLink.trim() || undefined,
      instructions: interviewInstructions.trim() || undefined,
    }
  }

  const changeApplicationStatus = async () => {
    if (!selected || !newStatus) return

    if (
      (newStatus === 'ACTION_REQUIRED' || newStatus === 'REJECTED') &&
      !reason.trim()
    ) {
      setError('Please provide a reason for this decision.')
      return
    }

    let interview: InterviewScheduleInput | undefined

    if (newStatus === 'READY_FOR_INTERVIEW') {
      const interviewInput = buildInterviewInput()

      if (!interviewInput) return

      interview = interviewInput
    }

    setBusy(true)
    setError('')
    setNotice('')

    try {
      await updateAdminApplicationStatus(
        token,
        selected.id,
        newStatus,
        reason.trim() || undefined,
        interview,
      )

      await refreshSelected()
      setNewStatus('')
      setReason('')
      setNotice('Application status updated successfully.')
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const rescheduleInterview = async () => {
    if (!selected || !selected.interviewSchedule) return

    const interview = buildInterviewInput()

    if (!interview) return

    setBusy(true)
    setError('')
    setNotice('')

    try {
      await rescheduleAdminInterview(token, selected.id, interview)

      await refreshSelected()
      setNotice('Interview rescheduled successfully.')
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const reviewDocument = async (
    documentId: number,
    documentStatus: 'APPROVED' | 'CORRECTION_REQUIRED',
  ) => {
    const note = reviewNotes[documentId] ?? ''

    if (documentStatus === 'CORRECTION_REQUIRED' && !note.trim()) {
      setError('Please enter a note explaining the required correction.')
      return
    }

    setBusy(true)
    setError('')
    setNotice('')

    try {
      await reviewAdminDocument(
        token,
        documentId,
        documentStatus,
        note.trim() || undefined,
      )

      await refreshSelected()
      setNotice('Document review saved successfully.')
    } catch (err) {
      setError(getApiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const viewDocument = async (documentId: number) => {
    const tab = window.open('about:blank', '_blank')

    try {
      const blob = await getAdminDocumentFile(token, documentId)
      const url = URL.createObjectURL(blob)

      if (tab) {
        tab.location.href = url
      } else {
        const link = document.createElement('a')
        link.href = url
        link.target = '_blank'
        link.rel = 'noopener'
        link.click()
      }

      window.setTimeout(() => URL.revokeObjectURL(url), 60_000)
    } catch (err) {
      tab?.close()
      setError(getApiErrorMessage(err))
    }
  }

  const displayName = (item: AdminApplicationListItem) =>
    [item.firstName, item.middleName, item.lastName].filter(Boolean).join(' ') ||
    item.applicantEmail

  const renderInterviewFields = () => (
    <div style={{ display: 'grid', gap: 10 }}>
      <label>
        Interview date
        <input
          style={styles.field}
          type="date"
          value={interviewDate}
          onChange={(event) => setInterviewDate(event.target.value)}
        />
      </label>

      <label>
        Interview time
        <input
          style={styles.field}
          type="time"
          value={interviewTime}
          onChange={(event) => setInterviewTime(event.target.value)}
        />
      </label>

      <label>
        Interview mode
        <select
          style={styles.field}
          value={interviewMode}
          onChange={(event) =>
            setInterviewMode(event.target.value as 'IN_PERSON' | 'ONLINE')
          }
        >
          <option value="IN_PERSON">In person</option>
          <option value="ONLINE">Online</option>
        </select>
      </label>

      {interviewMode === 'IN_PERSON' ? (
        <label>
          Interview location
          <input
            style={styles.field}
            type="text"
            value={interviewLocation}
            onChange={(event) => setInterviewLocation(event.target.value)}
            placeholder="Enter interview venue or address"
          />
        </label>
      ) : (
        <label>
          Online meeting link
          <input
            style={styles.field}
            type="url"
            value={meetingLink}
            onChange={(event) => setMeetingLink(event.target.value)}
            placeholder="https://..."
          />
        </label>
      )}

      <label>
        Additional instructions (optional)
        <textarea
          style={styles.field}
          rows={3}
          value={interviewInstructions}
          onChange={(event) =>
            setInterviewInstructions(event.target.value)
          }
          placeholder="Enter any instructions for the applicant"
        />
      </label>
    </div>
  )

  return (
    <div style={styles.page}>
      <header style={styles.header}>
        <div>
          <div style={{ fontSize: 12, opacity: 0.8 }}>PASSPORT SERVICES</div>
          <h1 style={{ fontSize: 22, margin: '5px 0' }}>Admin Dashboard</h1>
          <div style={{ fontSize: 13 }}>{applicant.email}</div>
        </div>

        <button style={styles.button} onClick={onLogout}>
          Log out
        </button>
      </header>

      <main className="admin-layout" style={styles.content}>
        <aside className="admin-sidebar">
          <div className="admin-sidebar-title">WORKSPACE</div>

          <button
            type="button"
            className={`admin-nav-button ${activeView === 'visuals' ? 'active' : ''}`}
            onClick={() => setActiveView('visuals')}
          >
            <span>▦</span> Visuals
          </button>

          <button
            type="button"
            className={`admin-nav-button ${activeView === 'applications' ? 'active' : ''}`}
            onClick={() => setActiveView('applications')}
          >
            <span>▤</span> Applications
          </button>
        </aside>

        <div className="admin-main-content">
          {error && (
            <div role="alert" style={{ ...styles.panel, color: '#a11b1b' }}>
              {error}
              <button
                style={{ ...styles.button, marginLeft: 12 }}
                onClick={() => setError('')}
              >
                Dismiss
              </button>
            </div>
          )}

          {notice && (
            <div role="status" style={{ ...styles.panel, color: '#17653a' }}>
              {notice}
            </div>
          )}

          {activeView === 'visuals' && (
            <section
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
                gap: 14,
                marginBottom: 20,
              }}
            >
              <div style={styles.panel}>
                <div style={{ color: '#526176', fontSize: 13 }}>
                  Total applications
                </div>
                <strong style={{ fontSize: 30 }}>
                  {stats?.totalApplications ?? '—'}
                </strong>
              </div>

              {Object.entries(stats?.applicationsByStatus ?? {})
                .filter(([key]) => key !== 'SUBMITTED' && key !== 'DRAFT')
                .map(([key, value]) => (
                  <div style={styles.panel} key={key}>
                    <div style={{ color: '#526176', fontSize: 13 }}>
                      {key.replaceAll('_', ' ')}
                    </div>
                    <strong style={{ fontSize: 26 }}>{value}</strong>
                  </div>
                ))}
            </section>
          )}

          {activeView === 'applications' && (
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'minmax(0, 1fr)',
                gap: 18,
                alignItems: 'start',
              }}
            >
              <section style={styles.panel}>
                <h2 style={{ marginTop: 0 }}>Applications</h2>

                <form
                  onSubmit={(event) => {
                    event.preventDefault()
                    setPage(0)
                    void loadList()
                  }}
                  style={{ display: 'grid', gap: 10, marginBottom: 16 }}
                >
                  <input
                    style={styles.field}
                    value={search}
                    onChange={(event) => setSearch(event.target.value)}
                    placeholder="Search application number or applicant"
                    aria-label="Search applications"
                  />

                  <select
                    style={styles.field}
                    value={statusFilter}
                    onChange={(event) => {
                      setStatusFilter(event.target.value)
                      setPage(0)
                    }}
                    aria-label="Filter by status"
                  >
                    <option value="">All statuses</option>
                    {statuses.map((status) => (
                      <option key={status} value={status}>
                        {status.replaceAll('_', ' ')}
                      </option>
                    ))}
                  </select>

                  <button style={styles.button} type="submit">
                    Search
                  </button>
                </form>

                {loading ? (
                  <p>Loading applications…</p>
                ) : applications.length === 0 ? (
                  <p>No applications found.</p>
                ) : (
                  <div style={{ overflowX: 'auto' }}>
                    <table
                      style={{
                        width: '100%',
                        borderCollapse: 'collapse',
                        fontSize: 13,
                      }}
                    >
                      <thead>
                        <tr>
                          <th align="left">Applicant / application</th>
                          <th align="left">Status</th>
                        </tr>
                      </thead>

                      <tbody>
                        {applications.map((item) => (
                          <tr key={item.id}>
                            <td
                              style={{
                                padding: '12px 6px',
                                borderTop: '1px solid #e5eaf1',
                              }}
                            >
                              <button
                                style={{
                                  ...styles.button,
                                  textAlign: 'left',
                                  maxWidth: '100%',
                                  overflowWrap: 'anywhere',
                                }}
                                onClick={() => void openApplication(item.id)}
                              >
                                <strong>{displayName(item)}</strong>
                                <br />
                                <span>{item.applicationNumber}</span>
                                <br />
                                <span>{item.applicantEmail}</span>
                              </button>
                            </td>

                            <td
                              style={{
                                padding: '12px 6px',
                                borderTop: '1px solid #e5eaf1',
                              }}
                            >
                              {item.applicationStatus.replaceAll('_', ' ')}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}

                <div
                  style={{
                    display: 'flex',
                    gap: 10,
                    alignItems: 'center',
                    marginTop: 16,
                  }}
                >
                  <button
                    style={styles.button}
                    disabled={page <= 0 || loading}
                    onClick={() =>
                      setPage((value) => Math.max(0, value - 1))
                    }
                  >
                    Previous
                  </button>

                  <span>
                    Page {page + 1} of {Math.max(1, totalPages)}
                  </span>

                  <button
                    style={styles.button}
                    disabled={loading || page + 1 >= totalPages}
                    onClick={() => setPage((value) => value + 1)}
                  >
                    Next
                  </button>
                </div>
              </section>

              <section style={styles.panel}>
                <h2 style={{ marginTop: 0 }}>Application details</h2>

                {detailLoading && <p>Loading application details…</p>}

                {!detailLoading && !selected && (
                  <p>Select an application from the list to review its details.</p>
                )}

                {selected && (
                  <div style={{ overflowWrap: 'anywhere' }}>
                    <h3>{selected.applicationNumber}</h3>

                    <p>
                      <strong>Status:</strong>{' '}
                      {selected.applicationStatus.replaceAll('_', ' ')}
                    </p>

                    <p>
                      <strong>Email:</strong> {selected.applicantEmail}
                    </p>

                    <p>
                      <strong>Passport type:</strong> {selected.passportType}
                    </p>

                    <h3>Applicant profile</h3>

                    {selected.applicantProfile ? (
                      <dl>
                        {Object.entries(selected.applicantProfile)
                          .filter(
                            ([key, value]) =>
                              key !== 'id' &&
                              key !== 'userId' &&
                              value != null &&
                              value !== '',
                          )
                          .map(([key, value]) => (
                            <div key={key} style={{ marginBottom: 8 }}>
                              <dt style={{ fontWeight: 700 }}>
                                {key.replace(/([A-Z])/g, ' $1')}
                              </dt>
                              <dd style={{ margin: '2px 0' }}>
                                {String(value)}
                              </dd>
                            </div>
                          ))}
                      </dl>
                    ) : (
                      <p>No applicant profile is available.</p>
                    )}

                    <h3>Review application</h3>

                    {(nextStatuses[selected.applicationStatus] ?? []).length >
                    0 ? (
                      <div style={{ display: 'grid', gap: 10 }}>
                        <select
                          style={styles.field}
                          value={newStatus}
                          onChange={(event) => setNewStatus(event.target.value)}
                          aria-label="New application status"
                        >
                          <option value="">Choose next status</option>

                          {(nextStatuses[selected.applicationStatus] ?? []).map(
                            (status) => (
                              <option key={status} value={status}>
                                {status.replaceAll('_', ' ')}
                              </option>
                            ),
                          )}
                        </select>

                        <textarea
                          style={styles.field}
                          value={reason}
                          onChange={(event) => setReason(event.target.value)}
                          placeholder="Reason or instructions (required for rejection or action required)"
                          rows={3}
                        />

                        {newStatus === 'READY_FOR_INTERVIEW' && (
                          <div
                            style={{
                              border: '1px solid #d5dfeb',
                              borderRadius: 8,
                              padding: 14,
                              display: 'grid',
                              gap: 12,
                            }}
                          >
                            <h4 style={{ margin: 0 }}>
                              Interview scheduling details
                            </h4>

                            {renderInterviewFields()}
                          </div>
                        )}

                        <button
                          type="button"
                          style={styles.button}
                          disabled={busy || !newStatus}
                          onClick={() => void changeApplicationStatus()}
                        >
                          Save application decision
                        </button>
                      </div>
                    ) : (
                      <p>
                        No further application status transitions are available
                        here.
                      </p>
                    )}

                    {selected.interviewSchedule && (
                      <div
                        style={{
                          border: '1px solid #d5dfeb',
                          borderRadius: 8,
                          padding: 14,
                          marginTop: 18,
                        }}
                      >
                        <h3 style={{ marginTop: 0 }}>Interview schedule</h3>

                        <p>
                          <strong>Date:</strong>{' '}
                          {selected.interviewSchedule.interviewDate}
                        </p>

                        <p>
                          <strong>Time:</strong>{' '}
                          {selected.interviewSchedule.interviewTime.slice(0, 5)}
                        </p>

                        <p>
                          <strong>Mode:</strong>{' '}
                          {selected.interviewSchedule.mode.replaceAll('_', ' ')}
                        </p>

                        {selected.interviewSchedule.mode === 'IN_PERSON' ? (
                          <p>
                            <strong>Location:</strong>{' '}
                            {selected.interviewSchedule.location}
                          </p>
                        ) : (
                          <p>
                            <strong>Meeting link:</strong>{' '}
                            {selected.interviewSchedule.meetingLink ? (
                              <a
                                href={selected.interviewSchedule.meetingLink}
                                target="_blank"
                                rel="noopener noreferrer"
                              >
                                Join online interview
                              </a>
                            ) : (
                              'Not provided'
                            )}
                          </p>
                        )}

                        {selected.interviewSchedule.instructions && (
                          <p>
                            <strong>Instructions:</strong>{' '}
                            {selected.interviewSchedule.instructions}
                          </p>
                        )}

                        {selected.applicationStatus ===
                          'READY_FOR_INTERVIEW' && (
                          <div
                            style={{
                              borderTop: '1px solid #e0e6ef',
                              marginTop: 14,
                              paddingTop: 14,
                              display: 'grid',
                              gap: 12,
                            }}
                          >
                            <h4 style={{ margin: 0 }}>
                              Reschedule interview
                            </h4>

                            {renderInterviewFields()}

                            <button
                              type="button"
                              style={styles.button}
                              disabled={busy}
                              onClick={() => void rescheduleInterview()}
                            >
                              Save new interview schedule
                            </button>
                          </div>
                        )}
                      </div>
                    )}

                    <h3>Documents</h3>

                    {selected.documents.length === 0 ? (
                      <p>No documents have been uploaded.</p>
                    ) : (
                      selected.documents.map((doc) => (
                        <div
                          key={doc.id}
                          style={{
                            border: '1px solid #e0e6ef',
                            borderRadius: 8,
                            padding: 12,
                            marginBottom: 12,
                          }}
                        >
                          <strong>
                            {doc.documentType.replaceAll('_', ' ')}
                          </strong>

                          <p style={{ overflowWrap: 'anywhere' }}>
                            {doc.originalFilename}
                          </p>

                          <p>
                            Status: {doc.documentStatus.replaceAll('_', ' ')}
                          </p>

                          {doc.reviewNote && (
                            <p>Review note: {doc.reviewNote}</p>
                          )}

                          <div
                            style={{
                              display: 'flex',
                              gap: 8,
                              flexWrap: 'wrap',
                            }}
                          >
                            <button
                              style={styles.button}
                              disabled={busy}
                              onClick={() => void viewDocument(doc.id)}
                            >
                              View file
                            </button>

                            <button
                              style={styles.button}
                              disabled={busy}
                              onClick={() =>
                                void reviewDocument(doc.id, 'APPROVED')
                              }
                            >
                              Approve document
                            </button>

                            <button
                              style={styles.button}
                              disabled={busy}
                              onClick={() =>
                                void reviewDocument(
                                  doc.id,
                                  'CORRECTION_REQUIRED',
                                )
                              }
                            >
                              Request correction
                            </button>
                          </div>

                          <textarea
                            style={{ ...styles.field, marginTop: 10 }}
                            rows={2}
                            value={reviewNotes[doc.id] ?? ''}
                            onChange={(event) =>
                              setReviewNotes((current) => ({
                                ...current,
                                [doc.id]: event.target.value,
                              }))
                            }
                            placeholder="Document review note (required for correction)"
                            aria-label={`Review note for ${doc.originalFilename}`}
                          />
                        </div>
                      ))
                    )}

                    <h3>Status history</h3>

                    {selected.statusHistory.length === 0 ? (
                      <p>No status history is available.</p>
                    ) : (
                      <ol>
                        {selected.statusHistory.map((history) => (
                          <li
                            key={history.id}
                            style={{ marginBottom: 10 }}
                          >
                            <strong>
                              {history.newStatus.replaceAll('_', ' ')}
                            </strong>

                            {history.changedAt && (
                              <div>
                                {new Date(history.changedAt).toLocaleString()}
                              </div>
                            )}

                            {history.changeNote && (
                              <div>{history.changeNote}</div>
                            )}
                          </li>
                        ))}
                      </ol>
                    )}
                  </div>
                )}
              </section>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
