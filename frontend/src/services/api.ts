import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
})

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  id: number
  email: string
  role: string
}

export interface Applicant {
  id: number
  email: string
  role: string
}

export interface RegisterResponse {
  message: string
  id: number
  email: string
  role: string
}

export async function registerApplicant(
  email: string,
  password: string,
): Promise<RegisterResponse> {
  const response = await api.post<RegisterResponse>('/auth/register', {
    email,
    password,
  })

  return response.data
}

export async function loginApplicant(
  email: string,
  password: string,
): Promise<AuthResponse> {
  const response = await api.post<AuthResponse>('/auth/login', {
    email,
    password,
  })

  return response.data
}

export async function getCurrentApplicant(
  accessToken: string,
): Promise<Applicant> {
  const response = await api.get<Applicant>('/auth/me', {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  })

  return response.data
}

export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const responseData = error.response?.data

    if (
      responseData &&
      typeof responseData === 'object' &&
      'error' in responseData &&
      typeof responseData.error === 'string'
    ) {
      return responseData.error
    }

    if (error.response?.status === 400) {
      return 'Please check your details and try again.'
    }

    if (error.response?.status === 401) {
      return 'Your session has expired. Please log in again.'
    }

    if (error.response?.status === 403) {
      return 'You do not have permission to perform this action.'
    }

    if (!error.response) {
      return 'Cannot connect to the passport service. Check that the backend is running.'
    }
  }

  return 'Something went wrong. Please try again.'
}

// Applicant applications

export interface PassportApplication {
  id: number
  applicationNumber: string
  applicantId: number
  passportType: string
  applicationStatus: string
  submissionDate: string | null
  decisionDate: string | null
  decisionReason: string | null
  requestedDocumentType: ApplicationDocumentType | null
  createdAt: string
  updatedAt: string
}

export async function getMyPassportApplications(
  accessToken: string,
): Promise<PassportApplication[]> {
  const response = await api.get<PassportApplication[]>(
    '/applicant/applications',
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  )

  return response.data
}

export async function createPassportApplication(
  accessToken: string,
): Promise<PassportApplication> {
  const response = await api.post<PassportApplication>(
    '/applicant/applications',
    {},
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  )

  return response.data
}

export async function getMyPassportApplication(
  accessToken: string,
  applicationId: number,
): Promise<PassportApplication> {
  const response = await api.get<PassportApplication>(
    `/applicant/applications/${applicationId}`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  )

  return response.data
}

export async function submitPassportApplication(
  accessToken: string,
  applicationId: number,
): Promise<PassportApplication> {
  const response = await api.post<PassportApplication>(
    `/applicant/applications/${applicationId}/submit`,
    {},
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  )

  return response.data
}

// Applicant profile

export interface ApplicantProfile {
  id?: number
  userId?: number
  firstName: string
  middleName: string
  lastName: string
  dateOfBirth: string
  placeOfBirth: string
  gender: string
  nationality: string
  phoneNumber: string
  addressLine: string
  cityOrTown: string
  district: string
  region: string
  country: string
}

export type ApplicantProfileRequest = Omit<
  ApplicantProfile,
  'id' | 'userId'
>

export async function getApplicantProfile(
  accessToken: string,
): Promise<ApplicantProfile> {
  const response = await api.get<ApplicantProfile>(
    '/applicant/profile',
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  )

  return response.data
}

export async function saveApplicantProfile(
  accessToken: string,
  profile: ApplicantProfileRequest,
): Promise<ApplicantProfile> {
  const response = await api.put<ApplicantProfile>(
    '/applicant/profile',
    profile,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  )

  return response.data
}

// Application documents

export interface ApplicationDocument {
  id: number
  applicationId: number
  documentType: string
  originalFilename: string
  contentType: string
  fileSizeBytes: number
  documentStatus: string
  uploadedAt: string | null
  reviewedAt: string | null
  reviewNote: string | null
}

export type ApplicationDocumentType =
  | 'BIRTH_CERTIFICATE'
  | 'NATIONAL_ID'
  | 'PROOF_OF_CITIZENSHIP'
  | 'PASSPORT_PHOTO'
  | 'SUPPORTING_DOCUMENT'

export async function getApplicationDocuments(
  accessToken: string,
  applicationId: number,
): Promise<ApplicationDocument[]> {
  const response = await api.get<ApplicationDocument[]>(
    `/applicant/applications/${applicationId}/documents`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  )

  return response.data
}

