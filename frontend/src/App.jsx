import "./App.css";
import React, { useState } from "react";
import Home from "./components/Home";
import Navbar from "./components/Navbar";
import Cart from "./components/Cart";
import AddProduct from "./components/AddProduct";
import Product from "./components/Product";
import Login from "./components/Login";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AppProvider } from "./Context/Context";
import UpdateProduct from "./components/UpdateProduct";
import AdminDashboard from "./components/admin/AdminDashboard";
import AdminCategories from "./components/admin/AdminCategories";
import AdminProducts from "./components/admin/AdminProducts";
import AdminUsers from "./components/admin/AdminUsers";
import AdminOrders from "./components/admin/AdminOrders";
import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap/dist/js/bootstrap.bundle.min.js";

const ProtectedRoute = ({ children }) => {
  const token = localStorage.getItem("token");
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return children;
};

const UserRoute = ({ children }) => {
  const token = localStorage.getItem("token");
  const role = localStorage.getItem("role");
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  if (role === "ADMIN") {
    return <Navigate to="/admin" replace />;
  }
  return children;
};

const AdminRoute = ({ children }) => {
  const token = localStorage.getItem("token");
  const role = localStorage.getItem("role");
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  if (role !== "ADMIN") {
    return <Navigate to="/" replace />;
  }
  return children;
};

function App() {
  const [cart, setCart] = useState([]);
  const [selectedCategory, setSelectedCategory] = useState("");

  const isLoggedIn = !!localStorage.getItem("token");
  const role = localStorage.getItem("role");
  const isUser = isLoggedIn && role !== "ADMIN";

  const handleCategorySelect = (category) => {
    setSelectedCategory(category);
  };

  const addToCart = (product) => {
    const existingProduct = cart.find((item) => item.id === product.id);
    if (existingProduct) {
      setCart(
        cart.map((item) =>
          item.id === product.id
            ? { ...item, quantity: item.quantity + 1 }
            : item
        )
      );
    } else {
      setCart([...cart, { ...product, quantity: 1 }]);
    }
  };

  return (
    <AppProvider>
      <BrowserRouter>
        {/* Only show user navbar for logged-in USER role */}
        {isUser && <Navbar onSelectCategory={handleCategorySelect} />}

        <Routes>
          {/* Public */}
          <Route path="/login" element={<Login />} />

          {/* User Routes */}
          <Route
            path="/"
            element={
              <UserRoute>
                <Home addToCart={addToCart} selectedCategory={selectedCategory} />
              </UserRoute>
            }
          />
          <Route path="/product" element={<UserRoute><Product /></UserRoute>} />
          <Route path="product/:id" element={<UserRoute><Product /></UserRoute>} />
          <Route path="/cart" element={<UserRoute><Cart /></UserRoute>} />

          {/* Admin Routes - completely separate layout */}
          <Route
            path="/admin"
            element={
              <AdminRoute>
                <AdminDashboard />
              </AdminRoute>
            }
          >
            <Route index element={<Navigate to="categories" replace />} />
            <Route path="categories" element={<AdminCategories />} />
            <Route path="products" element={<AdminProducts />} />
            <Route path="users" element={<AdminUsers />} />
            <Route path="orders" element={<AdminOrders />} />
          </Route>

          {/* Admin-only standalone pages */}
          <Route path="/add_product" element={<AdminRoute><AddProduct /></AdminRoute>} />
          <Route path="/product/update/:id" element={<AdminRoute><UpdateProduct /></AdminRoute>} />
        </Routes>
      </BrowserRouter>
    </AppProvider>
  );
}

export default App;
