import { useCallback, useEffect, useState } from "react";
import {
    availabilityApi,
    bookingApi,
    interviewerApi,
} from "../api/interviewApi";
import "./InterviewWorkspace.css";
import InterviewCalendar from "./InterviewCalendar";
// const [bookingPage, setBookingPage] = useState(0);
// const [bookingPageSize] = useState(10);
// const [bookingTotalPages, setBookingTotalPages] = useState(0);
// const [bookingTotalElements, setBookingTotalElements] = useState(0);

function getNextMonday() {
    const date = new Date();
    date.setHours(0, 0, 0, 0);

    const daysUntilMonday = (8 - date.getDay()) % 7 || 7;
    date.setDate(date.getDate() + daysUntilMonday);

    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function apiError(error, fallback) {
    if (!error.response) {
        return "Cannot connect to the backend. Check that Spring Boot and PostgreSQL are running.";
    }

    if (error.response.status === 401) {
        return "Your session is invalid or expired. Please log out and sign in again.";
    }

    if (error.response.status === 403) {
        return "Your account does not have permission for this action.";
    }

    if (error.response.status === 409) {
        return "This time conflicts with another booking. Refresh available slots and try again.";
    }

    return (
        error.response.data?.message ||
        error.response.data?.error ||
        fallback
    );
}

function normalizeList(data) {
    if (Array.isArray(data)) return data;
    if (Array.isArray(data?.content)) return data.content;
    if (Array.isArray(data?.slots)) return data.slots;
    return [];
}

function formatTime(value) {
    if (!value) return "Time unavailable";

    const timePart = value.includes("T")
        ? value.split("T")[1].slice(0, 5)
        : value;

    const [hourString, minute] = timePart.split(":");
    const hour = Number(hourString);

    if (Number.isNaN(hour)) return value;

    return `${hour % 12 || 12}:${minute} ${hour >= 12 ? "PM" : "AM"}`;
}

function formatDateTime(value) {
    if (!value) return "Date unavailable";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) return value;

    return date.toLocaleString(undefined, {
        dateStyle: "medium",
        timeStyle: "short",
    });
}

const WEEK_DAYS = [
    "SUNDAY",
    "MONDAY",
    "TUESDAY",
    "WEDNESDAY",
    "THURSDAY",
    "FRIDAY",
    "SATURDAY",
];

function getWeekDayForDate(dateString) {
    if (!dateString) return "MONDAY";

    return WEEK_DAYS[
        new Date(`${dateString}T12:00:00`).getDay()
        ];
}

