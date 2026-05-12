INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
JOIN public.permissions p ON p.name = 'user.clients.read'
WHERE r.name = 'AGENT'
ON CONFLICT DO NOTHING;