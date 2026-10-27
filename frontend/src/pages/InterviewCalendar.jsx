import "./InterviewCalendar.css";
import { useEffect, useMemo, useState } from "react";
import "./InterviewCalendar.css";

const WEEK_DAYS = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
const DAY_LABELS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

function toDateKey(date) {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function getDateKeyFromValue(value) {
    if (!value) return "";
    if (typeof value === "string" && /^\d{4}-\d{2}-\d{2}/.test(value)) {
        // Booking timestamps usually arrive as ISO strings. Use the local date for display.
        const parsed = new Date(value);
        if (!Number.isNaN(parsed.getTime())) return toDateKey(parsed);
        return value.slice(0, 10);
    }
    const parsed = new Date(value);
    return Number.isNaN(parsed.getTime()) ? "" : toDateKey(parsed);
}

function getTodayKey() {
    return toDateKey(new Date());
}

function getBookedItems(bookings) {
    const map = {};
    bookings.forEach((booking) => {
        const status = String(booking.status || "").toUpperCase();
        if (["CANCELLED", "CANCELED"].includes(status)) return;
        const key = getDateKeyFromValue(booking.startAt || booking.startsAt || booking.startTime);
if (!key) return;
if (!map[key]) map[key] = [];
map[key].push(booking);
});
return map;
}

export default function InterviewCalendar({
                                              title = "Calendar",
                                              subtitle = "View your schedule.",
                                              role,
                                              bookings = [],
                                              selectedDate,
                                              availabilityByDay = {},
                                              onSelectDate,
                                          }) {
    const initialDate = selectedDate ? new Date(`${selectedDate}T12:00:00`) : new Date();
    const [visibleMonth, setVisibleMonth] = useState(
        new Date(initialDate.getFullYear(), initialDate.getMonth(), 1)
    );

    useEffect(() => {
        if (!selectedDate) return;
        const selected = new Date(`${selectedDate}T12:00:00`);
        if (!Number.isNaN(selected.getTime())) {
            setVisibleMonth(new Date(selected.getFullYear(), selected.getMonth(), 1));
        }
    }, [selectedDate]);

    const bookedByDate = useMemo(() => getBookedItems(bookings), [bookings]);
    const year = visibleMonth.getFullYear();
    const month = visibleMonth.getMonth();
    const monthLabel = visibleMonth.toLocaleDateString(undefined, { month: "long", year: "numeric" });
    const firstWeekday = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const cellCount = Math.ceil((firstWeekday + daysInMonth) / 7) * 7;
    const todayKey = getTodayKey();
    const isInterviewer = role === "INTERVIEWER";

    const cells = Array.from({ length: cellCount }, (_, index) => {
        const dayNumber = index - firstWeekday + 1;
        if (dayNumber < 1 || dayNumber > daysInMonth) return null;
        const dayDate = new Date(year, month, dayNumber, 12);
        return { date: dayDate, key: toDateKey(dayDate), dayNumber, weekday: WEEK_DAYS[dayDate.getDay()] };
    });

    function moveMonth(amount) {
        setVisibleMonth((current) => new Date(current.getFullYear(), current.getMonth() + amount, 1));
    }

    return (
        <section className="iw-calendar-panel" aria-label={title}>
            <header className="iw-calendar-header">
                <div>
                    <span className="iw-calendar-eyebrow">SCHEDULE OVERVIEW</span>
                    <h3>{title}</h3>
                    <p>{subtitle}</p>
                </div>
                <div className="iw-calendar-month-controls">
                    <button type="button" onClick={() => moveMonth(-1)} aria-label="Previous month">
                        ‹
                    </button>
                    <strong>{monthLabel}</strong>
                    <button type="button" onClick={() => moveMonth(1)} aria-label="Next month">
                        ›
                    </button>
                </div>
            </header>

            <div className="iw-calendar-grid" role="grid" aria-label={monthLabel}>
                {DAY_LABELS.map((day) => (
                    <div className="iw-calendar-weekday" role="columnheader" key={day}>{day}</div>
                ))}
                {cells.map((cell, index) => {
                    if (!cell) return <div className="iw-calendar-empty" role="gridcell" key={`empty-${index}`} />;

                    const isSelected = selectedDate === cell.key;
                    const isToday = todayKey === cell.key;
                    const booked = bookedByDate[cell.key] || [];
                    const hasAvailability = isInterviewer
                        && cell.key >= todayKey
                        && (availabilityByDay[cell.weekday] || []).length > 0;

                    return (
                        <button
                            type="button"
                            role="gridcell"
                            key={cell.key}
                            className={`iw-calendar-day${isSelected ? " is-selected" : ""}${isToday ? " is-today" : ""}`}
                            onClick={() => onSelectDate?.(cell.key)}
                            aria-pressed={isSelected}
                            aria-label={`${cell.key}${booked.length ? `, ${booked.length} interview(s)` : ""}${hasAvailability ? ", availability configured" : ""}`}
                        >
                            <span className="iw-calendar-day-number">{cell.dayNumber}</span>
                            <span className="iw-calendar-day-markers">
                                {booked.length > 0 && (
                                    <span className="iw-calendar-marker is-booked">
                                        {booked.length} interview{booked.length > 1 ? "s" : ""}
                                    </span>
                                )}
                                {hasAvailability && (
                                    <span className="iw-calendar-marker is-available">Available</span>
                                )}
                            </span>
                        </button>
                    );
                })}
            </div>

            <footer className="iw-calendar-footer">
                <div className="iw-calendar-legend">
                    {isInterviewer && <span><i className="iw-legend-dot is-available-dot" /> Weekly availability</span>}
                    <span><i className="iw-legend-dot is-booked-dot" /> Scheduled interview</span>
                    <span><i className="iw-legend-dot is-today-dot" /> Today</span>
                </div>
                <p>
                    {selectedDate
                        ? `Selected date: ${new Date(`${selectedDate}T12:00:00`).toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" })}`
                        : "Select a date on the calendar."}
                </p>
            </footer>
        </section>
    );
}

