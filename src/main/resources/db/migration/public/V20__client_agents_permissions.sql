INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM public.roles r
JOIN public.permissions p ON p.name IN (
    'user.agents.list',
    'user.agents.all',
    'user.agents.read'
)
WHERE r.name = 'CLIENT'
ON CONFLICT DO NOTHING;