import { useState } from "react";
import { useNavigate } from "react-router-dom";
import InterviewWorkspace from "./InterviewWorkspace";
import "./Dashboard.css";

function Icon({ name, size = 19 }) {
    const paths = {
        grid: (
            <>
                <rect x="3" y="3" width="7" height="7" rx="1.5" />
                <rect x="14" y="3" width="7" height="7" rx="1.5" />
                <rect x="3" y="14" width="7" height="7" rx="1.5" />
                <rect x="14" y="14" width="7" height="7" rx="1.5" />
            </>
        ),
        calendar: (
            <>
                <rect x="3" y="5" width="18" height="16" rx="2" />
                <path d="M16 3v4M8 3v4M3 10h18" />
            </>
        ),
        clock: (
            <>
                <circle cx="12" cy="12" r="9" />
                <path d="M12 7v5l3 2" />
            </>
        ),
        users: (
            <>
                <path d="M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="10" cy="7" r="4" />
                <path d="M20 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
            </>
        ),
        chart: (
            <>
                <path d="M3 3v18h18" />
                <path d="m7 14 4-4 4 3 6-7" />
            </>
        ),
        search: (
            <>
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-4-4" />
            </>
        ),
        bell: (
            <>
                <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
                <path d="M10 21h4" />
            </>
        ),
        briefcase: (
            <>
                <rect x="3" y="7" width="18" height="14" rx="2" />
                <path d="M8 7V4h8v3M3 12h18" />
            </>
        ),
        plus: <path d="M12 5v14M5 12h14" />,
        chevron: <path d="m9 18 6-6-6-6" />,
        down: <path d="m7 10 5 5 5-5" />,
        logout: (
            <>
                <path d="M10 17l5-5-5-5M15 12H3" />
                <path d="M12 3h6a3 3 0 0 1 3 3v12a3 3 0 0 1-3 3h-6" />
            </>
        ),
        sparkle: (
            <>
                <path d="m12 3 1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3Z" />
                <path d="m19 14 1.2 2.8L23 18l-2.8 1.2L19 22l-1.2-2.8L15 18l2.8-1.2L19 14Z" />
            </>
        ),
        shield: (
            <>
                <path d="M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11Z" />
                <path d="m9 12 2 2 4-4" />
            </>
        ),
        menu: <path d="M4 6h16M4 12h16M4 18h16" />,
        close: <path d="m18 6-12 12M6 6l12 12" />,
        arrowRight: <path d="M5 12h14m-7-7 7 7-7 7" />,
    };

    return (
        <svg
            width={size}
            height={size}
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
        >
            {paths[name] || paths.grid}
        </svg>
    );
}

const navigation = [
    { label: "Overview", icon: "grid", href: "#overview" },
    { label: "Interviews", icon: "calendar", href: "#live-workspace" },
    { label: "Availability", icon: "clock", href: "#live-workspace" },
    { label: "Insights", icon: "chart", href: "#live-workspace" },
];

function getGreeting() {
    const hour = new Date().getHours();

    if (hour < 12) return "Good morning";
    if (hour < 17) return "Good afternoon";

    return "Good evening";
}

