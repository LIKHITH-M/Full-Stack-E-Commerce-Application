import React, { useState, useEffect } from "react";
import API from "../../axios";

const AdminOrders = () => {
    const [orders, setOrders] = useState([]);

    const fetchOrders = async () => {
        try {
            const res = await API.get("/orders/all");
            setOrders(res.data);
        } catch (err) {
            console.error("Error fetching orders:", err);
        }
    };

    useEffect(() => {
        fetchOrders();
        const interval = setInterval(fetchOrders, 5000); // Auto-refresh orders every 5s
        return () => clearInterval(interval);
    }, []);

    return (
        <div className="container mt-4">
            <div className="d-flex justify-content-between align-items-center mb-3">
                <h2><i className="bi bi-receipt me-2"></i>All Orders</h2>
                <button className="btn btn-outline-secondary" onClick={fetchOrders}>
                    <i className="bi bi-arrow-clockwise me-1"></i>Refresh
                </button>
            </div>

            <table className="table table-striped table-hover">
                <thead className="table-dark">
                    <tr>
                        <th>Order ID</th>
                        <th>User</th>
                        <th>Email</th>
                        <th>Total</th>
                        <th>Payment ID</th>
                        <th>Status</th>
                        <th>Date</th>
                    </tr>
                </thead>
                <tbody>
                    {orders.map((order) => (
                        <tr key={order.id}>
                            <td>{order.orderId}</td>
                            <td>{order.username}</td>
                            <td>{order.email || "N/A"}</td>
                            <td>₹{order.totalAmount}</td>
                            <td><code>{order.paymentId}</code></td>
                            <td>
                                <span className="badge bg-success">{order.status}</span>
                            </td>
                            <td>{new Date(order.createdAt).toLocaleDateString()}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
            {orders.length === 0 && <p className="text-muted">No orders yet.</p>}
        </div>
    );
};

export default AdminOrders;
