import { useEffect, useState, type FormEvent } from 'react'
import './App.css'
import AdminDashboard from './AdminDashboard'
import {
  getApiErrorMessage,
  getCurrentApplicant,
  loginApplicant,
  registerApplicant,
  getMyPassportApplications,
  createPassportApplication,
  submitPassportApplication,
  getApplicantProfile,
  saveApplicantProfile,
  getApplicationDocuments,
  uploadApplicationDocument,
  type ApplicationDocument,
  type ApplicationDocumentType,
  type Applicant,
  type PassportApplication,
  type ApplicantProfileRequest,
} from './services/api'

type View = 'login' | 'register' | 'dashboard' | 'admin'

function emptyApplicantProfile(): ApplicantProfileRequest {
  return {
    firstName: '',
    middleName: '',
    lastName: '',
    dateOfBirth: '',
    placeOfBirth: '',
    gender: '',
    nationality: 'Gambian',
    phoneNumber: '',
    addressLine: '',
    cityOrTown: '',
    district: '',
    region: '',
    country: 'The Gambia',
  }
}

function getLocalDateString(date = new Date()): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

function getLatestValidDateOfBirth(): string {
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)

  return getLocalDateString(yesterday)
}

function App() {
  const [view, setView] = useState<View>('login')
  const [activeStep, setActiveStep] = useState<0 | 1 | 2 | 3 | 4>(0)
  const [dashboardSection, setDashboardSection] = useState<
  'my-application' | 'track-application'
  >('my-application')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [applicant, setApplicant] = useState<Applicant | null>(null)
  const [loading, setLoading] = useState(false)
  const [checkingSession, setCheckingSession] = useState(true)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const [applications, setApplications] = useState<PassportApplication[]>([])
  const [applicationsLoading, setApplicationsLoading] = useState(false)
  const [applicationAction, setApplicationAction] = useState<
    number | 'create' | null
  >(null)
  const [applicationError, setApplicationError] = useState('')
  const [applicationNotice, setApplicationNotice] = useState('')

  const [applicationDocuments, setApplicationDocuments] = useState<
    Record<number, ApplicationDocument[]>
  >({})
  const [documentsLoading, setDocumentsLoading] = useState<
    Record<number, boolean>
  >({})
  const [selectedDocumentTypes, setSelectedDocumentTypes] = useState<
    Record<number, ApplicationDocumentType>
  >({})
  const [selectedDocumentFiles, setSelectedDocumentFiles] = useState<
  Record<number, File[]>
>({})
  const [documentAction, setDocumentAction] = useState<number | null>(null)
  const [documentErrors, setDocumentErrors] = useState<Record<number, string>>(
    {},
  )
  const [documentNotices, setDocumentNotices] = useState<
    Record<number, string>
  >({})

  const [profile, setProfile] = useState<ApplicantProfileRequest>(
    emptyApplicantProfile(),
  )
  const [profileExists, setProfileExists] = useState(false)
  const [profileLoading, setProfileLoading] = useState(false)
  const [profileSaving, setProfileSaving] = useState(false)
  const [profileError, setProfileError] = useState('')
  const [profileNotice, setProfileNotice] = useState('')

  useEffect(() => {
    let active = true

    async function restoreSession() {
      const token = sessionStorage.getItem('passport_access_token')

      if (!token) {
        if (active) setCheckingSession(false)
        return
      }

      try {
        const currentApplicant = await getCurrentApplicant(token)

        if (active) {
          setApplicant(currentApplicant)
          setView(
            currentApplicant.role === 'ADMIN' ? 'admin' : 'dashboard',
          )
        }
      } catch {
        sessionStorage.removeItem('passport_access_token')
      } finally {
        if (active) setCheckingSession(false)
      }
    }

    void restoreSession()

    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    if (view !== 'dashboard' || !applicant) return

    let active = true

    async function loadApplications() {
      const token = sessionStorage.getItem('passport_access_token')

      if (!token) {
        if (active) {
          setApplicationError(
            'Your session has expired. Please sign in again.',
          )
        }
        return
      }

      setApplicationsLoading(true)
      setApplicationError('')

      try {
        const data = await getMyPassportApplications(token)

        if (active) setApplications(data)
      } catch (loadError) {
        if (active) {
          setApplicationError(getApiErrorMessage(loadError))
        }
      } finally {
        if (active) setApplicationsLoading(false)
      }
    }

    void loadApplications()

    return () => {
      active = false
    }
  }, [view, applicant])

  useEffect(() => {
    if (
      view !== 'dashboard' ||
      !applicant ||
      applicant.role !== 'APPLICANT' ||
      applications.length === 0
    ) {
      return
    }

    let active = true
    const token = sessionStorage.getItem('passport_access_token')

    if (!token) return

    async function loadDocuments() {
      setDocumentsLoading((current) => ({
        ...current,
        ...Object.fromEntries(applications.map((item) => [item.id, true])),
      }))

      const results = await Promise.all(
        applications.map(async (application) => {
          try {
            const documents = await getApplicationDocuments(
              token,
              application.id,
            )

            return { id: application.id, documents, error: '' }
          } catch (loadError) {
            return {
              id: application.id,
              documents: [],
              error: getApiErrorMessage(loadError),
            }
          }
        }),
      )

      if (!active) return

      setApplicationDocuments((current) => ({
        ...current,
        ...Object.fromEntries(
          results.map((result) => [result.id, result.documents]),
        ),
      }))

      setDocumentsLoading((current) => ({
        ...current,
        ...Object.fromEntries(results.map((result) => [result.id, false])),
      }))

      setDocumentErrors((current) => {
        const updated = { ...current }

        results.forEach((result) => {
          if (result.error) {
            updated[result.id] = result.error
          } else {
            delete updated[result.id]
          }
        })

        return updated
      })
    }

    void loadDocuments()

    return () => {
      active = false
    }
  }, [view, applicant, applications])

  useEffect(() => {
    if (
      view !== 'dashboard' ||
      !applicant ||
      applicant.role !== 'APPLICANT'
      ) {
  return
}

    let active = true

    async function loadProfile() {
      const token = sessionStorage.getItem('passport_access_token')

      if (!token) {
        if (active) {
          setProfileError(
            'Your session has expired. Please sign in again.',
          )
        }
        return
      }

      setProfileLoading(true)
      setProfileError('')
      setProfileNotice('')

      try {
        const data = await getApplicantProfile(token)

        if (active) {
          setProfile({
            firstName: data.firstName ?? '',
            middleName: data.middleName ?? '',
            lastName: data.lastName ?? '',
            dateOfBirth: data.dateOfBirth ?? '',
            placeOfBirth: data.placeOfBirth ?? '',
            gender: data.gender ?? '',
            nationality: data.nationality ?? '',
            phoneNumber: data.phoneNumber ?? '',
            addressLine: data.addressLine ?? '',
            cityOrTown: data.cityOrTown ?? '',
            district: data.district ?? '',
            region: data.region ?? '',
            country: data.country ?? '',
          })
          setProfileExists(true)
        }
      } catch (loadError) {
        if (!active) return

        const status =
          typeof loadError === 'object' &&
          loadError !== null &&
          'response' in loadError
            ? (
                loadError as {
                  response?: { status?: number }
                }
              ).response?.status
            : undefined

        if (status === 404) {
          setProfile(emptyApplicantProfile())
          setProfileExists(false)
        } else {
          setProfileError(getApiErrorMessage(loadError))
        }
      } finally {
        if (active) setProfileLoading(false)
      }
    }

    void loadProfile()

    return () => {
      active = false
    }
  }, [view, applicant])

  function changeView(nextView: View) {
    setView(nextView)
    setError('')
    setNotice('')
    setPassword('')
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError('')
    setNotice('')

    const normalizedEmail = email.trim().toLowerCase()

    if (view === 'register' && password.length < 12) {
      setError('Your password must contain at least 12 characters.')
      return
    }

    setLoading(true)

    try {
      if (view === 'register') {
        await registerApplicant(normalizedEmail, password)
        setEmail(normalizedEmail)
        setPassword('')
        setView('login')
        setNotice('Your account has been created. Please sign in.')
      } else {
        const response = await loginApplicant(normalizedEmail, password)

        sessionStorage.setItem(
          'passport_access_token',
          response.accessToken,
        )

        try {
          const currentApplicant = await getCurrentApplicant(
            response.accessToken,
          )
          setApplicant(currentApplicant)
          setView(
            currentApplicant.role === 'ADMIN' ? 'admin' : 'dashboard',
          )
          setPassword('')
        } catch {
          sessionStorage.removeItem('passport_access_token')
          throw new Error(
            'We could not verify your account. Please sign in again.',
          )
        }
      }
    } catch (submitError) {
      setError(
        submitError instanceof Error && !('response' in submitError)
          ? submitError.message
          : getApiErrorMessage(submitError),
      )
    } finally {
      setLoading(false)
    }
  }

  async function handleSaveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const token = sessionStorage.getItem('passport_access_token')

    if (!token) {
      setProfileError(
        'Your session has expired. Please sign in again.',
      )
      return
    }

    setProfileSaving(true)
    setProfileError('')
    setProfileNotice('')

    const request: ApplicantProfileRequest = {
      firstName: profile.firstName.trim(),
      middleName: profile.middleName.trim(),
      lastName: profile.lastName.trim(),
      dateOfBirth: profile.dateOfBirth,
      placeOfBirth: profile.placeOfBirth.trim(),
      gender: profile.gender.trim(),
      nationality: profile.nationality.trim(),
      phoneNumber: profile.phoneNumber.trim(),
      addressLine: profile.addressLine.trim(),
      cityOrTown: profile.cityOrTown.trim(),
      district: profile.district.trim(),
      region: profile.region.trim(),
      country: profile.country.trim(),
    }

    const requiredFields: Array<{
      label: string
      value: string
    }> = [
      { label: 'First name', value: request.firstName },
      { label: 'Last name', value: request.lastName },
      { label: 'Date of birth', value: request.dateOfBirth },
      { label: 'Nationality', value: request.nationality },
      { label: 'Phone number', value: request.phoneNumber },
      { label: 'Address', value: request.addressLine },
      { label: 'City or town', value: request.cityOrTown },
      { label: 'Country', value: request.country },
    ]

    const missingField = requiredFields.find(
      (field) => !field.value.trim(),
    )

    if (missingField) {
      setProfileError(`${missingField.label} is required.`)
      setProfileSaving(false)
      return
    }

    if (request.dateOfBirth >= getLocalDateString()) {
      setProfileError('Date of birth must be before today.')
      setProfileSaving(false)
      return
    }

    try {
      const saved = await saveApplicantProfile(token, request)

      setProfile({
        firstName: saved.firstName ?? '',
        middleName: saved.middleName ?? '',
        lastName: saved.lastName ?? '',
        dateOfBirth: saved.dateOfBirth ?? '',
        placeOfBirth: saved.placeOfBirth ?? '',
        gender: saved.gender ?? '',
        nationality: saved.nationality ?? '',
        phoneNumber: saved.phoneNumber ?? '',
        addressLine: saved.addressLine ?? '',
        cityOrTown: saved.cityOrTown ?? '',
        district: saved.district ?? '',
        region: saved.region ?? '',
        country: saved.country ?? '',
      })

      setProfileExists(true)
setProfileNotice(
  'Your applicant profile has been saved successfully.',
)

setApplicationError('')
setApplicationNotice('')
setApplicationAction('create')

try {
  const currentApplications = await getMyPassportApplications(token)

  const existingApplication = currentApplications[0]

setApplications(currentApplications)

if (existingApplication) {
      setActiveStep(
        ['DRAFT', 'ACTION_REQUIRED'].includes(
        existingApplication.applicationStatus,
        )
          ? 2
          : 4
      )
  setApplicationNotice(
    existingApplication.applicationStatus === 'DRAFT'
      ? `Continuing your draft ${existingApplication.applicationNumber}.`
      : `Your existing application ${existingApplication.applicationNumber} was found. You can track its status.`,
  )
} else {
  const created = await createPassportApplication(token)

  setApplications([created])
  setActiveStep(2)
  setApplicationNotice(
    `Draft ${created.applicationNumber} created successfully.`,
  )
}
} catch (applicationError) {
  setApplicationError(
    `Your profile was saved, but we could not open your application: ${getApiErrorMessage(applicationError)}`,
  )
} finally {
  setApplicationAction(null)
}
    } catch (saveError) {
      setProfileError(getApiErrorMessage(saveError))
    } finally {
      setProfileSaving(false)
    }
  }

  async function handleCreateApplication() {
    if (!profileExists) {
      setApplicationError(
        'Please complete and save your applicant profile before creating a passport application.',
      )
      setApplicationNotice('')
      return
    }

    const token = sessionStorage.getItem('passport_access_token')

    if (!token) {
      setApplicationError(
        'Your session has expired. Please sign in again.',
      )
      return
    }

    setApplicationAction('create')
    setApplicationError('')
    setApplicationNotice('')

    try {
      const currentApplications = await getMyPassportApplications(token)

    if (currentApplications.length > 0) {
      setApplications(currentApplications)

      const existingApplication = currentApplications[0]

      setActiveStep(
        ['DRAFT', 'ACTION_REQUIRED'].includes(
        existingApplication.applicationStatus,
      )
        ? 2
        : 4
      )
      setApplicationNotice(
        `You already have an application (${existingApplication.applicationNumber}). Continue with that application instead of creating another.`,
      )
      return
    }

    const created = await createPassportApplication(token)

    setApplications([created])
    setActiveStep(2)
    setApplicationNotice(
      `Draft ${created.applicationNumber} created successfully.`,
    )
    } catch (actionError) {
      setApplicationError(getApiErrorMessage(actionError))
    } finally {
      setApplicationAction(null)
    }
  }

  async function handleSubmitApplication(applicationId: number) {
  const token = sessionStorage.getItem('passport_access_token')

  if (!token) {
    setApplicationError(
      'Your session has expired. Please sign in again.',
    )
    return
  }

  setApplicationAction(applicationId)
  setApplicationError('')
  setApplicationNotice('')

  try {
    const submitted = await submitPassportApplication(
      token,
      applicationId,
    )

    setApplications((current) =>
      current.map((application) =>
        application.id === submitted.id ? submitted : application,
      ),
    )

    setApplicationNotice(
      `Application ${submitted.applicationNumber} submitted successfully.`,
    )

    setActiveStep(4)
  } catch (actionError) {
    setApplicationError(getApiErrorMessage(actionError))
  } finally {
    setApplicationAction(null)
  }
}

  async function handleUploadDocument(applicationId: number) {
    const token = sessionStorage.getItem('passport_access_token')
    const file = selectedDocumentFiles[applicationId]?.[0]
    const documentType =
      selectedDocumentTypes[applicationId] ?? 'BIRTH_CERTIFICATE'

    if (!token) {
      setDocumentErrors((current) => ({
        ...current,
        [applicationId]: 'Your session has expired. Please sign in again.',
      }))
      return
    }

    if (!file) {
      setDocumentErrors((current) => ({
        ...current,
        [applicationId]: 'Please select a document file first.',
      }))
      return
    }

    if (file.size === 0 || file.size > 5 * 1024 * 1024) {
      setDocumentErrors((current) => ({
        ...current,
        [applicationId]: 'Choose a non-empty file no larger than 5 MB.',
      }))
      return
    }

    const allowedTypes = [
      'application/pdf',
      'image/jpeg',
      'image/png',
    ]

    if (!allowedTypes.includes(file.type)) {
      setDocumentErrors((current) => ({
        ...current,
        [applicationId]: 'Upload a PDF, JPG, or PNG file.',
      }))
      return
    }

    setDocumentAction(applicationId)
    setDocumentErrors((current) => ({
      ...current,
      [applicationId]: '',
    }))
    setDocumentNotices((current) => ({
      ...current,
      [applicationId]: '',
    }))

    try {
      await uploadApplicationDocument(
        token,
        applicationId,
        documentType,
        file,
      )

      const [updatedDocuments, updatedApplications] = await Promise.all([
        getApplicationDocuments(token, applicationId),
        getMyPassportApplications(token),
      ])

      setApplicationDocuments((current) => ({
        ...current,
        [applicationId]: updatedDocuments,
      }))

      setApplications(updatedApplications)

      setSelectedDocumentFiles((current) => ({
        ...current,
        [applicationId]: [],
      }))

      setSelectedDocumentTypes((current) => ({
        ...current,
        [applicationId]: 'BIRTH_CERTIFICATE',
      }))

      setDocumentNotices((current) => ({
        ...current,
        [applicationId]: `${file.name} uploaded successfully. You can select another document.`,
      }))

      // Stay on Step 2 until the applicant chooses Save & Continue.
      const updatedApplication = updatedApplications.find(
        (application) => application.id === applicationId,
      )

      if (updatedApplication?.applicationStatus === 'UNDER_REVIEW') {
        setActiveStep(4)
      } else {
        // Keep draft applicants on Step 2.
        setActiveStep(2)
      }
    } catch (uploadError) {
      setDocumentErrors((current) => ({
        ...current,
        [applicationId]: getApiErrorMessage(uploadError),
      }))
    } finally {
      setDocumentAction(null)
    }
  }

  function handleLogout() {
    sessionStorage.removeItem('passport_access_token')
    setApplicant(null)
    setApplications([])
    setApplicationError('')
    setApplicationNotice('')
    setProfile(emptyApplicantProfile())
    setProfileExists(false)
    setProfileError('')
    setProfileNotice('')
    setPassword('')
    setError('')
    setNotice('You have signed out successfully.')
    setView('login')
  }

  if (checkingSession) {
    return (
      <main className="loading-screen">
        <div className="loading-spinner" />
        <p>Checking your session...</p>
      </main>
    )
  }

  if (view === 'admin' && applicant?.role === 'ADMIN') {
    return (
      <AdminDashboard
        applicant={applicant}
        onLogout={handleLogout}
      />
    )
  }
  if (view === 'dashboard' && applicant) {
    return (
      <main className="dashboard-page">
        <header className="topbar">
          <a className="brand" href="/" aria-label="Passport Portal home">
            <span className="brand-mark">
              <PassportIcon />
            </span>
            <span>
              <strong>Passport Portal</strong>
              <small>ONLINE PASSPORT SERVICES</small>
            </span>
          </a>

          <button
            type="button"
            className="button button-outline logout-button"
            onClick={handleLogout}
          >
            Sign out
          </button>
        </header>

        <section className="dashboard-content">

          <div className="applicant-dashboard-layout">
      <aside className="applicant-sidebar" aria-label="Applicant navigation">
        <p className="applicant-sidebar-heading">MENU</p>

        <button
          type="button"
          className={`applicant-sidebar-link ${
            dashboardSection === 'my-application' ? 'active' : ''
          }`}
          onClick={() => setDashboardSection('my-application')}
          aria-current={
            dashboardSection === 'my-application' ? 'page' : undefined
          }
        >
          <span>📄</span>
          My Application
        </button>

        <button
          type="button"
          className={`applicant-sidebar-link ${
            dashboardSection === 'track-application' ? 'active' : ''
          }`}
          onClick={() => setDashboardSection('track-application')}
          aria-current={
            dashboardSection === 'track-application' ? 'page' : undefined
          }
        >
          <span>📄</span>
          Track Application
        </button>
      </aside>

  <div className="applicant-dashboard-main">
    <nav className="applicant-breadcrumb" aria-label="Breadcrumb">
      <ol className="applicant-breadcrumb-list">
        <li>
          <button
            type="button"
            onClick={() => {
              setDashboardSection('my-application')
              setActiveStep(0)
            }}
          >
            Dashboard
          </button>
        </li>
        <li className="breadcrumb-separator" aria-hidden="true">/</li>

        {dashboardSection === 'track-application' ? (
          <li aria-current="page">Track Application</li>
        ) : (
          <>
            <li>
              <button type="button" onClick={() => setActiveStep(0)}>
                My Application
              </button>
            </li>

            {activeStep >= 1 && activeStep <= 3 && (
              <>
                <li className="breadcrumb-separator" aria-hidden="true">/</li>
                <li aria-current="page">
                  {activeStep === 1 && 'Applicant Profile'}
                  {activeStep === 2 && 'Upload Documents'}
                  {activeStep === 3 && 'Review & Submit'}
                </li>
              </>
            )}
          </>
        )}
      </ol>
    </nav>
    {dashboardSection === 'my-application' ? (
      <>
        <section className="passport-workflow" aria-label="Passport application steps">
            <div className="workflow-heading">
              <p className="eyebrow">APPLICATION JOURNEY</p>
              <h2>Complete your passport application</h2>
              <p>Follow these four steps to prepare and track your application.</p>
            </div>

            <ol className="workflow-steps">
             <li
                className={`workflow-step ${
                  activeStep === 1
                    ? 'workflow-step-active'
                    : activeStep > 1
                      ? 'workflow-step-complete'
                      : ''
                }`}
              >
                <button
                  type="button"
                  className="workflow-step-button"
                  onClick={() => setActiveStep(1)}
                  aria-current={activeStep === 1 ? 'step' : undefined}
                >
                  <span className="workflow-step-number">1</span>
                  <span className="workflow-step-text">
                    <strong>Applicant Profile</strong>
                    <small>Personal and address details</small>
                  </span>
                </button>
              </li>

              <li
                className={`workflow-step ${
                  activeStep === 2
                    ? 'workflow-step-active'
                    : activeStep > 2
                      ? 'workflow-step-complete'
                      : ''
                }`}
              >
                <button
                  type="button"
                  className="workflow-step-button"
                  onClick={() => setActiveStep(2)}
                  aria-current={activeStep === 2 ? 'step' : undefined}
                >
                  <span className="workflow-step-number">2</span>
                  <span className="workflow-step-text">
                    <strong>Upload Documents</strong>
                    <small>Supporting documents</small>
                  </span>
                </button>
              </li>

              <li
                className={`workflow-step ${
                  activeStep === 3
                    ? 'workflow-step-active'
                    : activeStep > 3
                      ? 'workflow-step-complete'
                      : ''
                }`}
              >
                <button
                  type="button"
                  className="workflow-step-button"
                  onClick={() => setActiveStep(3)}
                  aria-current={activeStep === 3 ? 'step' : undefined}
                >
                  <span className="workflow-step-number">3</span>
                  <span className="workflow-step-text">
                    <strong>Review &amp; Submit</strong>
                    <small>Check before submitting</small>
                  </span>
                </button>
              </li>

              <li
                className={`workflow-step ${
                  activeStep === 4 ? 'workflow-step-active' : ''
                }`}
              >
                <button
                  type="button"
                  className="workflow-step-button"
                  onClick={() => setDashboardSection('track-application')}
                >
                  <span className="workflow-step-number">4</span>
                  <span className="workflow-step-text">
                    <strong>Track Application</strong>
                    <small>Reference and status</small>
                  </span>
                </button>
              </li>
            </ol>
          </section>
          <section className="account-card">
            <div className="section-heading">
              <div>
                <p className="eyebrow">YOUR ACCOUNT</p>

              </div>
              <span className="status-pill">
                <span className="status-dot" /> Authenticated
              </span>
            </div>



            {activeStep === 1 && (
              <section className="profile-section">
              <div className="section-heading">
                <div>
                  <p className="eyebrow">PERSONAL INFORMATION</p>
                  <h2>My profile</h2>
                </div>
                <span className="profile-state">
                  {profileLoading
                    ? 'Loading profile...'
                    : profileExists
                      ? 'Profile saved'
                      : 'Profile not completed'}
                </span>
              </div>

              <p className="profile-description">
                Enter your personal and contact details. Your information
                will be linked to your applicant account.
              </p>

              {profileError && (
                <div className="message message-error" role="alert">
                  {profileError}
                </div>
              )}

              {profileNotice && (
                <div className="message message-success" role="status">
                  {profileNotice}
                </div>
              )}

              {profileLoading ? (
                <p className="applications-feedback">
                  Loading your profile...
                </p>
              ) : (
                <form
                  className="profile-form"
                  onSubmit={handleSaveProfile}
                >
                  <div className="profile-fields">
                    <ProfileInput
                      label="First name"
                      name="firstName"
                      value={profile.firstName}
                      onChange={(value) =>
                        setProfile((current) => ({
                          ...current,
                          firstName: value,
                        }))
                      }
                      required
                      maxLength={100}
                    />

                    <ProfileInput
                      label="Middle name"
                      name="middleName"
                      value={profile.middleName}
                      onChange={(value) =>
                        setProfile((current) => ({
                          ...current,
                          middleName: value,
                        }))
                      }
                      maxLength={100}
                    />

                    <ProfileInput
                      label="Last name"
                      name="lastName"
                      value={profile.lastName}
                      onChange={(value) =>
                        setProfile((current) => ({
                          ...current,
                          lastName: value,
                        }))
                      }
                      required
                      maxLength={100}
                    />

                    <ProfileInput
                      label="Date of birth"
                      name="dateOfBirth"
                      type="date"
                      value={profile.dateOfBirth}
                      onChange={(value) =>
                        setProfile((current) => ({
                          ...current,
                          dateOfBirth: value,
                        }))
                      }
                      required
                      max={getLatestValidDateOfBirth()}
                    />

                    <ProfileInput
                      label="Place of birth"
                      name="placeOfBirth"
                      value={profile.placeOfBirth}
                      onChange={(value) =>
                        setProfile((current) => ({
                          ...current,
                          placeOfBirth: value,
                        }))
                      }
                      maxLength={150}
                    />

                    <div className="profile-field">
                      <label htmlFor="gender">Gender</label>
                      <select
                        id="gender"
                        name="gender"
                        value={profile.gender}
                        onChange={(event) =>
                          setProfile((current) => ({
                            ...current,
                            gender: event.target.value,
                          }))
                        }
                      >
                        <option value="">Select gender</option>
                        <option value="Male">Male</option>
                        <option value="Female">Female</option>
                        <option value="Other">Other</option>
                        <option value="Prefer not to say">
                          Prefer not to say
                        </option>
                      </select>
                    </div>

                    <ProfileInput
                      label="Nationality"
                      name="nationality"
                      value={profile.nationality}
                      onChange={(value) =>
                        setProfile((current) => ({
                          ...current,
                          nationality: value,
                        }))
                      }
                      required
                      maxLength={100}
                    />

                    <ProfileInput
                      label="Phone number"
                      name="phoneNumber"
                      type="tel"
                      value={profile.phoneNumber}
                      onChange={(value) =>
                        setProfile((current) => ({
                          ...current,
                          phoneNumber: value,
                        }))
                      }
                      required
                      maxLength={30}
                    />

                    <ProfileInput
                      label="Address"
                      name="addressLine"
                      value={profile.addressLine}
                      onChange={(value) =>
                        setProfile((current) => ({
                          ...current,
                          addressLine: value,
                        }))
                      }
                      required
                      maxLength={255}
                    />

                    <ProfileInput
                      label="City or town"
                      name="cityOrTown"
                      value={profile.cityOrTown}
                      onChange={(value) =>
                        setProfile((current) => ({
                          ...current,
                          cityOrTown: value,
                        }))
                      }
                      required
                      maxLength={100}
                    />

                    <ProfileInput
                      label="District"
                      name="district"
                      value={profile.district}
                      onChange={(value) =>
                        setProfile((current) => ({
                          ...current,
                          district: value,
                        }))
                      }
                      maxLength={100}
                    />

                    <ProfileInput
                      label="Region"
                      name="region"
                      value={profile.region}
                      onChange={(value) =>
                        setProfile((current) => ({
                          ...current,
                          region: value,
                        }))
                      }
                      maxLength={100}
                    />

                    <ProfileInput
                      label="Country"
                      name="country"
                      value={profile.country}
                      onChange={(value) =>
                        setProfile((current) => ({
                          ...current,
                          country: value,
                        }))
                      }
                      required
                      maxLength={100}
                    />
                  </div>

                  <button
                    type="submit"
                    className="button button-primary profile-save-button"
                    disabled={profileSaving || profileLoading}
                  >
                    {profileSaving
                      ? 'Saving profile...'
                      : profileExists
                        ? 'Save changes'
                        : 'Save profile'}
                  </button>
                </form>
              )}
              </section>
            )}

            {activeStep >= 2 && (
              <section className="applications-section">
              <div className="section-heading">
                <div>
                  <p className="eyebrow">
                    {activeStep === 2
                      ? 'STEP 2 OF 4'
                      : activeStep === 3
                        ? 'STEP 3 OF 4'
                        : 'STEP 4 OF 4'}
                  </p>
                  <h2>
                    {activeStep === 2
                      ? 'Upload Documents'
                      : activeStep === 3
                        ? 'Review & Submit'
                        : 'Track Application'}
                  </h2>
                </div>
               {activeStep === 2 && (
                <button
                  type="button"
                  className="button button-primary create-application-button"
                  onClick={handleCreateApplication}
                  disabled={
                    applicationAction !== null ||
                    !profileExists ||
                    profileLoading ||
                    profileSaving
                  }
                >
                  {applicationAction === 'create'
                    ? 'Creating...'
                    : '+ New application'}
                </button>
                )}
              </div>

              {!profileExists && !profileLoading && (
                <p className="profile-description">
                  Save your applicant profile before creating a passport
                  application.
                </p>
              )}

              {applicationNotice && (
                <div className="message message-success" role="status">
                  {applicationNotice}
                </div>
              )}

              {applicationError && (
                <div className="message message-error" role="alert">
                  {applicationError}
                </div>
              )}

              {applicationsLoading ? (
                <p className="applications-feedback">
                  Loading your applications...
                </p>
              ) : applications.length === 0 ? (
                <div className="applications-empty">
                  <span className="note-icon"><InfoIcon /></span>
                  <div>
                    <strong>No applications yet</strong>
                    <p>
                      Save your applicant profile, then create a draft to
                      begin your passport application.
                    </p>
                  </div>
                </div>
                           ) : activeStep === 4 ? (
                <div className="applications-list">
                  {applications.map((application) => (
                    <article className="application-item" key={application.id}>
                      <span
                        className={`application-status status-${application.applicationStatus.toLowerCase()}`}
                      >
                        {application.applicationStatus.replaceAll('_', ' ')}
                      </span>
                    </article>
                  ))}
                </div>
              ) : (
                <div className="applications-list">
                  {applications.map((application) => (
                    <article
                      className="application-item"
                      key={application.id}
                    >
                      <div className="application-item-header">
                        <div>
                          <span className="application-number">
                            {application.applicationNumber}
                          </span>
                          <h3>
                            {application.passportType.replaceAll('_', ' ')}
                            {' '}passport
                          </h3>
                        </div>

                        <span
                          className={`application-status status-${application.applicationStatus.toLowerCase()}`}
                        >
                          {application.applicationStatus.replaceAll('_', ' ')}
                        </span>
                      </div>

                      <div className="application-meta">

                        <span>
                          Created:{' '}
                          {new Date(application.createdAt).toLocaleDateString()}
                        </span>
                        {application.submissionDate && (
                          <span>
                            Submitted:{' '}
                            {new Date(
                              application.submissionDate,
                            ).toLocaleDateString()}
                          </span>
                        )}
                      </div>

                      {application.decisionReason && (
                        <p className="application-decision">
                          {application.decisionReason}
                        </p>
                      )}

                      <section className="application-documents">
                        <h4>Supporting documents</h4>

                        {documentsLoading[application.id] ? (
                          <p>Loading documents...</p>
                        ) : (applicationDocuments[application.id] ?? [])
                            .length === 0 ? (
                          <p className="document-empty">
                            No documents uploaded yet.
                          </p>
                        ) : (
                          <ul className="document-list">
                            {(applicationDocuments[application.id] ?? []).map(
                              (document) => (
                                <li key={document.id}>
                                  <div className="document-details">
                                    <strong>
                                      {document.documentType.replaceAll(
                                        '_',
                                        ' ',
                                      )}
                                    </strong>
                                    <span>{document.originalFilename}</span>
                                    <small>
                                      {(document.fileSizeBytes / 1024).toFixed(
                                        1,
                                      )}{' '}
                                      KB
                                    </small>
                                  </div>
                                  <span className="document-status">
                                    {document.documentStatus.replaceAll(
                                      '_',
                                      ' ',
                                    )}
                                  </span>
                                </li>
                              ),
                            )}
                          </ul>
                        )}

                        {documentErrors[application.id] && (
                          <div className="message message-error" role="alert">
                            {documentErrors[application.id]}
                          </div>
                        )}

                        {documentNotices[application.id] && (
                          <div
                            className="message message-success"
                            role="status"
                          >
                            {documentNotices[application.id]}
                          </div>
                        )}
                         {activeStep === 2 &&
                            ['DRAFT', 'ACTION_REQUIRED'].includes(
                              application.applicationStatus,
                            ) && (
                            <>
                          <form
                            className="document-upload-form"
                            onSubmit={(event: FormEvent<HTMLFormElement>) => {
                              event.preventDefault()
                              void handleUploadDocument(application.id)
                            }}
                          >
                            <div className="document-form-field">
                              <label
                                htmlFor={`document-type-${application.id}`}
                              >
                                Document type
                              </label>
                              <select
                                id={`document-type-${application.id}`}
                                value={
                                  application.applicationStatus === 'ACTION_REQUIRED'
                                    ? (application.requestedDocumentType ?? '')
                                    : (selectedDocumentTypes[application.id] ?? '')
                                }
                                disabled={application.applicationStatus === 'ACTION_REQUIRED'}
                                onChange={(event) =>
                                  setSelectedDocumentTypes((current) => ({
                                    ...current,
                                    [application.id]: event.target.value as ApplicationDocumentType,
                                  }))
                                }
                              >
                                {application.applicationStatus === 'ACTION_REQUIRED' ? (
                                  application.requestedDocumentType && (
                                    <option value={application.requestedDocumentType}>
                                      {application.requestedDocumentType.replaceAll('_', ' ')}
                                    </option>
                                  )
                                ) : (
                                  <>
                                    <option value="">Select a document</option>
                                    <option value="BIRTH_CERTIFICATE">Birth certificate</option>
                                    <option value="NATIONAL_ID">Expired ID</option>
                                    <option value="PROOF_OF_CITIZENSHIP">
                                      Parent's ID
                                    </option>
                                    <option value="PASSPORT_PHOTO">Passport photograph</option>
                                    <option value="SUPPORTING_DOCUMENT">
                                      Other supporting document
                                    </option>
                                  </>
                                )}
                              </select>
                            </div>
                            <div className="document-form-field">
                              <label htmlFor={`document-file-${application.id}`}>
                                Select document (PDF, JPG, or PNG; max 5 MB)
                              </label>

                              <input
                                key={`${application.id}-${(applicationDocuments[application.id] ?? []).length}`}
                                id={`document-file-${application.id}`}
                                type="file"
                                accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
                                onChange={(event) =>
                                  setSelectedDocumentFiles((current) => ({
                                    ...current,
                                    [application.id]: event.target.files
                                      ? Array.from(event.target.files)
                                      : [],
                                  }))
                                }
                              />
                              {(selectedDocumentFiles[application.id] ?? []).length > 0 && (
                                <p className="applications-feedback">
                                  Selected: {selectedDocumentFiles[application.id][0].name}
                                </p>
                              )}
                            </div>
                            <button
                              type="submit"
                              className="button button-primary"
                              disabled={documentAction !== null}
                            >
                              {documentAction === application.id
                                ? 'Uploading...'
                                : 'Upload document'}
                            </button>
                                                    </form>

                          <button
                            type="button"
                            className="button button-primary application-submit-button"
                            onClick={() => setActiveStep(3)}
                            disabled={
                              documentAction !== null ||
                              documentsLoading[application.id] === true ||
                              (applicationDocuments[application.id] ?? []).length === 0
                            }
                          >
                            Save &amp; Continue to Review
                          </button>
                        </>
                      )}
                    </section>
                      {activeStep === 3 &&
                        application.applicationStatus === 'DRAFT' && (
                        <button
                          type="button"
                          className="button button-primary application-submit-button"
                          onClick={() =>
                            handleSubmitApplication(application.id)
                          }
                          disabled={applicationAction !== null}
                        >
                          {applicationAction === application.id
                            ? 'Submitting...'
                            : 'Submit application'}
                        </button>
                      )}
                    </article>
                  ))}
                                </div>
              )}

              {activeStep !== 4 && (
                <p className="applications-disclaimer">
                  This is the current development-stage workflow. Complete
                  the applicant details and required document-upload steps
                  before submitting a real passport application.
                </p>
              )}
              </section>
              )}
          </section>
                </>
    ) : (
      <section className="account-card tracking-card">
        <div className="section-heading">
          <div>
            <p className="eyebrow">APPLICATION TRACKING</p>
            <h2>Application Overview</h2>
          </div>
          <span className="status-pill">
            <span className="status-dot" /> Your applications
          </span>
        </div>

        {applicationError && (
          <div className="message message-error" role="alert">
            {applicationError}
          </div>
        )}

        {applicationsLoading ? (
          <p className="applications-feedback">
            Loading your applications...
          </p>
        ) : applications.length === 0 ? (
          <div className="applications-empty">
            <span className="note-icon"><InfoIcon /></span>
            <div>
              <strong>No applications available</strong>
              <p>
                Your passport application details will appear here
                after you create an application.
              </p>
            </div>
          </div>
        ) : (
          <div className="applications-list">
            {applications.map((application) => (
              <article className="application-item" key={application.id}>
                <div className="application-item-header">
                  <div>
                    <span className="application-number">
                      {application.applicationNumber}
                    </span>
                    <h3>
                      {application.passportType.replaceAll('_', ' ')}
                      {' '}passport
                    </h3>
                  </div>

                  <span
                    className={`application-status status-${application.applicationStatus.toLowerCase()}`}
                  >
                    {application.applicationStatus.replaceAll('_', ' ')}
                  </span>
                </div>

                <div className="application-meta">

                  <span>
                    Created:{' '}
                    {new Date(application.createdAt).toLocaleDateString()}
                  </span>

                  {application.submissionDate && (
                    <span>
                      Submitted:{' '}
                      {new Date(
                        application.submissionDate,
                      ).toLocaleDateString()}
                    </span>
                  )}
                </div>

                {application.decisionReason && (
                  <p className="application-decision">
                    {application.decisionReason}
                  </p>
                )}

                <section className="application-documents">
                  <h4>Supporting documents</h4>

                  {documentsLoading[application.id] ? (
                    <p>Loading documents...</p>
                  ) : (applicationDocuments[application.id] ?? []).length === 0 ? (
                    <p className="document-empty">
                      No documents uploaded yet.
                    </p>
                  ) : (
                    <ul className="document-list">
                      {(applicationDocuments[application.id] ?? []).map(
                        (document) => (
                          <li key={document.id}>
                            <div className="document-details">
                              <strong>
                                {document.documentType.replaceAll('_', ' ')}
                              </strong>
                              <span>{document.originalFilename}</span>
                            </div>
                            <span className="document-status">
                              {document.documentStatus.replaceAll('_', ' ')}
                            </span>
                          </li>
                        ),
                      )}
                    </ul>
                  )}
                </section>
              </article>
            ))}
          </div>
        )}
      </section>
    )}
  </div>
</div>
        </section>

        <footer className="page-footer">
          <span>Passport Portal</span>
          <span>Secure account access</span>
        </footer>
      </main>
    )
  }

  const isRegister = view === 'register'

  return (
    <main className="auth-page">
      <section className="auth-intro">
        <a
          className="brand brand-light"
          href="/"
          aria-label="Passport Portal home"
        >
          <span className="brand-mark">
            <PassportIcon />
          </span>
          <span>
            <strong>Passport Portal</strong>
            <small>ONLINE PASSPORT SERVICES</small>
          </span>
        </a>

        <div className="intro-content">
          <span className="intro-tag">
            <span className="intro-tag-dot" />
            YOUR JOURNEY STARTS HERE
          </span>

          <h1>
            A simpler way to begin your <span>passport journey.</span>
          </h1>

          <p>
            Access your applicant account through one secure place.
            Register or sign in to get started.
          </p>

          <div className="intro-feature">
            <span className="feature-icon"><ShieldIcon /></span>
            <div>
              <strong>Secure account access</strong>
              <p>Your sign-in is verified by the passport service.</p>
            </div>
          </div>

          <div className="intro-feature">
            <span className="feature-icon"><UserIcon /></span>
            <div>
              <strong>Your account, in one place</strong>
              <p>Access your applicant details after signing in.</p>
            </div>
          </div>
        </div>

        <div className="intro-footer">
          <span className="footer-line" />
          <span>ONLINE PASSPORT PORTAL</span>
        </div>
      </section>

      <section className="auth-panel">
        <div className="auth-form-wrap">
          <div className="mobile-brand">
            <span className="brand-mark"><PassportIcon /></span>
            <strong>Passport Portal</strong>
          </div>

          <div className="form-heading">
            <p className="eyebrow">
              {isRegister ? 'CREATE YOUR ACCOUNT' : 'APPLICANT ACCESS'}
            </p>
            <h2>{isRegister ? 'Create an account' : 'Welcome back'}</h2>
            <p>
              {isRegister
                ? 'Enter your details to get started.'
                : 'Sign in with your registered email and password.'}
            </p>
          </div>

          <div className="auth-tabs" aria-label="Account access options">
            <button
              type="button"
              className={!isRegister ? 'auth-tab active' : 'auth-tab'}
              onClick={() => changeView('login')}
              aria-pressed={!isRegister}
            >
              Sign in
            </button>
            <button
              type="button"
              className={isRegister ? 'auth-tab active' : 'auth-tab'}
              onClick={() => changeView('register')}
              aria-pressed={isRegister}
            >
              Create account
            </button>
          </div>

          {notice && (
            <div className="message message-success" role="status">
              {notice}
            </div>
          )}

          {error && (
            <div className="message message-error" role="alert">
              {error}
            </div>
          )}

          <form className="auth-form" onSubmit={handleSubmit}>
            <label htmlFor="email">Email address</label>
            <div className="input-wrap">
              <span className="input-icon"><MailIcon /></span>
              <input
                id="email"
                type="email"
                autoComplete="email"
                placeholder="you@example.com"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                maxLength={254}
                required
              />
            </div>

            <div className="password-label-row">
              <label htmlFor="password">Password</label>
              {isRegister && (
                <span className="field-hint">12 characters minimum</span>
              )}
            </div>

            <div className="input-wrap">
              <span className="input-icon"><LockIcon /></span>
              <input
                id="password"
                type="password"
                autoComplete={isRegister ? 'new-password' : 'current-password'}
                placeholder={
                  isRegister
                    ? 'Create a strong password'
                    : 'Enter your password'
                }
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                minLength={isRegister ? 12 : undefined}
                maxLength={72}
                required
              />
            </div>

            <button
              className="button button-primary submit-button"
              type="submit"
              disabled={loading}
            >
              {loading
                ? 'Please wait...'
                : isRegister
                  ? 'Create account'
                  : 'Sign in'}
              {!loading && <span className="button-arrow">→</span>}
            </button>
          </form>

          <p className="switch-prompt">
            {isRegister ? 'Already have an account?' : 'New to the portal?'}{' '}
            <button
              type="button"
              className="text-button"
              onClick={() =>
                changeView(isRegister ? 'login' : 'register')
              }
            >
              {isRegister ? 'Sign in' : 'Create an account'}
            </button>
          </p>

          <div className="security-caption">
            <ShieldIcon />
            <span>
              Your credentials are transmitted to the passport service for
              verification.
            </span>
          </div>
        </div>

        <footer className="panel-footer">
          <span>Passport Portal</span>
          <span>Secure applicant access</span>
        </footer>
      </section>
    </main>
  )
}

