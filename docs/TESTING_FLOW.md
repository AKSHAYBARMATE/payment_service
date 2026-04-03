# Razorpay Payment Service Testing Flow

## Option 1: Fastest test in browser

1. Start your Spring Boot application.
2. Open `http://localhost:8080/test-checkout.html`
3. Click `Create Order`.
4. Click `Pay Now`.
5. Complete payment with Razorpay test credentials.
6. The page automatically calls `/api/payment/verify`.
7. Click `Fetch User History` to confirm the DB row is updated.

## Option 2: Postman flow

1. Import `docs/razorpay-payment-service.postman_collection.json`
2. Run `Create Order`
3. Copy the returned `orderId`
4. Complete the payment from frontend or checkout page
5. Collect:
   - `razorpay_order_id`
   - `razorpay_payment_id`
   - `razorpay_signature`
6. Put those values into the Postman variables
7. Run `Verify Payment`
8. Run `Get Payment By Order ID` or `Get All Payments`

## Option 3: React flow

1. Add Razorpay checkout script to your `public/index.html` or root HTML:

```html
<script src="https://checkout.razorpay.com/v1/checkout.js"></script>
```

2. Copy `examples/react/RazorpayCheckoutDemo.jsx` into your React app.
3. Render the component.
4. Make sure your backend runs on `http://localhost:8080`
5. Use the component to create and verify a payment.

## Important note

`paymentId` and `razorpaySignature` are not created during order creation.
They only appear after the customer completes payment in Razorpay Checkout.