function getTodayDateKey() {
    const today = new Date();

    return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}-${String(today.getDate()).padStart(2, "0")}`;
}

export default function InterviewWorkspace() {
    const userId = Number(localStorage.getItem("userId"));
    const role = localStorage.getItem("userRole");

    const isCandidate = role === "CANDIDATE";
    const isInterviewer = role === "INTERVIEWER";

    // Booking history and filters
    const [bookings, setBookings] = useState([]);
    const [bookingsLoading, setBookingsLoading] = useState(true);
    const [bookingsError, setBookingsError] = useState("");
    const [bookingFilter, setBookingFilter] = useState("ALL");
    const [bookingSearch, setBookingSearch] = useState("");

    // Interviewer dropdown
    const [interviewerId, setInterviewerId] = useState("");
    const [interviewers, setInterviewers] = useState([]);
    const [interviewersLoading, setInterviewersLoading] = useState(false);
    const [interviewersError, setInterviewersError] = useState("");

    // Booking and available slots
    const [date, setDate] = useState(getNextMonday);
    const [duration, setDuration] = useState("30");
    const [slots, setSlots] = useState([]);
    const [selectedSlot, setSelectedSlot] = useState(null);
    const [slotsLoading, setSlotsLoading] = useState(false);
    const [slotsError, setSlotsError] = useState("");

    const [bookingInProgress, setBookingInProgress] = useState(false);
    const [reschedulingBookingId, setReschedulingBookingId] = useState(null);
    const [actionInProgress, setActionInProgress] = useState(null);
    const [message, setMessage] = useState("");
    const [bookingPage, setBookingPage] = useState(0);
    const [bookingPageSize] = useState(10);
    const [bookingTotalPages, setBookingTotalPages] = useState(0);
    const [bookingTotalElements, setBookingTotalElements] = useState(0);

    // Interviewer availability
    const [availabilityDay, setAvailabilityDay] = useState("MONDAY");

    const [availabilityForm, setAvailabilityForm] = useState({
        startTime: "09:00",
        endTime: "12:00",
    });

    const [availabilityWindows, setAvailabilityWindows] = useState([]);
    const [availabilityLoading, setAvailabilityLoading] = useState(false);

    // Calendar
    const [calendarSelectedDate, setCalendarSelectedDate] =
        useState(getTodayDateKey);

    const [weeklyCalendarAvailability, setWeeklyCalendarAvailability] =
        useState({});

    // Load participant bookings
    const loadBookings = useCallback(async () => {
        if (!Number.isInteger(userId) || userId <= 0) {
            setBookingsError(
                "Your user ID is missing. Please sign in again."
            );
            setBookingsLoading(false);
            return;
        }

        setBookingsLoading(true);
        setBookingsError("");

        try {
            const response = await bookingApi.getParticipantBookings(
                userId,
                bookingPage,
                bookingPageSize
            );

            const data = response.data;

            setBookings(normalizeList(data));
            setBookingTotalPages(data.totalPages ?? 0);
            setBookingTotalElements(data.totalElements ?? 0);
        } catch (error) {
            setBookingsError(
                apiError(error, "Could not load your interview history.")
            );
        } finally {
            setBookingsLoading(false);
        }
    }, [userId, bookingPage, bookingPageSize]);
    // Fetch registered interviewers for the dropdown
    const loadInterviewers = useCallback(async () => {
        if (!isCandidate) return;

        setInterviewersLoading(true);
        setInterviewersError("");

        try {
            const response = await interviewerApi.list();

            const list = normalizeList(response.data).filter((person) => {
                const id = Number(person.id);
                return Number.isInteger(id) && id > 0;
            });

            setInterviewers(list);

            setInterviewerId((current) => {
                if (
                    current &&
                    list.some(
                        (person) => String(person.id) === current
                    )
                ) {
                    return current;
                }

                return list.length > 0 ? String(list[0].id) : "";
            });

            if (list.length === 0) {
                setInterviewersError(
                    "No interviewers are registered yet."
                );
            }
        } catch (error) {
            setInterviewers([]);
            setInterviewerId("");

            setInterviewersError(
                apiError(
                    error,
                    "Could not load interviewers. Check the GET /api/interviewers endpoint."
                )
            );
        } finally {
            setInterviewersLoading(false);
        }
    }, [isCandidate]);

    // Load available slots for the selected interviewer
    const loadSlots = useCallback(async () => {
        if (!isCandidate) return;

        if (!interviewerId) {
            setSlots([]);
            setSelectedSlot(null);
            setSlotsError(
                interviewersLoading
                    ? ""
                    : "Choose an interviewer from the dropdown first."
            );
            return;
        }

        if (!Number.isInteger(userId) || userId <= 0) {
            setSlots([]);
            setSlotsError("User ID is missing. Please sign in again.");
            return;
        }

        const numericInterviewerId = Number(interviewerId);

        if (
            !Number.isInteger(numericInterviewerId) ||
            numericInterviewerId <= 0
        ) {
            setSlots([]);
            setSlotsError("Choose a valid interviewer.");
            return;
        }

        if (numericInterviewerId === userId) {
            setSlots([]);
            setSlotsError("You cannot book an interview with yourself.");
            return;
        }

        if (!date) return;

        setSlotsLoading(true);
        setSlotsError("");
        setSelectedSlot(null);

        try {
            const response = await availabilityApi.getSlots(
                numericInterviewerId,
                userId,
                date,
                Number(duration)
            );

            setSlots(normalizeList(response.data));
        } catch (error) {
            setSlots([]);

            setSlotsError(
                apiError(error, "Could not load available slots.")
            );
        } finally {
            setSlotsLoading(false);
        }
    }, [
        isCandidate,
        userId,
        interviewerId,
        date,
        duration,
        interviewersLoading,
    ]);

    // Load availability for the selected weekday
    const loadAvailability = useCallback(async () => {
        if (
            !isInterviewer ||
            !Number.isInteger(userId) ||
            userId <= 0
        ) {
            return;
        }

        setAvailabilityLoading(true);

        try {
            const response = await availabilityApi.get(
                userId,
                availabilityDay
            );

            setAvailabilityWindows(normalizeList(response.data));
        } catch (error) {
            setMessage(
                apiError(
                    error,
                    "Could not load interviewer availability."
                )
            );
        } finally {
            setAvailabilityLoading(false);
        }
    }, [isInterviewer, userId, availabilityDay]);

    // Load weekly availability for the calendar
    const loadCalendarAvailability = useCallback(async () => {
        if (
            !isInterviewer ||
            !Number.isInteger(userId) ||
            userId <= 0
        ) {
            return;
        }

        try {
            const entries = await Promise.all(
                WEEK_DAYS.map(async (day) => {
                    const response = await availabilityApi.get(
                        userId,
                        day
                    );

                    return [day, normalizeList(response.data)];
                })
            );

            setWeeklyCalendarAvailability(
                Object.fromEntries(entries)
            );
        } catch {
            setWeeklyCalendarAvailability({});
        }
    }, [isInterviewer, userId]);

    // Initial data loading
    useEffect(() => {
        loadBookings();
    }, [loadBookings]);

    useEffect(() => {
        loadInterviewers();
    }, [loadInterviewers]);

    useEffect(() => {
        loadSlots();
    }, [loadSlots]);

    useEffect(() => {
        loadAvailability();
    }, [loadAvailability]);

    useEffect(() => {
        loadCalendarAvailability();
    }, [loadCalendarAvailability]);

    // Book an interview or reschedule an existing booking
    async function handleBookInterview() {
        if (!selectedSlot) {
            setMessage("Please select an available time slot first.");
            return;
        }

        if (!selectedSlot.startAt || !selectedSlot.endAt) {
            setMessage(
                "This slot does not contain valid start and end times."
            );
            return;
        }

        if (!Number.isInteger(userId) || userId <= 0) {
            setMessage("User ID is missing. Please sign in again.");
            return;
        }

        if (!interviewerId) {
            setMessage("Choose an interviewer first.");
            return;
        }

        const currentReschedulingId = reschedulingBookingId;

        setBookingInProgress(true);
        setMessage("");

        try {
            if (currentReschedulingId !== null) {
                await bookingApi.reschedule(currentReschedulingId, {
                    startAt: selectedSlot.startAt,
                    endAt: selectedSlot.endAt,
                });

                setMessage(
                    `Interview #${currentReschedulingId} rescheduled successfully!`
                );

                setReschedulingBookingId(null);
            } else {
                await bookingApi.schedule({
                    candidateId: userId,
                    interviewerId: Number(interviewerId),
                    startAt: selectedSlot.startAt,
                    endAt: selectedSlot.endAt,
                });

                setMessage("Interview booked successfully!");
            }

            setSelectedSlot(null);

            await loadBookings();
            await loadSlots();
        } catch (error) {
            setMessage(
                apiError(
                    error,
                    currentReschedulingId !== null
                        ? "Could not reschedule this interview."
                        : "Could not book this interview."
                )
            );

            await loadSlots();
        } finally {
            setBookingInProgress(false);
        }
    }

    // Enter rescheduling mode
    function startReschedule(booking) {
        setReschedulingBookingId(booking.id);
        setInterviewerId(String(booking.interviewerId));

        if (booking.startAt) {
            setDate(String(booking.startAt).slice(0, 10));
        }

        setSelectedSlot(null);
        setSlots([]);
        setSlotsError("");

        setMessage(
            `Choose a new available slot for interview #${booking.id}.`
        );

        window.setTimeout(() => {
            document.querySelector(".live-booking-form")?.scrollIntoView({
                behavior: "smooth",
                block: "center",
            });
        }, 0);
    }

    // Exit rescheduling without changing the original booking
    function stopReschedule() {
        setReschedulingBookingId(null);
        setSelectedSlot(null);
        setSlots([]);
        setSlotsError("");

        setMessage(
            "Rescheduling cancelled. Your original booking is unchanged."
        );
    }

    // Cancel an interview
    async function handleCancel(bookingId) {
        if (!window.confirm("Cancel this interview booking?")) return;

        setActionInProgress(bookingId);
        setMessage("");

        try {
            await bookingApi.cancel(bookingId);

            setMessage(`Booking #${bookingId} cancelled.`);

            if (reschedulingBookingId === bookingId) {
                setReschedulingBookingId(null);
                setSelectedSlot(null);
            }

            await loadBookings();
            await loadSlots();
        } catch (error) {
            setMessage(
                apiError(error, "Could not cancel this interview.")
            );
        } finally {
            setActionInProgress(null);
        }
    }

    // Mark an interview as completed
    async function handleComplete(bookingId) {
        setActionInProgress(bookingId);
        setMessage("");

        try {
            await bookingApi.complete(bookingId);

            setMessage(
                `Booking #${bookingId} marked as completed.`
            );

            await loadBookings();
        } catch (error) {
            setMessage(
                apiError(error, "Could not complete this interview.")
            );
        } finally {
            setActionInProgress(null);
        }
    }

    // Add weekly availability
    async function handleAddAvailability(event) {
        event.preventDefault();
        setMessage("");

        if (
            availabilityForm.startTime >=
            availabilityForm.endTime
        ) {
            setMessage("End time must be after start time.");
            return;
        }

        setActionInProgress("availability");

        try {
            await availabilityApi.add(userId, {
                dayOfWeek: availabilityDay,
                startTime: availabilityForm.startTime,
                endTime: availabilityForm.endTime,
            });

            setMessage("Availability added successfully.");

            await Promise.all([
                loadAvailability(),
                loadCalendarAvailability(),
            ]);
        } catch (error) {
            setMessage(
                apiError(error, "Could not add availability.")
            );
        } finally {
            setActionInProgress(null);
        }
    }

    // Search and filter existing booking records
    const filteredBookings = bookings.filter((booking) => {
        const matchesStatus =
            bookingFilter === "ALL" ||
            booking.status === bookingFilter;

        const query = bookingSearch.trim().toLowerCase();

        const searchableText = [
            booking.id,
            booking.interviewerId,
            booking.candidateId,
            booking.status,
            booking.startAt,
        ]
            .filter((value) => value != null)
            .join(" ")
            .toLowerCase();

        return matchesStatus && searchableText.includes(query);
    });

    // Show a separate message for accounts without a supported role
    if (!isCandidate && !isInterviewer) {
        return (
            <section className="live-workspace">
                <h2>Workspace unavailable</h2>
                <p>
                    This workspace is configured for candidate and interviewer
                    accounts. Your current role is {role || "unknown"}.
                </p>
            </section>
        );
    }

    // Main dashboard
    return (
        <section className="live-workspace" id="live-workspace">
            {/* Workspace heading */}
            <div className="live-workspace-heading">
                <div>
                    <span className="live-kicker">
                        CONNECTED TO YOUR BACKEND
                    </span>

                    <h2>
                        {isCandidate
                            ? "Your interview workspace"
                            : "Your interviewer workspace"}
                    </h2>

                    <p>
                        {isCandidate
                            ? "Find available time slots and manage your bookings."
                            : "Manage your availability and scheduled interviews."}
                    </p>
                </div>

                <button
                    type="button"
                    className="live-refresh"
                    onClick={loadBookings}
                    disabled={bookingsLoading}
                >
                    {bookingsLoading
                        ? "Refreshing…"
                        : "Refresh bookings"}
                </button>
            </div>

            {/* Success and error messages */}
            {message && (
                <div className="live-message" role="status">
                    <span>{message}</span>

                    <button
                        type="button"
                        onClick={() => setMessage("")}
                        aria-label="Dismiss message"
                    >
                        ×
                    </button>
                </div>
            )}

            {/* Calendar */}
            <InterviewCalendar
                title={
                    isCandidate
                        ? "Interview calendar"
                        : "Your schedule calendar"
                }
                subtitle={
                    isCandidate
                        ? "Choose a date to find available interview slots."
                        : "View your schedule and weekly availability."
                }
                role={role}
                bookings={bookings}
                selectedDate={
                    isCandidate ? date : calendarSelectedDate
                }
                availabilityByDay={weeklyCalendarAvailability}
                onSelectDate={(selectedDate) => {
                    if (isCandidate) {
                        setDate(selectedDate);
                    } else {
                        setCalendarSelectedDate(selectedDate);
                        setAvailabilityDay(
                            getWeekDayForDate(selectedDate)
                        );
                    }
                }}
            />

            {/* Candidate booking and rescheduling form */}
            {isCandidate && (
                <div className="live-panel">
                    <div className="live-panel-heading">
                        <div>
                            <span className="live-kicker">
                                BOOK A REAL INTERVIEW
                            </span>

                            <h3>
                                {reschedulingBookingId !== null
                                    ? `Reschedule interview #${reschedulingBookingId}`
                                    : "Find available time"}
                            </h3>

                            <p>
                                {reschedulingBookingId !== null
                                    ? "Choose a new available slot. Your existing booking remains unchanged until rescheduling succeeds."
                                    : "Select an interviewer and find an available slot."}
                            </p>
                        </div>
                    </div>

                    <div className="live-booking-form">
                        <label>
                            Interviewer

                            <select
                                value={interviewerId}
                                onChange={(event) =>
                                    setInterviewerId(event.target.value)
                                }
                                disabled={
                                    interviewersLoading ||
                                    reschedulingBookingId !== null
                                }
                                required
                            >
                                <option value="">
                                    {interviewersLoading
                                        ? "Loading interviewers…"
                                        : "Choose an interviewer"}
                                </option>

                                {interviewers.map((person) => (
                                    <option
                                        value={String(person.id)}
                                        key={person.id}
                                    >
                                        {person.fullName ||
                                            person.name ||
                                            `Interviewer #${person.id}`}{" "}
                                        (ID: {person.id})
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label>
                            Interview date

                            <input
                                type="date"
                                min={getTodayDateKey()}
                                value={date}
                                onChange={(event) =>
                                    setDate(event.target.value)
                                }
                                required
                            />
                        </label>

                        <label>
                            Duration

                            <select
                                value={duration}
                                onChange={(event) =>
                                    setDuration(event.target.value)
                                }
                            >
                                <option value="15">15 minutes</option>
                                <option value="30">30 minutes</option>
                                <option value="45">45 minutes</option>
                                <option value="60">60 minutes</option>
                                <option value="90">90 minutes</option>
                                <option value="120">120 minutes</option>
                            </select>
                        </label>

                        <button
                            type="button"
                            className="live-primary-button"
                            onClick={loadSlots}
                            disabled={
                                slotsLoading ||
                                interviewersLoading ||
                                !interviewerId
                            }
                        >
                            {slotsLoading
                                ? "Loading slots…"
                                : "Find slots"}
                        </button>
                    </div>

                    {interviewersError && (
                        <div className="live-inline-error" role="alert">
                            {interviewersError}

                            <button
                                type="button"
                                className="live-refresh"
                                onClick={loadInterviewers}
                                disabled={interviewersLoading}
                            >
                                {interviewersLoading ? "Loading…" : "Retry"}
                            </button>
                        </div>
                    )}

                    {slotsError && (
                        <div className="live-inline-error" role="alert">
                            {slotsError}
                        </div>
                    )}

                    <div className="live-slots-heading">
                        <strong>Available slots</strong>

                        <span>
                            {slots.length} slot(s) returned by the server
                        </span>
                    </div>

                    {slotsLoading ? (
                        <p className="live-empty">
                            Checking available slots…
                        </p>
                    ) : slots.length === 0 ? (
                        <p className="live-empty">
                            No available slots were returned for this date.
                            Try another date or verify the interviewer's availability.
                        </p>
                    ) : (
                        <div className="live-slots-grid">
                            {slots.map((slot) => (
                                <button
                                    type="button"
                                    key={`${slot.startAt}-${slot.endAt}`}
                                    className={`live-slot ${
                                        selectedSlot?.startAt === slot.startAt
                                            ? "live-slot-selected"
                                            : ""
                                    }`}
                                    onClick={() => setSelectedSlot(slot)}
                                    aria-pressed={
                                        selectedSlot?.startAt === slot.startAt
                                    }
                                    disabled={bookingInProgress}
                                >
                                    <strong>
                                        {formatTime(
                                            slot.localStartAt ||
                                            slot.startAt
                                        )}
                                    </strong>

                                    <span>
                                        {slot.localEndAt
                                            ? `Ends ${formatTime(slot.localEndAt)}`
                                            : `${duration} minutes`}
                                    </span>
                                </button>
                            ))}
                        </div>
                    )}

                    <div className="live-booking-footer">
                        <span>
                            {selectedSlot
                                ? `Selected: ${formatTime(
                                    selectedSlot.localStartAt ||
                                    selectedSlot.startAt
                                )}`
                                : "Choose a slot before continuing."}
                        </span>

                        <button
                            type="button"
                            className="live-primary-button"
                            onClick={handleBookInterview}
                            disabled={
                                !selectedSlot ||
                                bookingInProgress
                            }
                        >
                            {bookingInProgress
                                ? "Processing…"
                                : reschedulingBookingId !== null
                                    ? "Confirm reschedule"
                                    : "Book interview"}
                        </button>

                        {reschedulingBookingId !== null && (
                            <button
                                type="button"
                                className="live-cancel-button"
                                onClick={stopReschedule}
                                disabled={bookingInProgress}
                            >
                                Keep original booking
                            </button>
                        )}
                    </div>
                </div>
            )}

            {/* Interviewer availability management */}
            {isInterviewer && (
                <div className="live-panel">
                    <div className="live-panel-heading">
                        <div>
                            <span className="live-kicker">
                                SET YOUR SCHEDULE
                            </span>

                            <h3>Manage availability</h3>

                            <p>
                                Add a weekly availability window for candidates to book.
                            </p>
                        </div>
                    </div>

                    <form
                        className="live-booking-form"
                        onSubmit={handleAddAvailability}
                    >
                        <label>
                            Day

                            <select
                                value={availabilityDay}
                                onChange={(event) =>
                                    setAvailabilityDay(event.target.value)
                                }
                            >
                                {[
                                    "MONDAY",
                                    "TUESDAY",
                                    "WEDNESDAY",
                                    "THURSDAY",
                                    "FRIDAY",
                                    "SATURDAY",
                                    "SUNDAY",
                                ].map((day) => (
                                    <option value={day} key={day}>
                                        {day.charAt(0) +
                                            day.slice(1).toLowerCase()}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label>
                            Start time

                            <input
                                type="time"
                                value={availabilityForm.startTime}
                                onChange={(event) =>
                                    setAvailabilityForm((current) => ({
                                        ...current,
                                        startTime: event.target.value,
                                    }))
                                }
                                required
                            />
                        </label>

                        <label>
                            End time

                            <input
                                type="time"
                                value={availabilityForm.endTime}
                                onChange={(event) =>
                                    setAvailabilityForm((current) => ({
                                        ...current,
                                        endTime: event.target.value,
                                    }))
                                }
                                required
                            />
                        </label>

                        <button
                            type="submit"
                            className="live-primary-button"
                            disabled={
                                actionInProgress === "availability"
                            }
                        >
                            {actionInProgress === "availability"
                                ? "Saving…"
                                : "Add availability"}
                        </button>
                    </form>

                    <div className="live-slots-heading">
                        <strong>Configured windows</strong>

                        <button
                            type="button"
                            className="live-refresh"
                            onClick={loadAvailability}
                            disabled={availabilityLoading}
                        >
                            {availabilityLoading ? "Loading…" : "Refresh"}
                        </button>
                    </div>

                    {availabilityLoading ? (
                        <p className="live-empty">
                            Loading availability…
                        </p>
                    ) : availabilityWindows.length === 0 ? (
                        <p className="live-empty">
                            No enabled availability windows found for this day.
                        </p>
                    ) : (
                        <div className="live-availability-list">
                            {availabilityWindows.map((window, index) => (
                                <div
                                    className="live-availability-item"
                                    key={
                                        window.id ??
                                        `${window.startTime}-${index}`
                                    }
                                >
                                    <span>{window.dayOfWeek}</span>

                                    <strong>
                                        {formatTime(window.startTime)} –{" "}
                                        {formatTime(window.endTime)}
                                    </strong>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            )}

            {/* Booking history with search and filters */}
            <div className="live-panel">
                <div className="live-panel-heading">
                    <div>
                        <span className="live-kicker">
                            YOUR ACTUAL DATABASE RECORDS
                        </span>

                        <h3>
                            {isCandidate
                                ? "My bookings"
                                : "Assigned interviews"}
                        </h3>

                        <p>
                            Search and filter your existing interview records.
                        </p>
                    </div>

                    <span className="live-count">
                        {filteredBookings.length}
                    </span>
                </div>

                {/* Search input and status dropdown */}
                <div className="live-booking-form">
                    <label>
                        Search interviews

                        <input
                            type="text"
                            placeholder="Search ID, participant, status or date"
                            value={bookingSearch}
                            onChange={(event) =>
                                setBookingSearch(event.target.value)
                            }
                        />
                    </label>

                    <label>
                        Filter by status

                        <select
                            value={bookingFilter}
                            onChange={(event) =>
                                setBookingFilter(event.target.value)
                            }
                        >
                            <option value="ALL">All interviews</option>
                            <option value="SCHEDULED">Scheduled</option>
                            <option value="COMPLETED">Completed</option>
                            <option value="CANCELLED">Cancelled</option>
                        </select>
                    </label>
                </div>

                {bookingsLoading ? (
                    <p className="live-empty">
                        Loading bookings…
                    </p>
                ) : bookingsError ? (
                    <div className="live-inline-error" role="alert">
                        {bookingsError}
                    </div>
                ) : bookings.length === 0 ? (
                    <p className="live-empty">
                        No bookings found for this account yet.
                    </p>
                ) : filteredBookings.length === 0 ? (
                    <p className="live-empty">
                        No bookings match your search or selected status.
                        Try changing the filters.
                    </p>
                ) : (
                    <div className="live-bookings-list">
                        {filteredBookings.map((booking) => {
                            const scheduled =
                                booking.status === "SCHEDULED";

                            return (
                                <article
                                    className="live-booking-row"
                                    key={booking.id}
                                >
                                    <div className="live-booking-date-icon">
                                        IS
                                    </div>

                                    <div className="live-booking-details">
                                        <strong>
                                            Interview #{booking.id}
                                        </strong>

                                        <span>
                                            {isCandidate
                                                ? `Interviewer #${booking.interviewerId}`
                                                : `Candidate #${booking.candidateId}`}
                                        </span>

                                        <span>
                                            {formatDateTime(booking.startAt)}
                                        </span>

                                        <span
                                            className={`live-status live-status-${String(
                                                booking.status || "UNKNOWN"
                                            ).toLowerCase()}`}
                                        >
                                            {booking.status || "Unknown"}
                                        </span>
                                    </div>

                                    {scheduled && (
                                        <div className="live-booking-actions">
                                            {isInterviewer && (
                                                <button
                                                    type="button"
                                                    className="live-action-button"
                                                    onClick={() =>
                                                        handleComplete(booking.id)
                                                    }
                                                    disabled={
                                                        actionInProgress ===
                                                        booking.id
                                                    }
                                                >
                                                    {actionInProgress ===
                                                    booking.id
                                                        ? "Working…"
                                                        : "Complete"}
                                                </button>
                                            )}

                                            {isCandidate && (
                                                <button
                                                    type="button"
                                                    className="live-action-button"
                                                    onClick={() =>
                                                        startReschedule(booking)
                                                    }
                                                    disabled={
                                                        actionInProgress ===
                                                        booking.id
                                                    }
                                                >
                                                    Reschedule
                                                </button>
                                            )}

                                            <button
                                                type="button"
                                                className="live-cancel-button"
                                                onClick={() =>
                                                    handleCancel(booking.id)
                                                }
                                                disabled={
                                                    actionInProgress ===
                                                    booking.id
                                                }
                                            >
                                                {actionInProgress ===
                                                booking.id
                                                    ? "Working…"
                                                    : "Cancel"}
                                            </button>

                                        </div>

                                    )}
                                </article>
                            );
                        })}

                    </div>
                )}

                <div className="live-pagination">
                        <span>
                            Showing {bookings.length} of{" "}
                            {bookingTotalElements} bookings
                        </span>

                    <div>
                        <button
                            type="button"
                            className="live-action-button"
                            disabled={
                                bookingPage <= 0 || bookingsLoading
                            }
                            onClick={() =>
                                setBookingPage((page) => page - 1)
                            }
                        >
                            Previous
                        </button>

                        <span>
                                Page{" "}
                            {bookingTotalPages === 0
                                ? 0
                                : bookingPage + 1}{" "}
                            of {bookingTotalPages}
                            </span>

                        <button
                            type="button"
                            className="live-action-button"
                            disabled={
                                bookingsLoading ||
                                bookingPage + 1 >= bookingTotalPages
                            }
                            onClick={() =>
                                setBookingPage((page) => page + 1)
                            }
                        >
                            Next
                        </button>
                    </div>
                </div>
            </div>
        </section>
    );
}

