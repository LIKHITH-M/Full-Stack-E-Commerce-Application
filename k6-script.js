import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 50,          // 50 concurrent virtual users (or override via CLI: k6 run --vus 100 k6-script.js)
    duration: '20s',  // Run test for 20 seconds
};

export function setup() {
    const username = `k6user_${Date.now()}`;
    const password = 'Password123!';
    const email = `${username}@example.com`;

    // 1. Register temporary user
    http.post('http://localhost:8081/api/auth/register', JSON.stringify({ username, password, email }), {
        headers: { 'Content-Type': 'application/json' },
    });

    // 2. Login to get JWT token
    let token = '';
    const loginRes = http.post('http://localhost:8081/api/auth/login', JSON.stringify({ username, password }), {
        headers: { 'Content-Type': 'application/json' },
    });

    if (loginRes.status === 200) {
        const body = JSON.parse(loginRes.body);
        token = body.token;
        console.log(`[k6 setup] Authentication successful for '${username}'`);
    } else {
        console.error(`[k6 setup] Login failed with HTTP ${loginRes.status}: ${loginRes.body}`);
    }

    // 3. Dynamically discover an active product ID
    let productId = 1;
    let price = 500;
    const productsRes = http.get('http://localhost:8081/api/products');
    if (productsRes.status === 200) {
        const products = JSON.parse(productsRes.body);
        if (products && products.length > 0) {
            productId = products[0].id;
            price = products[0].price || 500;
            console.log(`[k6 setup] Dynamically selected active product ID: ${productId} ('${products[0].name}') with price: ${price}`);
        } else {
            console.warn('[k6 setup] Warning: No products found in backend! Using fallback ID: 1');
        }
    }

    return { token, email, productId, price };
}

export default function (data) {
    const randomId = Math.random().toString(36).substring(2, 9);
    const payload = JSON.stringify({
        paymentId: `PAY_MANUAL_K6_${Date.now()}_${randomId}`,
        orderId: `K6-ORDER-${Date.now()}-${randomId}`,
        email: data.email,
        items: [{ productId: data.productId, quantity: 1 }]
    });

    const res = http.post('http://localhost:8081/api/orders/checkout', payload, {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${data.token}`
        },
    });

    const passed = check(res, { 'status is 200 or 201': (r) => r.status === 200 || r.status === 201 });
    if (!passed && Math.random() < 0.05) {
        console.error(`[k6 error sample] HTTP ${res.status}: ${res.body}`);
    }
}