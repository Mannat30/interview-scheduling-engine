import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../api/axios";
import "./Login.css";

export default function Login() {
    const navigate = useNavigate();

    const [form, setForm] = useState({
        email: "",
        password: "",
    });

    const [error, setError] = useState("");
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
        setLoading(true);

        try {
            const response = await api.post("/auth/login", {
                email: form.email.trim(),
                password: form.password,
            });

            const data = response.data;

            if (!data.accessToken) {
                throw new Error("Login response did not contain an access token.");
            }

            // Save authentication details only once.
            localStorage.setItem("accessToken", data.accessToken);
            localStorage.setItem("userRole", data.role);
            localStorage.setItem("userEmail", data.email);

            // Redirect after successful login.
            navigate("/dashboard", { replace: true });
        } catch (err) {
            if (err.response?.status === 401) {
                setError("Invalid email or password.");
            } else if (err.response?.status === 403) {
                setError("You are not permitted to perform this action.");
            } else if (!err.response) {
                setError(
                    "Cannot connect to the server. Check the backend and CORS configuration."
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

                <form onSubmit={handleSubmit} className="login-form">
                    <div className="form-group">
                        <label htmlFor="email">Email address</label>

                        <input
                            id="email"
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
                        <label htmlFor="password">Password</label>

                        <input
                            id="password"
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

                <p className="login-footer">
                    Interview Scheduling Engine · Secure access
                </p>
            </section>
        </main>
    );
}