type ProfileInputProps = {
  label: string
  name: string
  value: string
  onChange: (value: string) => void
  required?: boolean
  maxLength?: number
  type?: string
  max?: string
}

function ProfileInput({
  label,
  name,
  value,
  onChange,
  required = false,
  maxLength,
  type = 'text',
  max,
}: ProfileInputProps) {
  return (
    <div className="profile-field">
      <label htmlFor={name}>
        {label}{required ? ' *' : ''}
      </label>
      <input
        id={name}
        name={name}
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        required={required}
        maxLength={maxLength}
        max={max}
      />
    </div>
  )
}

function PassportIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="5" y="2.75" width="14" height="18.5" rx="2" />
      <circle cx="12" cy="10" r="3.2" />
      <path d="M8.8 10h6.4M12 6.8c1.1 1.1 1.1 5.3 0 6.4M12 6.8c-1.1 1.1-1.1 5.3 0 6.4M9 17h6" />
    </svg>
  )
}

function MailIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="3" y="5" width="18" height="14" rx="2" />
      <path d="m4 7 8 6 8-6" />
    </svg>
  )
}

function LockIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="4" y="10" width="16" height="11" rx="2" />
      <path d="M8 10V7a4 4 0 0 1 8 0v3M12 14v3" />
    </svg>
  )
}

function ShieldIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z" />
      <path d="m9 12 2 2 4-4" />
    </svg>
  )
}

function UserIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21a8 8 0 0 1 16 0" />
    </svg>
  )
}

function InfoIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="9" />
      <path d="M12 11v5M12 8h.01" />
    </svg>
  )
}

export default App
