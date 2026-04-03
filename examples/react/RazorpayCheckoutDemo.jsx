import { useState } from "react";

const API_BASE_URL = "http://localhost:8080";

function RazorpayCheckoutDemo() {
  const [form, setForm] = useState({
    userId: "user_1001",
    amount: "1499.00",
    currency: "INR",
    description: "Premium subscription payment",
    metadata: "{\"plan\":\"premium-monthly\",\"source\":\"react\",\"customerEmail\":\"rahul@example.com\"}",
    paymentMethod: "card"
  });
  const [orderData, setOrderData] = useState(null);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(false);

  const updateField = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const createOrder = async () => {
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/payment/create-order`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          userId: form.userId,
          amount: Number(form.amount),
          currency: form.currency,
          description: form.description,
          metadata: form.metadata
        })
      });

      const data = await response.json();
      setResult(data);

      if (!response.ok || !data.data) {
        throw new Error(data.message || "Unable to create order");
      }

      setOrderData(data.data);
    } finally {
      setLoading(false);
    }
  };

  const verifyPayment = async (response) => {
    const verifyResponse = await fetch(`${API_BASE_URL}/api/payment/verify`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        orderId: response.razorpay_order_id,
        paymentId: response.razorpay_payment_id,
        razorpaySignature: response.razorpay_signature,
        paymentMethod: form.paymentMethod
      })
    });

    const verifyData = await verifyResponse.json();
    setResult(verifyData);

    if (!verifyResponse.ok) {
      throw new Error(verifyData.message || "Payment verification failed");
    }
  };

  const openCheckout = () => {
    if (!orderData) {
      return;
    }

    const options = {
      key: orderData.razorpayKey,
      amount: Math.round(Number(orderData.amount) * 100),
      currency: orderData.currency,
      name: "Payment Service Demo",
      description: orderData.description,
      order_id: orderData.orderId,
      handler: verifyPayment,
      theme: {
        color: "#8a4fff"
      }
    };

    const razorpay = new window.Razorpay(options);
    razorpay.open();
  };

  return (
    <div style={{ maxWidth: 720, margin: "40px auto", fontFamily: "sans-serif" }}>
      <h2>Razorpay Checkout Demo</h2>
      <div style={{ display: "grid", gap: 12 }}>
        <input name="userId" value={form.userId} onChange={updateField} placeholder="User ID" />
        <input name="amount" value={form.amount} onChange={updateField} placeholder="Amount" />
        <input name="currency" value={form.currency} onChange={updateField} placeholder="Currency" />
        <input name="description" value={form.description} onChange={updateField} placeholder="Description" />
        <input name="paymentMethod" value={form.paymentMethod} onChange={updateField} placeholder="Payment method" />
        <textarea name="metadata" value={form.metadata} onChange={updateField} placeholder="Metadata JSON string" rows={5} />
      </div>

      <div style={{ display: "flex", gap: 12, marginTop: 16 }}>
        <button onClick={createOrder} disabled={loading}>
          {loading ? "Creating..." : "Create Order"}
        </button>
        <button onClick={openCheckout} disabled={!orderData}>
          Pay Now
        </button>
      </div>

      <pre style={{ marginTop: 20, background: "#111", color: "#fff", padding: 16, borderRadius: 8 }}>
        {JSON.stringify(result, null, 2)}
      </pre>
    </div>
  );
}

export default RazorpayCheckoutDemo;