export default function Dashboard() {
    const navigate = useNavigate();

    const email = localStorage.getItem("userEmail") || "User";
    const role = localStorage.getItem("userRole") || "";
    const userId = localStorage.getItem("userId");

    const [activeNav, setActiveNav] = useState("Overview");
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
    const [notificationOpen, setNotificationOpen] = useState(false);

    const firstName = email
        .split("@")[0]
        .replace(/[._-]+/g, " ")
        .replace(/\b\w/g, (char) => char.toUpperCase())
        .split(" ")[0];

    const roleLabel = role
        ? role.charAt(0) + role.slice(1).toLowerCase()
        : "User";

    const isCandidate = role === "CANDIDATE";
    const isInterviewer = role === "INTERVIEWER";
    const isRecruiter = role === "RECRUITER";

    function handleLogout() {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("userRole");
        localStorage.removeItem("userEmail");
        localStorage.removeItem("userId");

        navigate("/", { replace: true });
    }

    function handleNavigation(item) {
        setActiveNav(item.label);
        setMobileMenuOpen(false);

        if (item.label === "Overview") {
            window.scrollTo({ top: 0, behavior: "smooth" });
            return;
        }

        document.getElementById("live-workspace")?.scrollIntoView({
            behavior: "smooth",
            block: "start",
        });
    }

    return (
        <div className="app-shell">
            {mobileMenuOpen && (
                <button
                    className="sidebar-backdrop"
                    aria-label="Close navigation"
                    onClick={() => setMobileMenuOpen(false)}
                />
            )}

            {/* Sidebar */}
            <aside
                className={`sidebar ${
                    mobileMenuOpen ? "sidebar-open" : ""
                }`}
            >
                <a
                    className="brand"
                    href="#overview"
                    onClick={() => setActiveNav("Overview")}
                >
                    <span className="brand-mark">is</span>

                    <span className="brand-copy">
            <strong>interviewly</strong>
            <small>SMART SCHEDULING</small>
          </span>
                </a>

                <div className="workspace-switcher">
                    <div className="workspace-avatar">
                        <Icon name="briefcase" size={17} />
                    </div>

                    <div className="workspace-copy">
                        <span>Workspace</span>
                        <strong>Personal workspace</strong>
                    </div>

                    <Icon name="down" size={15} />
                </div>

                <p className="nav-caption">WORKSPACE</p>

                <nav className="side-nav" aria-label="Dashboard navigation">
                    {navigation.map((item) => (
                        <a
                            key={item.label}
                            className={`nav-link ${
                                activeNav === item.label ? "nav-link-active" : ""
                            }`}
                            href={item.href}
                            onClick={(event) => {
                                event.preventDefault();
                                handleNavigation(item);
                            }}
                        >
                            <Icon name={item.icon} size={18} />
                            <span>{item.label}</span>
                        </a>
                    ))}
                </nav>

                <p className="nav-caption tools-caption">ACCOUNT</p>

                <button
                    className="nav-link"
                    onClick={handleLogout}
                    style={{
                        width: "100%",
                        textAlign: "left",
                        cursor: "pointer",
                        fontFamily: "inherit",
                    }}
                >
                    <Icon name="logout" size={18} />
                    <span>Logout</span>
                </button>

                <div className="sidebar-spacer" />

                <div className="sidebar-help">
                    <div className="help-sparkle">
                        <Icon name="sparkle" size={19} />
                    </div>

                    <strong>Make every meeting count.</strong>

                    <p>
                        Your schedule, thoughtfully organized in one place.
                    </p>

                    <a
                        href="#live-workspace"
                        onClick={() => setActiveNav("Interviews")}
                    >
                        Explore schedule <Icon name="arrowRight" size={14} />
                    </a>
                </div>

                <div className="sidebar-user">
                    <div className="user-avatar">
                        {firstName.charAt(0) || "U"}
                    </div>

                    <div className="user-info">
                        <strong>{firstName}</strong>
                        <span>{roleLabel}</span>
                    </div>

                    <button
                        className="icon-button sidebar-logout"
                        aria-label="Logout"
                        title="Logout"
                        onClick={handleLogout}
                    >
                        <Icon name="logout" size={17} />
                    </button>
                </div>
            </aside>

            {/* Main application */}
            <div className="main-shell">
                <header className="topbar">
                    <div className="topbar-left">
                        <button
                            className="icon-button mobile-menu-button"
                            onClick={() => setMobileMenuOpen((open) => !open)}
                            aria-label="Toggle sidebar"
                        >
                            <Icon
                                name={mobileMenuOpen ? "close" : "menu"}
                                size={21}
                            />
                        </button>

                        <div className="breadcrumb">
                            <span>Workspace</span>
                            <Icon name="chevron" size={14} />
                            <strong>{activeNav}</strong>
                        </div>
                    </div>

                    <div className="topbar-actions">
                        <div className="notification-wrapper">
                            <button
                                className={`icon-button notification-button ${
                                    notificationOpen ? "icon-button-selected" : ""
                                }`}
                                aria-label="Notifications"
                                onClick={() =>
                                    setNotificationOpen((open) => !open)
                                }
                            >
                                <Icon name="bell" size={19} />
                                <span className="notification-dot" />
                            </button>

                            {notificationOpen && (
                                <div className="notification-popover">
                                    <div className="notification-popover-head">
                                        <strong>Notifications</strong>
                                        <span>Preview</span>
                                    </div>

                                    <div className="notification-item">
                    <span className="notification-icon blue-soft">
                      <Icon name="calendar" size={17} />
                    </span>

                                        <div>
                                            <strong>Interview schedule</strong>
                                            <p>
                                                Your latest bookings appear in your workspace below.
                                            </p>
                                        </div>
                                    </div>

                                    <div className="notification-item">
                    <span className="notification-icon violet-soft">
                      <Icon name="shield" size={17} />
                    </span>

                                        <div>
                                            <strong>Account status</strong>
                                            <p>
                                                Signed in as {roleLabel}. Keep your account secure.
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </div>

                        <button
                            className="top-profile"
                            onClick={handleLogout}
                            title="Click to log out"
                        >
              <span className="top-avatar">
                {firstName.charAt(0) || "U"}
              </span>

                            <span className="top-profile-copy">
                <strong>{firstName}</strong>
                <small>{roleLabel}</small>
              </span>

                            <Icon name="down" size={15} />
                        </button>
                    </div>
                </header>

                <main className="dashboard-main">
                    {/* Welcome section */}
                    <section className="welcome-row" id="overview">
                        <div>
                            <div className="overline">
                                <span className="overline-dot" />
                                YOUR SCHEDULING SPACE
                            </div>

                            <h1>
                                {getGreeting()}, {firstName}
                                <span className="wave"> ✦</span>
                            </h1>

                            <p className="welcome-description">
                                {isCandidate
                                    ? "Find interview times that work for you and keep track of every booking."
                                    : isInterviewer
                                        ? "Manage your availability and keep your interview schedule organized."
                                        : isRecruiter
                                            ? "Coordinate interviews and keep the hiring process moving."
                                            : "Manage your interviews and keep your schedule organized."}
                            </p>
                        </div>

                        <a
                            className="primary-button"
                            href="#live-workspace"
                            onClick={() => setActiveNav("Interviews")}
                        >
                            <Icon name="plus" size={18} />
                            {isCandidate ? "Find a time" : "Open workspace"}
                        </a>
                    </section>

                    {/* Premium hero banner */}
                    <section className="hero-card">
                        <div className="hero-gradient-orb hero-orb-one" />
                        <div className="hero-gradient-orb hero-orb-two" />

                        <div className="hero-copy">
              <span className="hero-eyebrow">
                <Icon name="sparkle" size={15} />
                YOUR WEEK, AT A GLANCE
              </span>

                            <h2>
                                Great things happen
                                <br />
                                when schedules <em>click.</em>
                            </h2>

                            <p>
                                Keep your interviews organized, your time protected,
                                and your next step in sight.
                            </p>

                            <a className="hero-button" href="#live-workspace">
                                Open interview workspace
                                <Icon name="arrowRight" size={17} />
                            </a>
                        </div>

                        <div className="hero-visual" aria-label="Scheduling illustration">
                            <div className="hero-orbit orbit-outer" />
                            <div className="hero-orbit orbit-inner" />

                            <div className="hero-center-icon">
                                <Icon name="calendar" size={32} />
                            </div>

                            <div className="floating-card floating-card-top">
                <span className="floating-check">
                  <Icon name="shield" size={14} />
                </span>

                                <span>
                  <strong>One organized workspace</strong>
                  <small>Interviews in one place</small>
                </span>
                            </div>

                            <div className="floating-card floating-card-bottom">
                                <div className="mini-avatars">
                                    <span>IN</span>
                                    <span>TV</span>
                                    <span>IS</span>
                                </div>

                                <span>
                  <strong>Better conversations</strong>
                  <small>Thoughtful scheduling</small>
                </span>
                            </div>
                        </div>

                        <div className="hero-bottom">
              <span>
                <Icon name="shield" size={15} />
                Role-aware workspace
              </span>

                            <span>
                <Icon name="clock" size={15} />
                Time-zone conscious scheduling
              </span>
                        </div>
                    </section>

                    {/* Account information */}
                    <section
                        className="stats-grid"
                        aria-label="Account information"
                    >
                        <article className="stat-card">
                            <div className="stat-card-top">
                <span className="stat-icon stat-blue">
                  <Icon name="users" size={20} />
                </span>
                                <span className="stat-change change-blue">Account</span>
                            </div>

                            <p className="stat-label">Your role</p>
                            <div className="stat-value-row">
                                <strong className="stat-value">
                                    {roleLabel}
                                </strong>
                            </div>
                            <p className="stat-note">
                                Your workspace is role-based
                            </p>
                        </article>

                        <article className="stat-card">
                            <div className="stat-card-top">
                <span className="stat-icon stat-green">
                  <Icon name="shield" size={20} />
                </span>
                                <span className="stat-change change-green">
                  Session
                </span>
                            </div>

                            <p className="stat-label">Authentication</p>
                            <div className="stat-value-row">
                                <strong className="stat-value">JWT</strong>
                            </div>
                            <p className="stat-note">
                                Signed-in application session
                            </p>
                        </article>

                        <article className="stat-card">
                            <div className="stat-card-top">
                <span className="stat-icon stat-violet">
                  <Icon name="calendar" size={20} />
                </span>
                                <span className="stat-change change-violet">
                  Booking
                </span>
                            </div>

                            <p className="stat-label">Scheduling</p>
                            <div className="stat-value-row">
                                <strong className="stat-value">Live API</strong>
                            </div>
                            <p className="stat-note">
                                Booking tools below use backend requests
                            </p>
                        </article>

                        <article className="stat-card">
                            <div className="stat-card-top">
                <span className="stat-icon stat-orange">
                  <Icon name="briefcase" size={20} />
                </span>
                                <span className="stat-change change-orange">
                  Profile
                </span>
                            </div>

                            <p className="stat-label">User ID</p>
                            <div className="stat-value-row">
                                <strong className="stat-value">
                                    {userId || "—"}
                                </strong>
                            </div>
                            <p className="stat-note">
                                Your logged-in database identity
                            </p>
                        </article>
                    </section>

                    {/* REAL BACKEND INTEGRATION */}
                    <InterviewWorkspace />

                    <footer className="dashboard-footer">
            <span>
              © {new Date().getFullYear()} Interviewly
            </span>

                        <span>
              <Icon name="shield" size={14} />
              Thoughtful interview scheduling
            </span>

                        <span className="footer-role">
              {roleLabel} workspace
            </span>
                    </footer>
                </main>
            </div>
        </div>
    );
}