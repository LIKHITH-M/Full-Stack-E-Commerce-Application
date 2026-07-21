import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import API from "../../axios";

const AdminProducts = () => {
    const [products, setProducts] = useState([]);
    const [message, setMessage] = useState("");

    const fetchProducts = async () => {
        try {
            const res = await API.get("/products");
            setProducts(res.data);
        } catch (err) {
            console.error("Error fetching products:", err);
        }
    };

    useEffect(() => {
        fetchProducts();
        const interval = setInterval(fetchProducts, 5000); // Auto-refresh stock counts every 5s
        return () => clearInterval(interval);
    }, []);

    const handleDelete = async (id) => {
        if (window.confirm("Are you sure you want to delete this product?")) {
            try {
                await API.delete(`/product/${id}`);
                setMessage("Product deleted!");
                fetchProducts();
                setTimeout(() => setMessage(""), 3000);
            } catch (err) {
                setMessage("Failed to delete product.");
            }
        }
    };

    const fallbackImage = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='50' height='50' fill='%236c757d' viewBox='0 0 16 16'><rect width='100%' height='100%' fill='%23e9ecef'/><text x='50%' y='50%' dominant-baseline='middle' text-anchor='middle' fill='%236c757d' font-size='7'>No Image</text></svg>";

    return (
        <div className="container mt-4">
            <div className="d-flex justify-content-between align-items-center mb-3">
                <h2><i className="bi bi-box-seam-fill me-2"></i>Manage Products</h2>
                <div>
                    <button className="btn btn-outline-secondary me-2" onClick={fetchProducts}>
                        <i className="bi bi-arrow-clockwise me-1"></i>Refresh
                    </button>
                    <Link to="/add_product" className="btn btn-primary">
                        <i className="bi bi-plus-lg me-1"></i>Add Product
                    </Link>
                </div>
            </div>
            {message && <div className="alert alert-info">{message}</div>}

            <table className="table table-striped table-hover">
                <thead className="table-dark">
                    <tr>
                        <th>ID</th>
                        <th>Image</th>
                        <th>Name</th>
                        <th>Brand</th>
                        <th>Category</th>
                        <th>Price</th>
                        <th>Stock</th>
                        <th>Available</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {products.map((product) => (
                        <tr key={product.id}>
                            <td>{product.id}</td>
                            <td>
                                <img
                                    src={`http://localhost:8081/api/product/${product.id}/image`}
                                    alt={product.name}
                                    style={{ width: "50px", height: "50px", objectFit: "cover", borderRadius: "5px" }}
                                    onError={(e) => {
                                        e.target.onerror = null;
                                        e.target.src = fallbackImage;
                                    }}
                                />
                            </td>
                            <td>{product.name}</td>
                            <td>{product.brand}</td>
                            <td>{product.category}</td>
                            <td>₹{product.price}</td>
                            <td>
                                <span className={`badge ${product.stockQuantity > 5 ? "bg-success" : product.stockQuantity > 0 ? "bg-warning" : "bg-danger"}`}>
                                    {product.stockQuantity}
                                </span>
                            </td>
                            <td>
                                {product.available ? (
                                    <span className="badge bg-success">Yes</span>
                                ) : (
                                    <span className="badge bg-danger">No</span>
                                )}
                            </td>
                            <td>
                                <Link to={`/product/update/${product.id}`} className="btn btn-warning btn-sm me-2">
                                    <i className="bi bi-pencil-fill"></i>
                                </Link>
                                <button className="btn btn-danger btn-sm" onClick={() => handleDelete(product.id)}>
                                    <i className="bi bi-trash3-fill"></i>
                                </button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
            {products.length === 0 && <p className="text-muted">No products found.</p>}
        </div>
    );
};

export default AdminProducts;
