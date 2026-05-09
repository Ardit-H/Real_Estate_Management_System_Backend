INSERT INTO public.permissions (name, http_method, api_path, description)
VALUES ('sale.payment.pay', 'PATCH', '/api/sales/payments/*/pay', 'Mark sale payment as paid')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
JOIN public.permissions p ON p.name = 'sale.payment.pay'
WHERE r.name = 'AGENT'
ON CONFLICT DO NOTHING;