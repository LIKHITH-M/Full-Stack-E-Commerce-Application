import React, { useContext, useState, useEffect } from "react";
import AppContext from "../Context/Context";
import axios from "axios";
import API from "../axios";
import CheckoutPopup from "./CheckoutPopup";
import { Button } from 'react-bootstrap';

const Cart = () => {
  const { cart, removeFromCart, clearCart } = useContext(AppContext);
  const [cartItems, setCartItems] = useState([]);
  const [totalPrice, setTotalPrice] = useState(0);
  const [cartImage, setCartImage] = useState([]);
  const [showModal, setShowModal] = useState(false);
  const [paymentProcessing, setPaymentProcessing] = useState(false);

  useEffect(() => {
    const fetchImagesAndUpdateCart = async () => {
      console.log("Cart", cart);
      try {
        const response = await axios.get("http://localhost:8081/api/products", {
          headers: { Authorization: `Bearer ${localStorage.getItem("token")}` }
        });
        const backendProductIds = response.data.map((product) => product.id);

        const updatedCartItems = cart.filter((item) => backendProductIds.includes(item.id));
        const cartItemsWithImages = await Promise.all(
          updatedCartItems.map(async (item) => {
            try {
              const response = await axios.get(
                `http://localhost:8081/api/product/${item.id}/image`,
                { responseType: "blob", headers: { Authorization: `Bearer ${localStorage.getItem("token")}` } }
              );
              const imageFile = await converUrlToFile(response.data, response.data.imageName);
              setCartImage(imageFile)
              const imageUrl = URL.createObjectURL(response.data);
              return { ...item, imageUrl };
            } catch (error) {
              console.error("Error fetching image:", error);
              return { ...item, imageUrl: "placeholder-image-url" };
            }
          })
        );
        console.log("cart", cart)
        setCartItems(cartItemsWithImages);
      } catch (error) {
        console.error("Error fetching product data:", error);
      }
    };

    if (cart.length) {
      fetchImagesAndUpdateCart();
    }
  }, [cart]);

  useEffect(() => {
    const total = cartItems.reduce(
      (acc, item) => acc + item.price * item.quantity,
      0
    );
    setTotalPrice(total);
  }, [cartItems]);

  const converUrlToFile = async (blobData, fileName) => {
    const file = new File([blobData], fileName, { type: blobData.type });
    return file;
  }

  const handleIncreaseQuantity = (itemId) => {
    const newCartItems = cartItems.map((item) => {
      if (item.id === itemId) {
        if (item.quantity < item.stockQuantity) {
          return { ...item, quantity: item.quantity + 1 };
        } else {
          alert("Cannot add more than available stock");
        }
      }
      return item;
    });
    setCartItems(newCartItems);
  };


  const handleDecreaseQuantity = (itemId) => {
    const newCartItems = cartItems.map((item) =>
      item.id === itemId
        ? { ...item, quantity: Math.max(item.quantity - 1, 1) }
        : item
    );
    setCartItems(newCartItems);
  };

  const handleRemoveFromCart = (itemId) => {
    removeFromCart(itemId);
    const newCartItems = cartItems.filter((item) => item.id !== itemId);
    setCartItems(newCartItems);
  };

  // Load Razorpay script dynamically
  const loadRazorpayScript = () => {
    return new Promise((resolve) => {
      if (document.querySelector('script[src="https://checkout.razorpay.com/v1/checkout.js"]')) {
        resolve(true);
        return;
      }
      const script = document.createElement("script");
      script.src = "https://checkout.razorpay.com/v1/checkout.js";
      script.onload = () => resolve(true);
      script.onerror = () => resolve(false);
      document.body.appendChild(script);
    });
  };

  // Razorpay payment flow
  const handlePayWithRazorpay = async () => {
    setPaymentProcessing(true);
    try {
      const scriptLoaded = await loadRazorpayScript();
      if (!scriptLoaded) {
        alert("Failed to load Razorpay. Check your internet connection.");
        setPaymentProcessing(false);
        return;
      }

      // Create Razorpay order on backend
      const orderResponse = await API.post(
        `/payments/create-order?amount=${Math.round(totalPrice)}&currency=INR&receiptId=receipt_${Date.now()}`
      );

      const orderData = JSON.parse(orderResponse.data.order);
      const razorpayKeyId = orderResponse.data.razorpayKeyId;

      const options = {
        key: razorpayKeyId,
        amount: orderData.amount,
        currency: orderData.currency,
        name: "WebCart E-Commerce",
        description: "Order Payment",
        order_id: orderData.id,
        handler: async function (response) {
          // Payment successful - finalize order via backend
          try {
            const orderItems = cartItems.map((item) => ({
              productId: item.id,
              productName: item.name,
              quantity: item.quantity,
              price: item.price,
            }));

            await API.post("/orders/checkout", {
              paymentId: response.razorpay_payment_id,
              orderId: response.razorpay_order_id,
              email: localStorage.getItem("email") || "",
              totalAmount: totalPrice,
              items: orderItems,
            });

            alert("Payment successful! Order placed.");
            clearCart();
            setCartItems([]);
            setShowModal(false);
          } catch (err) {
            console.error("Error finalizing order:", err);
            alert("Payment received but order processing failed. Contact support.");
          }
        },
        prefill: {
          name: localStorage.getItem("username") || "",
          email: localStorage.getItem("email") || "",
        },
        theme: {
          color: "#3399cc",
        },
        modal: {
          ondismiss: function () {
            setPaymentProcessing(false);
          },
        },
      };

      const rzp = new window.Razorpay(options);
      rzp.open();
    } catch (error) {
      console.error("Error creating Razorpay order:", error);
      alert("Failed to initiate payment. Please try again.");
    }
    setPaymentProcessing(false);
  };

  // Manual Checkout flow (places order, saves to DB, publishes Kafka event for stock & email)
  const handleCheckout = async () => {
    if (!cartItems || cartItems.length === 0) return;
    try {
      const orderItems = cartItems.map((item) => ({
        productId: item.id,
        productName: item.name,
        quantity: item.quantity,
        price: item.price,
      }));

      const manualOrderId = `ORDER_MANUAL_${Date.now()}`;
      const manualPaymentId = `PAY_MANUAL_${Date.now()}`;

      await API.post("/orders/checkout", {
        paymentId: manualPaymentId,
        orderId: manualOrderId,
        email: localStorage.getItem("email") || "",
        totalAmount: totalPrice,
        items: orderItems,
      });

      setShowModal(false);
      alert("Order placed successfully!");
      clearCart();
      setCartItems([]);
      refreshData();
    } catch (error) {
      console.error("Error during manual checkout:", error);
      alert("Checkout failed. Please try again.");
    }
  };

  return (
    <div className="cart-container">
      <div className="shopping-cart">
        <div className="title">Shopping Bag</div>
        {cartItems.length === 0 ? (
          <div className="empty" style={{ textAlign: "left", padding: "2rem" }}>
            <h4>Your cart is empty</h4>
          </div>
        ) : (
          <>
            {cartItems.map((item) => (
              <li key={item.id} className="cart-item">
                <div
                  className="item"
                  style={{ display: "flex", alignContent: "center" }}
                  key={item.id}
                >

                  <div>
                    <img
                      src={item.imageUrl}
                      alt={item.name}
                      className="cart-item-image"
                    />
                  </div>
                  <div className="description">
                    <span>{item.brand}</span>
                    <span>{item.name}</span>
                  </div>

                  <div className="quantity">
                    <button
                      className="plus-btn"
                      type="button"
                      name="button"
                      onClick={() => handleIncreaseQuantity(item.id)}
                    >
                      <i className="bi bi-plus-square-fill"></i>
                    </button>
                    <input
                      type="button"
                      name="name"
                      value={item.quantity}
                      readOnly
                    />
                    <button
                      className="minus-btn"
                      type="button"
                      name="button"
                      onClick={() => handleDecreaseQuantity(item.id)}
                    >
                      <i className="bi bi-dash-square-fill"></i>
                    </button>
                  </div>

                  <div className="total-price " style={{ textAlign: "center" }}>
                    ₹{item.price * item.quantity}
                  </div>
                  <button
                    className="remove-btn"
                    onClick={() => handleRemoveFromCart(item.id)}
                  >
                    <i className="bi bi-trash3-fill"></i>
                  </button>
                </div>
              </li>
            ))}
            <div className="total">Grand Total: ₹{totalPrice}</div>
            <div className="d-flex gap-2 mt-2">
              <Button
                className="btn btn-success flex-grow-1"
                onClick={handlePayWithRazorpay}
                disabled={paymentProcessing}
              >
                <i className="bi bi-credit-card-fill me-2"></i>
                {paymentProcessing ? "Processing..." : "Pay with Razorpay"}
              </Button>
              <Button
                className="btn btn-primary flex-grow-1"
                onClick={() => setShowModal(true)}
              >
                <i className="bi bi-bag-check-fill me-2"></i>
                Checkout (Manual)
              </Button>
            </div>
          </>
        )}
      </div>
      <CheckoutPopup
        show={showModal}
        handleClose={() => setShowModal(false)}
        cartItems={cartItems}
        totalPrice={totalPrice}
        handleCheckout={handleCheckout}
      />
    </div>

  );
};

export default Cart;