export async function uploadApplicationDocument(
  accessToken: string,
  applicationId: number,
  documentType: ApplicationDocumentType,
  file: File,
): Promise<ApplicationDocument> {
  const formData = new FormData()
  formData.append('documentType', documentType)
  formData.append('file', file)

  const response = await api.post<ApplicationDocument>(
    `/applicant/applications/${applicationId}/documents`,
    formData,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  )

  return response.data
}

// Admin dashboard and application management

export interface AdminDashboardResponse {
  totalApplications: number
  applicationsByStatus: Record<string, number>
}

export interface AdminApplicationListItem {
  id: number
  applicationNumber: string
  applicantId: number
  applicantEmail: string
  firstName: string | null
  middleName: string | null
  lastName: string | null
  passportType: string
  applicationStatus: string
  submissionDate: string | null
  createdAt: string
}

export interface AdminApplicationStatusHistory {
  id: number
  previousStatus: string | null
  newStatus: string
  changedBy: number | null
  changeNote: string | null
  changedAt: string | null
}

export interface InterviewSchedule {
  id: number
  interviewDate: string
  interviewTime: string
  location: string
  mode: 'IN_PERSON' | 'ONLINE'
  meetingLink: string | null
  instructions: string | null
  createdAt: string
  updatedAt: string
}

export interface InterviewScheduleInput {
  interviewDate: string
  interviewTime: string
  location: string
  mode: 'IN_PERSON' | 'ONLINE'
  meetingLink?: string
  instructions?: string
}

export interface AdminApplicationDetail {
  id: number
  applicationNumber: string
  applicantId: number
  applicantEmail: string
  passportType: string
  applicationStatus: string
  submissionDate: string | null
  decisionDate: string | null
  decisionReason: string | null
  createdAt: string
  updatedAt: string
  applicantProfile: ApplicantProfile | null
  documents: ApplicationDocument[]
  statusHistory: AdminApplicationStatusHistory[]
  interviewSchedule: InterviewSchedule | null
}

export interface AdminApplicationPage {
  content: AdminApplicationListItem[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export async function getAdminDashboard(
  accessToken: string,
): Promise<AdminDashboardResponse> {
  const response = await api.get<AdminDashboardResponse>(
    '/admin/dashboard',
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  )

  return response.data
}

export async function getAdminApplications(
  accessToken: string,
  options: {
    search?: string
    status?: string
    page?: number
    size?: number
  } = {},
): Promise<AdminApplicationPage> {
  const response = await api.get<AdminApplicationPage>(
    '/admin/applications',
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
      params: options,
    },
  )

  return response.data
}

export async function getAdminApplicationDetail(
  accessToken: string,
  applicationId: number,
): Promise<AdminApplicationDetail> {
  const response = await api.get<AdminApplicationDetail>(
    `/admin/applications/${applicationId}`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  )

  return response.data
}

export async function updateAdminApplicationStatus(
  accessToken: string,
  applicationId: number,
  status: string,
  reason?: string,
  interview?: InterviewScheduleInput,
): Promise<AdminApplicationDetail> {
  const response = await api.patch<AdminApplicationDetail>(
    `/admin/applications/${applicationId}/status`,
    {
      status,
      reason,
      ...interview,
    },
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  )

  return response.data
}

export async function rescheduleAdminInterview(
  accessToken: string,
  applicationId: number,
  interview: InterviewScheduleInput,
): Promise<AdminApplicationDetail> {
  const response = await api.put<AdminApplicationDetail>(
    `/admin/applications/${applicationId}/interview`,
    interview,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  )

  return response.data
}

export async function reviewAdminDocument(
  accessToken: string,
  documentId: number,
  status: 'APPROVED' | 'CORRECTION_REQUIRED',
  note?: string,
): Promise<AdminApplicationDetail> {
  const response = await api.patch<AdminApplicationDetail>(
    `/admin/documents/${documentId}/review`,
    {
      status,
      note,
    },
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    },
  )

  return response.data
}

export async function getAdminDocumentFile(
  accessToken: string,
  documentId: number,
): Promise<Blob> {
  const response = await api.get<Blob>(
    `/admin/documents/${documentId}/file`,
    {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
      responseType: 'blob',
    },
  )

  return response.data
}

export default api
