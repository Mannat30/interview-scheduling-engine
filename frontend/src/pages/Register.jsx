import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { authApi } from "../api/interviewApi";
import "./Login.css";
import "./Registration.css";

export default function Register() {
    const navigate = useNavigate();

    const [form, setForm] = useState({
        fullName: "",
        email: "",
        password: "",
        confirmPassword: "",
        timeZone: "Asia/Kolkata",
        role: "CANDIDATE",
        interviewerInviteCode: "",
    });

    const [error, setError] = useState("");
    const [loading, setLoading] = useState(false);

    function handleChange(event) {
        const { name, value } = event.target;

        setForm((previous) => ({
            ...previous,
            [name]: value,
        }));

        setError("");
    }

    async function handleSubmit(event) {
        event.preventDefault();
        setError("");

        if (form.password !== form.confirmPassword) {
            setError("Passwords do not match.");
            return;
        }

        if (form.password.length < 8) {
            setError("Password must contain at least 8 characters.");
            return;
        }

        if (
            form.role === "INTERVIEWER" &&
            !form.interviewerInviteCode.trim()
        ) {
            setError("Enter your interviewer invitation code.");
            return;
        }

        setLoading(true);

        try {
            await authApi.register({
                fullName: form.fullName.trim(),
                email: form.email.trim(),
                password: form.password,
                timeZone: form.timeZone,
                role: form.role,
                interviewerInviteCode:
                    form.role === "INTERVIEWER"
                        ? form.interviewerInviteCode.trim()
                        : null,
            });

            navigate("/", {
                replace: true,
                state: {
                    registered: true,
                    email: form.email.trim(),
                },
            });
        } catch (err) {
            if (err.response?.status === 409) {
                setError(
                    "This email is already registered. Please sign in instead."
                );
            } else if (err.response?.status === 400) {
                setError(
                    err.response.data?.message ||
                    err.response.data?.error ||
                    "Registration details or interviewer invitation code are invalid."
                );
            } else if (!err.response) {
                setError(
                    "Cannot connect to the backend. Check Spring Boot and CORS."
                );
            } else {
                setError(
                    err.response.data?.message ||
                    err.response.data?.error ||
                    "Registration failed. Please try again."
                );
            }
        } finally {
            setLoading(false);
        }
    }

    return (
        <main className="login-page">
            <section className="login-card registration-card">
                <div className="login-brand">
                    <div className="brand-icon">IS</div>
                    <span>Interview Scheduler</span>
                </div>

                <div className="login-heading">
                    <p className="eyebrow">GET STARTED</p>

                    <h1>Create your account</h1>

                    <p className="login-subtitle">
                        Join Interviewly and organize your interview journey.
                    </p>
                </div>

                <form onSubmit={handleSubmit} className="login-form">
                    <div className="form-group">
                        <label htmlFor="register-fullName">Full name</label>

                        <input
                            id="register-fullName"
                            name="fullName"
                            type="text"
                            placeholder="Enter your full name"
                            value={form.fullName}
                            onChange={handleChange}
                            autoComplete="name"
                            minLength={2}
                            maxLength={100}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="register-email">Email address</label>

                        <input
                            id="register-email"
                            name="email"
                            type="email"
                            placeholder="you@example.com"
                            value={form.email}
                            onChange={handleChange}
                            autoComplete="email"
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="register-role">I want to join as</label>

                        <select
                            id="register-role"
                            name="role"
                            value={form.role}
                            onChange={handleChange}
                            required
                        >
                            <option value="CANDIDATE">Candidate</option>
                            <option value="INTERVIEWER">Interviewer</option>
                        </select>

                        <span className="field-hint">
              Interviewer registration requires a valid invitation code.
            </span>
                    </div>

                    {form.role === "INTERVIEWER" && (
                        <div className="form-group">
                            <label htmlFor="inviteCode">
                                Interviewer invitation code
                            </label>

                            <input
                                id="inviteCode"
                                name="interviewerInviteCode"
                                type="password"
                                placeholder="Enter your invitation code"
                                value={form.interviewerInviteCode}
                                onChange={handleChange}
                                autoComplete="off"
                                required
                            />
                        </div>
                    )}

                    <div className="form-group">
                        <label htmlFor="register-password">Password</label>

                        <input
                            id="register-password"
                            name="password"
                            type="password"
                            placeholder="Create a password"
                            value={form.password}
                            onChange={handleChange}
                            autoComplete="new-password"
                            minLength={8}
                            maxLength={72}
                            required
                        />

                        <span className="field-hint">
              Use at least 8 characters.
            </span>
                    </div>

                    <div className="form-group">
                        <label htmlFor="confirmPassword">Confirm password</label>

                        <input
                            id="confirmPassword"
                            name="confirmPassword"
                            type="password"
                            placeholder="Re-enter your password"
                            value={form.confirmPassword}
                            onChange={handleChange}
                            autoComplete="new-password"
                            minLength={8}
                            maxLength={72}
                            required
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="register-timeZone">Time zone</label>

                        <select
                            id="register-timeZone"
                            name="timeZone"
                            value={form.timeZone}
                            onChange={handleChange}
                            required
                        >
                            <option value="Asia/Kolkata">
                                India — Kolkata (IST)
                            </option>
                            <option value="UTC">UTC</option>
                            <option value="Europe/London">London</option>
                            <option value="America/New_York">New York</option>
                            <option value="Asia/Singapore">Singapore</option>
                            <option value="Australia/Sydney">Sydney</option>
                        </select>
                    </div>

                    {error && (
                        <div
                            className="login-message error-message"
                            role="alert"
                        >
                            {error}
                        </div>
                    )}

                    <button
                        type="submit"
                        className="login-button"
                        disabled={loading}
                    >
                        {loading ? "Creating account..." : "Create account"}
                    </button>
                </form>

                <p className="auth-switch">
                    Already have an account?{" "}
                    <Link className="auth-link" to="/">
                        Sign in
                    </Link>
                </p>

                <p className="login-footer">
                    Interview Scheduling Engine · Secure registration
                </p>
            </section>
        </main>
    );
}