// benchmark.js
const TOTAL_ORDERS = 10; // Change to 20, 50, or 100 to test larger loads
const BASE_URL = 'http://localhost:8081';

async function getJwtToken() {
    const username = `benchuser_${Date.now()}`;
    const password = 'Password123!';
    const email = `${username}@example.com`;

    // Step 1: Register temporary user
    try {
        await fetch(`${BASE_URL}/api/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, email })
        });
    } catch (e) {
        // Proceed to login if already exists
    }

    // Step 2: Login to acquire JWT token
    const res = await fetch(`${BASE_URL}/api/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    });

    if (!res.ok) {
        throw new Error(`Login failed with HTTP status ${res.status}`);
    }

    const data = await res.json();
    return { token: data.token, username, email };
}

async function runBenchmark() {
    console.log(`\n🔑 Authenticating with backend...`);
    let auth;
    try {
        auth = await getJwtToken();
        console.log(`✅ Authenticated as '${auth.username}'. Received JWT token.\n`);
    } catch (err) {
        console.error(`❌ Authentication Error: ${err.message}`);
        return;
    }

    console.log(`🚀 Starting Automated Benchmark for ${TOTAL_ORDERS} orders...\n`);
    const latencies = [];
    let failures = 0;

    for (let i = 1; i <= TOTAL_ORDERS; i++) {
        const payload = JSON.stringify({
            orderId: `AUTO-ORDER-${Date.now()}-${i}`,
            totalAmount: 999,
            email: auth.email,
            items: [{ productId: 1, productName: "Laptop", price: 999, quantity: 1 }]
        });

        const startTime = Date.now();
        try {
            const res = await fetch(`${BASE_URL}/api/orders/checkout`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${auth.token}`
                },
                body: payload
            });
            const duration = Date.now() - startTime;

            if (res.ok || res.status === 201) {
                latencies.push(duration);
                console.log(`  Order #${i}: ✅ Success in ${duration} ms (${(duration / 1000).toFixed(2)}s)`);
            } else {
                failures++;
                console.log(`  Order #${i}: ❌ Failed (Status: ${res.status}) in ${duration} ms`);
            }
        } catch (err) {
            const duration = Date.now() - startTime;
            failures++;
            console.log(`  Order #${i}: ❌ Connection Error (${err.message}) in ${duration} ms`);
        }
    }

  const sum = latencies.reduce((a, b) => a + b, 0);
  const avg = latencies.length ? (sum / latencies.length).toFixed(2) : 0;
  const min = latencies.length ? Math.min(...latencies) : 0;
  const max = latencies.length ? Math.max(...latencies) : 0;

  console.log('\n==================================================');
  console.log('📊 BENCHMARK RESULTS SUMMARY');
  console.log('==================================================');
  console.log(`Total Orders Sent       : ${TOTAL_ORDERS}`);
  console.log(`Successful Orders       : ${latencies.length}`);
  console.log(`Failed Orders           : ${failures}`);
  console.log(`Average Processing Time : ${avg} ms (${(avg / 1000).toFixed(2)} sec)`);
  console.log(`Fastest Processing Time : ${min} ms`);
  console.log(`Slowest Processing Time : ${max} ms`);
  console.log('==================================================\n');
}

runBenchmark();

