import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { authApi } from "../api/interviewApi";
import "./Login.css";

export default function Login() {
    const navigate = useNavigate();
    const location = useLocation();

    const [form, setForm] = useState({
        email: location.state?.email || "",
        password: "",
    });

    const [error, setError] = useState("");
    const [success, setSuccess] = useState(
        location.state?.registered
            ? "Account created successfully. Please sign in."
            : ""
    );
    const [loading, setLoading] = useState(false);

    function handleChange(event) {
        const { name, value } = event.target;

        setForm((previous) => ({
            ...previous,
            [name]: value,
        }));
    }

    async function handleSubmit(event) {
        event.preventDefault();

        setError("");
        setSuccess("");
        setLoading(true);

        try {
            const response = await authApi.login({
                email: form.email.trim(),
                password: form.password,
            });

            const data = response.data;

            if (!data.accessToken) {
                setError("Login response did not contain an access token.");
                return;
            }

            localStorage.setItem("accessToken", data.accessToken);
            localStorage.setItem("userRole", data.role);
            localStorage.setItem("userEmail", data.email);

            if (data.userId != null) {
                localStorage.setItem("userId", String(data.userId));
            } else {
                localStorage.removeItem("userId");
            }

            navigate("/dashboard", { replace: true });
        } catch (err) {
            if (err.response?.status === 401) {
                setError("Invalid email or password.");
            } else if (err.response?.status === 400) {
                setError("Please check your email and password.");
            } else if (err.response?.status === 403) {
                setError("You are not permitted to perform this action.");
            } else if (!err.response) {
                setError(
                    "Cannot reach the server. Check that Spring Boot is running and CORS is configured."
                );
            } else {
                setError(
                    err.response.data?.message ||
                    err.response.data?.detail ||
                    "Login failed. Please try again."
                );
            }
        } finally {
            setLoading(false);
        }
    }

    return (
        <main className="login-page">
            <section className="login-card">
                <div className="login-brand">
                    <div className="brand-icon">IS</div>
                    <span>Interview Scheduler</span>
                </div>

                <div className="login-heading">
                    <p className="eyebrow">WELCOME BACK</p>
                    <h1>Sign in to your account</h1>
                    <p className="login-subtitle">
                        Manage your interviews, availability, and bookings.
                    </p>
                </div>

                {success && (
                    <div className="login-message success-message" role="status">
                        {success}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="login-form">
                    <div className="form-group">
                        <label htmlFor="login-email">Email address</label>
                        <input
                            id="login-email"
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
                        <label htmlFor="login-password">Password</label>
                        <input
                            id="login-password"
                            name="password"
                            type="password"
                            placeholder="Enter your password"
                            value={form.password}
                            onChange={handleChange}
                            autoComplete="current-password"
                            required
                        />
                    </div>

                    {error && (
                        <div className="login-message error-message" role="alert">
                            {error}
                        </div>
                    )}

                    <button
                        type="submit"
                        className="login-button"
                        disabled={loading}
                    >
                        {loading ? "Signing in..." : "Sign in"}
                    </button>
                </form>

                <p className="auth-switch">
                    Don't have an account?{" "}
                    <Link className="auth-link" to="/register">
                        Create account
                    </Link>
                </p>

                <p className="login-footer">
                    Interview Scheduling Engine · Secure access
                </p>
            </section>
        </main>
    );
}