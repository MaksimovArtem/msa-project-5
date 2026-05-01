CREATE DATABASE marketing OWNER admin;

\connect marketing;

CREATE SCHEMA IF NOT EXISTS marketing AUTHORIZATION admin;

CREATE TABLE IF NOT EXISTS marketing.orders (
    order_id INTEGER PRIMARY KEY,
    user_id INTEGER NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    customer_segment VARCHAR(20) NOT NULL
);

INSERT INTO marketing.orders (order_id, user_id, total_amount, payment_status, customer_segment)
VALUES
    (1001, 501, 124.90, 'paid', 'vip'),
    (1002, 502, 89.00, 'paid', 'regular'),
    (1003, 503, 59.90, 'pending', 'new'),
    (1004, 504, 210.50, 'paid', 'vip'),
    (1005, 505, 42.00, 'paid', 'regular'),
    (1006, 506, 319.99, 'failed', 'vip')
ON CONFLICT (order_id) DO NOTHING;
