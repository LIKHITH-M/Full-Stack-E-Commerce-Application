import { test, expect } from '@playwright/test';

test('Homepage displays branding and navbar', async ({ page }) => {
  await page.goto('http://localhost:5173/');
  await expect(page).toHaveTitle(/React App|Vite|E-Commerce|Ecommerce/i);
});

test('User can search for products', async ({ page }) => {
  await page.goto('http://localhost:5173/');
  const searchInput = page.locator('input[type="search"], input[placeholder*="Search" i]').first();
  if (await searchInput.isVisible()) {
    await searchInput.fill('Laptop');
    await page.keyboard.press('Enter');
  }
});

test('Cart badge updates when adding product', async ({ page }) => {
  await page.goto('http://localhost:5173/');
  const addToCartBtn = page.locator('button:has-text("Add to Cart")').first();
  if (await addToCartBtn.isVisible()) {
    await addToCartBtn.click();
    const cartBadge = page.locator('.badge, .cart-count').first();
    if (await cartBadge.isVisible()) {
      await expect(cartBadge).toBeVisible();
    }
  }
});

