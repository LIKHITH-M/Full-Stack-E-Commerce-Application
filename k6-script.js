import http from 'k6/http';
import { check } from 'k6';

export const options = {
    vus: 10,          // 10 concurrent virtual users
    duration: '10s',  // Run test for 10 seconds
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
    const res = http.post('http://localhost:8081/api/auth/login', JSON.stringify({ username, password }), {
        headers: { 'Content-Type': 'application/json' },
    });

    if (res.status === 200) {
        const body = JSON.parse(res.body);
        return { token: body.token, email };
    }
    return { token: '', email };
}

export default function (data) {
    const payload = JSON.stringify({
        orderId: `K6-ORDER-${Math.random()}`,
        totalAmount: 500,
        email: data.email,
        items: [{ productId: 1, productName: 'Item', price: 500, quantity: 1 }]
    });

    const res = http.post('http://localhost:8081/api/orders/checkout', payload, {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${data.token}`
        },
    });

    check(res, { 'status is 200 or 201': (r) => r.status === 200 || r.status === 201 });
}