import React, { useState, useEffect } from "react";
import API from "../../axios";

const AdminCategories = () => {
    const [categories, setCategories] = useState([]);
    const [newCategory, setNewCategory] = useState("");
    const [editId, setEditId] = useState(null);
    const [editName, setEditName] = useState("");
    const [message, setMessage] = useState("");

    const fetchCategories = async () => {
        try {
            const res = await API.get("/admin/categories");
            setCategories(res.data);
        } catch (err) {
            console.error("Error fetching categories:", err);
        }
    };

    useEffect(() => {
        fetchCategories();
    }, []);

    const handleAdd = async (e) => {
        e.preventDefault();
        try {
            await API.post("/admin/categories", { name: newCategory });
            setNewCategory("");
            setMessage("Category added successfully!");
            fetchCategories();
            setTimeout(() => setMessage(""), 3000);
        } catch (err) {
            setMessage("Failed to add category.");
        }
    };

    const handleUpdate = async (id) => {
        try {
            await API.put(`/admin/categories/${id}`, { name: editName });
            setEditId(null);
            setEditName("");
            setMessage("Category updated!");
            fetchCategories();
            setTimeout(() => setMessage(""), 3000);
        } catch (err) {
            setMessage("Failed to update category.");
        }
    };

    const handleDelete = async (id) => {
        if (window.confirm("Are you sure you want to delete this category?")) {
            try {
                await API.delete(`/admin/categories/${id}`);
                setMessage("Category deleted!");
                fetchCategories();
                setTimeout(() => setMessage(""), 3000);
            } catch (err) {
                setMessage("Failed to delete category.");
            }
        }
    };

    return (
        <div className="container mt-4">
            <h2><i className="bi bi-tags-fill me-2"></i>Manage Categories</h2>
            {message && <div className="alert alert-info">{message}</div>}

            <form onSubmit={handleAdd} className="mb-4">
                <div className="input-group">
                    <input
                        type="text"
                        className="form-control"
                        placeholder="New category name"
                        value={newCategory}
                        onChange={(e) => setNewCategory(e.target.value)}
                        required
                    />
                    <button className="btn btn-primary" type="submit">
                        <i className="bi bi-plus-lg me-1"></i>Add
                    </button>
                </div>
            </form>

            <table className="table table-striped table-hover">
                <thead className="table-dark">
                    <tr>
                        <th>ID</th>
                        <th>Name</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {categories.map((cat) => (
                        <tr key={cat.id}>
                            <td>{cat.id}</td>
                            <td>
                                {editId === cat.id ? (
                                    <input
                                        type="text"
                                        className="form-control form-control-sm"
                                        value={editName}
                                        onChange={(e) => setEditName(e.target.value)}
                                    />
                                ) : (
                                    cat.name
                                )}
                            </td>
                            <td>
                                {editId === cat.id ? (
                                    <>
                                        <button className="btn btn-success btn-sm me-2" onClick={() => handleUpdate(cat.id)}>
                                            <i className="bi bi-check-lg"></i> Save
                                        </button>
                                        <button className="btn btn-secondary btn-sm" onClick={() => setEditId(null)}>
                                            Cancel
                                        </button>
                                    </>
                                ) : (
                                    <>
                                        <button className="btn btn-warning btn-sm me-2" onClick={() => { setEditId(cat.id); setEditName(cat.name); }}>
                                            <i className="bi bi-pencil-fill"></i> Edit
                                        </button>
                                        <button className="btn btn-danger btn-sm" onClick={() => handleDelete(cat.id)}>
                                            <i className="bi bi-trash3-fill"></i> Delete
                                        </button>
                                    </>
                                )}
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
            {categories.length === 0 && <p className="text-muted">No categories found.</p>}
        </div>
    );
};

export default AdminCategories;
