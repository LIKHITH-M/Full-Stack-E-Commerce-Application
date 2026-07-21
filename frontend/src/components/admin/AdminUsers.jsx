import React, { useState, useEffect } from "react";
import API from "../../axios";

const AdminUsers = () => {
    const [users, setUsers] = useState([]);
    const [message, setMessage] = useState("");

    const fetchUsers = async () => {
        try {
            const res = await API.get("/admin/users");
            setUsers(res.data);
        } catch (err) {
            console.error("Error fetching users:", err);
        }
    };

    useEffect(() => {
        fetchUsers();
    }, []);

    const handleDelete = async (id) => {
        if (window.confirm("Are you sure you want to delete this user?")) {
            try {
                await API.delete(`/admin/users/${id}`);
                setMessage("User deleted successfully!");
                fetchUsers();
                setTimeout(() => setMessage(""), 3000);
            } catch (err) {
                setMessage("Failed to delete user.");
            }
        }
    };

    return (
        <div className="container mt-4">
            <h2><i className="bi bi-people-fill me-2"></i>Manage Users</h2>
            {message && <div className="alert alert-info">{message}</div>}

            <table className="table table-striped table-hover">
                <thead className="table-dark">
                    <tr>
                        <th>ID</th>
                        <th>Username</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {users.map((user) => (
                        <tr key={user.id}>
                            <td>{user.id}</td>
                            <td>{user.username}</td>
                            <td>{user.email || "N/A"}</td>
                            <td>
                                <span className={`badge ${user.role === "ADMIN" ? "bg-danger" : "bg-primary"}`}>
                                    {user.role}
                                </span>
                            </td>
                            <td>
                                <button
                                    className="btn btn-danger btn-sm"
                                    onClick={() => handleDelete(user.id)}
                                    disabled={user.role === "ADMIN"}
                                >
                                    <i className="bi bi-trash3-fill"></i> Delete
                                </button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
            {users.length === 0 && <p className="text-muted">No users found.</p>}
        </div>
    );
};

export default AdminUsers;
