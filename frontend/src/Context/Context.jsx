import axios from "../axios";
import { useState, useEffect, createContext } from "react";

const AppContext = createContext({
  data: [],
  isError: "",
  cart: [],
  addToCart: (product) => {},
  removeFromCart: (productId) => {},
  refreshData: () => {},
  clearCart: () => {},
  fetchCart: () => {},
});

export const AppProvider = ({ children }) => {
  const [data, setData] = useState([]);
  const [isError, setIsError] = useState("");
  const [cart, setCart] = useState([]);

  // Fetch cart from backend DB
  const fetchCart = async () => {
    try {
      const token = localStorage.getItem("token");
      if (!token) {
        setCart([]);
        return;
      }
      const response = await axios.get("/cart");
      setCart(response.data);
    } catch (error) {
      console.error("Error fetching cart:", error);
    }
  };

  // Add a product to cart (backend DB)
  const addToCart = async (product) => {
    try {
      await axios.post("/cart", {
        productId: product.id,
        productName: product.name,
        brand: product.brand,
        price: product.price,
        quantity: 1,
        stockQuantity: product.stockQuantity,
      });
      // Refresh cart from DB
      await fetchCart();
    } catch (error) {
      console.error("Error adding to cart:", error);
    }
  };

  // Remove a product from cart (backend DB)
  const removeFromCart = async (productId) => {
    try {
      await axios.delete(`/cart/${productId}`);
      await fetchCart();
    } catch (error) {
      console.error("Error removing from cart:", error);
    }
  };

  // Clear entire cart (backend DB)
  const clearCart = async () => {
    try {
      await axios.delete("/cart");
      setCart([]);
    } catch (error) {
      console.error("Error clearing cart:", error);
      setCart([]);
    }
    // Also clean up any leftover localStorage cart
    localStorage.removeItem("cart");
  };

  const refreshData = async () => {
    try {
      const response = await axios.get("/products");
      setData(response.data);
    } catch (error) {
      setIsError(error.message);
    }
  };

  useEffect(() => {
    refreshData();
    fetchCart();
  }, []);

  return (
    <AppContext.Provider value={{ data, isError, cart, addToCart, removeFromCart, refreshData, clearCart, fetchCart }}>
      {children}
    </AppContext.Provider>
  );
};

export default AppContext;