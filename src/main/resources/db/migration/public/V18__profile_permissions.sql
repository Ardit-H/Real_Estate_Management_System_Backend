INSERT INTO public.permissions (name, http_method, api_path, description)
VALUES
    ('user.agents.read',  'GET', '/api/users/agents/*',  'Get agent profile'),
    ('user.clients.read', 'GET', '/api/users/clients/*', 'Get client profile')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM public.roles r, public.permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('user.agents.read', 'user.clients.read')
ON CONFLICT DO NOTHING;