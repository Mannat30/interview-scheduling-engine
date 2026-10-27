import api from "./axios";

export const authApi = {
    register: (request) => api.post("/auth/register", request),

    login: (request) => api.post("/auth/login", request),
};

export const interviewerApi = {
    list: () => api.get("/interviewers"),
};

export const bookingApi = {
    schedule: (request) =>
        api.post("/bookings", request),

    getParticipantBookings: (
        participantId,
        page = 0,
        size = 10
    ) =>
        api.get(`/bookings/participant/${participantId}`, {
            params: { page, size },
        }),

    cancel: (bookingId) =>
        api.patch(`/bookings/${bookingId}/cancel`),

    reschedule: (bookingId, request) =>
        api.patch(`/bookings/${bookingId}/reschedule`, request),

    complete: (bookingId) =>
        api.patch(`/bookings/${bookingId}/complete`),
};

export const availabilityApi = {
    add: (interviewerId, request) =>
        api.post(
            `/interviewers/${interviewerId}/availability`,
            request
        ),

    get: (interviewerId, dayOfWeek) =>
        api.get(
            `/interviewers/${interviewerId}/availability`,
            {
                params: { dayOfWeek },
            }
        ),

    getSlots: (
        interviewerId,
        candidateId,
        date,
        durationMinutes = 30
    ) =>
        api.get(
            `/interviewers/${interviewerId}/available-slots`,
            {
                params: {
                    candidateId,
                    date,
                    durationMinutes,
                },
            }
        ),
};