import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

const Login = () => {
    const [isRegister, setIsRegister] = useState(false);
    const [loginMode, setLoginMode] = useState("USER"); // "USER" or "ADMIN"
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [email, setEmail] = useState("");
    const [message, setMessage] = useState("");
    const [error, setError] = useState("");
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage("");
        setError("");

        try {
            if (isRegister) {
                const response = await axios.post(
                    "http://localhost:8081/api/auth/register",
                    { username, password, email }
                );
                setMessage("Registration successful! You can now login.");
                setIsRegister(false);
                setUsername("");
                setPassword("");
                setEmail("");
            } else {
                const response = await axios.post(
                    "http://localhost:8081/api/auth/login",
                    { username, password }
                );

                const role = response.data.role;

                // Validate role matches login mode
                if (loginMode === "ADMIN" && role !== "ADMIN") {
                    setError("This account does not have admin privileges.");
                    return;
                }
                if (loginMode === "USER" && role === "ADMIN") {
                    setError("Admin accounts must login via the Admin tab.");
                    return;
                }

                localStorage.setItem("token", response.data.token);
                localStorage.setItem("username", response.data.username);
                localStorage.setItem("email", response.data.email || "");
                localStorage.setItem("role", role);

                if (role === "ADMIN") {
                    navigate("/admin");
                } else {
                    navigate("/");
                }
                window.location.reload();
            }
        } catch (err) {
            setError(
                err.response?.data?.error || "Something went wrong. Please try again."
            );
        }
    };

    return (
        <div
            className="d-flex justify-content-center align-items-center"
            style={{
                minHeight: "100vh",
                background: "linear-gradient(135deg, #667eea 0%, #764ba2 100%)",
            }}
        >
            <div className="card shadow-lg" style={{ width: "420px", border: "none", borderRadius: "12px" }}>
                <div className="card-body p-4">
                    {/* Login Mode Tabs */}
                    {!isRegister && (
                        <div className="d-flex mb-4" style={{ borderRadius: "8px", overflow: "hidden", border: "2px solid #e0e0e0" }}>
                            <button
                                className={`btn flex-fill py-2 ${loginMode === "USER" ? "" : ""}`}
                                style={{
                                    backgroundColor: loginMode === "USER" ? "#0d6efd" : "#fff",
                                    color: loginMode === "USER" ? "#fff" : "#333",
                                    border: "none",
                                    borderRadius: "0",
                                    fontWeight: "600",
                                    fontSize: "0.95rem",
                                }}
                                onClick={() => { setLoginMode("USER"); setError(""); }}
                            >
                                <i className="bi bi-person-fill me-2" style={{ color: loginMode === "USER" ? "#fff" : "#333" }}></i>
                                User Login
                            </button>
                            <button
                                className={`btn flex-fill py-2`}
                                style={{
                                    backgroundColor: loginMode === "ADMIN" ? "#dc3545" : "#fff",
                                    color: loginMode === "ADMIN" ? "#fff" : "#333",
                                    border: "none",
                                    borderRadius: "0",
                                    fontWeight: "600",
                                    fontSize: "0.95rem",
                                }}
                                onClick={() => { setLoginMode("ADMIN"); setError(""); }}
                            >
                                <i className="bi bi-shield-lock-fill me-2" style={{ color: loginMode === "ADMIN" ? "#fff" : "#333" }}></i>
                                Admin Login
                            </button>
                        </div>
                    )}

                    <h3 className="card-title text-center mb-4" style={{ color: "#333" }}>
                        {isRegister ? "Create Account" : loginMode === "ADMIN" ? "Admin Login" : "User Login"}
                    </h3>

                    {message && (
                        <div className="alert alert-success" role="alert">
                            {message}
                        </div>
                    )}
                    {error && (
                        <div className="alert alert-danger" role="alert">
                            {error}
                        </div>
                    )}

                    <form onSubmit={handleSubmit}>
                        <div className="mb-3">
                            <label htmlFor="username" className="form-label" style={{ color: "#333" }}>
                                Username
                            </label>
                            <input
                                type="text"
                                className="form-control"
                                id="username"
                                value={username}
                                onChange={(e) => setUsername(e.target.value)}
                                required
                            />
                        </div>
                        {isRegister && (
                            <div className="mb-3">
                                <label htmlFor="email" className="form-label" style={{ color: "#333" }}>
                                    Email
                                </label>
                                <input
                                    type="email"
                                    className="form-control"
                                    id="email"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    required
                                />
                            </div>
                        )}
                        <div className="mb-3">
                            <label htmlFor="password" className="form-label" style={{ color: "#333" }}>
                                Password
                            </label>
                            <input
                                type="password"
                                className="form-control"
                                id="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                required
                            />
                        </div>
                        <button
                            type="submit"
                            className="btn w-100 mb-3"
                            style={{
                                backgroundColor: loginMode === "ADMIN" && !isRegister ? "#dc3545" : "#0d6efd",
                                color: "#fff",
                                fontWeight: "600",
                            }}
                        >
                            {isRegister ? "Register" : "Login"}
                        </button>
                    </form>

                    {loginMode === "USER" && (
                        <div className="text-center">
                            <span style={{ color: "#555" }}>
                                {isRegister
                                    ? "Already have an account? "
                                    : "Don't have an account? "}
                            </span>
                            <button
                                className="btn btn-link p-0"
                                style={{ color: "#0d6efd" }}
                                onClick={() => {
                                    setIsRegister(!isRegister);
                                    setMessage("");
                                    setError("");
                                }}
                            >
                                {isRegister ? "Login" : "Register"}
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default Login;
