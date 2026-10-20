import { useNavigate } from "react-router-dom";
import "./Dashboard.css";

export default function Dashboard() {
    const navigate = useNavigate();

    const email = localStorage.getItem("userEmail");
    const role = localStorage.getItem("userRole");

    function handleLogout() {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("userRole");
        localStorage.removeItem("userEmail");

        navigate("/", { replace: true });
    }

    return (
        <main className="dashboard-page">
            <header className="dashboard-header">
                <div className="dashboard-brand">
                    <span className="dashboard-logo">IS</span>
                    <span>Interview Scheduler</span>
                </div>

                <button className="logout-button" onClick={handleLogout}>
                    Logout
                </button>
            </header>

            <section className="dashboard-content">
                <p className="dashboard-eyebrow">YOUR WORKSPACE</p>

                <h1>Welcome back!</h1>

                <p className="dashboard-description">
                    Manage your interviews and keep your schedule organized.
                </p>

                <div className="profile-card">
                    <div>
                        <p className="profile-label">Logged in as</p>
                        <h2>{email || "User"}</h2>
                    </div>

                    <span className="role-badge">{role || "USER"}</span>
                </div>

                {role === "CANDIDATE" && (
                    <section className="role-section">
                        <h2>Candidate Dashboard</h2>
                        <p>
                            Your workspace for finding available interview slots,
                            scheduling interviews, and viewing your bookings.
                        </p>
                    </section>
                )}

                {role === "INTERVIEWER" && (
                    <section className="role-section">
                        <h2>Interviewer Dashboard</h2>
                        <p>
                            Your workspace for managing availability and reviewing
                            scheduled interviews.
                        </p>
                    </section>
                )}

                {role === "RECRUITER" && (
                    <section className="role-section">
                        <h2>Recruiter Dashboard</h2>
                        <p>
                            Your workspace for coordinating interview schedules.
                        </p>
                    </section>
                )}

                <div className="dashboard-note">
                    Next, we will connect these dashboard sections to your backend APIs.
                </div>
            </section>
        </main>
    );
}