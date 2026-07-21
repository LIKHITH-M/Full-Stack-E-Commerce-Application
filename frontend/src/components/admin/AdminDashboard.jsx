import React from "react";
import { NavLink, Outlet, Navigate, useNavigate } from "react-router-dom";

const AdminDashboard = () => {
    const role = localStorage.getItem("role");
    const username = localStorage.getItem("username");
    const navigate = useNavigate();

    if (role !== "ADMIN") {
        return <Navigate to="/login" replace />;
    }

    const handleLogout = () => {
        localStorage.removeItem("token");
        localStorage.removeItem("username");
        localStorage.removeItem("role");
        window.location.href = "/login";
    };

    return (
        <div className="admin-panel" style={{ minHeight: "100vh", backgroundColor: "#f4f6f9" }}>
            {/* Admin Top Navbar */}
            <nav className="navbar navbar-expand-lg" style={{
                backgroundColor: "#1a1a2e",
                padding: "10px 20px",
                position: "fixed",
                top: 0,
                left: 0,
                width: "100%",
                zIndex: 1000,
            }}>
                <a className="navbar-brand" href="/admin" style={{ color: "#e94560", fontWeight: "bold", fontSize: "1.4rem" }}>
                    <i className="bi bi-shield-lock-fill me-2" style={{ color: "#e94560" }}></i>
                    Admin Panel
                </a>
                <div className="ms-auto d-flex align-items-center">
                    <span style={{ color: "#ccc", marginRight: "15px" }}>
                        <i className="bi bi-person-circle me-1" style={{ color: "#ccc" }}></i>
                        {username}
                    </span>
                    <button
                        className="btn btn-outline-danger btn-sm"
                        onClick={handleLogout}
                    >
                        <i className="bi bi-box-arrow-right me-1"></i>Logout
                    </button>
                </div>
            </nav>

            <div className="d-flex" style={{ paddingTop: "60px" }}>
                {/* Sidebar */}
                <div style={{
                    width: "220px",
                    minHeight: "calc(100vh - 60px)",
                    backgroundColor: "#16213e",
                    padding: "20px 0",
                    position: "fixed",
                    top: "60px",
                    left: 0,
                }}>
                    <NavLink
                        to="/admin/categories"
                        className={({ isActive }) => isActive ? "admin-sidebar-link active" : "admin-sidebar-link"}
                        style={({ isActive }) => ({
                            display: "block",
                            padding: "12px 24px",
                            color: isActive ? "#e94560" : "#a8a8b3",
                            textDecoration: "none",
                            fontSize: "0.95rem",
                            fontWeight: isActive ? "600" : "400",
                            backgroundColor: isActive ? "rgba(233, 69, 96, 0.1)" : "transparent",
                            borderLeft: isActive ? "3px solid #e94560" : "3px solid transparent",
                            transition: "all 0.2s ease",
                        })}
                    >
                        <i className="bi bi-tags-fill me-2" style={{ color: "inherit" }}></i>Categories
                    </NavLink>
                    <NavLink
                        to="/admin/products"
                        className={({ isActive }) => isActive ? "admin-sidebar-link active" : "admin-sidebar-link"}
                        style={({ isActive }) => ({
                            display: "block",
                            padding: "12px 24px",
                            color: isActive ? "#e94560" : "#a8a8b3",
                            textDecoration: "none",
                            fontSize: "0.95rem",
                            fontWeight: isActive ? "600" : "400",
                            backgroundColor: isActive ? "rgba(233, 69, 96, 0.1)" : "transparent",
                            borderLeft: isActive ? "3px solid #e94560" : "3px solid transparent",
                            transition: "all 0.2s ease",
                        })}
                    >
                        <i className="bi bi-box-seam-fill me-2" style={{ color: "inherit" }}></i>Products
                    </NavLink>
                    <NavLink
                        to="/admin/users"
                        className={({ isActive }) => isActive ? "admin-sidebar-link active" : "admin-sidebar-link"}
                        style={({ isActive }) => ({
                            display: "block",
                            padding: "12px 24px",
                            color: isActive ? "#e94560" : "#a8a8b3",
                            textDecoration: "none",
                            fontSize: "0.95rem",
                            fontWeight: isActive ? "600" : "400",
                            backgroundColor: isActive ? "rgba(233, 69, 96, 0.1)" : "transparent",
                            borderLeft: isActive ? "3px solid #e94560" : "3px solid transparent",
                            transition: "all 0.2s ease",
                        })}
                    >
                        <i className="bi bi-people-fill me-2" style={{ color: "inherit" }}></i>Users
                    </NavLink>
                    <NavLink
                        to="/admin/orders"
                        className={({ isActive }) => isActive ? "admin-sidebar-link active" : "admin-sidebar-link"}
                        style={({ isActive }) => ({
                            display: "block",
                            padding: "12px 24px",
                            color: isActive ? "#e94560" : "#a8a8b3",
                            textDecoration: "none",
                            fontSize: "0.95rem",
                            fontWeight: isActive ? "600" : "400",
                            backgroundColor: isActive ? "rgba(233, 69, 96, 0.1)" : "transparent",
                            borderLeft: isActive ? "3px solid #e94560" : "3px solid transparent",
                            transition: "all 0.2s ease",
                        })}
                    >
                        <i className="bi bi-receipt me-2" style={{ color: "inherit" }}></i>Orders
                    </NavLink>
                </div>

                {/* Main Content */}
                <div style={{ marginLeft: "220px", padding: "30px", width: "calc(100% - 220px)" }}>
                    <Outlet />
                </div>
            </div>
        </div>
    );
};

export default AdminDashboard;
